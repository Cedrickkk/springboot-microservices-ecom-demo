package com.microservices.ecom.dto;

public record OrderLineResponse(Integer id, double quantity, Integer productId) {
}
