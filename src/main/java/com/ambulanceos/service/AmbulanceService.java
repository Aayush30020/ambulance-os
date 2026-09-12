package com.ambulanceos.service;

import com.ambulanceos.dto.AmbulanceRequest;
import com.ambulanceos.dto.AmbulanceResponse;
import com.ambulanceos.entity.Ambulance;
import com.ambulanceos.exception.AmbulanceNotFoundException;
import com.ambulanceos.repository.AmbulanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AmbulanceService {

    private final AmbulanceRepository ambulanceRepository;


    // =========================================================
    // CREATE AMBULANCE
    // =========================================================

    public AmbulanceResponse createAmbulance(
            AmbulanceRequest request
    ) {

        Ambulance ambulance =
                Ambulance.builder()

                        .ambulanceNumber(
                                request.ambulanceNumber()
                        )

                        .latitude(
                                request.latitude()
                        )

                        .longitude(
                                request.longitude()
                        )

                        .status(
                                request.status().toUpperCase()
                        )

                        .type(
                                request.type().toUpperCase()
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


        ambulance.setStatus(
                status.toUpperCase()
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