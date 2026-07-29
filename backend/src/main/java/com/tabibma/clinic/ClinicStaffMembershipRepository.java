package com.tabibma.clinic;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ClinicStaffMembershipRepository extends JpaRepository<ClinicStaffMembership, UUID> {

    boolean existsByClinicIdAndDoctorProfileId(UUID clinicId, UUID doctorProfileId);
}
