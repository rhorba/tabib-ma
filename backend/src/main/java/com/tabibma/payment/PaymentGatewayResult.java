package com.tabibma.payment;

public record PaymentGatewayResult(boolean succeeded, String transactionRef) {

    public static PaymentGatewayResult success(String transactionRef) {
        return new PaymentGatewayResult(true, transactionRef);
    }

    public static PaymentGatewayResult failure() {
        return new PaymentGatewayResult(false, null);
    }
}
