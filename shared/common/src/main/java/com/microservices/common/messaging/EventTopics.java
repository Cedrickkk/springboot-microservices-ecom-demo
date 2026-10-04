package com.microservices.common.messaging;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public enum EventTopics {
    ORDER_CONFIRMATION("order-topic"),
    PAYMENT_CONFIRMATION("payment-topic");

    private final String defaultTopic;
}
