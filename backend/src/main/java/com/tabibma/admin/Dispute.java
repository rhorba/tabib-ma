package com.tabibma.admin;

import com.tabibma.shared.exception.ConflictException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Story 10.1: a flagged issue against an appointment, queued for a platform admin to act on.
 * {@code reportedByUserId} is null when the dispute was system-generated (a doctor marking
 * NO_SHOW, or an auto-flagged payment failure — Story 10.1 Batch 2) rather than self-reported by
 * the patient/doctor on the appointment.
 */
@Entity
@Table(name = "disputes")
public class Dispute {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "appointment_id", nullable = false)
    private UUID appointmentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DisputeType type;

    private String reason;

    @Column(name = "reported_by_user_id")
    private UUID reportedByUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DisputeStatus status = DisputeStatus.OPEN;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolved_by_user_id")
    private UUID resolvedByUserId;

    protected Dispute() {
        // JPA
    }

    public Dispute(UUID appointmentId, DisputeType type, String reason, UUID reportedByUserId) {
        this.appointmentId = appointmentId;
        this.type = type;
        this.reason = reason;
        this.reportedByUserId = reportedByUserId;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public void resolve(UUID resolvedByUserId) {
        if (status != DisputeStatus.OPEN) {
            throw new ConflictException("This dispute has already been resolved.");
        }
        this.status = DisputeStatus.RESOLVED;
        this.resolvedAt = Instant.now();
        this.resolvedByUserId = resolvedByUserId;
    }

    public UUID getId() {
        return id;
    }

    public UUID getAppointmentId() {
        return appointmentId;
    }

    public DisputeType getType() {
        return type;
    }

    public String getReason() {
        return reason;
    }

    public UUID getReportedByUserId() {
        return reportedByUserId;
    }

    public DisputeStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public UUID getResolvedByUserId() {
        return resolvedByUserId;
    }
}
