package com.ambulanceos.service;

import com.ambulanceos.dto.AmbulanceRequest;
import com.ambulanceos.dto.AmbulanceResponse;
import com.ambulanceos.entity.Ambulance;
import com.ambulanceos.exception.AmbulanceNotFoundException;
import com.ambulanceos.repository.AmbulanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AmbulanceService {

    private final AmbulanceRepository ambulanceRepository;

    /*
     * =========================================================
     * VALID AMBULANCE STATUSES
     * =========================================================
     *
     * AVAILABLE     → Ambulance is ready for dispatch
     * EN_ROUTE      → Ambulance is travelling to emergency
     * AT_EMERGENCY  → Ambulance has reached emergency location
     * TO_HOSPITAL   → Ambulance is transporting patient
     *
     * COMPLETED dispatches return the ambulance to AVAILABLE.
     *
     * Keeping these values centralized prevents invalid states
     * from entering the system.
     */
    private static final Set<String> VALID_STATUSES = Set.of(
            "AVAILABLE",
            "EN_ROUTE",
            "AT_EMERGENCY",
            "TO_HOSPITAL"
    );


    // =========================================================
    // CREATE AMBULANCE
    // =========================================================

    public AmbulanceResponse createAmbulance(
            AmbulanceRequest request
    ) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Ambulance request cannot be null"
            );
        }

        if (
                request.ambulanceNumber() == null
                        || request.ambulanceNumber().isBlank()
        ) {
            throw new IllegalArgumentException(
                    "Ambulance number cannot be empty"
            );
        }

        if (
                request.status() == null
                        || request.status().isBlank()
        ) {
            throw new IllegalArgumentException(
                    "Ambulance status cannot be empty"
            );
        }

        if (
                request.type() == null
                        || request.type().isBlank()
        ) {
            throw new IllegalArgumentException(
                    "Ambulance type cannot be empty"
            );
        }


        String normalizedStatus =
                normalizeStatus(
                        request.status()
                );


        Ambulance ambulance =
                Ambulance.builder()

                        .ambulanceNumber(
                                request.ambulanceNumber().trim()
                        )

                        .latitude(
                                request.latitude()
                        )

                        .longitude(
                                request.longitude()
                        )

                        .status(
                                normalizedStatus
                        )

                        .type(
                                request.type()
                                        .trim()
                                        .toUpperCase(Locale.ROOT)
                        )

                        .build();


        Ambulance savedAmbulance =
                ambulanceRepository.save(
                        ambulance
                );


        return mapToResponse(
                savedAmbulance
        );
    }


    // =========================================================
    // GET ALL AMBULANCES
    // =========================================================

    public List<AmbulanceResponse>
    getAllAmbulances() {

        return ambulanceRepository
                .findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }


    // =========================================================
    // GET AMBULANCE BY ID
    // =========================================================

    public AmbulanceResponse getAmbulanceById(
            Long id
    ) {

        Ambulance ambulance =
                ambulanceRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new AmbulanceNotFoundException(
                                        id
                                )
                        );


        return mapToResponse(
                ambulance
        );
    }


    // =========================================================
    // UPDATE AMBULANCE STATUS
    // =========================================================
    //
    // Only valid lifecycle transitions are allowed.
    //
    // AVAILABLE
    //     ↓
    // EN_ROUTE
    //     ↓
    // AT_EMERGENCY
    //     ↓
    // TO_HOSPITAL
    //     ↓
    // AVAILABLE
    //
    // Returning directly to AVAILABLE is also allowed from an
    // active state because a completed dispatch releases the
    // ambulance.
    //
    // =========================================================

    public AmbulanceResponse updateStatus(
            Long id,
            String status
    ) {

        Ambulance ambulance =
                ambulanceRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new AmbulanceNotFoundException(
                                        id
                                )
                        );


        if (
                status == null
                        || status.isBlank()
        ) {

            throw new IllegalArgumentException(
                    "Ambulance status cannot be empty"
            );
        }


        String normalizedStatus =
                normalizeStatus(status);


        String currentStatus =
                normalizeExistingStatus(
                        ambulance.getStatus()
                );


        if (
                currentStatus.equals(
                        normalizedStatus
                )
        ) {

            /*
             * Setting the same status again is harmless and
             * idempotent. This is useful for frontend refreshes
             * or repeated API requests.
             */
            return mapToResponse(
                    ambulance
            );
        }


        if (
                !isValidTransition(
                        currentStatus,
                        normalizedStatus
                )
        ) {

            throw new IllegalArgumentException(
                    "Invalid ambulance status transition: "
                            + currentStatus
                            + " -> "
                            + normalizedStatus
            );
        }


        ambulance.setStatus(
                normalizedStatus
        );


        Ambulance updatedAmbulance =
                ambulanceRepository.save(
                        ambulance
                );


        return mapToResponse(
                updatedAmbulance
        );
    }


    // =========================================================
    // NORMALIZE STATUS
    // =========================================================

    private String normalizeStatus(
            String status
    ) {

        String normalized =
                status
                        .trim()
                        .toUpperCase(Locale.ROOT);


        if (!VALID_STATUSES.contains(normalized)) {

            throw new IllegalArgumentException(
                    "Invalid ambulance status: "
                            + status
                            + ". Allowed statuses: "
                            + VALID_STATUSES
            );
        }


        return normalized;
    }


    // =========================================================
    // NORMALIZE EXISTING ENTITY STATUS
    // =========================================================

    private String normalizeExistingStatus(
            String status
    ) {

        if (
                status == null
                        || status.isBlank()
        ) {

            throw new IllegalArgumentException(
                    "Existing ambulance status cannot be empty"
            );
        }


        String normalized =
                status
                        .trim()
                        .toUpperCase(Locale.ROOT);


        if (!VALID_STATUSES.contains(normalized)) {

            throw new IllegalArgumentException(
                    "Invalid existing ambulance status: "
                            + status
            );
        }


        return normalized;
    }


    // =========================================================
    // VALIDATE STATUS TRANSITION
    // =========================================================

    private boolean isValidTransition(
            String currentStatus,
            String newStatus
    ) {

        /*
         * AVAILABLE → EN_ROUTE
         */
        if (
                currentStatus.equals("AVAILABLE")
                        && newStatus.equals("EN_ROUTE")
        ) {
            return true;
        }


        /*
         * EN_ROUTE → AT_EMERGENCY
         */
        if (
                currentStatus.equals("EN_ROUTE")
                        && newStatus.equals("AT_EMERGENCY")
        ) {
            return true;
        }


        /*
         * AT_EMERGENCY → TO_HOSPITAL
         */
        if (
                currentStatus.equals("AT_EMERGENCY")
                        && newStatus.equals("TO_HOSPITAL")
        ) {
            return true;
        }


        /*
         * TO_HOSPITAL → AVAILABLE
         */
        if (
                currentStatus.equals("TO_HOSPITAL")
                        && newStatus.equals("AVAILABLE")
        ) {
            return true;
        }


        /*
         * An active ambulance can be released directly to
         * AVAILABLE when a dispatch is completed/cancelled.
         *
         * This also keeps the manual status endpoint compatible
         * with the dispatch lifecycle.
         */
        if (
                (
                        currentStatus.equals("EN_ROUTE")
                                || currentStatus.equals("AT_EMERGENCY")
                                || currentStatus.equals("TO_HOSPITAL")
                )
                        && newStatus.equals("AVAILABLE")
        ) {
            return true;
        }


        return false;
    }


    // =========================================================
    // MAP ENTITY → RESPONSE
    // =========================================================

    private AmbulanceResponse mapToResponse(
            Ambulance ambulance
    ) {

        return new AmbulanceResponse(

                ambulance.getId(),

                ambulance.getAmbulanceNumber(),

                ambulance.getLatitude(),

                ambulance.getLongitude(),

                ambulance.getStatus(),

                ambulance.getType()
        );
    }
}