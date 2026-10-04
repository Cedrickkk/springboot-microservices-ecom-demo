package com.microservices.ecom.client;

import com.microservices.common.response.SuccessApiResponse;
import com.microservices.ecom.dto.PaymentRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "payment-service", url = "${application.config.payment-url}")
public interface PaymentClient {
    @PostMapping
    SuccessApiResponse<Integer> createPayment(@RequestBody PaymentRequest request);
}
