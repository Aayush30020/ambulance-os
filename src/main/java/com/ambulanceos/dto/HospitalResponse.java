package com.ambulanceos.dto;

public record HospitalResponse(

        Long id,

        String hospitalCode,

        String name,

        Double latitude,

        Double longitude,

        Integer availableBeds,

        String facilityType
) {
}