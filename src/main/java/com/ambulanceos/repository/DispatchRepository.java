package com.ambulanceos.repository;

import com.ambulanceos.entity.Dispatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DispatchRepository
        extends JpaRepository<Dispatch, Long> {

    List<Dispatch> findAllByOrderByDispatchedAtDesc();

    Optional<Dispatch>
    findFirstByEmergencyIdAndStatusOrderByDispatchedAtDesc(
            Long emergencyId,
            String status
    );

    List<Dispatch>
    findByStatusIgnoreCaseAndDispatchedAtBefore(
            String status,
            LocalDateTime cutoff
    );
}