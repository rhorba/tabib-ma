package com.tabibma.booking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** A clinic resource (room/equipment) required by a generated slot, copied from the
 * AvailabilityRule's own resource requirements at generation time (Story 8.2 Batch 3). */
@Entity
@Table(name = "availability_slot_resources")
public class AvailabilitySlotResource {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "availability_slot_id", nullable = false)
    private UUID availabilitySlotId;

    @Column(name = "resource_id", nullable = false)
    private UUID resourceId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AvailabilitySlotResource() {
        // JPA
    }

    public AvailabilitySlotResource(UUID availabilitySlotId, UUID resourceId) {
        this.availabilitySlotId = availabilitySlotId;
        this.resourceId = resourceId;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getAvailabilitySlotId() {
        return availabilitySlotId;
    }

    public UUID getResourceId() {
        return resourceId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
