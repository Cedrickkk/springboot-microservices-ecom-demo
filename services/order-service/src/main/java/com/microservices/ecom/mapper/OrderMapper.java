package com.microservices.ecom.mapper;

import com.microservices.ecom.domain.Order;
import com.microservices.ecom.domain.OrderLine;
import com.microservices.ecom.dto.OrderLineResponse;
import com.microservices.ecom.dto.OrderRequest;
import com.microservices.ecom.dto.OrderResponse;
import org.springframework.stereotype.Component;

@Component
public class OrderMapper {
    public Order toEntity(OrderRequest request) {
        var order = Order.builder()
                .reference(request.reference())
                .customerId(request.customerId())
                .totalAmount(request.amount())
                .paymentMethod(request.paymentMethod())
                .build();
        request.products().forEach(product ->
                order.getOrderLines().add(
                        OrderLine.builder()
                                .order(order)
                                .productId(product.productId())
                                .quantity(product.quantity())
                                .build()
                )
        );
        return order;
    }

    public OrderResponse toResponse(Order order) {
        return new OrderResponse(order.getId(), order.getReference(), order.getTotalAmount(),
                order.getPaymentMethod(), order.getCustomerId());
    }

    public OrderLineResponse toLineResponse(OrderLine line) {
        return new OrderLineResponse(line.getId(), line.getQuantity(), line.getProductId());
    }
}
