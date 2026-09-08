package com.ambulanceos.controller;

import com.ambulanceos.dto.EmergencyRequest;
import com.ambulanceos.dto.EmergencyResponse;
import com.ambulanceos.service.EmergencyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/emergencies")
@RequiredArgsConstructor
public class EmergencyController {

    private final EmergencyService emergencyService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EmergencyResponse createEmergency(
            @Valid @RequestBody EmergencyRequest request
    ) {
        return emergencyService.createEmergency(request);
    }

    @GetMapping
    public List<EmergencyResponse> getAllEmergencies() {
        return emergencyService.getAllEmergencies();
    }

    @GetMapping("/{id}")
    public EmergencyResponse getEmergencyById(
            @PathVariable Long id
    ) {
        return emergencyService.getEmergencyById(id);
    }

    @PutMapping("/{id}/status")
    public EmergencyResponse updateStatus(
            @PathVariable Long id,
            @RequestParam String status
    ) {
        return emergencyService.updateStatus(
                id,
                status
        );
    }
}
