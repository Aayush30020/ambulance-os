package com.ambulanceos.service;

import com.ambulanceos.entity.SystemSetting;
import com.ambulanceos.repository.SystemSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class RoutingSettingsService {

    private static final String ROUTING_ALGORITHM_KEY =
            "ROUTING_ALGORITHM";

    private final SystemSettingRepository systemSettingRepository;


    // =========================================================
    // GET CURRENT ROUTING ALGORITHM
    // =========================================================

    @Transactional(readOnly = true)
    public RoutingAlgorithm getRoutingAlgorithm() {

        return systemSettingRepository
                .findBySettingKey(
                        ROUTING_ALGORITHM_KEY
                )
                .map(setting ->
                        parseAlgorithm(
                                setting.getSettingValue()
                        )
                )
                .orElse(
                        RoutingAlgorithm.DIJKSTRA
                );
    }


    // =========================================================
    // UPDATE ROUTING ALGORITHM
    // =========================================================

    @Transactional
    public RoutingAlgorithm setRoutingAlgorithm(
            RoutingAlgorithm algorithm
    ) {

        if (algorithm == null) {

            throw new IllegalArgumentException(
                    "Routing algorithm cannot be null"
            );
        }


        SystemSetting setting =
                systemSettingRepository
                        .findBySettingKey(
                                ROUTING_ALGORITHM_KEY
                        )
                        .orElseGet(() ->
                                SystemSetting.builder()
                                        .settingKey(
                                                ROUTING_ALGORITHM_KEY
                                        )
                                        .build()
                        );


        setting.setSettingValue(
                algorithm.name()
        );


        systemSettingRepository.save(setting);


        return algorithm;
    }


    // =========================================================
    // PARSE STORED VALUE
    // =========================================================

    private RoutingAlgorithm parseAlgorithm(
            String value
    ) {

        if (value == null || value.isBlank()) {

            return RoutingAlgorithm.DIJKSTRA;
        }


        try {

            return RoutingAlgorithm.valueOf(
                    value.trim()
                            .toUpperCase(Locale.ROOT)
            );

        } catch (IllegalArgumentException exception) {

            // -------------------------------------------------
            // Invalid persisted configuration falls back to the
            // safe production default.
            // -------------------------------------------------

            return RoutingAlgorithm.DIJKSTRA;
        }
    }
}