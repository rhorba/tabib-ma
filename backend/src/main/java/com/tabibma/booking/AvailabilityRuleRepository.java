package com.tabibma.booking;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AvailabilityRuleRepository extends JpaRepository<AvailabilityRule, UUID> {

    List<AvailabilityRule> findAllByDoctorProfileId(UUID doctorProfileId);

    List<AvailabilityRule> findAllByDoctorProfileIdAndActiveTrue(UUID doctorProfileId);
}
