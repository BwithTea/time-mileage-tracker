package com.tea.time_mileage_tracker.controller;

import com.tea.time_mileage_tracker.model.Store;
import com.tea.time_mileage_tracker.repository.StoreRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stores")
public class StoreController {

    private final StoreRepository storeRepository;

    public StoreController(StoreRepository storeRepository) {
        this.storeRepository = storeRepository;
    }

    @GetMapping
    public List<Store> listStores() {
        return storeRepository.findAll();
    }

    @PostMapping
    public Store addStore(@RequestBody Store store) {
        return storeRepository.save(store);
    }
}