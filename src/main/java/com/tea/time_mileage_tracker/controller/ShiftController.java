package com.tea.time_mileage_tracker.controller;

import com.tea.time_mileage_tracker.model.*;
import com.tea.time_mileage_tracker.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/shifts")
public class ShiftController {

    private final ShiftRepository shiftRepository;
    private final StoreVisitRepository storeVisitRepository;
    private final StoreRepository storeRepository;

    public ShiftController(ShiftRepository shiftRepository, StoreVisitRepository storeVisitRepository,
                           StoreRepository storeRepository) {
        this.shiftRepository = shiftRepository;
        this.storeVisitRepository = storeVisitRepository;
        this.storeRepository = storeRepository;
    }

    // What the frontend gets back for a store visit. Flat and simple, no nested Shift object.
    public record VisitView(Long id, Integer sequenceOrder, Long storeId, String storeName, String note) {
        static VisitView from(StoreVisit v) {
            return new VisitView(v.getId(), v.getSequenceOrder(),
                    v.getStore().getId(), v.getStore().getName(), v.getNote());
        }
    }

    public record ShiftUpdateRequest(String clockInTime, String clockOutTime,
                                     String lunchStartTime, String lunchEndTime) {}

    public record VisitUpdateRequest(Long storeId, String note) {}

    // ---------- helpers ----------

    private void recomputeHours(Shift shift) {
        if (shift.getClockInTime() == null || shift.getClockOutTime() == null) return;
        long workedMinutes = Duration.between(shift.getClockInTime(), shift.getClockOutTime()).toMinutes();
        long lunchMinutes = 0;
        if (shift.getLunchStartTime() != null && shift.getLunchEndTime() != null
                && shift.getLunchEndTime().isAfter(shift.getLunchStartTime())) {
            lunchMinutes = Duration.between(shift.getLunchStartTime(), shift.getLunchEndTime()).toMinutes();
        }
        shift.setTotalHours((workedMinutes - lunchMinutes) / 60.0);
    }

    private String cleanNote(String note) {
        if (note == null) return null;
        String trimmed = note.trim();
        if (trimmed.isEmpty()) return null;
        return trimmed.length() > 500 ? trimmed.substring(0, 500) : trimmed;
    }

    private StoreVisit addVisit(Shift shift, Long storeId, String note) {
        Store store = storeRepository.findById(storeId).orElseThrow();
        List<StoreVisit> existing = storeVisitRepository.findByShiftIdOrderBySequenceOrderAsc(shift.getId());

        StoreVisit visit = new StoreVisit();
        visit.setShift(shift);
        visit.setStore(store);
        visit.setSequenceOrder(existing.size() + 1);
        visit.setNote(cleanNote(note));
        return storeVisitRepository.save(visit);
    }

    private void resequence(Long shiftId) {
        List<StoreVisit> visits = storeVisitRepository.findByShiftIdOrderBySequenceOrderAsc(shiftId);
        for (int i = 0; i < visits.size(); i++) {
            visits.get(i).setSequenceOrder(i + 1);
        }
        storeVisitRepository.saveAll(visits);
    }

    // ---------- shift endpoints ----------

    @GetMapping("/current")
    public ResponseEntity<Shift> getCurrentShift() {
        return shiftRepository.findFirstByClockOutTimeIsNullOrderByClockInTimeDesc()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @GetMapping("/recent")
    public List<Shift> recentShifts() {
        return shiftRepository.findTop20ByOrderByClockInTimeDesc();
    }

    @PostMapping("/clock-in")
    public Shift clockIn(@RequestParam Long storeId, @RequestParam(required = false) String note) {
        // Guard: you can't start a new shift while one is still open.
        if (shiftRepository.findFirstByClockOutTimeIsNullOrderByClockInTimeDesc().isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Already clocked in. Clock out first.");
        }

        Shift shift = new Shift();
        shift.setClockInTime(Instant.now());
        shift = shiftRepository.save(shift);

        addVisit(shift, storeId, note);
        return shift;
    }

    @PostMapping("/{shiftId}/lunch/start")
    public Shift startLunch(@PathVariable Long shiftId) {
        Shift shift = shiftRepository.findById(shiftId).orElseThrow();
        shift.setLunchStartTime(Instant.now());
        shift.setLunchEndTime(null);
        return shiftRepository.save(shift);
    }

    @PostMapping("/{shiftId}/lunch/end")
    public Shift endLunch(@PathVariable Long shiftId) {
        Shift shift = shiftRepository.findById(shiftId).orElseThrow();
        if (shift.getLunchStartTime() != null) {
            shift.setLunchEndTime(Instant.now());
        }
        recomputeHours(shift);
        return shiftRepository.save(shift);
    }

    @PostMapping("/{shiftId}/clock-out")
    public Shift clockOut(@PathVariable Long shiftId, @RequestParam Long storeId,
                          @RequestParam(required = false) String note) {
        Shift shift = shiftRepository.findById(shiftId).orElseThrow();
        if (shift.getClockOutTime() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This shift is already clocked out.");
        }

        addVisit(shift, storeId, note);
        shift.setClockOutTime(Instant.now());
        recomputeHours(shift);
        return shiftRepository.save(shift);
    }

    @PatchMapping("/{shiftId}")
    public Shift updateShift(@PathVariable Long shiftId, @RequestBody ShiftUpdateRequest req) {
        Shift shift = shiftRepository.findById(shiftId).orElseThrow();
        if (req.clockInTime() != null) shift.setClockInTime(Instant.parse(req.clockInTime()));
        if (req.clockOutTime() != null) shift.setClockOutTime(Instant.parse(req.clockOutTime()));
        if (req.lunchStartTime() != null) shift.setLunchStartTime(Instant.parse(req.lunchStartTime()));
        if (req.lunchEndTime() != null) shift.setLunchEndTime(Instant.parse(req.lunchEndTime()));
        recomputeHours(shift);
        return shiftRepository.save(shift);
    }

    // ---------- store visit endpoints ----------

    @GetMapping("/{shiftId}/visits")
    public List<VisitView> getVisits(@PathVariable Long shiftId) {
        return storeVisitRepository.findByShiftIdOrderBySequenceOrderAsc(shiftId)
                .stream().map(VisitView::from).toList();
    }

    @PostMapping("/{shiftId}/visit")
    public VisitView logVisit(@PathVariable Long shiftId, @RequestParam Long storeId,
                              @RequestParam(required = false) String note) {
        Shift shift = shiftRepository.findById(shiftId).orElseThrow();
        return VisitView.from(addVisit(shift, storeId, note));
    }

    // Fix the wrong store and/or edit the note on a stop.
    @PatchMapping("/visits/{visitId}")
    public VisitView updateVisit(@PathVariable Long visitId, @RequestBody VisitUpdateRequest req) {
        StoreVisit visit = storeVisitRepository.findById(visitId).orElseThrow();
        if (req.storeId() != null) {
            visit.setStore(storeRepository.findById(req.storeId()).orElseThrow());
        }
        visit.setNote(cleanNote(req.note()));
        return VisitView.from(storeVisitRepository.save(visit));
    }

    // Remove a stop logged by mistake, then renumber the rest 1, 2, 3...
    @DeleteMapping("/visits/{visitId}")
    public ResponseEntity<Void> deleteVisit(@PathVariable Long visitId) {
        StoreVisit visit = storeVisitRepository.findById(visitId).orElseThrow();
        Long shiftId = visit.getShift().getId();
        storeVisitRepository.delete(visit);
        resequence(shiftId);
        return ResponseEntity.noContent().build();
    }
}