package com.ambulanceos.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "ambulances",
        indexes = {
                @Index(
                        name = "idx_ambulances_status",
                        columnList = "status"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ambulance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Each ambulance must have a unique fleet number.
     */
    @Column(nullable = false, unique = true)
    private String ambulanceNumber;

    /*
     * Current ambulance latitude.
     */
    @Column(nullable = false)
    private Double latitude;

    /*
     * Current ambulance longitude.
     */
    @Column(nullable = false)
    private Double longitude;

    /*
     * Indexed because dispatch frequently searches for
     * AVAILABLE ambulances.
     */
    @Column(nullable = false)
    private String status;

    /*
     * Ambulance type, for example ALS or BLS.
     */
    @Column(nullable = false)
    private String type;
}