package com.ambulanceos.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "hospitals",
        indexes = {
                @Index(
                        name = "idx_hospitals_available_beds",
                        columnList = "available_beds"
                ),
                @Index(
                        name = "idx_hospitals_facility_type",
                        columnList = "facility_type"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Hospital {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Unique hospital identifier.
     *
     * unique = true also creates a database uniqueness constraint
     * and associated unique index.
     */
    @Column(nullable = false, unique = true)
    private String hospitalCode;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    /*
     * Indexed because hospital selection filters hospitals
     * based on available emergency capacity.
     */
    @Column(nullable = false)
    private Integer availableBeds;

    /*
     * Indexed because hospital selection filters by the
     * emergency's required facility type.
     */
    @Column(nullable = false)
    private String facilityType;
}