package com.tabibma.clinic;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DoctorProfileRepository extends JpaRepository<DoctorProfile, UUID> {

    Optional<DoctorProfile> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);

    List<DoctorProfile> findAllByVerificationStatus(VerificationStatus status);

    @Query("SELECT d FROM DoctorProfile d WHERE d.verificationStatus = com.tabibma.clinic.VerificationStatus.APPROVED "
            + "AND (:specialty IS NULL OR LOWER(d.specialty) = LOWER(CAST(:specialty AS string))) "
            + "AND (:city IS NULL OR LOWER(d.city) = LOWER(CAST(:city AS string)))")
    Page<DoctorProfile> search(@Param("specialty") String specialty, @Param("city") String city, Pageable pageable);
}
