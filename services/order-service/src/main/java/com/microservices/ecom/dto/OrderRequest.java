package com.microservices.ecom.dto;

import com.microservices.common.messaging.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public record OrderRequest(
        @Null
        Integer id,
        @NotBlank
        @Size(max = 255)
        String reference,
        @NotNull
        @Positive
        @Digits(integer = 36, fraction = 2)
        BigDecimal amount,
        @NotNull
        PaymentMethod paymentMethod,
        @NotBlank
        @Size(max = 255)
        String customerId,
        @NotEmpty
        List<@NotNull @Valid ProductPurchaseRequest> products
) {
}
