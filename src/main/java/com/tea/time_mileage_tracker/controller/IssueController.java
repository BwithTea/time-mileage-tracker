package com.tea.time_mileage_tracker.controller;

import com.tea.time_mileage_tracker.model.Issue;
import com.tea.time_mileage_tracker.repository.IssueRepository;
import com.tea.time_mileage_tracker.repository.StoreRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/issues")
public class IssueController {

    private final IssueRepository issueRepository;
    private final StoreRepository storeRepository;

    public IssueController(IssueRepository issueRepository, StoreRepository storeRepository) {
        this.issueRepository = issueRepository;
        this.storeRepository = storeRepository;
    }

    // One request shape for both create and update.
    // On PATCH, a null field means "leave it alone"; an empty string means "clear it".
    public record IssueRequest(Long storeId, String title, String details, String reportedBy,
                               String followUpDate, String status, String resolution) {}

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private LocalDate parseDate(String s) {
        return (s == null || s.isBlank()) ? null : LocalDate.parse(s); // expects yyyy-MM-dd
    }

    @GetMapping
    public List<Issue> list(@RequestParam(required = false) Long storeId,
                            @RequestParam(required = false) String status) {
        return issueRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(i -> storeId == null || (i.getStore() != null && storeId.equals(i.getStore().getId())))
                .filter(i -> status == null || i.getStatus().name().equalsIgnoreCase(status))
                .toList();
    }

    @PostMapping
    public Issue create(@RequestBody IssueRequest req) {
        if (req.storeId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Store is required");
        }
        if (req.title() == null || req.title().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title is required");
        }

        Issue issue = new Issue();
        issue.setStore(storeRepository.findById(req.storeId()).orElseThrow());
        issue.setTitle(req.title().trim());
        issue.setDetails(blankToNull(req.details()));
        issue.setReportedBy(blankToNull(req.reportedBy()));
        issue.setFollowUpDate(parseDate(req.followUpDate()));
        issue.setStatus(Issue.Status.OPEN);
        issue.setCreatedAt(Instant.now());
        return issueRepository.save(issue);
    }

    @PatchMapping("/{id}")
    public Issue update(@PathVariable Long id, @RequestBody IssueRequest req) {
        Issue issue = issueRepository.findById(id).orElseThrow();

        if (req.storeId() != null) issue.setStore(storeRepository.findById(req.storeId()).orElseThrow());
        if (req.title() != null && !req.title().isBlank()) issue.setTitle(req.title().trim());
        if (req.details() != null) issue.setDetails(blankToNull(req.details()));
        if (req.reportedBy() != null) issue.setReportedBy(blankToNull(req.reportedBy()));
        if (req.followUpDate() != null) issue.setFollowUpDate(parseDate(req.followUpDate()));
        if (req.resolution() != null) issue.setResolution(blankToNull(req.resolution()));

        if (req.status() != null) {
            Issue.Status next = Issue.Status.valueOf(req.status().toUpperCase());
            if (next == Issue.Status.RESOLVED && issue.getStatus() != Issue.Status.RESOLVED) {
                issue.setResolvedAt(Instant.now());
            } else if (next != Issue.Status.RESOLVED) {
                issue.setResolvedAt(null);
            }
            issue.setStatus(next);
        }

        return issueRepository.save(issue);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        issueRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}