package com.microservices.ecom.kafka;

import com.microservices.common.messaging.EventCodec;
import com.microservices.common.messaging.EventTopics;
import com.microservices.common.messaging.OrderConfirmationEvent;
import com.microservices.common.messaging.PaymentConfirmationEvent;
import com.microservices.ecom.service.NotificationService;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationsConsumer {
    private final EventCodec codec;
    private final NotificationService service;

    @KafkaListener(topics = "${application.messaging.topics.ORDER_CONFIRMATION:order-topic}")
    public void consumeOrderConfirmation(String payload) throws MessagingException {
        service.processOrderConfirmation((OrderConfirmationEvent) codec.decode(EventTopics.ORDER_CONFIRMATION, payload));
    }

    @KafkaListener(topics = "${application.messaging.topics.PAYMENT_CONFIRMATION:payment-topic}")
    public void consumePaymentConfirmation(String payload) throws MessagingException {
        service.processPaymentConfirmation((PaymentConfirmationEvent) codec.decode(EventTopics.PAYMENT_CONFIRMATION, payload));
    }
}
