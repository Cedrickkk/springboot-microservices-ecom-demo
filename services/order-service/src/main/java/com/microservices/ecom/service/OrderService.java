package com.microservices.ecom.service;

import com.microservices.common.messaging.OrderConfirmationEvent;
import com.microservices.ecom.client.CustomerClient;
import com.microservices.ecom.client.ProductClient;
import com.microservices.ecom.domain.Order;
import com.microservices.ecom.dto.OrderLineResponse;
import com.microservices.ecom.dto.OrderRequest;
import com.microservices.ecom.dto.OrderResponse;
import com.microservices.ecom.exception.CustomerNotFoundException;
import com.microservices.ecom.exception.OrderNotFoundException;
import com.microservices.ecom.exception.OrderPurchaseException;
import com.microservices.ecom.kafka.OrderProducer;
import com.microservices.ecom.mapper.OrderMapper;
import com.microservices.ecom.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository repository;
    private final OrderMapper mapper;
    private final CustomerClient customerClient;
    private final ProductClient productClient;
    private final OrderProducer producer;

    @Transactional
    public Integer createOrder(OrderRequest request) {
        var customer = customerClient.existsById(request.customerId());
        if (customer == null || !Boolean.TRUE.equals(customer.getData())) {
            throw new CustomerNotFoundException("Customer with ID '" + request.customerId() + "' not found.");
        }
        var purchase = productClient.purchaseProducts(request.products());
        if (purchase == null || purchase.getData() == null || purchase.getData().isEmpty()) {
            throw new OrderPurchaseException("Product service returned an invalid purchase response.");
        }
        var order = repository.save(mapper.toEntity(request));
        var purchasedProducts = purchase.getData().stream().map(product ->
                new OrderConfirmationEvent.Product(product.productId(), product.name(), product.description(),
                        product.price(), product.quantity())).toList();
        producer.sendOrderConfirmation(new OrderConfirmationEvent(UUID.randomUUID(), order.getId().toString(),
                order.getReference(), order.getTotalAmount(), order.getPaymentMethod(), order.getCustomerId(),
                purchasedProducts));
        // TODO: start payment processing.
        return order.getId();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> findAll() {
        return repository.findAll().stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse findById(Integer id) {
        return mapper.toResponse(findOrder(id));
    }

    @Transactional(readOnly = true)
    public List<OrderLineResponse> findOrderLines(Integer id) {
        return findOrder(id).getOrderLines().stream().map(mapper::toLineResponse).toList();
    }

    private Order findOrder(Integer id) {
        return repository.findById(id).orElseThrow(() ->
                new OrderNotFoundException("Order with ID '" + id + "' not found."));
    }
}
