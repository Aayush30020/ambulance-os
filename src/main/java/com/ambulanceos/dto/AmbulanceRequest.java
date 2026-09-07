package com.ambulanceos.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AmbulanceRequest(

        @NotBlank(message = "Ambulance number is required")
        String ambulanceNumber,

        @NotNull(message = "Latitude is required")
        @DecimalMin(value = "-90.0")
        @DecimalMax(value = "90.0")
        Double latitude,

        @NotNull(message = "Longitude is required")
        @DecimalMin(value = "-180.0")
        @DecimalMax(value = "180.0")
        Double longitude,

        @NotBlank(message = "Status is required")
        String status,

        @NotBlank(message = "Type is required")
        String type
) {
}