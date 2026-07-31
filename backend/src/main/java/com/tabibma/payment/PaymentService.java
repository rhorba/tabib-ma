package com.tabibma.payment;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentGateway paymentGateway;

    public PaymentService(PaymentRepository paymentRepository, PaymentGateway paymentGateway) {
        this.paymentRepository = paymentRepository;
        this.paymentGateway = paymentGateway;
    }

    /**
     * Idempotent by {@code idempotencyKey}: a repeated call (client retry) with the same key
     * returns the already-persisted Payment instead of charging again. The gateway is still
     * invoked before the idempotency check can catch a true concurrent double-submit (Test
     * Strategy §3's "double-submit race") — that residual risk is intentionally pushed onto the
     * gateway itself, since real payment gateways are built to dedupe on a client-supplied
     * idempotency key. This method only guarantees it never persists two Payment rows for the
     * same key, falling back to the row a racing call already committed.
     */
    @Transactional
    public Payment capturePayment(UUID appointmentId, BigDecimal amountMad, String idempotencyKey) {
        return paymentRepository.findByIdempotencyKey(idempotencyKey)
                .orElseGet(() -> chargeAndRecord(appointmentId, amountMad, idempotencyKey));
    }

    private Payment chargeAndRecord(UUID appointmentId, BigDecimal amountMad, String idempotencyKey) {
        Payment payment = new Payment(appointmentId, amountMad, idempotencyKey);
        PaymentGatewayResult result = paymentGateway.charge(appointmentId, amountMad, idempotencyKey);
        if (result.succeeded()) {
            payment.succeed(result.transactionRef());
        } else {
            payment.fail();
        }
        try {
            return paymentRepository.saveAndFlush(payment);
        } catch (DataIntegrityViolationException e) {
            return paymentRepository.findByIdempotencyKey(idempotencyKey).orElseThrow(() -> e);
        }
    }
}
