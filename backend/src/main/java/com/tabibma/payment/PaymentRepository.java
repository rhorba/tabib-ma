package com.tabibma.payment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    Optional<Payment> findByAppointmentId(UUID appointmentId);

    List<Payment> findAllByAppointmentIdInAndStatus(Collection<UUID> appointmentIds, PaymentStatus status);

    long countByStatus(PaymentStatus status);
}
