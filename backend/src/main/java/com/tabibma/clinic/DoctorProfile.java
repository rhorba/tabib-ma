package com.tabibma.clinic;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "doctor_profiles")
public class DoctorProfile {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(nullable = false)
    private String specialty;

    @Column
    private String bio;

    @Column(name = "consultation_fee_mad", nullable = false)
    private BigDecimal consultationFeeMad;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false)
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;

    @Column(nullable = false)
    private String city;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected DoctorProfile() {
        // JPA
    }

    public DoctorProfile(UUID userId, String specialty, String bio, BigDecimal consultationFeeMad, String city) {
        this.userId = userId;
        this.specialty = specialty;
        this.bio = bio;
        this.consultationFeeMad = consultationFeeMad;
        this.city = city;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getSpecialty() {
        return specialty;
    }

    public String getBio() {
        return bio;
    }

    public BigDecimal getConsultationFeeMad() {
        return consultationFeeMad;
    }

    public VerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public String getCity() {
        return city;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void approve() {
        this.verificationStatus = VerificationStatus.APPROVED;
    }

    public void reject() {
        this.verificationStatus = VerificationStatus.REJECTED;
    }
}
