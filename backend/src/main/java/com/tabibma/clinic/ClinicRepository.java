package com.tabibma.clinic;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ClinicRepository extends JpaRepository<Clinic, UUID> {

    Optional<Clinic> findByAdminUserId(UUID adminUserId);

    boolean existsByAdminUserId(UUID adminUserId);
}
