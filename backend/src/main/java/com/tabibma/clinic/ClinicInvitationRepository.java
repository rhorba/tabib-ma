package com.tabibma.clinic;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ClinicInvitationRepository extends JpaRepository<ClinicInvitation, UUID> {

    List<ClinicInvitation> findAllByClinicId(UUID clinicId);

    List<ClinicInvitation> findAllByInvitedEmailAndStatus(String invitedEmail, InvitationStatus status);

    boolean existsByClinicIdAndInvitedEmailAndStatus(UUID clinicId, String invitedEmail, InvitationStatus status);
}
