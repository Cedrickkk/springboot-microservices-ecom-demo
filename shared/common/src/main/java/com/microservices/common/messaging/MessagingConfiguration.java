package com.microservices.common.messaging;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import tools.jackson.databind.json.JsonMapper;

/** Import explicitly in each service that owns an outbox. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(MessagingProperties.class)
public class MessagingConfiguration {
    @Bean
    public EventCodec eventCodec(JsonMapper mapper) {
        return new EventCodec(mapper);
    }

    @Bean
    public EventDestinations eventDestinations(MessagingProperties properties) {
        return new EventDestinations(properties);
    }

    @Bean
    public OutboxRepository outboxRepository(JdbcTemplate jdbc) {
        return new OutboxRepository(jdbc);
    }

    @Bean
    public OutboxStore outboxStore(OutboxRepository repository, EventCodec codec, EventDestinations destinations) {
        return new OutboxStore(repository, codec, destinations);
    }

    @Bean
    public OutboxRelay outboxRelay(OutboxRepository repository, KafkaTemplate<String, Object> kafka,
                                  EventCodec codec, PlatformTransactionManager transactionManager,
                                  MessagingProperties properties) {
        return new OutboxRelay(repository, kafka, codec, transactionManager, properties);
    }
}
