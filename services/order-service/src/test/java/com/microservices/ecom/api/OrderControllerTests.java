package com.microservices.ecom.api;
import com.microservices.ecom.exception.*;
import com.microservices.ecom.mapper.ErrorApiResponseMapper;
import com.microservices.ecom.service.OrderService;
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
class OrderControllerTests {
    private OrderService service;
    private MockMvc mvc;
    @BeforeEach
    void setUp() {
        service = mock(OrderService.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        mvc = MockMvcBuilders.standaloneSetup(new OrderController(service), new OrderLineController(service))
                .setControllerAdvice(new GlobalExceptionHandler(new ErrorApiResponseMapper())).build();
    }
    @Test
    void createsOrderWithSharedEnvelope() throws Exception {
        when(service.createOrder(any())).thenReturn(7);
        mvc.perform(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON).content("""
                {"reference":"ORD-1","amount":10,"paymentMethod":"VISA","customerId":"c1",
                 "products":[{"productId":1,"quantity":2}]}
                """))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.data").value(7)).andExpect(jsonPath("$.meta").exists());
    }
    @Test
    void invalidOrdersAndNestedProductsAreRejected() throws Exception {
        for (String body : new String[]{"{}", """
                {"reference":"ORD-1","amount":10,"paymentMethod":"VISA","customerId":"c1","products":[]}
                """, """
                {"reference":"ORD-1","amount":10,"paymentMethod":"VISA","customerId":"c1",
                 "products":[{"productId":1,"quantity":-1}]}
                """, """
                {"reference":"ORD-1","amount":10,"paymentMethod":"VISA","customerId":"c1","products":[null]}
                """}) {
            mvc.perform(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors").isArray());
        }
        verifyNoInteractions(service);
    }
    @Test
    void invalidPaymentResponseReturns502Envelope() throws Exception {
        when(service.createOrder(any())).thenThrow(new PaymentProcessingException("Invalid payment response"));
        mvc.perform(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON).content("""
                {"reference":"ORD-1","amount":10,"paymentMethod":"VISA","customerId":"c1",
                 "products":[{"productId":1,"quantity":2}]}
                """))
                .andExpect(status().isBadGateway()).andExpect(jsonPath("$.code").value(502));
    }

    @Test
    void missingOrderReturns404Envelope() throws Exception {
        when(service.findById(99)).thenThrow(new OrderNotFoundException("Order not found"));
        mvc.perform(get("/api/v1/orders/99"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value(404));
    }
}
