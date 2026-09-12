package com.ambulanceos.service;

import com.ambulanceos.dto.HospitalRequest;
import com.ambulanceos.dto.HospitalResponse;
import com.ambulanceos.entity.Hospital;
import com.ambulanceos.exception.HospitalNotFoundException;
import com.ambulanceos.repository.HospitalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

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

        Hospital hospital = Hospital.builder()

                .hospitalCode(
                        request.hospitalCode()
                )

                .name(
                        request.name()
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

        return hospitalRepository.findAll()

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
                hospitalRepository.findById(id)

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
                hospitalRepository.findById(id)

                        .orElseThrow(() ->
                                new HospitalNotFoundException(id)
                        );


        if (availableBeds < 0) {

            throw new IllegalArgumentException(
                    "Available beds cannot be negative"
            );

        }


        hospital.setAvailableBeds(
                availableBeds
        );


        Hospital updatedHospital =
                hospitalRepository.save(hospital);


        return mapToResponse(updatedHospital);
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