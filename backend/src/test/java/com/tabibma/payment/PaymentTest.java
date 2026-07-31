package com.tabibma.payment;

import com.tabibma.shared.exception.ConflictException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentTest {

    @Test
    void succeed_setsStatusAndTransactionRef() {
        Payment payment = new Payment(UUID.randomUUID(), BigDecimal.TEN, "key-1");

        payment.succeed("CMI-123");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(payment.getCmiTransactionRef()).isEqualTo("CMI-123");
    }

    @Test
    void succeed_rejectsWhenAlreadyDecided() {
        Payment payment = new Payment(UUID.randomUUID(), BigDecimal.TEN, "key-2");
        payment.fail();

        assertThatThrownBy(() -> payment.succeed("CMI-456")).isInstanceOf(ConflictException.class);
    }

    @Test
    void fail_setsFailedStatus() {
        Payment payment = new Payment(UUID.randomUUID(), BigDecimal.TEN, "key-3");

        payment.fail();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void fail_rejectsWhenAlreadyDecided() {
        Payment payment = new Payment(UUID.randomUUID(), BigDecimal.TEN, "key-4");
        payment.succeed("CMI-789");

        assertThatThrownBy(payment::fail).isInstanceOf(ConflictException.class);
    }
}
