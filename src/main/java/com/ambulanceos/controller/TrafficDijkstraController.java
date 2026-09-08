package com.ambulanceos.controller;

import com.ambulanceos.dto.TrafficDijkstraResponse;
import com.ambulanceos.service.TrafficAwareDijkstraService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/traffic-dijkstra")
@RequiredArgsConstructor
public class TrafficDijkstraController {

    private final TrafficAwareDijkstraService trafficAwareDijkstraService;

    @GetMapping("/{sourceNode}/{destinationNode}")
    public TrafficDijkstraResponse findShortestPath(
            @PathVariable String sourceNode,
            @PathVariable String destinationNode
    ) {

        return trafficAwareDijkstraService.findShortestPath(
                sourceNode,
                destinationNode
        );
    }
}