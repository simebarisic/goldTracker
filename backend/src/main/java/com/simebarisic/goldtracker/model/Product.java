package com.simebarisic.goldtracker.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // URL proizvoda, jedinstveni identifikator (ekvivalent "slug" iz Node verzije)
    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private String name;

    @Column(name = "page_label")
    private String pageLabel;

    private Double price;

    @Column(nullable = false)
    private String currency = "EUR";

    @Column(name = "in_stock", nullable = false)
    private boolean inStock = true;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "first_seen", nullable = false)
    private OffsetDateTime firstSeen = OffsetDateTime.now();

    @Column(name = "last_seen", nullable = false)
    private OffsetDateTime lastSeen = OffsetDateTime.now();

    @Column(nullable = false)
    private boolean removed = false;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPageLabel() { return pageLabel; }
    public void setPageLabel(String pageLabel) { this.pageLabel = pageLabel; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public boolean isInStock() { return inStock; }
    public void setInStock(boolean inStock) { this.inStock = inStock; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public OffsetDateTime getFirstSeen() { return firstSeen; }
    public void setFirstSeen(OffsetDateTime firstSeen) { this.firstSeen = firstSeen; }

    public OffsetDateTime getLastSeen() { return lastSeen; }
    public void setLastSeen(OffsetDateTime lastSeen) { this.lastSeen = lastSeen; }

    public boolean isRemoved() { return removed; }
    public void setRemoved(boolean removed) { this.removed = removed; }
}
