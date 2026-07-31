package com.tabibma.booking;

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

/** Aggregate root for the booking module (Architecture doc §3): the invariant that a CONFIRMED
 * appointment always has a matching CONFIRMED Payment is enforced by BookingService (Story 4.2),
 * not here — this class only enforces its own status-transition rules. */
@Entity
@Table(name = "appointments")
public class Appointment {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "doctor_profile_id", nullable = false)
    private UUID doctorProfileId;

    @Column(name = "availability_slot_id", nullable = false)
    private UUID availabilitySlotId;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "location_type", nullable = false)
    private LocationType locationType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus status = AppointmentStatus.PENDING_PAYMENT;

    @Column(name = "cancellation_window_hours", nullable = false)
    private int cancellationWindowHours = 24;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "reminder_sent_at")
    private Instant reminderSentAt;

    protected Appointment() {
        // JPA
    }

    public Appointment(UUID patientId, UUID doctorProfileId, UUID availabilitySlotId, Instant startsAt,
                        Instant endsAt, LocationType locationType) {
        this.patientId = patientId;
        this.doctorProfileId = doctorProfileId;
        this.availabilitySlotId = availabilitySlotId;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.locationType = locationType;
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

    public UUID getPatientId() {
        return patientId;
    }

    public UUID getDoctorProfileId() {
        return doctorProfileId;
    }

    public UUID getAvailabilitySlotId() {
        return availabilitySlotId;
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

    public AppointmentStatus getStatus() {
        return status;
    }

    public int getCancellationWindowHours() {
        return cancellationWindowHours;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getReminderSentAt() {
        return reminderSentAt;
    }

    public void markReminderSent() {
        this.reminderSentAt = Instant.now();
    }

    public void confirm() {
        if (status != AppointmentStatus.PENDING_PAYMENT) {
            throw new ConflictException("Only a PENDING_PAYMENT appointment can be confirmed.");
        }
        this.status = AppointmentStatus.CONFIRMED;
    }

    public void cancel() {
        if (status == AppointmentStatus.CANCELLED || status == AppointmentStatus.COMPLETED) {
            throw new ConflictException("This appointment can no longer be cancelled.");
        }
        this.status = AppointmentStatus.CANCELLED;
    }
}
