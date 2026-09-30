package com.microservices.ecom.api;

import com.microservices.ecom.dto.CustomerRequest;
import com.microservices.ecom.dto.CustomerResponse;
import com.microservices.ecom.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    public ResponseEntity<String> createCustomer(@RequestBody @Valid CustomerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customerService.createCustomer(request));
    }

    @GetMapping
    public ResponseEntity<List<CustomerResponse>> findAll() {
        return ResponseEntity.ok(customerService.findAllCustomers());
    }

    @GetMapping("/{customerId}")
    public ResponseEntity<CustomerResponse> findById(@PathVariable String customerId) {
        return ResponseEntity.ok(customerService.findById(customerId));
    }

    @GetMapping("/exists/{customerId}")
    public ResponseEntity<Boolean> existsById(@PathVariable String customerId) {
        return ResponseEntity.ok(customerService.existsById(customerId));
    }

    @PutMapping
    public ResponseEntity<Void> updateCustomer(@RequestBody @Valid CustomerRequest request) {
        customerService.updateCustomer(request);
        return ResponseEntity.accepted().build();
    }

    @DeleteMapping("/{customerId}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable String customerId) {
        customerService.deleteCustomer(customerId);
        return ResponseEntity.noContent().build();
    }
}
