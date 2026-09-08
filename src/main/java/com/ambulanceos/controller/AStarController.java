package com.ambulanceos.controller;

import com.ambulanceos.dto.TrafficDijkstraResponse;
import com.ambulanceos.service.AStarService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/astar")
@RequiredArgsConstructor
public class AStarController {

    private final AStarService aStarService;

    @GetMapping("/{sourceNode}/{destinationNode}")
    public TrafficDijkstraResponse findShortestPath(
            @PathVariable String sourceNode,
            @PathVariable String destinationNode
    ) {
        return aStarService.findShortestPath(
                sourceNode,
                destinationNode
        );
    }
}