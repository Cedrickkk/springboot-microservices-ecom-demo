package com.microservices.ecom.dto;

import com.microservices.common.messaging.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PaymentRequest(
        @NotNull @Positive @Digits(integer = 36, fraction = 2) BigDecimal amount,
        @NotNull PaymentMethod paymentMethod,
        @NotNull @Positive Integer orderId,
        @NotBlank @Size(max = 255) String orderReference,
        @NotNull @Valid PaymentCustomer customer) {
}
