package com.tabibma.payment;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Stands in for the real CMI integration until merchant credentials exist — .env.example still
 * has CMI_MERCHANT_ID/CMI_API_KEY as "changeme" placeholders — same mock-external-vendor pattern
 * as Epic 1's mock TURN credential provider. Always succeeds synchronously; the real
 * CmiPaymentGatewayAdapter (redirect + async webhook + PaymentWebhookSignatureVerifier, per the
 * architecture doc) is not built yet since there is nothing to integrate against.
 */
@Component
public class MockCmiPaymentGatewayAdapter implements PaymentGateway {

    @Override
    public PaymentGatewayResult charge(UUID appointmentId, BigDecimal amountMad, String idempotencyKey) {
        return PaymentGatewayResult.success("MOCK-" + UUID.randomUUID());
    }
}
