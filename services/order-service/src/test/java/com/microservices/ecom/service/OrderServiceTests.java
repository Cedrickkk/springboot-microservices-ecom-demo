package com.microservices.ecom.service;

import com.microservices.common.response.ApiResponseUtil;
import com.microservices.ecom.client.*;
import com.microservices.ecom.domain.*;
import com.microservices.ecom.dto.*;
import com.microservices.ecom.exception.*;
import com.microservices.ecom.mapper.OrderMapper;
import com.microservices.ecom.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.mockito.MockMakers;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrderServiceTests {
    private OrderRepository repository;
    private CustomerClient customers;
    private ProductClient products;
    private OrderService service;
    private final OrderRequest request = new OrderRequest(null, "ORD-1", BigDecimal.TEN,
            PaymentMethod.VISA, "customer-1", List.of(new ProductPurchaseRequest(1, 2.0)));
    @BeforeEach
    void setUp() {
        repository = mock(OrderRepository.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        customers = mock(CustomerClient.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        products = mock(ProductClient.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        service = new OrderService(repository, new OrderMapper(), customers, products);
    }
    @Test
    void creationValidatesCustomerPurchasesProductsAndLinksLines() {
        when(customers.existsById("customer-1")).thenReturn(ApiResponseUtil.success(HttpStatus.OK, true, "OK"));
        when(products.purchaseProducts(request.products())).thenReturn(ApiResponseUtil.success(
                HttpStatus.OK, List.of(new ProductPurchaseResponse(1, "Book", "Book", BigDecimal.valueOf(5), 2)), "OK"));
        when(repository.save(any())).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            assertEquals(request.amount(), order.getTotalAmount());
            assertEquals(1, order.getOrderLines().size());
            assertSame(order, order.getOrderLines().getFirst().getOrder());
            assertEquals(2, order.getOrderLines().getFirst().getQuantity());
            order.setId(7);
            return order;
        });
        assertEquals(7, service.createOrder(request));
        var sequence = inOrder(customers, products, repository);
        sequence.verify(customers).existsById("customer-1");
        sequence.verify(products).purchaseProducts(request.products());
        sequence.verify(repository).save(any());
    }
    @Test
    void missingCustomerDoesNotPurchaseOrPersist() {
        when(customers.existsById("customer-1")).thenReturn(ApiResponseUtil.success(HttpStatus.OK, false, "OK"));
        assertThrows(CustomerNotFoundException.class, () -> service.createOrder(request));
        verifyNoInteractions(products, repository);
    }
    @Test
    void rejectedPurchaseDoesNotPersist() {
        when(customers.existsById("customer-1")).thenReturn(ApiResponseUtil.success(HttpStatus.OK, true, "OK"));
        when(products.purchaseProducts(any())).thenThrow(new OrderPurchaseException("Insufficient stock"));
        assertThrows(OrderPurchaseException.class, () -> service.createOrder(request));
        verifyNoInteractions(repository);
    }
    @Test
    void missingOrderAndItsLinesReturnNotFound() {
        when(repository.findById(99)).thenReturn(Optional.empty());
        assertThrows(OrderNotFoundException.class, () -> service.findById(99));
        assertThrows(OrderNotFoundException.class, () -> service.findOrderLines(99));
    }
    @Test
    void retrievesPersistedLines() {
        var order = new OrderMapper().toEntity(request);
        when(repository.findById(7)).thenReturn(Optional.of(order));
        assertEquals(1, service.findOrderLines(7).getFirst().productId());
        assertEquals(2, service.findOrderLines(7).getFirst().quantity());
    }
}
