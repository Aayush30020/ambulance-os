package com.ambulanceos.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class SystemController {

    @GetMapping("/")
    public Map<String, Object> getSystemStatus() {
        return Map.of(
                "application", "AmbulanceOS",
                "status", "UP",
                "message", "Emergency ambulance routing platform is running"
        );
    }
}