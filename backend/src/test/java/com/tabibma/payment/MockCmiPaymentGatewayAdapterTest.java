package com.tabibma.payment;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MockCmiPaymentGatewayAdapterTest {

    @Test
    void charge_alwaysSucceedsWithAUniqueTransactionRef() {
        MockCmiPaymentGatewayAdapter adapter = new MockCmiPaymentGatewayAdapter();

        PaymentGatewayResult first = adapter.charge(UUID.randomUUID(), BigDecimal.TEN, "key-1");
        PaymentGatewayResult second = adapter.charge(UUID.randomUUID(), BigDecimal.TEN, "key-2");

        assertThat(first.succeeded()).isTrue();
        assertThat(first.transactionRef()).isNotBlank();
        assertThat(second.transactionRef()).isNotEqualTo(first.transactionRef());
    }
}
