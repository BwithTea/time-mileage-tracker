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
    private final DistanceRepository distanceRepository;

    public ShiftController(ShiftRepository shiftRepository, StoreVisitRepository storeVisitRepository,
                            StoreRepository storeRepository, DistanceRepository distanceRepository) {
        this.shiftRepository = shiftRepository;
        this.storeVisitRepository = storeVisitRepository;
        this.storeRepository = storeRepository;
        this.distanceRepository = distanceRepository;
    }

    @GetMapping("/current")
    public ResponseEntity<Shift> getCurrentShift() {
        return shiftRepository.findFirstByClockOutTimeIsNullOrderByClockInTimeDesc()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
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
        existing.add(finalVisit);

        shift.setClockOutTime(Instant.now());

        double hours = Duration.between(shift.getClockInTime(), shift.getClockOutTime()).toMinutes() / 60.0;
        shift.setTotalHours(hours);

        double totalMiles = 0.0;
        for (int i = 0; i < existing.size() - 1; i++) {
            Long fromId = existing.get(i).getStore().getId();
            Long toId = existing.get(i + 1).getStore().getId();
            totalMiles += distanceRepository.findBetween(fromId, toId)
                    .map(Distance::getMiles)
                    .orElse(0.0);
        }
        shift.setTotalMiles(totalMiles);

        return shiftRepository.save(shift);
    }
}