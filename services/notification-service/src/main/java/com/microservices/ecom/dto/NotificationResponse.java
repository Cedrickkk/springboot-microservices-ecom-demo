package com.microservices.ecom.dto;

import com.microservices.ecom.domain.NotificationType;
import com.microservices.ecom.domain.OrderConfirmation;
import com.microservices.ecom.domain.PaymentConfirmation;

import java.time.LocalDateTime;

public record NotificationResponse(
        String id,
        NotificationType type,
        LocalDateTime notificationDate,
        LocalDateTime sentAt,
        OrderConfirmation orderConfirmation,
        PaymentConfirmation paymentConfirmation
) {
}
