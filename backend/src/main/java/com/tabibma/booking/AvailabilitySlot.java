package com.tabibma.booking;

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

@Entity
@Table(name = "availability_slots")
public class AvailabilitySlot {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "doctor_profile_id", nullable = false)
    private UUID doctorProfileId;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "location_type", nullable = false)
    private LocationType locationType;

    @Column(name = "clinic_id")
    private UUID clinicId;

    @Column(name = "is_booked", nullable = false)
    private boolean booked = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AvailabilitySlot() {
        // JPA
    }

    public AvailabilitySlot(UUID doctorProfileId, Instant startsAt, Instant endsAt, LocationType locationType, UUID clinicId) {
        this.doctorProfileId = doctorProfileId;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.locationType = locationType;
        this.clinicId = clinicId;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getDoctorProfileId() {
        return doctorProfileId;
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    public Instant getEndsAt() {
        return endsAt;
    }

    public LocationType getLocationType() {
        return locationType;
    }

    public UUID getClinicId() {
        return clinicId;
    }

    public boolean isBooked() {
        return booked;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    /** Used by the booking flow (Story 4.2/4.3) once it exists — not yet called in Story 4.1. */
    public void markBooked() {
        this.booked = true;
    }

    public void release() {
        this.booked = false;
    }
}
