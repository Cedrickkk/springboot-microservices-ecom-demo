package com.microservices.ecom.service;

import com.microservices.common.messaging.PaymentConfirmationEvent;
import com.microservices.common.messaging.PaymentMethod;
import com.microservices.ecom.domain.Payment;
import com.microservices.ecom.dto.PaymentCustomer;
import com.microservices.ecom.dto.PaymentRequest;
import com.microservices.ecom.kafka.PaymentProducer;
import com.microservices.ecom.mapper.PaymentMapper;
import com.microservices.ecom.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PaymentServiceTests {
    private final PaymentRepository repository = mock(PaymentRepository.class);
    private final PaymentProducer producer = mock(PaymentProducer.class);
    private final PaymentService service = new PaymentService(repository, new PaymentMapper(), producer);
    private final PaymentRequest request = new PaymentRequest(BigDecimal.TEN, PaymentMethod.VISA, 7, "ORD-7",
            new PaymentCustomer("c1", "Jane", "Doe", "jane@example.com"));

    @Test
    void savesPaymentThenQueuesConfirmationWithPersistedIdentity() {
        when(repository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            assertNull(payment.getId());
            assertEquals(BigDecimal.TEN, payment.getAmount());
            assertEquals(PaymentMethod.VISA, payment.getPaymentMethod());
            assertEquals(7, payment.getOrderId());
            payment.setId(42);
            return payment;
        });
        assertEquals(42, service.createPayment(request));
        var sequence = inOrder(repository, producer);
        sequence.verify(repository).save(any(Payment.class));
        var captor = ArgumentCaptor.forClass(PaymentConfirmationEvent.class);
        sequence.verify(producer).sendPaymentConfirmation(captor.capture());
        var event = captor.getValue();
        assertNotNull(event.eventId());
        assertEquals("42", event.aggregateId());
        assertEquals(7, event.orderId());
        assertEquals("ORD-7", event.orderReference());
        assertEquals(BigDecimal.TEN, event.amount());
        assertEquals(PaymentMethod.VISA, event.paymentMethod());
        assertEquals("c1", event.customerId());
        assertEquals("Jane", event.customerFirstname());
        assertEquals("Doe", event.customerLastname());
        assertEquals("jane@example.com", event.customerEmail());
    }

    @Test
    void persistenceFailureDoesNotQueueConfirmation() {
        when(repository.save(any())).thenThrow(new IllegalStateException("database unavailable"));
        assertThrows(IllegalStateException.class, () -> service.createPayment(request));
        verifyNoInteractions(producer);
    }

    @Test
    void enqueueFailurePropagatesToRollBackPaymentTransaction() {
        when(repository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(42);
            return payment;
        });
        doThrow(new IllegalStateException("outbox unavailable")).when(producer).sendPaymentConfirmation(any());
        assertThrows(IllegalStateException.class, () -> service.createPayment(request));
    }
}
