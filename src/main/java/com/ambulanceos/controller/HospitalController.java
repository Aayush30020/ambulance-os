package com.ambulanceos.controller;

import com.ambulanceos.dto.HospitalRequest;
import com.ambulanceos.dto.HospitalResponse;
import com.ambulanceos.service.HospitalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hospitals")
@RequiredArgsConstructor
public class HospitalController {

    private final HospitalService hospitalService;


    // =========================================================
    // CREATE HOSPITAL
    // =========================================================

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HospitalResponse createHospital(

            @Valid
            @RequestBody
            HospitalRequest request

    ) {

        return hospitalService.createHospital(
                request
        );
    }


    // =========================================================
    // GET ALL HOSPITALS
    // =========================================================

    @GetMapping
    public List<HospitalResponse> getAllHospitals() {

        return hospitalService.getAllHospitals();
    }


    // =========================================================
    // GET HOSPITAL BY ID
    // =========================================================

    @GetMapping("/{id}")
    public HospitalResponse getHospitalById(

            @PathVariable
            Long id

    ) {

        return hospitalService.getHospitalById(
                id
        );
    }


    // =========================================================
    // UPDATE AVAILABLE BEDS
    // =========================================================

    @PutMapping("/{id}/beds")
    public HospitalResponse updateBeds(

            @PathVariable
            Long id,

            @RequestParam
            Integer availableBeds

    ) {

        return hospitalService.updateBeds(
                id,
                availableBeds
        );
    }
}