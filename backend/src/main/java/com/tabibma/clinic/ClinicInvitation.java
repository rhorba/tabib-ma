package com.tabibma.clinic;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "clinic_invitations")
public class ClinicInvitation {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "clinic_id", nullable = false)
    private UUID clinicId;

    @Column(name = "invited_email", nullable = false)
    private String invitedEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvitationStatus status = InvitationStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "decided_at")
    private Instant decidedAt;

    protected ClinicInvitation() {
        // JPA
    }

    public ClinicInvitation(UUID clinicId, String invitedEmail) {
        this.clinicId = clinicId;
        this.invitedEmail = invitedEmail;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getClinicId() {
        return clinicId;
    }

    public String getInvitedEmail() {
        return invitedEmail;
    }

    public InvitationStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }

    public void accept() {
        this.status = InvitationStatus.ACCEPTED;
        this.decidedAt = Instant.now();
    }

    public void decline() {
        this.status = InvitationStatus.DECLINED;
        this.decidedAt = Instant.now();
    }
}
