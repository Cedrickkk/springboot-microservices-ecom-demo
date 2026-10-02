package com.microservices.ecom.api;

import com.microservices.common.response.ApiResponseUtil;
import com.microservices.common.response.SuccessApiResponse;
import com.microservices.ecom.dto.*;
import com.microservices.ecom.service.ProductService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    @PostMapping
    public ResponseEntity<SuccessApiResponse<Integer>> createProduct(@RequestBody @Valid ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponseUtil.success(
                HttpStatus.CREATED, productService.createProduct(request), "Product created successfully."));
    }

    @GetMapping
    public ResponseEntity<SuccessApiResponse<List<ProductResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponseUtil.success(productService.findAll(), "Products retrieved successfully."));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<SuccessApiResponse<ProductResponse>> findById(@PathVariable Integer productId) {
        return ResponseEntity.ok(ApiResponseUtil.success(productService.findById(productId), "Product retrieved successfully."));
    }

    @PostMapping("/purchase")
    public ResponseEntity<SuccessApiResponse<List<ProductPurchaseResponse>>> purchaseProducts(
            @RequestBody @NotEmpty List<@Valid ProductPurchaseRequest> requests) {
        return ResponseEntity.ok(ApiResponseUtil.success(productService.purchaseProducts(requests), "Products purchased successfully."));
    }
}
