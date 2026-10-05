package com.microservices.ecom.api;

import com.microservices.common.response.ApiResponseUtil;
import com.microservices.common.response.SuccessApiResponse;
import com.microservices.ecom.domain.NotificationType;
import com.microservices.ecom.dto.NotificationResponse;
import com.microservices.ecom.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService service;

    @GetMapping
    public ResponseEntity<SuccessApiResponse<List<NotificationResponse>>> findAll(
            @RequestParam(required = false) NotificationType type) {
        return ResponseEntity.ok(ApiResponseUtil.success(HttpStatus.OK, service.findAll(type),
                "Notifications retrieved successfully."));
    }

    @GetMapping("/{notificationId}")
    public ResponseEntity<SuccessApiResponse<NotificationResponse>> findById(@PathVariable String notificationId) {
        return ResponseEntity.ok(ApiResponseUtil.success(HttpStatus.OK, service.findById(notificationId),
                "Notification retrieved successfully."));
    }
}
