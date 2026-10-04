package com.microservices.ecom.kafka;

import com.microservices.common.messaging.EventTopics;
import com.microservices.common.messaging.OrderConfirmationEvent;
import com.microservices.common.messaging.OutboxStore;
import com.microservices.common.messaging.PaymentMethod;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class OrderProducerTests {
    @Test
    void queuesConfirmationThroughSharedOutbox() {
        var outbox = mock(OutboxStore.class);
        var event = new OrderConfirmationEvent(UUID.randomUUID(), "7", "ORD-7", BigDecimal.TEN,
                PaymentMethod.VISA, "c1", List.of());
        new OrderProducer(outbox).sendOrderConfirmation(event);
        verify(outbox).enqueue(EventTopics.ORDER_CONFIRMATION, "7", event);
    }
}
