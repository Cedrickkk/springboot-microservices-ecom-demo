package com.microservices.ecom;

import com.microservices.common.messaging.PaymentMethod;
import com.microservices.common.response.ApiResponseUtil;
import com.microservices.ecom.client.CustomerClient;
import com.microservices.ecom.client.PaymentClient;
import com.microservices.ecom.client.ProductClient;
import com.microservices.ecom.dto.CustomerResponse;
import com.microservices.ecom.dto.OrderRequest;
import com.microservices.ecom.dto.ProductPurchaseRequest;
import com.microservices.ecom.dto.ProductPurchaseResponse;
import com.microservices.ecom.exception.PaymentProcessingException;
import com.microservices.ecom.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false", "spring.config.import=", "eureka.client.enabled=false",
        "spring.datasource.username=outbox_test", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=update", "spring.kafka.admin.auto-create=false",
        "spring.flyway.placeholders.orderConfirmationTopic=test-orders",
        "application.config.customer-url=http://localhost/customers",
        "application.config.product-url=http://localhost/products",
        "application.config.payment-url=http://localhost/payments",
        "application.messaging.outbox-initial-delay-ms=3600000"})
@EnabledIfEnvironmentVariable(named = "ORDER_TEST_DATABASE_URL", matches = ".+")
class OrderPaymentTransactionTests {
    @Autowired
    private OrderService service;
    @Autowired
    private JdbcTemplate jdbc;
    @MockitoBean
    private CustomerClient customers;
    @MockitoBean
    private ProductClient products;
    @MockitoBean
    private PaymentClient payments;

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getenv("ORDER_TEST_DATABASE_URL"));
    }

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM outbox_events");
        jdbc.update("DELETE FROM customer_order_lines");
        jdbc.update("DELETE FROM customer_orders");
        when(customers.findById("c1")).thenReturn(ApiResponseUtil.success(HttpStatus.OK,
                new CustomerResponse("c1", "Jane", "Doe", "jane@example.com"), "OK"));
        when(products.purchaseProducts(any())).thenReturn(ApiResponseUtil.success(HttpStatus.OK,
                List.of(new ProductPurchaseResponse(1, "Book", "Book", BigDecimal.TEN, 1)), "OK"));
    }

    private OrderRequest request() {
        return new OrderRequest(null, "ORD-1", BigDecimal.TEN, PaymentMethod.VISA, "c1",
                List.of(new ProductPurchaseRequest(1, 1.0)));
    }

    @Test
    void successfulPaymentAllowsOrderAndConfirmationToCommit() {
        when(payments.createPayment(any())).thenAnswer(invocation -> {
            com.microservices.ecom.dto.PaymentRequest payment = invocation.getArgument(0);
            assertNotNull(payment.orderId());
            assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM customer_orders WHERE id = ?", Integer.class,
                    payment.orderId()));
            return ApiResponseUtil.success(HttpStatus.CREATED, 42, "OK");
        });
        var id = service.createOrder(request());
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM customer_orders WHERE id = ?", Integer.class, id));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM outbox_events", Integer.class));
    }

    @Test
    void invalidPaymentResponseRollsBackFlushedOrderLinesAndOutbox() {
        when(payments.createPayment(any())).thenReturn(ApiResponseUtil.success(HttpStatus.CREATED, null, "OK"));
        assertThrows(PaymentProcessingException.class, () -> service.createOrder(request()));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM customer_orders", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM customer_order_lines", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM outbox_events", Integer.class));
    }
}
