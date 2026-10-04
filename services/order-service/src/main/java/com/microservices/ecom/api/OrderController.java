package com.microservices.ecom.api;

import com.microservices.common.response.ApiResponseUtil;
import com.microservices.common.response.SuccessApiResponse;
import com.microservices.ecom.dto.OrderRequest;
import com.microservices.ecom.dto.OrderResponse;
import com.microservices.ecom.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService service;

    @PostMapping
    public ResponseEntity<SuccessApiResponse<Integer>> createOrder(@RequestBody @Valid OrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponseUtil.success(
                HttpStatus.CREATED, service.createOrder(request), "Order created successfully."));
    }

    @GetMapping
    public ResponseEntity<SuccessApiResponse<List<OrderResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponseUtil.success(HttpStatus.OK, service.findAll(), "Orders retrieved successfully."));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<SuccessApiResponse<OrderResponse>> findById(@PathVariable Integer orderId) {
        return ResponseEntity.ok(ApiResponseUtil.success(HttpStatus.OK, service.findById(orderId), "Order retrieved successfully."));
    }
}
