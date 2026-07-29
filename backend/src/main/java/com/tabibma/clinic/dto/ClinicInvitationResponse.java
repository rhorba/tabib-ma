package com.tabibma.clinic.dto;

import com.tabibma.clinic.ClinicInvitation;
import com.tabibma.clinic.InvitationStatus;

import java.time.Instant;
import java.util.UUID;

public record ClinicInvitationResponse(
        UUID id,
        UUID clinicId,
        String clinicName,
        String invitedEmail,
        InvitationStatus status,
        Instant createdAt
) {
    public static ClinicInvitationResponse from(ClinicInvitation invitation) {
        return from(invitation, null);
    }

    public static ClinicInvitationResponse from(ClinicInvitation invitation, String clinicName) {
        return new ClinicInvitationResponse(invitation.getId(), invitation.getClinicId(), clinicName,
                invitation.getInvitedEmail(), invitation.getStatus(), invitation.getCreatedAt());
    }
}
