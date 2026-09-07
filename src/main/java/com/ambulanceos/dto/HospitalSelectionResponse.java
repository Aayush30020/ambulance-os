package com.ambulanceos.dto;

public record HospitalSelectionResponse(

        Long emergencyId,

        Long hospitalId,

        String hospitalCode,

        String hospitalName,

        String facilityType,

        Integer availableBeds,

        Double distanceKm

) {
}