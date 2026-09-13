package com.ambulanceos.service;

import com.ambulanceos.dto.TrafficDijkstraResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoutingService {

    private final TrafficAwareDijkstraService
            trafficAwareDijkstraService;

    private final AStarService
            aStarService;

    private final RoutingSettingsService
            routingSettingsService;


    // =========================================================
    // FIND ROUTE USING CURRENT SYSTEM SETTING
    // =========================================================
    //
    // Production services should normally use this method.
    //
    // RoutingService owns the responsibility of determining
    // which routing algorithm is currently configured.
    //
    // =========================================================

    public TrafficDijkstraResponse findRoute(
            String sourceNode,
            String destinationNode
    ) {

        RoutingAlgorithm algorithm =
                routingSettingsService.getRoutingAlgorithm();

        return findRoute(
                sourceNode,
                destinationNode,
                algorithm
        );
    }


    // =========================================================
    // FIND ROUTE USING SPECIFIC ALGORITHM
    // =========================================================
    //
    // Used by:
    //
    // - Algorithm comparison
    // - Explicit dispatch routing
    // - Testing
    //
    // =========================================================

    public TrafficDijkstraResponse findRoute(
            String sourceNode,
            String destinationNode,
            RoutingAlgorithm algorithm
    ) {

        if (algorithm == null) {

            throw new IllegalArgumentException(
                    "Routing algorithm cannot be null"
            );
        }


        return switch (algorithm) {

            case DIJKSTRA ->
                    trafficAwareDijkstraService.findShortestPath(
                            sourceNode,
                            destinationNode
                    );

            case ASTAR ->
                    aStarService.findShortestPath(
                            sourceNode,
                            destinationNode
                    );
        };
    }


    // =========================================================
    // GET CURRENT ALGORITHM
    // =========================================================

    public RoutingAlgorithm getCurrentAlgorithm() {

        return routingSettingsService
                .getRoutingAlgorithm();
    }


    // =========================================================
    // CHANGE CURRENT ALGORITHM
    // =========================================================

    public RoutingAlgorithm setCurrentAlgorithm(
            RoutingAlgorithm algorithm
    ) {

        return routingSettingsService
                .setRoutingAlgorithm(algorithm);
    }
}