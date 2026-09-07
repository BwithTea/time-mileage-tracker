package com.tea.time_mileage_tracker.repository;

import com.tea.time_mileage_tracker.model.Distance;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DistanceRepository extends JpaRepository<Distance, Long> {
    @Query("SELECT d FROM Distance d WHERE (d.storeA.id = :a AND d.storeB.id = :b) OR (d.storeA.id = :b AND d.storeB.id = :a)")
    Optional<Distance> findBetween(@Param("a") Long a, @Param("b") Long b);

    List<Distance> findAll();
}