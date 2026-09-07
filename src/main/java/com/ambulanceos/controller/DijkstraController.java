package com.ambulanceos.controller;

import com.ambulanceos.dto.DijkstraResponse;
import com.ambulanceos.service.DijkstraService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/routes")
@RequiredArgsConstructor
public class DijkstraController {

    private final DijkstraService dijkstraService;


    // =========================================================
    // FIND SHORTEST PATH
    // =========================================================

    @GetMapping("/dijkstra")
    public DijkstraResponse findShortestPath(

            @RequestParam String source,

            @RequestParam String destination

    ) {

        return dijkstraService.findShortestPath(
                source,
                destination
        );
    }
}