package com.ambulanceos.repository;

import com.ambulanceos.entity.Ambulance;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AmbulanceRepository
        extends JpaRepository<Ambulance, Long> {
}