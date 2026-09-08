package com.ambulanceos.controller;

import com.ambulanceos.dto.AlgorithmComparisonResponse;
import com.ambulanceos.service.AlgorithmComparisonService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/algorithm-comparison")
@RequiredArgsConstructor
public class AlgorithmComparisonController {

    private final AlgorithmComparisonService algorithmComparisonService;

    @GetMapping("/{sourceNode}/{destinationNode}")
    public AlgorithmComparisonResponse compareAlgorithms(
            @PathVariable String sourceNode,
            @PathVariable String destinationNode
    ) {

        return algorithmComparisonService.compare(
                sourceNode,
                destinationNode
        );
    }
}