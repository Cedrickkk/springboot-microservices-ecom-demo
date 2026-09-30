package com.microservices.ecom.dto;

import com.microservices.ecom.domain.Address;

public record CustomerResponse(
        String id,
        String firstname,
        String lastname,
        String email,
        Address address
) {
}
