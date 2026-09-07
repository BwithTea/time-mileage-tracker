package com.tea.time_mileage_tracker.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
public class StoreVisit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Shift shift;

    @ManyToOne
    private Store store;

    private Integer sequenceOrder;
}