package com.ambulanceos.repository;

import com.ambulanceos.entity.Hospital;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HospitalRepository
        extends JpaRepository<Hospital, Long> {
}