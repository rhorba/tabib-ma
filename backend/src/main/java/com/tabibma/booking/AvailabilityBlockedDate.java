package com.tabibma.booking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Maps to the `availability_exceptions` table — named to match Story 4.1's "exception date" language. */
@Entity
@Table(name = "availability_exceptions")
public class AvailabilityBlockedDate {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "doctor_profile_id", nullable = false)
    private UUID doctorProfileId;

    @Column(name = "exception_date", nullable = false)
    private LocalDate exceptionDate;

    @Column
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AvailabilityBlockedDate() {
        // JPA
    }

    public AvailabilityBlockedDate(UUID doctorProfileId, LocalDate exceptionDate, String reason) {
        this.doctorProfileId = doctorProfileId;
        this.exceptionDate = exceptionDate;
        this.reason = reason;
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

    public LocalDate getExceptionDate() {
        return exceptionDate;
    }

    public String getReason() {
        return reason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
