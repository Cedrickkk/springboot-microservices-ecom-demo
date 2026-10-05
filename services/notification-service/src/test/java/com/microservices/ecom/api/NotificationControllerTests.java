package com.microservices.ecom.api;

import com.microservices.ecom.domain.NotificationType;
import com.microservices.ecom.dto.NotificationResponse;
import com.microservices.ecom.exception.GlobalExceptionHandler;
import com.microservices.ecom.exception.NotificationNotFoundException;
import com.microservices.ecom.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class NotificationControllerTests {
    private NotificationService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(NotificationService.class);
        mvc = MockMvcBuilders.standaloneSetup(new NotificationController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void listUsesSharedResponseAndPassesOptionalTypeFilter() throws Exception {
        when(service.findAll(NotificationType.PAYMENT_CONFIRMATION)).thenReturn(List.of());
        mvc.perform(get("/api/v1/notifications").param("type", "PAYMENT_CONFIRMATION"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray()).andExpect(jsonPath("$.meta").exists());
        verify(service).findAll(NotificationType.PAYMENT_CONFIRMATION);
    }

    @Test
    void retrievalReturnsNotificationDto() throws Exception {
        when(service.findById("event-1")).thenReturn(new NotificationResponse("event-1",
                NotificationType.ORDER_CONFIRMATION, LocalDateTime.now(), null, null, null));
        mvc.perform(get("/api/v1/notifications/event-1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value("event-1"))
                .andExpect(jsonPath("$.data.type").value("ORDER_CONFIRMATION"));
    }

    @Test
    void missingNotificationReturnsShared404() throws Exception {
        when(service.findById("missing")).thenThrow(new NotificationNotFoundException("Notification not found"));
        mvc.perform(get("/api/v1/notifications/missing"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("Notification not found"));
    }

    @Test
    void invalidTypeReturnsShared400BeforeCallingService() throws Exception {
        mvc.perform(get("/api/v1/notifications").param("type", "UNKNOWN"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(400));
        verifyNoInteractions(service);
    }
}
