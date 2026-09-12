package com.ambulanceos.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "emergencies",
        indexes = {
                @Index(
                        name = "idx_emergencies_status",
                        columnList = "status"
                ),
                @Index(
                        name = "idx_emergencies_created_at",
                        columnList = "created_at"
                ),
                @Index(
                        name = "idx_emergencies_priority",
                        columnList = "priority"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Emergency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String location;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false)
    private String priority;

    @Column(nullable = false)
    private String facility;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}