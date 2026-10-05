package com.microservices.ecom.service;

import com.microservices.common.messaging.OrderConfirmationEvent;
import com.microservices.common.messaging.PaymentConfirmationEvent;
import com.microservices.common.messaging.PaymentMethod;
import com.microservices.common.response.ApiResponseUtil;
import com.microservices.ecom.client.CustomerClient;
import com.microservices.ecom.domain.Notification;
import com.microservices.ecom.domain.NotificationType;
import com.microservices.ecom.dto.CustomerResponse;
import com.microservices.ecom.email.EmailService;
import com.microservices.ecom.mapper.NotificationMapper;
import com.microservices.ecom.repository.NotificationRepository;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NotificationServiceTests {
    private NotificationRepository repository;
    private CustomerClient customerClient;
    private EmailService emailService;
    private NotificationService service;

    @BeforeEach
    void setUp() {
        repository = mock(NotificationRepository.class);
        customerClient = mock(CustomerClient.class);
        emailService = mock(EmailService.class);
        service = new NotificationService(repository, new NotificationMapper(), customerClient, emailService);
    }

    private PaymentConfirmationEvent paymentEvent() {
        return new PaymentConfirmationEvent(UUID.randomUUID(), "7", 10, "ORD-10", BigDecimal.TEN,
                PaymentMethod.VISA, "c1", "Jane", "Doe", "jane@example.com");
    }

    @Test
    void orderEnrichesCustomerAndStoresDeliveryAfterEmailSucceeds() throws Exception {
        var event = new OrderConfirmationEvent(UUID.randomUUID(), "10", "ORD-10", BigDecimal.TEN,
                PaymentMethod.VISA, "c1", List.of(new OrderConfirmationEvent.Product(
                3, "Book", "A book", BigDecimal.TEN, 1)));
        when(repository.findById(event.eventId().toString())).thenReturn(Optional.empty());
        when(customerClient.findById("c1")).thenReturn(ApiResponseUtil.success(HttpStatus.OK,
                new CustomerResponse("c1", "Jane", "Doe", "jane@example.com"), "Customer found"));
        doAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            assertNull(notification.getSentAt());
            assertEquals(event.eventId().toString(), notification.getId());
            return notification;
        }).doAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            assertNotNull(notification.getSentAt());
            return notification;
        }).when(repository).save(any(Notification.class));

        service.processOrderConfirmation(event);

        var order = inOrder(repository, emailService);
        order.verify(repository).findById(event.eventId().toString());
        order.verify(repository).save(any(Notification.class));
        order.verify(emailService).sendOrderConfirmationEmail(argThat(confirmation ->
                confirmation.customer().email().equals("jane@example.com")
                        && confirmation.products().getFirst().productId() == 3
                        && confirmation.paymentMethod() == PaymentMethod.VISA));
        order.verify(repository).save(any(Notification.class));
    }

    @Test
    void duplicateDeliveredEventsDoNotResendOrLookUpCustomer() throws Exception {
        var payment = paymentEvent();
        var delivered = Notification.builder().id(payment.eventId().toString()).sentAt(LocalDateTime.now()).build();
        when(repository.findById(payment.eventId().toString())).thenReturn(Optional.of(delivered));
        service.processPaymentConfirmation(payment);
        var order = new OrderConfirmationEvent(payment.eventId(), "10", "ORD-10", BigDecimal.TEN,
                PaymentMethod.VISA, "c1", List.of());
        service.processOrderConfirmation(order);
        verifyNoInteractions(emailService, customerClient);
        verify(repository, never()).save(any());
    }

    @Test
    void emailFailureLeavesNotificationUnsentAndRetryUsesPersistedSnapshot() throws Exception {
        var event = paymentEvent();
        var mapper = new NotificationMapper();
        var pending = Notification.builder().id(event.eventId().toString())
                .type(NotificationType.PAYMENT_CONFIRMATION).notificationDate(LocalDateTime.now())
                .paymentConfirmation(mapper.toPaymentConfirmation(event)).build();
        when(repository.findById(event.eventId().toString())).thenReturn(Optional.of(pending));
        doThrow(new MessagingException("SMTP unavailable")).doNothing()
                .when(emailService).sendPaymentSuccessEmail(pending.getPaymentConfirmation());

        assertThrows(MessagingException.class, () -> service.processPaymentConfirmation(event));
        assertNull(pending.getSentAt());
        verify(repository, never()).save(any());
        service.processPaymentConfirmation(event);
        assertNotNull(pending.getSentAt());
        verify(repository).save(pending);
        verify(emailService, times(2)).sendPaymentSuccessEmail(pending.getPaymentConfirmation());
        verifyNoInteractions(customerClient);
    }

    @Test
    void missingCustomerDataFailsBeforeSavingOrSending() {
        var event = new OrderConfirmationEvent(UUID.randomUUID(), "10", "ORD-10", BigDecimal.TEN,
                PaymentMethod.VISA, "c1", List.of());
        when(repository.findById(event.eventId().toString())).thenReturn(Optional.empty());
        assertThrows(IllegalStateException.class, () -> service.processOrderConfirmation(event));
        verify(repository, never()).save(any());
        verifyNoInteractions(emailService);
    }

    @Test
    void paymentPersistsEventSnapshotAndMarksDelivery() throws Exception {
        var event = paymentEvent();
        when(repository.findById(event.eventId().toString())).thenReturn(Optional.empty());
        service.processPaymentConfirmation(event);
        verify(emailService).sendPaymentSuccessEmail(argThat(confirmation ->
                confirmation.orderReference().equals("ORD-10") && confirmation.amount().equals(BigDecimal.TEN)
                        && confirmation.customerEmail().equals("jane@example.com")));
        verify(repository, times(2)).save(any(Notification.class));
        verifyNoInteractions(customerClient);
    }
}
