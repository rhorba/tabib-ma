package com.tabibma.booking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Row backing the appointment_resource_allocations EXCLUDE constraint (Flyway V15, ADR-4-style
 * conflict prevention keyed on resource_id) — see {@link ResourceAllocationGuard}. */
@Entity
@Table(name = "appointment_resource_allocations")
public class AppointmentResourceAllocation {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "appointment_id", nullable = false)
    private UUID appointmentId;

    @Column(name = "resource_id", nullable = false)
    private UUID resourceId;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AppointmentResourceAllocation() {
        // JPA
    }

    public AppointmentResourceAllocation(UUID appointmentId, UUID resourceId, Instant startsAt, Instant endsAt) {
        this.appointmentId = appointmentId;
        this.resourceId = resourceId;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getAppointmentId() {
        return appointmentId;
    }

    public UUID getResourceId() {
        return resourceId;
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    public Instant getEndsAt() {
        return endsAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
