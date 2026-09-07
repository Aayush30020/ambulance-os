package com.ambulanceos.dto;

import java.time.LocalDateTime;

public record EmergencyResponse(

        Long id,

        String location,

        Double latitude,

        Double longitude,

        String priority,

        String facility,

        String notes,

        String status,

        LocalDateTime createdAt
) {
}