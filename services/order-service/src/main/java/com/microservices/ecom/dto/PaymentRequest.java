package com.microservices.ecom.dto;

import com.microservices.common.messaging.PaymentMethod;

import java.math.BigDecimal;

public record PaymentRequest(
        BigDecimal amount,
        PaymentMethod paymentMethod,
        Integer orderId,
        String orderReference,
        PaymentCustomer customer) {
}
