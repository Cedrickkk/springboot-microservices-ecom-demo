package com.microservices.ecom.domain;

public record Customer(
        String id,
        String firstname,
        String lastname,
        String email
) {
}
