package com.microservices.ecom.dto;

import com.microservices.ecom.domain.PaymentMethod;

import java.math.BigDecimal;
import java.util.List;

public record OrderConfirmation(
        String orderReference,
        BigDecimal totalAmount,
        PaymentMethod paymentMethod,
        String customerId,
        List<ProductPurchaseResponse> products
) {
}
