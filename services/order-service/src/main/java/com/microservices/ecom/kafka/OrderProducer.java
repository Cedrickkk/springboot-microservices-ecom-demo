package com.microservices.ecom.kafka;

import com.microservices.common.messaging.EventTopics;
import com.microservices.common.messaging.OrderConfirmationEvent;
import com.microservices.common.messaging.OutboxStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderProducer {
    private final OutboxStore outbox;

    public void sendOrderConfirmation(OrderConfirmationEvent confirmation) {
        outbox.enqueue(EventTopics.ORDER_CONFIRMATION, confirmation.aggregateId(), confirmation);
    }
}
