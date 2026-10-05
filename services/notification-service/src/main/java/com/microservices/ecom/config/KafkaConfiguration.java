package com.microservices.ecom.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration(proxyBeanMethods = false)
public class KafkaConfiguration {
    @Bean
    public DeadLetterPublishingRecoverer deadLetterPublishingRecoverer(KafkaTemplate<String, String> template) {
        var recoverer = new DeadLetterPublishingRecoverer(template,
                (record, exception) -> new TopicPartition(record.topic() + ".DLT", record.partition()));
        recoverer.setFailIfSendResultIsError(true);
        return recoverer;
    }

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(DeadLetterPublishingRecoverer recoverer) {
        return new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 2L));
    }

    @Bean
    public NewTopic orderConfirmationTopic(
            @Value("${application.messaging.topics.ORDER_CONFIRMATION:order-topic}") String topic,
            @Value("${application.messaging.topic-partitions:1}") int partitions,
            @Value("${application.messaging.topic-replicas:1}") int replicas) {
        return TopicBuilder.name(topic).partitions(partitions).replicas(replicas).build();
    }

    @Bean
    public NewTopic paymentConfirmationTopic(
            @Value("${application.messaging.topics.PAYMENT_CONFIRMATION:payment-topic}") String topic,
            @Value("${application.messaging.topic-partitions:1}") int partitions,
            @Value("${application.messaging.topic-replicas:1}") int replicas) {
        return TopicBuilder.name(topic).partitions(partitions).replicas(replicas).build();
    }

    @Bean
    public NewTopic orderDeadLetterTopic(
            @Value("${application.messaging.topics.ORDER_CONFIRMATION:order-topic}") String topic,
            @Value("${application.messaging.topic-partitions:1}") int partitions,
            @Value("${application.messaging.topic-replicas:1}") int replicas) {
        return TopicBuilder.name(topic + ".DLT").partitions(partitions).replicas(replicas).build();
    }

    @Bean
    public NewTopic paymentDeadLetterTopic(
            @Value("${application.messaging.topics.PAYMENT_CONFIRMATION:payment-topic}") String topic,
            @Value("${application.messaging.topic-partitions:1}") int partitions,
            @Value("${application.messaging.topic-replicas:1}") int replicas) {
        return TopicBuilder.name(topic + ".DLT").partitions(partitions).replicas(replicas).build();
    }
}
