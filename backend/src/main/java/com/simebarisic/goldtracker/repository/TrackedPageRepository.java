package com.simebarisic.goldtracker.repository;

import com.simebarisic.goldtracker.model.TrackedPage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrackedPageRepository extends JpaRepository<TrackedPage, Long> {
    List<TrackedPage> findByActiveTrue();
    List<TrackedPage> findAllByOrderByIdAsc();
}
