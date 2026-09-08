package com.ambulanceos.dto;

public record DispatchResponse(

        Long emergencyId,

        Long ambulanceId,

        String ambulanceNumber,

        String ambulanceType,

        String ambulanceStatus,

        double distanceKm,

        double estimatedTravelTimeMinutes,

        String trafficLevel

) {
}