package com.ambulanceos.dto;

public record AmbulanceResponse(

        Long id,

        String ambulanceNumber,

        Double latitude,

        Double longitude,

        String status,

        String type
) {
}