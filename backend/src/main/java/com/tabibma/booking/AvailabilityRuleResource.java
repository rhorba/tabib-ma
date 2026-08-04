package com.tabibma.booking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Join row: a clinic resource (room/equipment) required by an IN_PERSON availability rule. */
@Entity
@Table(name = "availability_rule_resources")
public class AvailabilityRuleResource {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "availability_rule_id", nullable = false)
    private UUID availabilityRuleId;

    @Column(name = "resource_id", nullable = false)
    private UUID resourceId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AvailabilityRuleResource() {
        // JPA
    }

    public AvailabilityRuleResource(UUID availabilityRuleId, UUID resourceId) {
        this.availabilityRuleId = availabilityRuleId;
        this.resourceId = resourceId;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getAvailabilityRuleId() {
        return availabilityRuleId;
    }

    public UUID getResourceId() {
        return resourceId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
