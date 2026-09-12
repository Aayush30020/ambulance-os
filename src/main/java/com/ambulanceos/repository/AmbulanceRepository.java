package com.ambulanceos.repository;

import com.ambulanceos.entity.Ambulance;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;

public interface AmbulanceRepository
        extends JpaRepository<Ambulance, Long> {

    /*
     * Locks AVAILABLE ambulance rows at the database level.
     *
     * This prevents two concurrent dispatch requests from
     * selecting the same ambulance at the same time.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Ambulance> findByStatusIgnoreCase(String status);
}