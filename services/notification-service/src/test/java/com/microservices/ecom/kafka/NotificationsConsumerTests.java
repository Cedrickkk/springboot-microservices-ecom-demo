package com.microservices.ecom.kafka;

import com.microservices.common.messaging.*;
import com.microservices.ecom.service.NotificationService;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class NotificationsConsumerTests {
    private final EventCodec codec = new EventCodec(JsonMapper.builder().build());
    private final NotificationService service = mock(NotificationService.class);
    private final NotificationsConsumer consumer = new NotificationsConsumer(codec, service);

    @Test
    void consumesActualSharedProducerPayloadsWithoutTypeHeaders() throws Exception {
        var order = new OrderConfirmationEvent(UUID.randomUUID(), "10", "ORD-10", BigDecimal.TEN,
                PaymentMethod.VISA, "c1", List.of(new OrderConfirmationEvent.Product(
                3, "Book", "Description", BigDecimal.TEN, 1)));
        var payment = new PaymentConfirmationEvent(UUID.randomUUID(), "7", 10, "ORD-10", BigDecimal.TEN,
                PaymentMethod.VISA, "c1", "Jane", "Doe", "jane@example.com");
        consumer.consumeOrderConfirmation(codec.encode(EventTopics.ORDER_CONFIRMATION, "10", order).payload());
        consumer.consumePaymentConfirmation(codec.encode(EventTopics.PAYMENT_CONFIRMATION, "7", payment).payload());
        verify(service).processOrderConfirmation(order);
        verify(service).processPaymentConfirmation(payment);
    }

    @Test
    void malformedPayloadFailsInsteadOfAcknowledgingIt() {
        assertThrows(IllegalArgumentException.class, () -> consumer.consumePaymentConfirmation("{broken"));
        verifyNoInteractions(service);
    }

    @Test
    void processingFailurePropagatesToKafkaErrorHandler() throws Exception {
        var payment = new PaymentConfirmationEvent(UUID.randomUUID(), "7", 10, "ORD-10", BigDecimal.TEN,
                PaymentMethod.VISA, "c1", "Jane", "Doe", "jane@example.com");
        doThrow(new IllegalStateException("MongoDB unavailable")).when(service).processPaymentConfirmation(payment);
        assertThrows(IllegalStateException.class, () -> consumer.consumePaymentConfirmation(
                codec.encode(EventTopics.PAYMENT_CONFIRMATION, "7", payment).payload()));
    }
}
