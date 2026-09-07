package com.ambulanceos.controller;

import com.ambulanceos.dto.RouteResponse;
import com.ambulanceos.service.RouteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/routes")
@RequiredArgsConstructor
public class RouteController {

    private final RouteService routeService;


    // =========================================================
    // AMBULANCE → HOSPITAL ROUTE
    // =========================================================

    @GetMapping("/ambulance/{ambulanceId}/hospital/{hospitalId}")
    public RouteResponse findRoute(

            @PathVariable Long ambulanceId,

            @PathVariable Long hospitalId

    ) {

        return routeService.findRoute(
                ambulanceId,
                hospitalId
        );
    }
}