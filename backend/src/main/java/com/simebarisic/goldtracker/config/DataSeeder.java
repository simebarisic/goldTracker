package com.simebarisic.goldtracker.config;

import com.simebarisic.goldtracker.model.TrackedPage;
import com.simebarisic.goldtracker.repository.TrackedPageRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

// Popis pocetnih (default) podstranica za pracenje - isto kao config.js u
// Node verziji. Kasnije se mogu dodavati/brisati kroz dashboard (tab "Stranice
// za pracenje"), ne treba dirati ovaj fajl.
@Component
public class DataSeeder implements CommandLineRunner {

    // POPRAVAK: originalna Node verzija je imala 5 stranica, s TOCNO ovim URL-ovima
    // (potvrdeno iz stare SQLite baze). Ranije sam ovdje pogresno upisao
    // "/srebrnjaci/" (ne postoji, vraca 404) umjesto ispravnog "/srebrne-kovanice/",
    // i izostavio petu stranicu "Aukcije".
    private static final List<TrackedPage> DEFAULT_TRACKED_PAGES = List.of(
            new TrackedPage("Zlatne poluge",
                    "https://www.centarzlata.com/kategorija/investicijsko-zlato/zlatne-poluge/?per_page=100"),
            new TrackedPage("Zlatnici",
                    "https://www.centarzlata.com/kategorija/investicijsko-zlato/zlatnici/?per_page=100"),
            new TrackedPage("Srebrne poluge",
                    "https://www.centarzlata.com/kategorija/investicijsko-srebro/srebrne-poluge/?per_page=100"),
            new TrackedPage("Srebrenjaci",
                    "https://www.centarzlata.com/kategorija/investicijsko-srebro/srebrne-kovanice/"),
            new TrackedPage("Aukcije",
                    "https://www.centarzlata.com/kategorija/aukcije/")
    );

    private final TrackedPageRepository trackedPageRepository;

    public DataSeeder(TrackedPageRepository trackedPageRepository) {
        this.trackedPageRepository = trackedPageRepository;
    }

    @Override
    public void run(String... args) {
        if (trackedPageRepository.count() == 0) {
            trackedPageRepository.saveAll(DEFAULT_TRACKED_PAGES);
        }
    }
}
