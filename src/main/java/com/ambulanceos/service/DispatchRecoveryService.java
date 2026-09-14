package com.ambulanceos.service;

import com.ambulanceos.entity.Dispatch;
import com.ambulanceos.repository.DispatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DispatchRecoveryService {

    private final DispatchRepository dispatchRepository;

    private final DispatchService dispatchService;

    @Value("${dispatch.recovery.stale-after-minutes:30}")
    private long staleAfterMinutes;

    // =========================================================
    // STALE DISPATCH RECOVERY
    // =========================================================
    //
    // Runs every 5 minutes.
    //
    // Any IN_PROGRESS dispatch older than the configured
    // recovery threshold is treated as abandoned and completed
    // through DispatchService.
    //
    // DispatchService performs the actual transactional cleanup:
    //
    //     Dispatch   → COMPLETED
    //     Emergency  → COMPLETED
    //     Ambulance  → AVAILABLE
    //
    // =========================================================

    @Scheduled(
            fixedDelayString = "${dispatch.recovery.interval-ms:300000}",
            initialDelayString = "${dispatch.recovery.initial-delay-ms:60000}"
    )
    public void recoverStaleDispatches() {

        LocalDateTime cutoff =
                LocalDateTime.now()
                        .minusMinutes(
                                staleAfterMinutes
                        );

        List<Dispatch> staleDispatches =
                dispatchRepository
                        .findByStatusIgnoreCaseAndDispatchedAtBefore(
                                "IN_PROGRESS",
                                cutoff
                        );

        if (staleDispatches.isEmpty()) {
            return;
        }

        log.warn(
                "Found {} stale IN_PROGRESS dispatch(es) older than {} minutes",
                staleDispatches.size(),
                staleAfterMinutes
        );

        for (Dispatch dispatch :
                staleDispatches) {

            try {

                log.warn(
                        "Recovering stale dispatch #{} " +
                                "(Emergency #{}, Ambulance {})",
                        dispatch.getId(),
                        dispatch.getEmergencyId(),
                        dispatch.getAmbulanceNumber()
                );

                dispatchService.completeDispatch(
                        dispatch.getId()
                );

                log.info(
                        "Successfully recovered dispatch #{}",
                        dispatch.getId()
                );

            } catch (Exception exception) {

                log.error(
                        "Failed to recover stale dispatch #{}: {}",
                        dispatch.getId(),
                        exception.getMessage(),
                        exception
                );
            }
        }
    }
}