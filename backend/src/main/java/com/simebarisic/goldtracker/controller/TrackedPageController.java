package com.simebarisic.goldtracker.controller;

import com.simebarisic.goldtracker.dto.CreatePageRequest;
import com.simebarisic.goldtracker.dto.UpdatePageRequest;
import com.simebarisic.goldtracker.model.TrackedPage;
import com.simebarisic.goldtracker.repository.TrackedPageRepository;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pages")
public class TrackedPageController {

    private final TrackedPageRepository trackedPageRepository;

    public TrackedPageController(TrackedPageRepository trackedPageRepository) {
        this.trackedPageRepository = trackedPageRepository;
    }

    @GetMapping
    public List<TrackedPage> getPages() {
        return trackedPageRepository.findAllByOrderByIdAsc();
    }

    @PostMapping
    public ResponseEntity<?> createPage(@Valid @RequestBody CreatePageRequest req) {
        try {
            TrackedPage page = new TrackedPage(req.getLabel(), req.getUrl());
            trackedPageRepository.save(page);
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Stranica s tim URL-om vec postoji"));
        }
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> updateActive(@PathVariable Long id, @RequestBody UpdatePageRequest req) {
        var pageOpt = trackedPageRepository.findById(id);
        if (pageOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        TrackedPage page = pageOpt.get();
        page.setActive(req.isActive());
        trackedPageRepository.save(page);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePage(@PathVariable Long id) {
        trackedPageRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("ok", true));
    }
}
