package com.tabibma.clinic;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DoctorProfileRepository extends JpaRepository<DoctorProfile, UUID> {

    Optional<DoctorProfile> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);

    List<DoctorProfile> findAllByVerificationStatus(VerificationStatus status);
}
