package com.microservices.common.messaging;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class OutboxStore {
    private final OutboxRepository repository;
    private final EventCodec codec;
    private final EventDestinations destinations;

    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueue(EventTopics type, String aggregateId, MessagingEvent event) {
        var encoded = codec.encode(type, aggregateId, event);
        repository.insert(type, aggregateId, destinations.topic(type), encoded);
    }
}
