package com.microservices.ecom.service;

import com.microservices.common.messaging.PaymentConfirmationEvent;
import com.microservices.ecom.dto.PaymentRequest;
import com.microservices.ecom.kafka.PaymentProducer;
import com.microservices.ecom.mapper.PaymentMapper;
import com.microservices.ecom.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository repository;
    private final PaymentMapper mapper;
    private final PaymentProducer producer;

    @Transactional
    public Integer createPayment(PaymentRequest request) {
        var payment = repository.save(mapper.toEntity(request));
        var customer = request.customer();
        producer.sendPaymentConfirmation(new PaymentConfirmationEvent(UUID.randomUUID(), payment.getId().toString(),
                payment.getOrderId(), request.orderReference(), payment.getAmount(), payment.getPaymentMethod(),
                customer.id(), customer.firstname(), customer.lastname(), customer.email()));
        return payment.getId();
    }
}
