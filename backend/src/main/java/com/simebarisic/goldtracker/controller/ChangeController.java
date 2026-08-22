package com.simebarisic.goldtracker.controller;

import com.simebarisic.goldtracker.model.ChangeLog;
import com.simebarisic.goldtracker.repository.ChangeLogRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/changes")
public class ChangeController {

    private final ChangeLogRepository changeLogRepository;

    public ChangeController(ChangeLogRepository changeLogRepository) {
        this.changeLogRepository = changeLogRepository;
    }

    @GetMapping
    public List<ChangeLog> getChanges(@RequestParam(defaultValue = "100") int limit) {
        return changeLogRepository.findAllByOrderByDetectedAtDesc(PageRequest.of(0, limit));
    }
}
