package com.simebarisic.goldtracker.repository;

import com.simebarisic.goldtracker.model.RunLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RunLogRepository extends JpaRepository<RunLog, Long> {
    List<RunLog> findAllByOrderByStartedAtDesc(Pageable pageable);
}
