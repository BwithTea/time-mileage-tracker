package com.tea.time_mileage_tracker.repository;

import com.tea.time_mileage_tracker.model.Issue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IssueRepository extends JpaRepository<Issue, Long> {
    List<Issue> findAllByOrderByCreatedAtDesc();
}