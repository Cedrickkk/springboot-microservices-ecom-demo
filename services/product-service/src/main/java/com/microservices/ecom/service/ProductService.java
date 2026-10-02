package com.microservices.ecom.service;

import com.microservices.ecom.domain.Product;
import com.microservices.ecom.dto.ProductPurchaseRequest;
import com.microservices.ecom.dto.ProductPurchaseResponse;
import com.microservices.ecom.dto.ProductRequest;
import com.microservices.ecom.dto.ProductResponse;
import com.microservices.ecom.exception.CategoryNotFoundException;
import com.microservices.ecom.exception.ProductNotFoundException;
import com.microservices.ecom.exception.ProductPurchaseException;
import com.microservices.ecom.mapper.ProductMapper;
import com.microservices.ecom.repository.CategoryRepository;
import com.microservices.ecom.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper mapper;

    @Transactional
    public Integer createProduct(ProductRequest request) {
        var category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new CategoryNotFoundException(
                        "Category with ID '" + request.categoryId() + "' not found."));
        return productRepository.save(mapper.toEntity(request, category)).getId();
    }

    @Transactional(readOnly = true)
    public ProductResponse findById(Integer id) {
        return productRepository.findById(id).map(mapper::toResponse)
                .orElseThrow(() -> new ProductNotFoundException("Product with ID '" + id + "' not found."));
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> findAll() {
        return productRepository.findAll().stream().map(mapper::toResponse).toList();
    }

    @Transactional
    public List<ProductPurchaseResponse> purchaseProducts(List<ProductPurchaseRequest> requests) {
        Map<Integer, Double> quantities = aggregatePurchaseQuantities(requests);
        Map<Integer, Product> products = loadProductsForPurchase(quantities);
        validateStock(products, quantities);
        deductStock(products, quantities);

        return requests.stream()
                .map(request -> mapper.toPurchaseResponse(products.get(request.productId()), request.quantity()))
                .toList();
    }

    private Map<Integer, Double> aggregatePurchaseQuantities(List<ProductPurchaseRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new ProductPurchaseException("At least one product is required for purchase.");
        }
        Map<Integer, Double> quantities = new TreeMap<>();
        for (var request : requests) {
            validatePurchaseRequest(request);
            double total = quantities.merge(request.productId(), request.quantity(), Double::sum);
            if (!Double.isFinite(total)) {
                throw new ProductPurchaseException("Total purchase quantity is too large.");
            }
        }
        return quantities;
    }

    private void validatePurchaseRequest(ProductPurchaseRequest request) {
        if (request == null || request.productId() == null || request.productId() <= 0
                || request.quantity() == null || !Double.isFinite(request.quantity())
                || request.quantity() <= 0) {
            throw new ProductPurchaseException("Product IDs and quantities must be positive and finite.");
        }
    }

    private Map<Integer, Product> loadProductsForPurchase(Map<Integer, Double> quantities) {
        return productRepository.findAllForPurchase(List.copyOf(quantities.keySet())).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
    }

    private void validateStock(Map<Integer, Product> products, Map<Integer, Double> quantities) {
        quantities.forEach((id, quantity) -> {
            Product product = products.get(id);
            if (product == null) {
                throw new ProductPurchaseException("Product with ID '" + id + "' not found for purchase.");
            }
            if (product.getAvailableQuantity() < quantity) {
                throw new ProductPurchaseException("Insufficient stock for product with ID '" + id + "'.");
            }
        });
    }

    private void deductStock(Map<Integer, Product> products, Map<Integer, Double> quantities) {
        quantities.forEach((id, quantity) -> {
            Product product = products.get(id);
            product.setAvailableQuantity(product.getAvailableQuantity() - quantity);
        });
    }
}
