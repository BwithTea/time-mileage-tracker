package com.tea.time_mileage_tracker.repository;

import com.tea.time_mileage_tracker.model.StoreVisit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StoreVisitRepository extends JpaRepository<StoreVisit, Long> {
    List<StoreVisit> findByShiftIdOrderBySequenceOrderAsc(Long shiftId);
}