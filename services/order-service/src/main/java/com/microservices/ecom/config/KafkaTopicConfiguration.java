package com.microservices.ecom.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfiguration {
    @Bean
    public NewTopic orderConfirmationTopic(
            @Value("${application.messaging.topics.ORDER_CONFIRMATION:order-topic}") String topic,
            @Value("${application.messaging.topic-partitions:1}") int partitions,
            @Value("${application.messaging.topic-replicas:1}") int replicas) {
        return TopicBuilder.name(topic).partitions(partitions).replicas(replicas).build();
    }
}
