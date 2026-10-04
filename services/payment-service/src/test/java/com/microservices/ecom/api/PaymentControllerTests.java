package com.microservices.ecom.api;

import com.microservices.ecom.exception.GlobalExceptionHandler;
import com.microservices.ecom.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PaymentControllerTests {
    private static final String VALID = """
            {"amount":10,"paymentMethod":"VISA","orderId":7,"orderReference":"ORD-7",
             "customer":{"id":"c1","firstname":"Jane","lastname":"Doe","email":"jane@example.com"}}
            """;
    private final PaymentService service = mock(PaymentService.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new PaymentController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void createsPaymentWithSharedEnvelope() throws Exception {
        when(service.createPayment(any())).thenReturn(42);
        mvc.perform(post("/api/v1/payments").contentType(MediaType.APPLICATION_JSON).content(VALID))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.data").value(42)).andExpect(jsonPath("$.meta.requestId").exists());
        verify(service).createPayment(any());
    }

    @Test
    void rejectsMissingFieldsInvalidAmountsAndNestedCustomer() throws Exception {
        for (String body : new String[]{"{}", VALID.replace("\"amount\":10", "\"amount\":0"),
                VALID.replace("\"amount\":10", "\"amount\":-1"), VALID.replace("\"amount\":10", "\"amount\":10.001"),
                VALID.replace("\"orderId\":7", "\"orderId\":0"), VALID.replace("ORD-7", " "),
                VALID.replace("jane@example.com", "invalid"), VALID.replace("Jane", " ")}) {
            mvc.perform(post("/api/v1/payments").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(400))
                    .andExpect(jsonPath("$.errors").isArray());
        }
        verifyNoInteractions(service);
    }

    @Test
    void rejectsMalformedJsonAndUnknownPaymentMethod() throws Exception {
        for (String body : new String[]{"{", VALID.replace("VISA", "UNKNOWN")}) {
            mvc.perform(post("/api/v1/payments").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(400));
        }
        verifyNoInteractions(service);
    }
}
