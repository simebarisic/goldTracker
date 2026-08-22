package com.simebarisic.goldtracker.controller;

import com.simebarisic.goldtracker.model.PriceHistory;
import com.simebarisic.goldtracker.model.Product;
import com.simebarisic.goldtracker.repository.PriceHistoryRepository;
import com.simebarisic.goldtracker.repository.ProductRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductRepository productRepository;
    private final PriceHistoryRepository priceHistoryRepository;

    public ProductController(ProductRepository productRepository, PriceHistoryRepository priceHistoryRepository) {
        this.productRepository = productRepository;
        this.priceHistoryRepository = priceHistoryRepository;
    }

    @GetMapping
    public List<Product> getProducts(@RequestParam(defaultValue = "0") String removed) {
        boolean showRemoved = "1".equals(removed);
        return productRepository.findByRemovedOrderByLastSeenDesc(showRemoved);
    }

    // NAPOMENA: slug je puni URL proizvoda i sadrzi "/", pa se ovdje salje kao
    // query parametar (?slug=...) umjesto path varijable - izbjegava dvosmislenost
    // s Spring-ovim path matchingom koju bi "/api/products/{slug}/history" imao.
    @GetMapping("/history")
    public List<PriceHistory> getHistory(@RequestParam String slug) {
        return priceHistoryRepository.findByProductSlugOrderByRecordedAtAsc(slug);
    }
}
