package com.microservices.common.messaging;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class EventCodecTests {
    private final EventCodec codec = new EventCodec(JsonMapper.builder().build());
    private final OrderConfirmationEvent event = new OrderConfirmationEvent(UUID.randomUUID(), "7", "ORD-7",
            BigDecimal.TEN, PaymentMethod.VISA, "c1", List.of(
            new OrderConfirmationEvent.Product(1, "Book", "Book", BigDecimal.TEN, 1)));

    @Test
    void roundTripPreservesIdentityAndContract() {
        var encoded = codec.encode(EventTopics.ORDER_CONFIRMATION, "7", event);
        assertEquals(event.eventId(), encoded.eventId());
        assertEquals(event, codec.decode(EventTopics.ORDER_CONFIRMATION, encoded.payload()));
    }

    @Test
    void paymentConfirmationRoundTripAndTypePairing() {
        var payment = new PaymentConfirmationEvent(UUID.randomUUID(), "42", 7, "ORD-7", BigDecimal.TEN,
                PaymentMethod.VISA, "c1", "Jane", "Doe", "jane@example.com");
        var encoded = codec.encode(EventTopics.PAYMENT_CONFIRMATION, "42", payment);
        assertEquals(payment, codec.decode(EventTopics.PAYMENT_CONFIRMATION, encoded.payload()));
        assertThrows(IllegalArgumentException.class, () -> codec.encode(EventTopics.ORDER_CONFIRMATION, "42", payment));
        assertThrows(IllegalArgumentException.class, () -> codec.encode(EventTopics.PAYMENT_CONFIRMATION, "7", event));
    }

    @Test
    void rejectsMismatchedAggregateAndUnregisteredClass() {
        assertThrows(IllegalArgumentException.class, () -> codec.encode(EventTopics.ORDER_CONFIRMATION, "8", event));
        MessagingEvent unsupported = new MessagingEvent() {
            public UUID eventId() { return event.eventId(); }
            public String aggregateId() { return "7"; }
        };
        assertThrows(IllegalArgumentException.class, () -> codec.encode(EventTopics.ORDER_CONFIRMATION, "7", unsupported));
        assertThrows(IllegalArgumentException.class, () -> codec.encode(null, "7", event));
        assertThrows(IllegalArgumentException.class, () -> codec.encode(EventTopics.ORDER_CONFIRMATION, "7", null));
    }

    @Test
    void rejectsMalformedOrMissingStoredIdentity() {
        assertThrows(IllegalArgumentException.class, () -> codec.decode(EventTopics.ORDER_CONFIRMATION, "invalid json"));
        assertThrows(IllegalArgumentException.class, () -> codec.decode(EventTopics.ORDER_CONFIRMATION, "null"));
        assertThrows(IllegalArgumentException.class, () -> codec.decode(EventTopics.ORDER_CONFIRMATION, "{}"));
        assertThrows(IllegalArgumentException.class, () -> codec.decode(null, "{}"));
    }
}
