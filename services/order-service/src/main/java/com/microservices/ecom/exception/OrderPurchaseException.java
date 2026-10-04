package com.microservices.ecom.exception;

public class OrderPurchaseException extends RuntimeException {
    public OrderPurchaseException(String message) {
        super(message);
    }
}
