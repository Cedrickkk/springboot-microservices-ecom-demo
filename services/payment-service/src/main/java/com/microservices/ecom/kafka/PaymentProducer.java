package com.microservices.ecom.kafka;

import com.microservices.common.messaging.EventTopics;
import com.microservices.common.messaging.OutboxStore;
import com.microservices.common.messaging.PaymentConfirmationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentProducer {
    private final OutboxStore outbox;

    public void sendPaymentConfirmation(PaymentConfirmationEvent confirmation) {
        outbox.enqueue(EventTopics.PAYMENT_CONFIRMATION, confirmation.aggregateId(), confirmation);
    }
}
