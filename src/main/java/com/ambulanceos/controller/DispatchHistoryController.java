package com.ambulanceos.controller;

import com.ambulanceos.entity.Dispatch;
import com.ambulanceos.exception.DispatchNotFoundException;
import com.ambulanceos.repository.DispatchRepository;
import com.ambulanceos.service.DispatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dispatches")
@RequiredArgsConstructor
public class DispatchHistoryController {

    private final DispatchRepository dispatchRepository;

    private final DispatchService dispatchService;


    // =========================================================
    // GET ALL DISPATCH HISTORY
    // =========================================================
    //
    // GET
    // /api/dispatches
    //
    // Returns dispatch records ordered from newest to oldest.
    //
    // =========================================================

    @GetMapping
    public List<Dispatch> getAllDispatches() {

        return dispatchRepository
                .findAllByOrderByDispatchedAtDesc();
    }


    // =========================================================
    // GET DISPATCH BY ID
    // =========================================================
    //
    // GET
    // /api/dispatches/{id}
    //
    // =========================================================

    @GetMapping("/{id}")
    public Dispatch getDispatchById(
            @PathVariable Long id
    ) {

        return dispatchRepository
                .findById(id)
                .orElseThrow(() ->
                        new DispatchNotFoundException(id)
                );
    }


    // =========================================================
    // COMPLETE DISPATCH
    // =========================================================
    //
    // PUT
    // /api/dispatches/{id}/complete
    //
    // Changes:
    //
    // IN_PROGRESS
    //      ↓
    // COMPLETED
    //
    // Ambulance:
    //
    // EN_ROUTE
    //      ↓
    // AVAILABLE
    //
    // The state changes are handled by DispatchService.
    //
    // =========================================================

    @PutMapping("/{id}/complete")
    public Dispatch completeDispatch(
            @PathVariable Long id
    ) {

        return dispatchService.completeDispatch(
                id
        );
    }
}