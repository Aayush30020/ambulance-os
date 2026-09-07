package com.ambulanceos.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record HospitalRequest(

        @NotBlank(message = "Hospital code is required")
        String hospitalCode,

        @NotBlank(message = "Hospital name is required")
        String name,

        @NotNull(message = "Latitude is required")
        @DecimalMin(value = "-90.0")
        @DecimalMax(value = "90.0")
        Double latitude,

        @NotNull(message = "Longitude is required")
        @DecimalMin(value = "-180.0")
        @DecimalMax(value = "180.0")
        Double longitude,

        @NotNull(message = "Available beds are required")
        @Min(value = 0, message = "Available beds cannot be negative")
        Integer availableBeds,

        @NotBlank(message = "Facility type is required")
        String facilityType
) {
}