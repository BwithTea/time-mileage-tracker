package com.tea.time_mileage_tracker.controller;

import com.tea.time_mileage_tracker.model.*;
import com.tea.time_mileage_tracker.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    private double lunchMinutes(Shift shift) {
        if (shift.getLunchStartTime() != null && shift.getLunchEndTime() != null) {
            return Duration.between(shift.getLunchStartTime(), shift.getLunchEndTime()).toMinutes();
        }
        return 0;
    }

    private void recomputeHours(Shift shift) {
        if (shift.getClockInTime() != null && shift.getClockOutTime() != null) {
            double totalMinutes = Duration.between(shift.getClockInTime(), shift.getClockOutTime()).toMinutes();
            shift.setTotalHours((totalMinutes - lunchMinutes(shift)) / 60.0);
        }
    }

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
    public Shift clockIn(@RequestParam Long storeId) {
        Store store = storeRepository.findById(storeId).orElseThrow();

        Shift shift = new Shift();
        shift.setClockInTime(Instant.now());
        shift = shiftRepository.save(shift);

        StoreVisit visit = new StoreVisit();
        visit.setShift(shift);
        visit.setStore(store);
        visit.setSequenceOrder(1);
        storeVisitRepository.save(visit);

        return shift;
    }

    @PostMapping("/{shiftId}/visit")
    public StoreVisit logVisit(@PathVariable Long shiftId, @RequestParam Long storeId) {
        Shift shift = shiftRepository.findById(shiftId).orElseThrow();
        Store store = storeRepository.findById(storeId).orElseThrow();

        List<StoreVisit> existing = storeVisitRepository.findByShiftIdOrderBySequenceOrderAsc(shiftId);

        StoreVisit visit = new StoreVisit();
        visit.setShift(shift);
        visit.setStore(store);
        visit.setSequenceOrder(existing.size() + 1);
        return storeVisitRepository.save(visit);
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
        shift.setLunchEndTime(Instant.now());
        recomputeHours(shift);
        return shiftRepository.save(shift);
    }

    @PostMapping("/{shiftId}/clock-out")
    public Shift clockOut(@PathVariable Long shiftId, @RequestParam Long storeId) {
        Shift shift = shiftRepository.findById(shiftId).orElseThrow();
        Store store = storeRepository.findById(storeId).orElseThrow();

        List<StoreVisit> existing = storeVisitRepository.findByShiftIdOrderBySequenceOrderAsc(shiftId);

        StoreVisit finalVisit = new StoreVisit();
        finalVisit.setShift(shift);
        finalVisit.setStore(store);
        finalVisit.setSequenceOrder(existing.size() + 1);
        storeVisitRepository.save(finalVisit);

        shift.setClockOutTime(Instant.now());
        recomputeHours(shift);

        return shiftRepository.save(shift);
    }

    public record ShiftUpdateRequest(String clockInTime, String clockOutTime, String lunchStartTime, String lunchEndTime) {}

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
}