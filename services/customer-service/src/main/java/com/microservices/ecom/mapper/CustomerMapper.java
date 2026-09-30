package com.microservices.ecom.mapper;

import com.microservices.ecom.domain.Customer;
import com.microservices.ecom.dto.CustomerRequest;
import com.microservices.ecom.dto.CustomerResponse;
import org.springframework.stereotype.Component;

@Component
public class CustomerMapper {

    public Customer toEntity(CustomerRequest request) {
        if (request == null) {
            return null;
        }
        return Customer.builder()
                .id(request.id())
                .firstname(request.firstname())
                .lastname(request.lastname())
                .email(request.email())
                .address(request.address())
                .build();
    }

    public CustomerResponse toResponse(Customer customer) {
        if (customer == null) {
            return null;
        }
        return new CustomerResponse(
                customer.getId(),
                customer.getFirstname(),
                customer.getLastname(),
                customer.getEmail(),
                customer.getAddress()
        );
    }
}