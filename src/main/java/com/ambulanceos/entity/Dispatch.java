package com.ambulanceos.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "dispatches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Dispatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long emergencyId;

    @Column(nullable = false)
    private Long ambulanceId;

    @Column(nullable = false)
    private String ambulanceNumber;

    @Column(nullable = false)
    private Long hospitalId;

    @Column(nullable = false)
    private String hospitalName;

    @Column(nullable = false)
    private String routingAlgorithm;

    @Column(nullable = false)
    private Double distanceToEmergencyKm;

    @Column(nullable = false)
    private Double timeToEmergencyMinutes;

    @Column(nullable = false)
    private Double distanceToHospitalKm;

    @Column(nullable = false)
    private Double timeToHospitalMinutes;

    @Column(nullable = false)
    private Double totalDistanceKm;

    @Column(nullable = false)
    private Double totalEstimatedTimeMinutes;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private LocalDateTime dispatchedAt;

    private LocalDateTime completedAt;
}