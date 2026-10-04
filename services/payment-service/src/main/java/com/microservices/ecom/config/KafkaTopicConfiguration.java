package com.microservices.ecom.config;

import com.microservices.common.messaging.EventDestinations;
import com.microservices.common.messaging.EventTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration(proxyBeanMethods = false)
public class KafkaTopicConfiguration {
    @Bean
    public NewTopic paymentConfirmationTopic(EventDestinations destinations,
            @Value("${application.messaging.topic-partitions:1}") int partitions,
            @Value("${application.messaging.topic-replicas:1}") int replicas) {
        return TopicBuilder.name(destinations.topic(EventTopics.PAYMENT_CONFIRMATION))
                .partitions(partitions).replicas(replicas).build();
    }
}
