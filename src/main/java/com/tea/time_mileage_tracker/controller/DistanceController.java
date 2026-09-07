package com.tea.time_mileage_tracker.controller;

import com.tea.time_mileage_tracker.model.Distance;
import com.tea.time_mileage_tracker.model.Store;
import com.tea.time_mileage_tracker.repository.DistanceRepository;
import com.tea.time_mileage_tracker.repository.StoreRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/distances")
public class DistanceController {

    private final DistanceRepository distanceRepository;
    private final StoreRepository storeRepository;

    public DistanceController(DistanceRepository distanceRepository, StoreRepository storeRepository) {
        this.distanceRepository = distanceRepository;
        this.storeRepository = storeRepository;
    }

    public record DistanceRequest(Long storeAId, Long storeBId, Double miles) {}

    @GetMapping
    public List<Distance> listDistances() {
        return distanceRepository.findAll();
    }

    @PostMapping
    public Distance addDistance(@RequestBody DistanceRequest req) {
        Store a = storeRepository.findById(req.storeAId()).orElseThrow();
        Store b = storeRepository.findById(req.storeBId()).orElseThrow();
        Distance d = new Distance();
        d.setStoreA(a);
        d.setStoreB(b);
        d.setMiles(req.miles());
        return distanceRepository.save(d);
    }
}