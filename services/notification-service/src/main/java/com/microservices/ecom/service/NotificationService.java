package com.microservices.ecom.service;

import com.microservices.common.messaging.OrderConfirmationEvent;
import com.microservices.common.messaging.PaymentConfirmationEvent;
import com.microservices.ecom.client.CustomerClient;
import com.microservices.ecom.domain.Notification;
import com.microservices.ecom.domain.NotificationType;
import com.microservices.ecom.dto.NotificationResponse;
import com.microservices.ecom.email.EmailService;
import com.microservices.ecom.exception.NotificationNotFoundException;
import com.microservices.ecom.mapper.NotificationMapper;
import com.microservices.ecom.repository.NotificationRepository;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository repository;
    private final NotificationMapper mapper;
    private final CustomerClient customerClient;
    private final EmailService emailService;

    public List<NotificationResponse> findAll(NotificationType type) {
        var notifications = type == null
                ? repository.findAll(Sort.by(Sort.Direction.DESC, "notificationDate"))
                : repository.findAllByTypeOrderByNotificationDateDesc(type);
        return notifications.stream().map(mapper::toResponse).toList();
    }

    public NotificationResponse findById(String notificationId) {
        return repository.findById(notificationId).map(mapper::toResponse)
                .orElseThrow(() -> new NotificationNotFoundException("Notification not found with ID: " + notificationId));
    }

    public void processOrderConfirmation(OrderConfirmationEvent event) throws MessagingException {
        var notification = repository.findById(event.eventId().toString()).orElse(null);
        if (notification != null && notification.getSentAt() != null) {
            return;
        }
        if (notification == null) {
            var response = customerClient.findById(event.customerId());
            if (response == null || response.getData() == null) {
                throw new IllegalStateException("Customer service returned no customer for ID: " + event.customerId());
            }
            notification = Notification.builder()
                    .id(event.eventId().toString())
                    .type(NotificationType.ORDER_CONFIRMATION)
                    .notificationDate(LocalDateTime.now())
                    .orderConfirmation(mapper.toOrderConfirmation(event, response.getData()))
                    .build();
            repository.save(notification);
        }
        emailService.sendOrderConfirmationEmail(notification.getOrderConfirmation());
        markSent(notification);
    }

    public void processPaymentConfirmation(PaymentConfirmationEvent event) throws MessagingException {
        var notification = repository.findById(event.eventId().toString()).orElse(null);
        if (notification != null && notification.getSentAt() != null) {
            return;
        }
        if (notification == null) {
            notification = Notification.builder()
                    .id(event.eventId().toString())
                    .type(NotificationType.PAYMENT_CONFIRMATION)
                    .notificationDate(LocalDateTime.now())
                    .paymentConfirmation(mapper.toPaymentConfirmation(event))
                    .build();
            repository.save(notification);
        }
        emailService.sendPaymentSuccessEmail(notification.getPaymentConfirmation());
        markSent(notification);
    }

    private void markSent(Notification notification) {
        notification.setSentAt(LocalDateTime.now());
        repository.save(notification);
    }
}
