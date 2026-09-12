package com.ambulanceos.service;

import com.ambulanceos.dto.DispatchResponse;
import com.ambulanceos.entity.Ambulance;
import com.ambulanceos.entity.Emergency;
import com.ambulanceos.exception.NoAvailableAmbulanceException;
import com.ambulanceos.repository.AmbulanceRepository;
import com.ambulanceos.repository.EmergencyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class DispatchConcurrencyIntegrationTest {

    @Autowired
    private DispatchService dispatchService;

    @Autowired
    private AmbulanceRepository ambulanceRepository;

    @Autowired
    private EmergencyRepository emergencyRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;


    // =========================================================
    // CONCURRENT DISPATCH TEST
    // =========================================================

    @Test
    void shouldNotAssignSameAmbulanceToConcurrentDispatches()
            throws Exception {

        /*
         * Store the original status of every existing ambulance.
         *
         * We temporarily make all existing ambulances unavailable
         * so that the test has exactly ONE AVAILABLE ambulance.
         */
        Map<Long, String> originalStatuses =
                new HashMap<>();

        List<Ambulance> existingAmbulances =
                ambulanceRepository.findAll();

        for (Ambulance ambulance :
                existingAmbulances) {

            originalStatuses.put(
                    ambulance.getId(),
                    ambulance.getStatus()
            );
        }


        Ambulance testAmbulance = null;

        Emergency emergencyOne = null;

        Emergency emergencyTwo = null;

        ExecutorService executor =
                Executors.newFixedThreadPool(2);


        try {

            // -------------------------------------------------
            // 1. Temporarily make existing ambulances
            //    unavailable.
            // -------------------------------------------------

            for (Ambulance ambulance :
                    existingAmbulances) {

                ambulance.setStatus("EN_ROUTE");
            }

            ambulanceRepository.saveAllAndFlush(
                    existingAmbulances
            );


            // -------------------------------------------------
            // 2. Create exactly ONE available ambulance.
            // -------------------------------------------------

            testAmbulance =
                    ambulanceRepository.saveAndFlush(
                            Ambulance.builder()
                                    .ambulanceNumber(
                                            "CONCURRENCY-AMB-001"
                                    )
                                    .latitude(28.4598)
                                    .longitude(77.0268)
                                    .status("AVAILABLE")
                                    .type("ALS")
                                    .build()
                    );


            // -------------------------------------------------
            // 3. Create first emergency.
            // -------------------------------------------------

            emergencyOne =
                    emergencyRepository.saveAndFlush(
                            Emergency.builder()
                                    .location(
                                            "Concurrency Emergency 1"
                                    )
                                    .latitude(28.4595)
                                    .longitude(77.0266)
                                    .priority("HIGH")
                                    .facility("TRAUMA")
                                    .notes(
                                            "Concurrency integration test 1"
                                    )
                                    .status("ACTIVE")
                                    .createdAt(
                                            LocalDateTime.now()
                                    )
                                    .build()
                    );

            final Long emergencyOneId =
                    emergencyOne.getId();


            // -------------------------------------------------
            // 4. Create second emergency.
            // -------------------------------------------------

            emergencyTwo =
                    emergencyRepository.saveAndFlush(
                            Emergency.builder()
                                    .location(
                                            "Concurrency Emergency 2"
                                    )
                                    .latitude(28.4605)
                                    .longitude(77.0276)
                                    .priority("HIGH")
                                    .facility("TRAUMA")
                                    .notes(
                                            "Concurrency integration test 2"
                                    )
                                    .status("ACTIVE")
                                    .createdAt(
                                            LocalDateTime.now()
                                    )
                                    .build()
                    );

            final Long emergencyTwoId =
                    emergencyTwo.getId();


            // -------------------------------------------------
            // 5. Verify that exactly ONE ambulance is available.
            // -------------------------------------------------
            //
            // IMPORTANT:
            //
            // Do NOT use findByStatusIgnoreCase() here.
            //
            // That repository method has PESSIMISTIC_WRITE and
            // therefore requires an active transaction.
            //
            // findAll() does not acquire a pessimistic lock.
            //

            long availableCount =
                    ambulanceRepository
                            .findAll()
                            .stream()
                            .filter(ambulance ->
                                    "AVAILABLE".equalsIgnoreCase(
                                            ambulance.getStatus()
                                    )
                            )
                            .count();


            assertEquals(
                    1,
                    availableCount,
                    "Concurrency test must have exactly one available ambulance"
            );


            // -------------------------------------------------
            // 6. Synchronization barrier.
            // -------------------------------------------------
            //
            // Both worker threads wait here before starting
            // their database transactions.
            //

            CountDownLatch startLatch =
                    new CountDownLatch(1);


            // -------------------------------------------------
            // 7. First concurrent dispatch.
            // -------------------------------------------------

            Future<DispatchResponse> firstResult =
                    executor.submit(() -> {

                        startLatch.await();

                        return transactionTemplate.execute(
                                status ->
                                        dispatchService
                                                .dispatchAmbulance(
                                                        emergencyOneId
                                                )
                        );
                    });


            // -------------------------------------------------
            // 8. Second concurrent dispatch.
            // -------------------------------------------------

            Future<DispatchResponse> secondResult =
                    executor.submit(() -> {

                        startLatch.await();

                        return transactionTemplate.execute(
                                status ->
                                        dispatchService
                                                .dispatchAmbulance(
                                                        emergencyTwoId
                                                )
                        );
                    });


            // -------------------------------------------------
            // 9. Release both requests simultaneously.
            // -------------------------------------------------

            startLatch.countDown();


            // -------------------------------------------------
            // 10. Collect first result.
            // -------------------------------------------------

            DispatchResponse firstResponse =
                    null;

            DispatchResponse secondResponse =
                    null;

            Throwable firstException =
                    null;

            Throwable secondException =
                    null;


            try {

                firstResponse =
                        firstResult.get(
                                60,
                                TimeUnit.SECONDS
                        );

            } catch (Exception exception) {

                firstException =
                        unwrapException(
                                exception
                        );
            }


            // -------------------------------------------------
            // 11. Collect second result.
            // -------------------------------------------------

            try {

                secondResponse =
                        secondResult.get(
                                60,
                                TimeUnit.SECONDS
                        );

            } catch (Exception exception) {

                secondException =
                        unwrapException(
                                exception
                        );
            }


            // -------------------------------------------------
            // 12. Exactly ONE request must succeed.
            // -------------------------------------------------

            int successfulDispatches =
                    0;

            if (firstResponse != null) {
                successfulDispatches++;
            }

            if (secondResponse != null) {
                successfulDispatches++;
            }


            assertEquals(
                    1,
                    successfulDispatches,
                    "Exactly one concurrent dispatch should claim the only available ambulance"
            );


            // -------------------------------------------------
            // 13. Exactly ONE request must fail.
            // -------------------------------------------------

            Throwable failedException =
                    firstResponse == null
                            ? firstException
                            : secondException;


            assertNotNull(
                    failedException,
                    "The second concurrent dispatch should fail"
            );


            assertTrue(
                    containsNoAvailableAmbulanceException(
                            failedException
                    ),
                    "The failed dispatch should result in NoAvailableAmbulanceException"
            );


            // -------------------------------------------------
            // 14. Get successful response.
            // -------------------------------------------------

            DispatchResponse successfulResponse =
                    firstResponse != null
                            ? firstResponse
                            : secondResponse;


            assertNotNull(
                    successfulResponse
            );


            // -------------------------------------------------
            // 15. Verify the only available ambulance was
            //     selected.
            // -------------------------------------------------

            assertEquals(
                    testAmbulance.getId(),
                    successfulResponse.ambulanceId()
            );


            assertEquals(
                    "EN_ROUTE",
                    successfulResponse.ambulanceStatus()
            );


            // -------------------------------------------------
            // 16. Verify final database state.
            // -------------------------------------------------

            Ambulance finalAmbulance =
                    ambulanceRepository
                            .findById(
                                    testAmbulance.getId()
                            )
                            .orElseThrow();


            assertEquals(
                    "EN_ROUTE",
                    finalAmbulance.getStatus()
            );

        } finally {

            // -------------------------------------------------
            // 17. Stop worker threads.
            // -------------------------------------------------

            executor.shutdownNow();

            executor.awaitTermination(
                    10,
                    TimeUnit.SECONDS
            );


            // -------------------------------------------------
            // 18. Delete test emergencies.
            // -------------------------------------------------

            List<Long> emergencyIds =
                    new ArrayList<>();


            if (emergencyOne != null) {

                emergencyIds.add(
                        emergencyOne.getId()
                );
            }


            if (emergencyTwo != null) {

                emergencyIds.add(
                        emergencyTwo.getId()
                );
            }


            if (!emergencyIds.isEmpty()) {

                emergencyRepository.deleteAllById(
                        emergencyIds
                );
            }


            // -------------------------------------------------
            // 19. Delete test ambulance.
            // -------------------------------------------------

            if (testAmbulance != null) {

                ambulanceRepository.deleteById(
                        testAmbulance.getId()
                );
            }


            // -------------------------------------------------
            // 20. Restore original ambulance statuses.
            // -------------------------------------------------

            List<Ambulance> ambulancesToRestore =
                    ambulanceRepository.findAll();


            for (Ambulance ambulance :
                    ambulancesToRestore) {

                String originalStatus =
                        originalStatuses.get(
                                ambulance.getId()
                        );


                if (originalStatus != null) {

                    ambulance.setStatus(
                            originalStatus
                    );
                }
            }


            ambulanceRepository.saveAllAndFlush(
                    ambulancesToRestore
            );
        }
    }


    // =========================================================
    // UNWRAP ASYNC EXCEPTION
    // =========================================================

    private Throwable unwrapException(
            Exception exception
    ) {

        Throwable cause =
                exception.getCause();


        if (cause == null) {

            return exception;
        }


        return cause;
    }


    // =========================================================
    // CHECK NO AVAILABLE AMBULANCE
    // =========================================================

    private boolean containsNoAvailableAmbulanceException(
            Throwable exception
    ) {

        Throwable current =
                exception;


        while (current != null) {

            if (
                    current
                            instanceof NoAvailableAmbulanceException
            ) {

                return true;
            }


            current =
                    current.getCause();
        }


        return false;
    }
}