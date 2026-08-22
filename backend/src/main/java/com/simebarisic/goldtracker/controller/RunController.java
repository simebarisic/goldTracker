package com.simebarisic.goldtracker.controller;

import com.simebarisic.goldtracker.dto.RunResult;
import com.simebarisic.goldtracker.model.RunLog;
import com.simebarisic.goldtracker.repository.RunLogRepository;
import com.simebarisic.goldtracker.service.ScraperService;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class RunController {

    private final RunLogRepository runLogRepository;
    private final ScraperService scraperService;

    public RunController(RunLogRepository runLogRepository, ScraperService scraperService) {
        this.runLogRepository = runLogRepository;
        this.scraperService = scraperService;
    }

    @GetMapping("/runs")
    public List<RunLog> getRuns() {
        return runLogRepository.findAllByOrderByStartedAtDesc(PageRequest.of(0, 30));
    }

    @PostMapping("/run-now")
    public ResponseEntity<?> runNow() {
        try {
            RunResult result = scraperService.runCheck();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
