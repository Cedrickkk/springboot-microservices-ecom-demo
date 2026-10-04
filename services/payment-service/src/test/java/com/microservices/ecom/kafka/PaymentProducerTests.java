package com.microservices.ecom.kafka;

import com.microservices.common.messaging.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.UUID;
import static org.mockito.Mockito.*;

class PaymentProducerTests {
    @Test
    void queuesPaymentConfirmationThroughSharedStore() {
        var outbox = mock(OutboxStore.class);
        var event = new PaymentConfirmationEvent(UUID.randomUUID(), "42", 7, "ORD-7", BigDecimal.TEN,
                PaymentMethod.VISA, "c1", "Jane", "Doe", "jane@example.com");
        new PaymentProducer(outbox).sendPaymentConfirmation(event);
        verify(outbox).enqueue(EventTopics.PAYMENT_CONFIRMATION, "42", event);
    }
}
