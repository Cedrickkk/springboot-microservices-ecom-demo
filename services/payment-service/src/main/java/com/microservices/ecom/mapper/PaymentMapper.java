package com.microservices.ecom.mapper;

import com.microservices.ecom.domain.Payment;
import com.microservices.ecom.dto.PaymentRequest;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {
    public Payment toEntity(PaymentRequest request) {
        return Payment.builder()
                .amount(request.amount())
                .paymentMethod(request.paymentMethod())
                .orderId(request.orderId())
                .build();
    }
}
