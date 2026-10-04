package com.microservices.ecom.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ProductPurchaseRequest(
        @NotNull
        @Positive
        Integer productId,
        @NotNull
        @Positive
        @DecimalMax("1.7976931348623157E308")
        Double quantity
) {
}
