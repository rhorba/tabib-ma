package com.tabibma.payment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PaymentGateway paymentGateway;

    private PaymentService service;

    @BeforeEach
    void setUp() {
        service = new PaymentService(paymentRepository, paymentGateway);
    }

    @Test
    void capturePayment_returnsExistingPaymentWithoutChargingAgain() {
        UUID appointmentId = UUID.randomUUID();
        Payment existing = new Payment(appointmentId, BigDecimal.TEN, "key-1");
        when(paymentRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.of(existing));

        Payment result = service.capturePayment(appointmentId, BigDecimal.TEN, "key-1");

        assertThat(result).isSameAs(existing);
        verify(paymentGateway, never()).charge(any(), any(), any());
    }

    @Test
    void capturePayment_recordsSucceededPaymentOnGatewaySuccess() {
        UUID appointmentId = UUID.randomUUID();
        when(paymentRepository.findByIdempotencyKey("key-2")).thenReturn(Optional.empty());
        when(paymentGateway.charge(eq(appointmentId), eq(BigDecimal.TEN), eq("key-2")))
                .thenReturn(PaymentGatewayResult.success("CMI-1"));
        when(paymentRepository.saveAndFlush(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        Payment result = service.capturePayment(appointmentId, BigDecimal.TEN, "key-2");

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(result.getCmiTransactionRef()).isEqualTo("CMI-1");
    }

    @Test
    void capturePayment_recordsFailedPaymentOnGatewayFailure() {
        UUID appointmentId = UUID.randomUUID();
        when(paymentRepository.findByIdempotencyKey("key-3")).thenReturn(Optional.empty());
        when(paymentGateway.charge(any(), any(), any())).thenReturn(PaymentGatewayResult.failure());
        when(paymentRepository.saveAndFlush(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        Payment result = service.capturePayment(appointmentId, BigDecimal.TEN, "key-3");

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void capturePayment_fallsBackToExistingRowWhenAConcurrentCallWinsTheInsertRace() {
        UUID appointmentId = UUID.randomUUID();
        Payment winner = new Payment(appointmentId, BigDecimal.TEN, "key-4");
        winner.succeed("CMI-2");
        when(paymentRepository.findByIdempotencyKey("key-4"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(winner));
        when(paymentGateway.charge(any(), any(), any())).thenReturn(PaymentGatewayResult.success("CMI-2"));
        when(paymentRepository.saveAndFlush(any(Payment.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate idempotency_key"));

        Payment result = service.capturePayment(appointmentId, BigDecimal.TEN, "key-4");

        assertThat(result).isSameAs(winner);
    }
}
