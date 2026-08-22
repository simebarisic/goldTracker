package com.simebarisic.goldtracker.repository;

import com.simebarisic.goldtracker.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findBySlug(String slug);
    List<Product> findByRemovedOrderByLastSeenDesc(boolean removed);
    List<Product> findByRemovedFalse();
}
