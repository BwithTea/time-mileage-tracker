package com.tea.time_mileage_tracker.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Data
@NoArgsConstructor
public class Issue {

    public enum Status { OPEN, IN_PROGRESS, RESOLVED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Store store;

    private String title;

    @Column(length = 2000)
    private String details;

    private String reportedBy;

    @Enumerated(EnumType.STRING)
    private Status status;

    // A plain date (no time) since a follow up is "check on this Friday", not "Friday at 2:14 PM".
    private LocalDate followUpDate;

    private Instant createdAt;
    private Instant resolvedAt;

    // What fixed it, so you have a record if it comes back.
    @Column(length = 1000)
    private String resolution;
}