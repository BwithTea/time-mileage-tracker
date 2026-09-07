package com.tea.time_mileage_tracker.repository;

import com.tea.time_mileage_tracker.model.Shift;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ShiftRepository extends JpaRepository<Shift, Long> {
    Optional<Shift> findFirstByClockOutTimeIsNullOrderByClockInTimeDesc();
    List<Shift> findByClockInTimeBetween(Instant start, Instant end);
}