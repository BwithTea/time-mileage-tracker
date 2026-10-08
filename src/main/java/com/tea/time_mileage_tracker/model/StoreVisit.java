package com.tea.time_mileage_tracker.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import main.java.com.tea.time_mileage_tracker.model.Shift;
import main.java.com.tea.time_mileage_tracker.model.Store;

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
    @Column(length = 500)
    
    private String note;
    private Store store;
            
    private Integer sequenceOrder;
}