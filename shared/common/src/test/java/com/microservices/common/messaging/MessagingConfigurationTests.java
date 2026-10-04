package com.microservices.common.messaging;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class MessagingConfigurationTests {
    private final ApplicationContextRunner context = new ApplicationContextRunner()
            .withUserConfiguration(MessagingConfiguration.class)
            .withBean(JsonMapper.class, () -> JsonMapper.builder().build())
            .withBean(JdbcTemplate.class, () -> mock(JdbcTemplate.class))
            .withBean(KafkaTemplate.class, () -> mock(KafkaTemplate.class))
            .withBean(PlatformTransactionManager.class, () -> mock(PlatformTransactionManager.class));

    @Test
    void bindsDefaultsAndWiresExplicitlyImportedInfrastructure() {
        context.run(application -> {
            assertThat(application).hasNotFailed();
            var properties = application.getBean(MessagingProperties.class);
            assertThat(properties.outboxBatchSize()).isEqualTo(50);
            assertThat(properties.publishTimeoutMs()).isEqualTo(10000);
            assertThat(properties.outboxPollIntervalMs()).isEqualTo(500);
            assertThat(properties.outboxInitialDelayMs()).isEqualTo(1000);
            assertThat(application.getBean(EventDestinations.class).topic(EventTopics.ORDER_CONFIRMATION))
                    .isEqualTo("order-topic");
            assertThat(application).hasSingleBean(OutboxStore.class).hasSingleBean(OutboxRelay.class);
        });
    }

    @Test
    void bindsEnumTopicOverrideAndRelayLimits() {
        context.withPropertyValues("application.messaging.topics.ORDER_CONFIRMATION=custom-orders",
                "application.messaging.outbox-batch-size=10", "application.messaging.publish-timeout-ms=2500")
                .run(application -> {
                    assertThat(application).hasNotFailed();
                    var properties = application.getBean(MessagingProperties.class);
                    assertThat(properties.outboxBatchSize()).isEqualTo(10);
                    assertThat(properties.publishTimeoutMs()).isEqualTo(2500);
                    assertThat(application.getBean(EventDestinations.class).topic(EventTopics.ORDER_CONFIRMATION))
                            .isEqualTo("custom-orders");
                });
    }

    @Test
    void rejectsInvalidLimitsAtStartup() {
        for (var property : new String[]{"outbox-batch-size=0", "publish-timeout-ms=0",
                "outbox-poll-interval-ms=0", "outbox-initial-delay-ms=-1"}) {
            context.withPropertyValues("application.messaging." + property)
                    .run(application -> assertThat(application).hasFailed());
        }
    }

    @Test
    void rejectsBlankTopicAtStartup() {
        context.withPropertyValues("application.messaging.topics.ORDER_CONFIRMATION= ")
                .run(application -> assertThat(application).hasFailed());
    }
}
