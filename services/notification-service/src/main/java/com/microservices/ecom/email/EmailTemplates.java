package com.microservices.ecom.email;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EmailTemplates {
    ORDER_CONFIRMATION("order-confirmation", "Order confirmation"),
    PAYMENT_CONFIRMATION("payment-confirmation", "Payment confirmation");

    private final String template;
    private final String subject;
}
