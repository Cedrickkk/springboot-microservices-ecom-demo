package com.microservices.ecom.mapper;

import com.microservices.ecom.domain.Category;
import com.microservices.ecom.domain.Product;
import com.microservices.ecom.dto.*;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {
    public Product toEntity(ProductRequest request, Category category) {
        return Product.builder()
                .name(request.name())
                .description(request.description())
                .availableQuantity(request.availableQuantity())
                .price(request.price())
                .category(category)
                .build();
    }

    public ProductResponse toResponse(Product product) {
        Category category = product.getCategory();
        return new ProductResponse(product.getId(), product.getName(), product.getDescription(),
                product.getAvailableQuantity(), product.getPrice(),
                category == null ? null : category.getId(),
                category == null ? null : category.getName(),
                category == null ? null : category.getDescription());
    }

    public ProductPurchaseResponse toPurchaseResponse(Product product, double quantity) {
        return new ProductPurchaseResponse(product.getId(), product.getName(),
                product.getDescription(), product.getPrice(), quantity);
    }
}
