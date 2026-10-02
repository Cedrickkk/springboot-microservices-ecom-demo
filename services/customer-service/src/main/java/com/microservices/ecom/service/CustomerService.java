package com.microservices.ecom.service;

import com.microservices.ecom.domain.Customer;
import com.microservices.ecom.dto.CustomerRequest;
import com.microservices.ecom.dto.CustomerResponse;
import com.microservices.ecom.exception.CustomerNotFoundException;
import com.microservices.ecom.mapper.CustomerMapper;
import com.microservices.ecom.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper mapper;

    public String createCustomer(CustomerRequest request) {
        Customer customer = customerRepository.save(mapper.toEntity(request));
        return customer.getId();
    }

    public List<CustomerResponse> findAllCustomers() {
        return customerRepository.findAll()
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    public CustomerResponse findById(String id) {
        return customerRepository.findById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new CustomerNotFoundException(
                        String.format("Customer with ID '%s' not found.", id)
                ));
    }

    public void updateCustomer(CustomerRequest request) {
        Customer customer = customerRepository.findById(request.id())
                .orElseThrow(() -> new CustomerNotFoundException(
                        String.format("Cannot update customer:: No customer found with ID '%s'", request.id())
                ));
        mergeCustomerInfo(customer, request);
        customerRepository.save(customer);
    }

    public void deleteCustomer(String id) {
        if (!customerRepository.existsById(id)) {
            throw new CustomerNotFoundException(
                    String.format("Cannot delete customer:: No customer found with ID '%s'", id)
            );
        }
        customerRepository.deleteById(id);
    }

    public boolean existsById(String id) {
        return customerRepository.existsById(id);
    }

    private void mergeCustomerInfo(Customer customer, CustomerRequest request) {
        if (request.firstname() != null && !request.firstname().isBlank()) {
            customer.setFirstname(request.firstname());
        }
        if (request.lastname() != null && !request.lastname().isBlank()) {
            customer.setLastname(request.lastname());
        }
        if (request.email() != null && !request.email().isBlank()) {
            customer.setEmail(request.email());
        }
        if (request.address() != null) {
            customer.setAddress(request.address());
        }
    }
}
