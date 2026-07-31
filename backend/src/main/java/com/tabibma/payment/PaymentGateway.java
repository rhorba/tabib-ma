package com.tabibma.payment;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Strategy interface (Architecture doc §4) — swaps CMI/vendor implementations without touching
 * BookingService. {@code idempotencyKey} is passed through so the gateway itself won't double-charge
 * on a retried call, mirroring real payment gateways' (CMI included) client-supplied idempotency keys.
 */
public interface PaymentGateway {

    PaymentGatewayResult charge(UUID appointmentId, BigDecimal amountMad, String idempotencyKey);
}
