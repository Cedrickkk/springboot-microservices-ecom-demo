package com.microservices.ecom.mapper;

import com.microservices.common.messaging.OrderConfirmationEvent;
import com.microservices.common.messaging.PaymentConfirmationEvent;
import com.microservices.ecom.domain.*;
import com.microservices.ecom.dto.CustomerResponse;
import com.microservices.ecom.dto.NotificationResponse;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {
    public OrderConfirmation toOrderConfirmation(OrderConfirmationEvent event, CustomerResponse customer) {
        return new OrderConfirmation(event.orderReference(), event.totalAmount(), event.paymentMethod(),
                new Customer(customer.id(), customer.firstname(), customer.lastname(), customer.email()),
                event.products().stream().map(product -> new Product(product.productId(), product.name(),
                        product.description(), product.price(), product.quantity())).toList());
    }

    public PaymentConfirmation toPaymentConfirmation(PaymentConfirmationEvent event) {
        return new PaymentConfirmation(event.orderReference(), event.amount(), event.paymentMethod(),
                event.customerFirstname(), event.customerLastname(), event.customerEmail());
    }

    public NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(notification.getId(), notification.getType(), notification.getNotificationDate(),
                notification.getSentAt(), notification.getOrderConfirmation(), notification.getPaymentConfirmation());
    }
}
