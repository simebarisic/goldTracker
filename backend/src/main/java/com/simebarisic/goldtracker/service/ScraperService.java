package com.simebarisic.goldtracker.service;

import com.simebarisic.goldtracker.dto.ChangeDto;
import com.simebarisic.goldtracker.dto.RunResult;
import com.simebarisic.goldtracker.dto.ScrapedProduct;
import com.simebarisic.goldtracker.model.RunLog;
import com.simebarisic.goldtracker.model.TrackedPage;
import com.simebarisic.goldtracker.repository.RunLogRepository;
import com.simebarisic.goldtracker.repository.TrackedPageRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ScraperService {

    private static final Logger log = LoggerFactory.getLogger(ScraperService.class);

    // NAPOMENA: u originalnoj JS verziji regex je bio "[\d.,]+(?=\s*€|\s*EUR)?" -
    // vanjski "?" na lookaheadu ga cini bespredmetnim (lookahead ionako uvijek
    // "prolazi" jer je opcionalan), pa regex u praksi samo hvata bilo koji niz
    // znamenki/tocaka/zareza. Ovdje je to isto, bez nepotrebne "phantom" grupe.
    private static final Pattern PRICE_PATTERN = Pattern.compile("[\\d.,]+");

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36";

    private final TrackedPageRepository trackedPageRepository;
    private final RunLogRepository runLogRepository;
    private final ScraperPersistenceService persistenceService;
    private final NotifierService notifierService;
    private final HttpClient httpClient;

    private final long requestDelayMs;

    public ScraperService(
            TrackedPageRepository trackedPageRepository,
            RunLogRepository runLogRepository,
            ScraperPersistenceService persistenceService,
            NotifierService notifierService,
            @Value("${app.scraper.request-delay-ms:3000}") long requestDelayMs) {
        this.trackedPageRepository = trackedPageRepository;
        this.runLogRepository = runLogRepository;
        this.persistenceService = persistenceService;
        this.notifierService = notifierService;
        this.requestDelayMs = requestDelayMs;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public String fetchHtml(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("User-Agent", USER_AGENT)
                .header("Accept-Language", "hr-HR,hr;q=0.9,en-US;q=0.8,en;q=0.7")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new RuntimeException("HTTP " + response.statusCode() + " za " + url);
        }
        return response.body();
    }

    /**
     * Parsira WooCommerce listing stranicu i vraca listu proizvoda.
     *
     * POPRAVAK (kolovoz 2026.): selektor "li.product, div.product, .products .product"
     * zna pogoditi ugnijezdene/preklapajuce elemente (npr. "srodni proizvodi" widget
     * unutar kartice proizvoda), sto je uzrokovalo da se isti proizvod obradi dvaput s
     * krivom cijenom (kontaminacija izmedu susjednih proizvoda). Zadrzavamo samo
     * "najvise" kandidate (one koji nisu potomci nekog drugog kandidata), plus dodatna
     * zastita koja odbacuje duplicirani slug unutar iste stranice.
     *
     * POPRAVAK #2 (kolovoz 2026., nakon uvida u stvarni HTML): stranica koristi Avada/
     * Fusion Builder temu, ne standardni WooCommerce loop markup. Generalne
     * pretpostavljene klase (".woocommerce-loop-product__title" i sl.) na njoj NE
     * postoje. Prvi <a> unutar kartice proizvoda vodi na opcenitu stranicu "garancija
     * najbolje cijene" - IDENTICNU na svakoj kartici - pa je uzimanje "prvog linka"
     * svakom proizvodu davalo isti slug, sto je (u kombinaciji sa zastitom protiv
     * duplikata iz popravka #1) odbacivalo gotovo sve proizvode osim jednog po
     * stranici. Sada se link/naziv/slug uzimaju iz "h3.product-title a", koji je
     * unikatan po kartici. Slika se uzima iz ".tp-image-wrapper"/".fusion-image-wrapper"
     * (preferirajuci "data-orig-src" jer je woocommerce thumbnail lazy-loadan s praznim
     * placeholderom u "src").
     */
    public List<ScrapedProduct> parseProducts(String html, String pageLabel) {
        Document doc = Jsoup.parse(html);
        List<ScrapedProduct> products = new ArrayList<>();

        Elements candidates = doc.select("li.product");
        List<Element> topLevel = new ArrayList<>();
        for (Element el : candidates) {
            boolean isNestedInOther = false;
            for (Element other : candidates) {
                if (other != el && isAncestor(other, el)) {
                    isNestedInOther = true;
                    break;
                }
            }
            if (!isNestedInOther) topLevel.add(el);
        }

        Set<String> seenSlugsOnPage = new HashSet<>();

        for (Element el : topLevel) {
            Element titleLink = el.selectFirst("h3.product-title a");
            if (titleLink == null) continue;
            String href = titleLink.attr("href");
            if (href == null || href.isBlank()) continue;

            String name = titleLink.text().trim();
            if (name.isBlank()) continue;

            String slug = href.split("\\?")[0];
            if (seenSlugsOnPage.contains(slug)) continue;
            seenSlugsOnPage.add(slug);

            Double price = null;
            Element priceContainer = el.selectFirst(".fusion-price-rating .price");
            if (priceContainer == null) priceContainer = el.selectFirst(".price");
            if (priceContainer != null) {
                // POPRAVAK: kod akcijskih artikala (<del>stara</del><ins>nova</ins>) WooCommerce
                // dodaje "screen-reader-text" recenice BEZ razmaka izmedu tagova (npr. "...je: 153,70€."
                // odmah nastavlja s "150,63€..."), pa regex nad CIJELIM tekstu .price elementa spaja
                // tocku s iducim brojem i pokvari parsing. Umjesto toga ciljano uzimamo samo tekst
                // unutar ".woocommerce-Price-amount bdi" - iz <ins> ako postoji (akcijska/trenutna
                // cijena), inace izravno iz .price (redovna cijena bez akcije).
                Element ins = priceContainer.selectFirst("ins");
                Element amountEl = ins != null
                        ? ins.selectFirst(".woocommerce-Price-amount bdi")
                        : priceContainer.selectFirst(".woocommerce-Price-amount bdi");

                String priceText = amountEl != null ? amountEl.text().trim() : priceContainer.text().trim();
                Matcher m = PRICE_PATTERN.matcher(priceText);
                if (m.find()) {
                    String raw = m.group().replace(".", "").replace(",", ".");
                    try {
                        price = Double.parseDouble(raw);
                    } catch (NumberFormatException ignored) {
                        // ostaje null, tretira se kao nepoznata cijena
                    }
                }
            }

            boolean outOfStock = el.hasClass("outofstock") || el.selectFirst(".outofstock") != null;

            String imageUrl = null;
            Element imgContainer = el.selectFirst(".tp-image-wrapper, .fusion-image-wrapper");
            if (imgContainer != null) {
                Element img = imgContainer.selectFirst("img");
                if (img != null) {
                    if (img.hasAttr("data-orig-src") && !img.attr("data-orig-src").isBlank()) {
                        imageUrl = img.attr("data-orig-src");
                    } else if (img.hasAttr("data-src") && !img.attr("data-src").isBlank()) {
                        imageUrl = img.attr("data-src");
                    } else {
                        String src = img.attr("src");
                        if (!src.isBlank() && !src.startsWith("data:")) imageUrl = src;
                    }
                }
            }

            ScrapedProduct sp = new ScrapedProduct();
            sp.slug = slug;
            sp.name = name;
            sp.price = price;
            sp.inStock = !outOfStock;
            sp.imageUrl = imageUrl;
            sp.pageLabel = pageLabel;
            products.add(sp);
        }

        return products;
    }

    private boolean isAncestor(Element possibleAncestor, Element el) {
        Element parent = el.parent();
        while (parent != null) {
            if (parent == possibleAncestor) return true;
            parent = parent.parent();
        }
        return false;
    }

    public RunResult runCheck() {
        RunLog runLog = new RunLog();
        runLog.setStatus("running");
        runLog = runLogRepository.save(runLog);

        int pagesChecked = 0;
        List<ScrapedProduct> allProducts = new ArrayList<>();
        Set<String> allSlugsThisRun = new HashSet<>();
        Set<String> successfulPageLabels = new HashSet<>();
        List<String> failedPages = new ArrayList<>();

        try {
            List<TrackedPage> pages = trackedPageRepository.findByActiveTrue();

            for (TrackedPage page : pages) {
                try {
                    String html = fetchHtml(page.getUrl());
                    List<ScrapedProduct> products = parseProducts(html, page.getLabel());
                    for (ScrapedProduct p : products) allSlugsThisRun.add(p.slug);
                    allProducts.addAll(products);
                    successfulPageLabels.add(page.getLabel());
                    pagesChecked++;
                } catch (Exception e) {
                    log.error("Greska kod dohvata \"{}\" ({}): {}", page.getLabel(), page.getUrl(), e.getMessage());
                    failedPages.add(page.getLabel());
                }
                sleep(requestDelayMs);
            }

            List<ChangeDto> changes = persistenceService.diffAndStore(allProducts);
            List<ChangeDto> removedChanges = persistenceService.markRemoved(allSlugsThisRun, successfulPageLabels);
            List<ChangeDto> allChanges = new ArrayList<>(changes);
            allChanges.addAll(removedChanges);

            // status 'partial' kad je barem jedna stranica pukla, umjesto da se to sakrije pod 'ok'
            String status = failedPages.isEmpty() ? "ok" : "partial";
            String errorMessage = failedPages.isEmpty() ? null : "Neuspjeli dohvat: " + String.join(", ", failedPages);

            runLog.setFinishedAt(OffsetDateTime.now());
            runLog.setStatus(status);
            runLog.setPagesChecked(pagesChecked);
            runLog.setProductsFound(allProducts.size());
            runLog.setChangesFound(allChanges.size());
            runLog.setErrorMessage(errorMessage);
            runLogRepository.save(runLog);

            if (!allChanges.isEmpty()) {
                notifierService.sendChangesEmail(allChanges);
            }

            return new RunResult(pagesChecked, allProducts.size(), allChanges);
        } catch (Exception err) {
            runLog.setFinishedAt(OffsetDateTime.now());
            runLog.setStatus("error");
            runLog.setErrorMessage(err.getMessage());
            runLogRepository.save(runLog);
            throw new RuntimeException("Provjera neuspjela: " + err.getMessage(), err);
        }
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
