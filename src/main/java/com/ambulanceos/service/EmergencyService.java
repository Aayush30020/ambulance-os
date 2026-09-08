package com.ambulanceos.service;

import com.ambulanceos.dto.EmergencyRequest;
import com.ambulanceos.dto.EmergencyResponse;
import com.ambulanceos.entity.Emergency;
import com.ambulanceos.repository.EmergencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmergencyService {

    private final EmergencyRepository emergencyRepository;

    public EmergencyResponse createEmergency(EmergencyRequest request) {

        Emergency emergency = Emergency.builder()
                .location(request.location())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .priority(request.priority())
                .facility(request.facility())
                .notes(request.notes())
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .build();

        Emergency savedEmergency =
                emergencyRepository.save(emergency);

        return mapToResponse(savedEmergency);
    }

    public List<EmergencyResponse> getAllEmergencies() {
        return emergencyRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public EmergencyResponse getEmergencyById(Long id) {

        Emergency emergency =
                emergencyRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Emergency not found with id: "
                                                + id
                                )
                        );

        return mapToResponse(emergency);
    }

    public EmergencyResponse updateStatus(
            Long id,
            String status
    ) {

        Emergency emergency =
                emergencyRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Emergency not found with id: "
                                                + id
                                )
                        );

        emergency.setStatus(status);

        Emergency updatedEmergency =
                emergencyRepository.save(emergency);

        return mapToResponse(updatedEmergency);
    }

    private EmergencyResponse mapToResponse(Emergency emergency) {
        return new EmergencyResponse(
                emergency.getId(),
                emergency.getLocation(),
                emergency.getLatitude(),
                emergency.getLongitude(),
                emergency.getPriority(),
                emergency.getFacility(),
                emergency.getNotes(),
                emergency.getStatus(),
                emergency.getCreatedAt()
        );
    }
}
