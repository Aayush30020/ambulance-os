package com.ambulanceos.service;

import com.ambulanceos.dto.EmergencyRequest;
import com.ambulanceos.dto.EmergencyResponse;
import com.ambulanceos.entity.Emergency;
import com.ambulanceos.exception.EmergencyNotFoundException;
import com.ambulanceos.repository.EmergencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class EmergencyService {

    private static final String ACTIVE = "ACTIVE";
    private static final String RESPONDING = "RESPONDING";
    private static final String COMPLETED = "COMPLETED";

    private static final Set<String> VALID_STATUSES = Set.of(
            ACTIVE,
            RESPONDING,
            COMPLETED
    );

    private final EmergencyRepository emergencyRepository;


    // =========================================================
    // CREATE EMERGENCY
    // =========================================================

    public EmergencyResponse createEmergency(
            EmergencyRequest request
    ) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Emergency request cannot be null"
            );
        }

        Emergency emergency =
                Emergency.builder()
                        .location(request.location())
                        .latitude(request.latitude())
                        .longitude(request.longitude())
                        .priority(request.priority())
                        .facility(request.facility())
                        .notes(request.notes())
                        .status(ACTIVE)
                        .createdAt(LocalDateTime.now())
                        .build();

        Emergency savedEmergency =
                emergencyRepository.save(emergency);

        return mapToResponse(savedEmergency);
    }


    // =========================================================
    // GET ALL EMERGENCIES
    // =========================================================

    public List<EmergencyResponse> getAllEmergencies() {

        return emergencyRepository
                .findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }


    // =========================================================
    // GET EMERGENCY BY ID
    // =========================================================

    public EmergencyResponse getEmergencyById(
            Long id
    ) {

        Emergency emergency =
                emergencyRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new EmergencyNotFoundException(id)
                        );

        return mapToResponse(emergency);
    }


    // =========================================================
    // UPDATE EMERGENCY STATUS
    // =========================================================
    //
    // Allowed lifecycle:
    //
    // ACTIVE
    //    ↓
    // RESPONDING
    //    ↓
    // COMPLETED
    //
    // COMPLETED is terminal.
    //
    // We also allow:
    //
    // RESPONDING → ACTIVE
    //
    // This is useful if an emergency was marked as responding
    // and the dispatch needs to be cancelled/reset.
    //
    // =========================================================

    public EmergencyResponse updateStatus(
            Long id,
            String status
    ) {

        Emergency emergency =
                emergencyRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new EmergencyNotFoundException(id)
                        );

        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException(
                    "Emergency status cannot be empty"
            );
        }

        String normalizedStatus =
                status.trim().toUpperCase(Locale.ROOT);

        validateStatus(normalizedStatus);

        String currentStatus =
                normalizeExistingStatus(
                        emergency.getStatus()
                );

        // -----------------------------------------------------
        // Idempotent update
        // -----------------------------------------------------

        if (currentStatus.equals(normalizedStatus)) {
            return mapToResponse(emergency);
        }

        validateTransition(
                currentStatus,
                normalizedStatus
        );

        emergency.setStatus(normalizedStatus);

        Emergency updatedEmergency =
                emergencyRepository.save(emergency);

        return mapToResponse(updatedEmergency);
    }


    // =========================================================
    // VALIDATE STATUS
    // =========================================================

    private void validateStatus(
            String status
    ) {

        if (!VALID_STATUSES.contains(status)) {
            throw new IllegalArgumentException(
                    "Invalid emergency status: " + status
                            + ". Allowed statuses: "
                            + VALID_STATUSES
            );
        }
    }


    // =========================================================
    // VALIDATE STATUS TRANSITION
    // =========================================================

    private void validateTransition(
            String currentStatus,
            String newStatus
    ) {

        boolean validTransition =
                switch (currentStatus) {

                    case ACTIVE ->
                            newStatus.equals(RESPONDING);

                    case RESPONDING ->
                            newStatus.equals(ACTIVE)
                                    || newStatus.equals(COMPLETED);

                    case COMPLETED ->
                            false;

                    default ->
                            false;
                };

        if (!validTransition) {
            throw new IllegalArgumentException(
                    "Invalid emergency status transition: "
                            + currentStatus
                            + " -> "
                            + newStatus
            );
        }
    }


    // =========================================================
    // NORMALIZE EXISTING STATUS
    // =========================================================

    private String normalizeExistingStatus(
            String status
    ) {

        if (status == null || status.isBlank()) {
            throw new IllegalStateException(
                    "Emergency has an invalid empty status"
            );
        }

        String normalized =
                status.trim().toUpperCase(Locale.ROOT);

        validateStatus(normalized);

        return normalized;
    }


    // =========================================================
    // MAP ENTITY → RESPONSE
    // =========================================================

    private EmergencyResponse mapToResponse(
            Emergency emergency
    ) {

        return new EmergencyResponse(
                emergency.getId(),
                emergency.getLocation(),
                emergency.getLatitude(),
                emergency.getLongitude(),
                emergency.getPriority(),
                emergency.getFacility(),
                emergency.getNotes(),
                emergency.getStatus(),
                emergency.getCreatedAt()
        );
    }
}