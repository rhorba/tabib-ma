package com.tabibma.clinic;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "verification_documents")
public class VerificationDocument {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "doctor_profile_id", nullable = false)
    private UUID doctorProfileId;

    @Column(name = "document_type", nullable = false)
    private String documentType;

    @Column(name = "object_storage_key", nullable = false)
    private String objectStorageKey;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected VerificationDocument() {
        // JPA
    }

    public VerificationDocument(UUID doctorProfileId, String documentType, String objectStorageKey) {
        this.doctorProfileId = doctorProfileId;
        this.documentType = documentType;
        this.objectStorageKey = objectStorageKey;
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

    public String getDocumentType() {
        return documentType;
    }

    public String getObjectStorageKey() {
        return objectStorageKey;
    }

    public UUID getReviewedBy() {
        return reviewedBy;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void markReviewed(UUID reviewerId) {
        this.reviewedBy = reviewerId;
        this.reviewedAt = Instant.now();
    }
}
