package com.tabibma.payment;

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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "appointment_id", nullable = false, unique = true)
    private UUID appointmentId;

    @Column(name = "amount_mad", nullable = false)
    private BigDecimal amountMad;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "cmi_transaction_ref", unique = true)
    private String cmiTransactionRef;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Payment() {
        // JPA
    }

    public Payment(UUID appointmentId, BigDecimal amountMad, String idempotencyKey) {
        this.appointmentId = appointmentId;
        this.amountMad = amountMad;
        this.idempotencyKey = idempotencyKey;
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

    public UUID getAppointmentId() {
        return appointmentId;
    }

    public BigDecimal getAmountMad() {
        return amountMad;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public String getCmiTransactionRef() {
        return cmiTransactionRef;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void succeed(String cmiTransactionRef) {
        if (status != PaymentStatus.PENDING) {
            throw new ConflictException("Only a PENDING payment can succeed.");
        }
        this.status = PaymentStatus.SUCCEEDED;
        this.cmiTransactionRef = cmiTransactionRef;
    }

    public void fail() {
        if (status != PaymentStatus.PENDING) {
            throw new ConflictException("Only a PENDING payment can fail.");
        }
        this.status = PaymentStatus.FAILED;
    }

    public void refund() {
        if (status != PaymentStatus.SUCCEEDED) {
            throw new ConflictException("Only a SUCCEEDED payment can be refunded.");
        }
        this.status = PaymentStatus.REFUNDED;
    }
}
