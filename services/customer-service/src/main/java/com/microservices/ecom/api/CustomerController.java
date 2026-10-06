package com.microservices.ecom.api;

import com.microservices.common.response.ApiResponseUtil;
import com.microservices.common.response.SuccessApiResponse;
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
    public ResponseEntity<SuccessApiResponse<CustomerResponse>> createCustomer(@RequestBody @Valid CustomerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponseUtil.success(
                HttpStatus.CREATED, customerService.createCustomer(request), "Customer created successfully."));
    }

    @GetMapping
    public ResponseEntity<SuccessApiResponse<List<CustomerResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponseUtil.success(
                HttpStatus.OK, customerService.findAllCustomers(), "Customers retrieved successfully."));
    }

    @GetMapping("/{customerId}")
    public ResponseEntity<SuccessApiResponse<CustomerResponse>> findById(@PathVariable String customerId) {
        return ResponseEntity.ok(ApiResponseUtil.success(
                HttpStatus.OK, customerService.findById(customerId), "Customer retrieved successfully."));
    }

    @GetMapping("/exists/{customerId}")
    public ResponseEntity<SuccessApiResponse<Boolean>> existsById(@PathVariable String customerId) {
        return ResponseEntity.ok(ApiResponseUtil.success(
                HttpStatus.OK, customerService.existsById(customerId), "Customer existence checked successfully."));
    }

    @PutMapping
    public ResponseEntity<SuccessApiResponse<Void>> updateCustomer(@RequestBody @Valid CustomerRequest request) {
        customerService.updateCustomer(request);
        return ResponseEntity.accepted().body(ApiResponseUtil.success(
                HttpStatus.ACCEPTED, null, "Customer updated successfully."));
    }

    @DeleteMapping("/{customerId}")
    public ResponseEntity<SuccessApiResponse<Void>> deleteCustomer(@PathVariable String customerId) {
        customerService.deleteCustomer(customerId);
        return ResponseEntity.ok(ApiResponseUtil.success(HttpStatus.OK, null, "Customer deleted successfully."));
    }
}
