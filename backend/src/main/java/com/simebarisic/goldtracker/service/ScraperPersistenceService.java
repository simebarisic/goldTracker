package com.simebarisic.goldtracker.service;

import com.simebarisic.goldtracker.dto.ChangeDto;
import com.simebarisic.goldtracker.dto.ScrapedProduct;
import com.simebarisic.goldtracker.model.ChangeLog;
import com.simebarisic.goldtracker.model.PriceHistory;
import com.simebarisic.goldtracker.model.Product;
import com.simebarisic.goldtracker.repository.ChangeLogRepository;
import com.simebarisic.goldtracker.repository.PriceHistoryRepository;
import com.simebarisic.goldtracker.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;

@Service
public class ScraperPersistenceService {

    private static final Logger log = LoggerFactory.getLogger(ScraperPersistenceService.class);

    private final ProductRepository productRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final ChangeLogRepository changeLogRepository;

    // Popravak (kolovoz 2026.): ako novoocitana cijena odstupa vise od ovog omjera
    // od zadnje poznate (npr. 5x veca ili 5x manja), tretiramo je kao vjerojatnu
    // gresku parsanja (npr. selektor je pogodio krivi/ugnijezdeni element), a ne
    // kao stvarnu promjenu cijene - preskacemo update i obavijest, samo upozorenje u log.
    private final double priceSanityMaxRatio;

    public ScraperPersistenceService(
            ProductRepository productRepository,
            PriceHistoryRepository priceHistoryRepository,
            ChangeLogRepository changeLogRepository,
            @Value("${app.scraper.price-sanity-max-ratio:5}") double priceSanityMaxRatio) {
        this.productRepository = productRepository;
        this.priceHistoryRepository = priceHistoryRepository;
        this.changeLogRepository = changeLogRepository;
        this.priceSanityMaxRatio = priceSanityMaxRatio;
    }

    @Transactional
    public List<ChangeDto> diffAndStore(List<ScrapedProduct> items) {
        List<ChangeDto> changes = new ArrayList<>();
        OffsetDateTime now = OffsetDateTime.now();

        for (ScrapedProduct item : items) {
            Optional<Product> existingOpt = productRepository.findBySlug(item.slug);

            if (existingOpt.isEmpty()) {
                Product p = new Product();
                p.setSlug(item.slug);
                p.setName(item.name);
                p.setPageLabel(item.pageLabel);
                p.setPrice(item.price);
                p.setInStock(item.inStock);
                p.setImageUrl(item.imageUrl);
                p.setFirstSeen(now);
                p.setLastSeen(now);
                p.setRemoved(false);
                productRepository.save(p);

                if (item.price != null) {
                    priceHistoryRepository.save(new PriceHistory(item.slug, item.price));
                }

                saveChange("new_product", item.slug, item.name, null, item.price != null ? String.valueOf(item.price) : "N/A");
                changes.add(new ChangeDto("new_product", item.name, null, item.price));
                continue;
            }

            Product existing = existingOpt.get();

            // cijena
            if (item.price != null && existing.getPrice() != null && !item.price.equals(existing.getPrice())) {
                double ratio = existing.getPrice() > 0 ? item.price / existing.getPrice() : Double.POSITIVE_INFINITY;
                boolean implausibleJump = ratio > priceSanityMaxRatio || ratio < 1.0 / priceSanityMaxRatio;

                if (implausibleJump) {
                    log.warn("Preskocena sumnjiva promjena cijene za \"{}\" ({}): {} -> {} (omjer {}x). Zadrzavam staru cijenu.",
                            item.name, item.slug, existing.getPrice(), item.price, String.format("%.2f", ratio));
                    // ne prepisujemo dobru cijenu sumnjivom vrijednoscu
                    item.price = existing.getPrice();
                } else {
                    saveChange("price_change", item.slug, item.name, String.valueOf(existing.getPrice()), String.valueOf(item.price));
                    priceHistoryRepository.save(new PriceHistory(item.slug, item.price));
                    changes.add(new ChangeDto("price_change", item.name, existing.getPrice(), item.price));
                }
            } else if (item.price != null && existing.getPrice() == null) {
                priceHistoryRepository.save(new PriceHistory(item.slug, item.price));
            }

            // dostupnost
            if (existing.isInStock() && !item.inStock) {
                saveChange("out_of_stock", item.slug, item.name, "dostupno", "nedostupno");
                changes.add(new ChangeDto("out_of_stock", item.name, null, null));
            } else if (!existing.isInStock() && item.inStock) {
                saveChange("back_in_stock", item.slug, item.name, "nedostupno", "dostupno");
                changes.add(new ChangeDto("back_in_stock", item.name, null, null));
            }

            existing.setName(item.name);
            existing.setPageLabel(item.pageLabel);
            existing.setPrice(item.price);
            existing.setInStock(item.inStock);
            existing.setImageUrl(item.imageUrl);
            existing.setLastSeen(now);
            existing.setRemoved(false);
            productRepository.save(existing);
        }

        return changes;
    }

    // POPRAVAK (kolovoz 2026.): proizvod se smije oznaciti kao "uklonjen" SAMO ako
    // je stranica na kojoj se inace nalazi ovaj put uspjesno dohvacena, a proizvoda
    // vise nema medu scrapeanim rezultatima. Prije bi svaki neuspjeli dohvat stranice
    // (blokada, timeout...) izgledao kao da su svi njeni proizvodi nestali, sto je
    // slalo lavinu laznih "uklonjen proizvod" obavijesti.
    @Transactional
    public List<ChangeDto> markRemoved(Set<String> slugsSeenThisRun, Set<String> successfulPageLabels) {
        List<ChangeDto> changes = new ArrayList<>();
        List<Product> allActive = productRepository.findByRemovedFalse();

        for (Product p : allActive) {
            if (!successfulPageLabels.contains(p.getPageLabel())) continue;
            if (!slugsSeenThisRun.contains(p.getSlug())) {
                saveChange("removed_product", p.getSlug(), p.getName(), "dostupno na stranici", "uklonjeno sa stranice");
                p.setRemoved(true);
                productRepository.save(p);
                changes.add(new ChangeDto("removed_product", p.getName(), null, null));
            }
        }

        return changes;
    }

    private void saveChange(String type, String slug, String name, String oldValue, String newValue) {
        ChangeLog c = new ChangeLog();
        c.setType(type);
        c.setProductSlug(slug);
        c.setProductName(name);
        c.setOldValue(oldValue);
        c.setNewValue(newValue);
        changeLogRepository.save(c);
    }
}
