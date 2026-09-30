package com.microservices.ecom.repository;

import com.microservices.ecom.domain.Customer;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CustomerRepository extends MongoRepository<Customer, String> {
}
