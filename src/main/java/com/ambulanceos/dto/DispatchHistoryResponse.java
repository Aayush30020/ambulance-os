package com.ambulanceos.dto;

import java.time.LocalDateTime;

public record DispatchHistoryResponse(

        Long id,

        Long emergencyId,

        Long ambulanceId,

        String ambulanceNumber,

        Long hospitalId,

        String hospitalName,

        String routingAlgorithm,

        Double distanceToEmergencyKm,

        Double timeToEmergencyMinutes,

        Double distanceToHospitalKm,

        Double timeToHospitalMinutes,

        Double totalDistanceKm,

        Double totalEstimatedTimeMinutes,

        String status,

        LocalDateTime dispatchedAt,

        LocalDateTime completedAt

) {
}