package com.microservices.common.messaging;

import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import tools.jackson.databind.json.JsonMapper;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OutboxStoreTests {
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final EventCodec codec = new EventCodec(JsonMapper.builder().build());
    private final OrderConfirmationEvent event = new OrderConfirmationEvent(UUID.randomUUID(), "7", "ORD-7",
            BigDecimal.TEN, PaymentMethod.VISA, "c1", List.of());
    private final OutboxStore store = new OutboxStore(new OutboxRepository(jdbc), codec, new EventDestinations(
            new MessagingProperties(java.util.Map.of(EventTopics.ORDER_CONFIRMATION, "custom-orders"), 50, 10000, 500, 1000)));

    @Test
    void validatesThenLocksAggregateBeforeInsertingRegisteredEvent() {
        store.enqueue(EventTopics.ORDER_CONFIRMATION, "7", event);
        var sequence = inOrder(jdbc);
        sequence.verify(jdbc).execute(any(PreparedStatementCreator.class), any(PreparedStatementCallback.class));
        sequence.verify(jdbc).update(contains("INSERT INTO outbox_events"), eq(event.eventId()), eq("7"),
                eq("ORDER_CONFIRMATION"), eq("custom-orders"), eq(codec.encode(EventTopics.ORDER_CONFIRMATION, "7", event).payload()));
    }

    @Test
    void invalidIdentityDoesNotTouchDatabase() {
        assertThrows(IllegalArgumentException.class, () -> store.enqueue(EventTopics.ORDER_CONFIRMATION, "8", event));
        verifyNoInteractions(jdbc);
    }

    @Test
    void mandatoryPropagationRejectsEnqueueOutsideBusinessTransaction() {
        var interceptor = new TransactionInterceptor();
        interceptor.setTransactionManager(new DataSourceTransactionManager(mock(DataSource.class)));
        interceptor.setTransactionAttributeSource(new AnnotationTransactionAttributeSource());
        var factory = new ProxyFactory(store);
        factory.setProxyTargetClass(true);
        factory.addAdvice(interceptor);
        var transactionalStore = (OutboxStore) factory.getProxy();
        assertThrows(IllegalTransactionStateException.class, () ->
                transactionalStore.enqueue(EventTopics.ORDER_CONFIRMATION, "7", event));
        verifyNoInteractions(jdbc);
    }
}
