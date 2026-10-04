package com.microservices.ecom;

import com.microservices.common.messaging.PaymentMethod;
import com.microservices.ecom.dto.PaymentCustomer;
import com.microservices.ecom.dto.PaymentRequest;
import com.microservices.ecom.kafka.PaymentProducer;
import com.microservices.ecom.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false", "spring.config.import=", "eureka.client.enabled=false",
        "spring.datasource.username=outbox_test", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate", "spring.kafka.admin.auto-create=false",
        "application.messaging.outbox-initial-delay-ms=3600000"})
@EnabledIfEnvironmentVariable(named = "PAYMENT_TEST_DATABASE_URL", matches = ".+")
class PaymentServiceApplicationTests {
    @Autowired
    private PaymentService service;
    @Autowired
    private JdbcTemplate jdbc;
    @MockitoSpyBean
    private PaymentProducer producer;

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getenv("PAYMENT_TEST_DATABASE_URL"));
    }

    @BeforeEach
    void clearDatabase() {
        jdbc.update("DELETE FROM outbox_events");
        jdbc.update("DELETE FROM payments");
    }

    private PaymentRequest request() {
        return new PaymentRequest(BigDecimal.TEN, PaymentMethod.VISA, 7, "ORD-7",
                new PaymentCustomer("c1", "Jane", "Doe", "jane@example.com"));
    }

    @Test
    void commitsPaymentAndConfirmationTogether() {
        var paymentId = service.createPayment(request());
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM payments WHERE id = ?", Integer.class, paymentId));
        var payload = jdbc.queryForMap("SELECT aggregate_id, event_type, payload::text FROM outbox_events");
        assertEquals(paymentId.toString(), payload.get("aggregate_id"));
        assertEquals("PAYMENT_CONFIRMATION", payload.get("event_type"));
        assertTrue(payload.get("payload").toString().contains("jane@example.com"));
    }

    @Test
    void failureAfterEnqueueRollsBackBothWrites() {
        doAnswer(invocation -> {
            invocation.callRealMethod();
            throw new IllegalStateException("failure after enqueue");
        }).when(producer).sendPaymentConfirmation(any());
        assertThrows(IllegalStateException.class, () -> service.createPayment(request()));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM payments", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM outbox_events", Integer.class));
    }
}
