package com.ambulanceos.controller;

import com.ambulanceos.service.RoutingAlgorithm;
import com.ambulanceos.service.RoutingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings/routing")
@RequiredArgsConstructor
public class RoutingSettingsController {

    private final RoutingService routingService;


    // =========================================================
    // GET CURRENT ALGORITHM
    // =========================================================

    @GetMapping
    public RoutingAlgorithm getRoutingAlgorithm() {

        return routingService
                .getCurrentAlgorithm();
    }


    // =========================================================
    // UPDATE ALGORITHM
    // =========================================================

    @PutMapping
    public RoutingAlgorithm updateRoutingAlgorithm(
            @RequestParam RoutingAlgorithm algorithm
    ) {

        return routingService
                .setCurrentAlgorithm(
                        algorithm
                );
    }
}