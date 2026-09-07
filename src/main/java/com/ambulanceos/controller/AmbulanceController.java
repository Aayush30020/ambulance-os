package com.ambulanceos.controller;

import com.ambulanceos.dto.AmbulanceRequest;
import com.ambulanceos.dto.AmbulanceResponse;
import com.ambulanceos.service.AmbulanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ambulances")
@RequiredArgsConstructor
public class AmbulanceController {

    private final AmbulanceService ambulanceService;


    // =========================================================
    // CREATE AMBULANCE
    // =========================================================

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AmbulanceResponse createAmbulance(

            @Valid
            @RequestBody
            AmbulanceRequest request

    ) {

        return ambulanceService.createAmbulance(request);
    }


    // =========================================================
    // GET ALL AMBULANCES
    // =========================================================

    @GetMapping
    public List<AmbulanceResponse> getAllAmbulances() {

        return ambulanceService.getAllAmbulances();
    }


    // =========================================================
    // GET AMBULANCE BY ID
    // =========================================================

    @GetMapping("/{id}")
    public AmbulanceResponse getAmbulanceById(

            @PathVariable
            Long id

    ) {

        return ambulanceService.getAmbulanceById(id);
    }


    // =========================================================
    // UPDATE STATUS
    // =========================================================

    @PutMapping("/{id}/status")
    public AmbulanceResponse updateStatus(

            @PathVariable
            Long id,

            @RequestParam
            String status

    ) {

        return ambulanceService.updateStatus(
                id,
                status
        );
    }
}