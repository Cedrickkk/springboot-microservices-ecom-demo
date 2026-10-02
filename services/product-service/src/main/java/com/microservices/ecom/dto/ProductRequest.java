package com.microservices.ecom.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ProductRequest(
        @Null(message = "Product ID must be omitted when creating a product.")
        Integer id,
        @NotBlank(message = "Product name is required.") @Size(max = 255)
        String name,
        @NotBlank(message = "Product description is required.") @Size(max = 255)
        String description,
        @NotNull @PositiveOrZero @DecimalMax("1.7976931348623157E308")
        Double availableQuantity,
        @NotNull @DecimalMin(value = "0", inclusive = false)
        @Digits(integer = 36, fraction = 2)
        BigDecimal price,
        @NotNull @Positive
        Integer categoryId
) {
}
