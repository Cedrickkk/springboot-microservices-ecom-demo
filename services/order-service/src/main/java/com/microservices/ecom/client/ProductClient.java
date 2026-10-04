package com.microservices.ecom.client;

import com.microservices.common.response.SuccessApiResponse;
import com.microservices.ecom.dto.ProductPurchaseRequest;
import com.microservices.ecom.dto.ProductPurchaseResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "product-service", url = "${application.config.product-url}")
public interface ProductClient {
    @PostMapping("/purchase")
    SuccessApiResponse<List<ProductPurchaseResponse>> purchaseProducts(
            @RequestBody List<ProductPurchaseRequest> requests
    );
}
