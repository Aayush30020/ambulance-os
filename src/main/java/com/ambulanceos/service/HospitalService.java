package com.ambulanceos.service;

import com.ambulanceos.dto.HospitalRequest;
import com.ambulanceos.dto.HospitalResponse;
import com.ambulanceos.entity.Hospital;
import com.ambulanceos.exception.HospitalNotFoundException;
import com.ambulanceos.repository.HospitalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class HospitalService {

    private final HospitalRepository hospitalRepository;


    // =========================================================
    // CREATE HOSPITAL
    // =========================================================

    public HospitalResponse createHospital(
            HospitalRequest request
    ) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Hospital request cannot be null"
            );
        }

        validateText(
                request.hospitalCode(),
                "Hospital code cannot be empty"
        );

        validateText(
                request.name(),
                "Hospital name cannot be empty"
        );

        validateText(
                request.facilityType(),
                "Facility type cannot be empty"
        );

        validateAvailableBeds(
                request.availableBeds()
        );

        Hospital hospital =
                Hospital.builder()
                        .hospitalCode(
                                request.hospitalCode()
                                        .trim()
                                        .toUpperCase(Locale.ROOT)
                        )
                        .name(
                                request.name().trim()
                        )
                        .latitude(
                                request.latitude()
                        )
                        .longitude(
                                request.longitude()
                        )
                        .availableBeds(
                                request.availableBeds()
                        )
                        .facilityType(
                                request.facilityType()
                                        .trim()
                                        .toUpperCase(Locale.ROOT)
                        )
                        .build();

        Hospital savedHospital =
                hospitalRepository.save(hospital);

        return mapToResponse(savedHospital);
    }


    // =========================================================
    // GET ALL HOSPITALS
    // =========================================================

    public List<HospitalResponse> getAllHospitals() {

        return hospitalRepository
                .findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }


    // =========================================================
    // GET HOSPITAL BY ID
    // =========================================================

    public HospitalResponse getHospitalById(
            Long id
    ) {

        Hospital hospital =
                hospitalRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new HospitalNotFoundException(id)
                        );

        return mapToResponse(hospital);
    }


    // =========================================================
    // UPDATE AVAILABLE BEDS
    // =========================================================

    public HospitalResponse updateBeds(
            Long id,
            Integer availableBeds
    ) {

        Hospital hospital =
                hospitalRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new HospitalNotFoundException(id)
                        );

        validateAvailableBeds(
                availableBeds
        );

        int currentBeds =
                hospital.getAvailableBeds();

        // -----------------------------------------------------
        // Idempotent update.
        //
        // If the requested capacity is already stored,
        // there is no need to perform another database save.
        // -----------------------------------------------------

        if (currentBeds == availableBeds) {
            return mapToResponse(hospital);
        }

        hospital.setAvailableBeds(
                availableBeds
        );

        Hospital updatedHospital =
                hospitalRepository.save(hospital);

        return mapToResponse(updatedHospital);
    }


    // =========================================================
    // VALIDATE AVAILABLE BEDS
    // =========================================================

    private void validateAvailableBeds(
            Integer availableBeds
    ) {

        if (availableBeds == null) {
            throw new IllegalArgumentException(
                    "Available beds cannot be null"
            );
        }

        if (availableBeds < 0) {
            throw new IllegalArgumentException(
                    "Available beds cannot be negative"
            );
        }
    }


    // =========================================================
    // VALIDATE TEXT FIELD
    // =========================================================

    private void validateText(
            String value,
            String message
    ) {

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }


    // =========================================================
    // ENTITY → RESPONSE DTO
    // =========================================================

    private HospitalResponse mapToResponse(
            Hospital hospital
    ) {

        return new HospitalResponse(
                hospital.getId(),
                hospital.getHospitalCode(),
                hospital.getName(),
                hospital.getLatitude(),
                hospital.getLongitude(),
                hospital.getAvailableBeds(),
                hospital.getFacilityType()
        );
    }
}