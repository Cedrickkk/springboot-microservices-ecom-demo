package com.microservices.ecom.service;

import com.microservices.ecom.domain.Category;
import com.microservices.ecom.domain.Product;
import com.microservices.ecom.dto.ProductPurchaseRequest;
import com.microservices.ecom.dto.ProductRequest;
import com.microservices.ecom.exception.CategoryNotFoundException;
import com.microservices.ecom.exception.ProductNotFoundException;
import com.microservices.ecom.exception.ProductPurchaseException;
import com.microservices.ecom.mapper.ProductMapper;
import com.microservices.ecom.repository.CategoryRepository;
import com.microservices.ecom.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockMakers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProductServiceTests {
    private ProductRepository products;
    private CategoryRepository categories;
    private ProductService service;

    @BeforeEach
    void setUp() {
        products = mock(ProductRepository.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        categories = mock(CategoryRepository.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        service = new ProductService(products, categories, new ProductMapper());
    }

    @Test
    void missingProductThrowsDomainException() {
        when(products.findById(99)).thenReturn(Optional.empty());
        assertThrows(ProductNotFoundException.class, () -> service.findById(99));
    }

    @Test
    void creationUsesExistingCategoryAndGeneratedId() {
        var category = Category.builder().id(3).name("Books").build();
        when(categories.findById(3)).thenReturn(Optional.of(category));
        when(products.save(any(Product.class))).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            assertSame(category, product.getCategory());
            assertNull(product.getId());
            product.setId(10);
            return product;
        });
        assertEquals(10, service.createProduct(request()));
    }

    @Test
    void missingCategoryPreventsCreation() {
        when(categories.findById(3)).thenReturn(Optional.empty());
        assertThrows(CategoryNotFoundException.class, () -> service.createProduct(request()));
        verifyNoInteractions(products);
    }

    @Test
    void purchaseMatchesIdsRegardlessOfInputOrderAndAggregatesDuplicates() {
        Product first = product(1, 10);
        Product second = product(2, 8);
        when(products.findAllForPurchase(List.of(1, 2))).thenReturn(List.of(first, second));
        var result = service.purchaseProducts(List.of(
                new ProductPurchaseRequest(2, 3.0),
                new ProductPurchaseRequest(1, 2.0),
                new ProductPurchaseRequest(2, 1.0)));
        assertEquals(8, first.getAvailableQuantity());
        assertEquals(4, second.getAvailableQuantity());
        assertEquals(List.of(2, 1, 2), result.stream().map(r -> r.productId()).toList());
        assertEquals(List.of(3.0, 2.0, 1.0), result.stream().map(r -> r.quantity()).toList());
    }

    @Test
    void insufficientAggregateStockDoesNotChangeAnyProduct() {
        Product first = product(1, 10);
        Product second = product(2, 3);
        when(products.findAllForPurchase(List.of(1, 2))).thenReturn(List.of(first, second));
        assertThrows(ProductPurchaseException.class, () -> service.purchaseProducts(List.of(
                new ProductPurchaseRequest(1, 2.0),
                new ProductPurchaseRequest(2, 2.0),
                new ProductPurchaseRequest(2, 2.0))));
        assertEquals(10, first.getAvailableQuantity());
        assertEquals(3, second.getAvailableQuantity());
    }

    @Test
    void missingProductInPurchaseDoesNotChangeStock() {
        Product product = product(1, 10);
        when(products.findAllForPurchase(List.of(1, 2))).thenReturn(List.of(product));
        assertThrows(ProductPurchaseException.class, () -> service.purchaseProducts(List.of(
                new ProductPurchaseRequest(1, 2.0), new ProductPurchaseRequest(2, 1.0))));
        assertEquals(10, product.getAvailableQuantity());
    }

    @Test
    void invalidQuantitiesAndEmptyPurchasesNeverReachDatabase() {
        assertThrows(ProductPurchaseException.class, () -> service.purchaseProducts(null));
        assertThrows(ProductPurchaseException.class, () -> service.purchaseProducts(List.of()));
        for (Double quantity : new Double[]{null, 0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertThrows(ProductPurchaseException.class, () -> service.purchaseProducts(
                    List.of(new ProductPurchaseRequest(1, quantity))));
        }
        verifyNoInteractions(products);
    }

    @Test
    void invalidProductIdsAndNullLinesNeverReachDatabase() {
        assertThrows(ProductPurchaseException.class, () -> service.purchaseProducts(
                java.util.Collections.singletonList(null)));
        for (Integer id : new Integer[]{null, 0, -1}) {
            assertThrows(ProductPurchaseException.class, () -> service.purchaseProducts(
                    List.of(new ProductPurchaseRequest(id, 1.0))));
        }
        verifyNoInteractions(products);
    }

    @Test
    void aggregateQuantityOverflowNeverReachesDatabase() {
        assertThrows(ProductPurchaseException.class, () -> service.purchaseProducts(List.of(
                new ProductPurchaseRequest(1, Double.MAX_VALUE),
                new ProductPurchaseRequest(1, Double.MAX_VALUE))));
        verifyNoInteractions(products);
    }

    @Test
    void purchaseCanConsumeExactlyTheRemainingStock() {
        Product product = product(1, 2.5);
        when(products.findAllForPurchase(List.of(1))).thenReturn(List.of(product));
        var result = service.purchaseProducts(List.of(new ProductPurchaseRequest(1, 2.5)));
        assertEquals(0, product.getAvailableQuantity());
        assertEquals(2.5, result.getFirst().quantity());
    }

    @Test
    void lookupsReturnMappedProducts() {
        Product product = product(1, 10);
        when(products.findById(1)).thenReturn(Optional.of(product));
        when(products.findAll()).thenReturn(List.of(product));
        assertEquals(1, service.findById(1).id());
        assertEquals("Product 1", service.findAll().getFirst().name());
    }

    private ProductRequest request() {
        return new ProductRequest(null, "Book", "A book", 10.0, new BigDecimal("12.50"), 3);
    }

    private Product product(int id, double quantity) {
        return Product.builder().id(id).name("Product " + id).description("Description")
                .price(new BigDecimal("12.50")).availableQuantity(quantity).build();
    }
}
