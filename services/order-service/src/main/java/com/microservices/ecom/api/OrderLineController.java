package com.microservices.ecom.api;

import com.microservices.common.response.ApiResponseUtil;
import com.microservices.common.response.SuccessApiResponse;
import com.microservices.ecom.dto.OrderLineResponse;
import com.microservices.ecom.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/order-lines")
@RequiredArgsConstructor
public class OrderLineController {
    private final OrderService service;

    @GetMapping("/order/{orderId}")
    public ResponseEntity<SuccessApiResponse<List<OrderLineResponse>>> findByOrderId(@PathVariable Integer orderId) {
        return ResponseEntity.ok(ApiResponseUtil.success(HttpStatus.OK, service.findOrderLines(orderId), "Order lines retrieved successfully."));
    }
}
