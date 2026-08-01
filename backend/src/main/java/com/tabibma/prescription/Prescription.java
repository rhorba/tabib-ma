package com.tabibma.prescription;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Architecture doc §3: "Immutable once signed (no edits — a correction creates a new prescription
 * referencing the old one)." Enforced at the Java level, not just by service-layer convention:
 * every field is set once in the constructor and there are no setters, so there is no code path
 * that could ever UPDATE a persisted row — a correction (PrescriptionService.correct) is always a
 * brand new row with {@code supersedesId} pointing at the original (Test Strategy §2, "Maximum
 * risk" AC).
 */
@Entity
@Table(name = "prescriptions")
public class Prescription {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "consultation_id", nullable = false)
    private UUID consultationId;

    @Column(name = "doctor_id", nullable = false)
    private UUID doctorId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "supersedes_id")
    private UUID supersedesId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "prescription_items", joinColumns = @JoinColumn(name = "prescription_id"))
    @OrderColumn(name = "item_order")
    private List<PrescriptionItem> items;

    @Column(name = "pdf_storage_key", nullable = false)
    private String pdfStorageKey;

    @Column(name = "signed_at", nullable = false)
    private Instant signedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Prescription() {
        // JPA
    }

    public Prescription(UUID consultationId, UUID doctorId, UUID patientId, UUID supersedesId,
                         List<PrescriptionItem> items, String pdfStorageKey, Instant signedAt) {
        this.consultationId = consultationId;
        this.doctorId = doctorId;
        this.patientId = patientId;
        this.supersedesId = supersedesId;
        this.items = List.copyOf(items);
        this.pdfStorageKey = pdfStorageKey;
        this.signedAt = signedAt;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getConsultationId() {
        return consultationId;
    }

    public UUID getDoctorId() {
        return doctorId;
    }

    public UUID getPatientId() {
        return patientId;
    }

    public UUID getSupersedesId() {
        return supersedesId;
    }

    public List<PrescriptionItem> getItems() {
        return items;
    }

    public String getPdfStorageKey() {
        return pdfStorageKey;
    }

    public Instant getSignedAt() {
        return signedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
