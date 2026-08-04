package com.tabibma.clinic;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ClinicResourceRepository extends JpaRepository<ClinicResource, UUID> {

    List<ClinicResource> findAllByClinicId(UUID clinicId);

    List<ClinicResource> findAllByClinicIdAndActiveTrue(UUID clinicId);
}
