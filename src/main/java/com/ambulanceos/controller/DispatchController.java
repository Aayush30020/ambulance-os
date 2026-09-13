package com.ambulanceos.controller;

import com.ambulanceos.dto.DispatchResponse;
import com.ambulanceos.service.DispatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dispatch")
@RequiredArgsConstructor
public class DispatchController {

    private final DispatchService dispatchService;


    // =========================================================
    // FIND BEST AMBULANCE
    // =========================================================
    //
    // GET
    // /api/dispatch/ambulance/{emergencyId}
    //
    // Finds the best AVAILABLE ambulance for an emergency.
    //
    // This endpoint only calculates the best ambulance.
    // It does NOT change the ambulance status.
    //
    // =========================================================

    @GetMapping("/ambulance/{emergencyId}")
    public DispatchResponse findBestAmbulance(
            @PathVariable Long emergencyId
    ) {

        return dispatchService.findBestAmbulance(
                emergencyId
        );
    }


    // =========================================================
    // DISPATCH AMBULANCE
    // =========================================================
    //
    // POST
    // /api/dispatch/{emergencyId}
    //
    // Finds the best available ambulance and changes:
    //
    // AVAILABLE
    //      ↓
    // EN_ROUTE
    //
    // =========================================================

    @PostMapping("/{emergencyId}")
    public DispatchResponse dispatchAmbulance(
            @PathVariable Long emergencyId
    ) {

        return dispatchService.dispatchAmbulance(
                emergencyId
        );
    }
}