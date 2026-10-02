package com.microservices.ecom.api;

import com.microservices.ecom.exception.GlobalExceptionHandler;
import com.microservices.ecom.exception.ProductNotFoundException;
import com.microservices.ecom.mapper.ErrorApiResponseMapper;
import com.microservices.ecom.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockMakers;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ProductControllerTests {
    private ProductService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(ProductService.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        mvc = MockMvcBuilders.standaloneSetup(new ProductController(service))
                .setControllerAdvice(new GlobalExceptionHandler(new ErrorApiResponseMapper())).build();
    }

    @Test
    void creationReturnsSharedResponseEnvelope() throws Exception {
        when(service.createProduct(any())).thenReturn(7);
        mvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"Book","description":"A book","availableQuantity":10,"price":12.50,"categoryId":3}
                """))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.data").value(7)).andExpect(jsonPath("$.meta").exists());
    }

    @Test
    void missingProductReturnsShared404Envelope() throws Exception {
        when(service.findById(99)).thenThrow(new ProductNotFoundException("Customer-facing message"));
        mvc.perform(get("/api/v1/products/99"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("Customer-facing message"));
    }

    @Test
    void invalidProductReturnsFieldErrors() throws Exception {
        mvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors").isArray());
        verifyNoInteractions(service);
    }

    @Test
    void invalidPurchaseLineAndEmptyListAreRejected() throws Exception {
        for (String body : new String[]{"[]", "[{\"productId\":1,\"quantity\":-1}]",
                "[{\"productId\":1}]"}) {
            mvc.perform(post("/api/v1/products/purchase").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(400));
        }
        verifyNoInteractions(service);
    }
}
