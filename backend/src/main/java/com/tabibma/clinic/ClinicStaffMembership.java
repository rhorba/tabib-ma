package com.tabibma.clinic;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "clinic_staff_memberships")
public class ClinicStaffMembership {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "clinic_id", nullable = false)
    private UUID clinicId;

    @Column(name = "doctor_profile_id", nullable = false)
    private UUID doctorProfileId;

    @Column(name = "role_in_clinic", nullable = false)
    private String roleInClinic = "DOCTOR";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ClinicStaffMembership() {
        // JPA
    }

    public ClinicStaffMembership(UUID clinicId, UUID doctorProfileId) {
        this.clinicId = clinicId;
        this.doctorProfileId = doctorProfileId;
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

    public UUID getDoctorProfileId() {
        return doctorProfileId;
    }

    public String getRoleInClinic() {
        return roleInClinic;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
