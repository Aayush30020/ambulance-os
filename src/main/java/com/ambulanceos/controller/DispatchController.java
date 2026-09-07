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

    @PostMapping("/{emergencyId}")
    public DispatchResponse dispatchAmbulance(
            @PathVariable Long emergencyId
    ) {

        return dispatchService.dispatchAmbulance(
                emergencyId
        );
    }
}