package com.tabibma.clinic.dto;

import com.tabibma.clinic.ClinicInvitation;
import com.tabibma.clinic.InvitationStatus;

import java.time.Instant;
import java.util.UUID;

public record ClinicInvitationResponse(
        UUID id,
        UUID clinicId,
        String invitedEmail,
        InvitationStatus status,
        Instant createdAt
) {
    public static ClinicInvitationResponse from(ClinicInvitation invitation) {
        return new ClinicInvitationResponse(invitation.getId(), invitation.getClinicId(),
                invitation.getInvitedEmail(), invitation.getStatus(), invitation.getCreatedAt());
    }
}
