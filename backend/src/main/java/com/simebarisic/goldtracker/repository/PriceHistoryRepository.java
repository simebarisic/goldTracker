package com.simebarisic.goldtracker.repository;

import com.simebarisic.goldtracker.model.PriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Long> {
    List<PriceHistory> findByProductSlugOrderByRecordedAtAsc(String productSlug);
}
