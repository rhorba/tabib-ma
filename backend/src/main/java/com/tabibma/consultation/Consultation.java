package com.tabibma.consultation;

import com.tabibma.shared.exception.ConflictException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** 1:1 with an Appointment (Architecture doc §3) — created only when that appointment is CONFIRMED
 * and its slot's locationType is VIDEO. See ConsultationBookingListener. */
@Entity
@Table(name = "consultations")
public class Consultation {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "appointment_id", nullable = false, unique = true)
    private UUID appointmentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConsultationStatus status = ConsultationStatus.SCHEDULED;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Consultation() {
        // JPA
    }

    public Consultation(UUID appointmentId) {
        this.appointmentId = appointmentId;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getAppointmentId() {
        return appointmentId;
    }

    public ConsultationStatus getStatus() {
        return status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    /** Idempotent: joining an already-IN_PROGRESS consultation (the other participant already
     * joined) is not an error. */
    public void start() {
        if (status == ConsultationStatus.SCHEDULED) {
            this.status = ConsultationStatus.IN_PROGRESS;
            this.startedAt = Instant.now();
        } else if (status != ConsultationStatus.IN_PROGRESS) {
            throw new ConflictException("This consultation can no longer be joined.");
        }
    }

    public void complete() {
        if (status != ConsultationStatus.IN_PROGRESS && status != ConsultationStatus.SCHEDULED) {
            throw new ConflictException("Only a SCHEDULED or IN_PROGRESS consultation can be completed.");
        }
        this.status = ConsultationStatus.COMPLETED;
        this.endedAt = Instant.now();
    }
}
