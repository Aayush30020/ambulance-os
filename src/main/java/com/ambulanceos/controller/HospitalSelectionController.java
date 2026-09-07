package com.ambulanceos.controller;

import com.ambulanceos.dto.HospitalSelectionResponse;
import com.ambulanceos.service.HospitalSelectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/hospital-selection")
@RequiredArgsConstructor
public class HospitalSelectionController {

    private final HospitalSelectionService hospitalSelectionService;


    // =========================================================
    // FIND BEST HOSPITAL
    // =========================================================

    @GetMapping("/{emergencyId}")
    public HospitalSelectionResponse findBestHospital(
            @PathVariable Long emergencyId
    ) {

        return hospitalSelectionService.findBestHospital(
                emergencyId
        );
    }
}