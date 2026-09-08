package com.ambulanceos.controller;

import com.ambulanceos.dto.TrafficDijkstraResponse;
import com.ambulanceos.service.TrafficAwareDijkstraService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/traffic-routes")
@RequiredArgsConstructor
public class TrafficRouteController {

    private final TrafficAwareDijkstraService
            trafficAwareDijkstraService;


    // =========================================================
    // TRAFFIC-AWARE SHORTEST ROUTE
    // =========================================================
    //
    // Example:
    //
    // GET /api/traffic-routes/2225581300/1567947792
    //
    // The algorithm uses:
    //
    // PriorityQueue
    //       +
    // Dijkstra
    //       +
    // Traffic multiplier
    //
    // to find the lowest-cost route.
    //
    // =========================================================

    @GetMapping("/{sourceNode}/{destinationNode}")
    public TrafficDijkstraResponse findTrafficAwareRoute(

            @PathVariable String sourceNode,

            @PathVariable String destinationNode

    ) {

        return trafficAwareDijkstraService.findShortestPath(

                sourceNode,

                destinationNode

        );
    }
}