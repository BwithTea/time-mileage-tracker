package com.tea.time_mileage_tracker.repository;

import com.tea.time_mileage_tracker.model.Store;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreRepository extends JpaRepository<Store, Long> {
}