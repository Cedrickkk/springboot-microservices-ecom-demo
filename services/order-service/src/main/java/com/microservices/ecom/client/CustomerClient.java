package com.microservices.ecom.client;

import com.microservices.common.response.SuccessApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "customer-service", url = "${application.config.customer-url}")
public interface CustomerClient {
    @GetMapping("/exists/{customerId}")
    SuccessApiResponse<Boolean> existsById(@PathVariable String customerId);
}
