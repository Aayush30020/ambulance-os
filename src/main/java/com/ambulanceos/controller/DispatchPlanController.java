package com.ambulanceos.controller;

import com.ambulanceos.dto.DispatchPlanResponse;
import com.ambulanceos.service.DispatchPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dispatch-plan")
@RequiredArgsConstructor
public class DispatchPlanController {

    private final DispatchPlanService dispatchPlanService;


    // =========================================================
    // CREATE COMPLETE DISPATCH PLAN
    // =========================================================

    @PostMapping("/{emergencyId}")
    public DispatchPlanResponse createDispatchPlan(
            @PathVariable Long emergencyId
    ) {

        return dispatchPlanService.createDispatchPlan(
                emergencyId
        );
    }
}