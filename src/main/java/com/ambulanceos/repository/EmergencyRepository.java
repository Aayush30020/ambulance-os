package com.ambulanceos.repository;

import com.ambulanceos.entity.Emergency;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmergencyRepository extends JpaRepository<Emergency, Long> {
}