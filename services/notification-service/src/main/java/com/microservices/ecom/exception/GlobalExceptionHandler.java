package com.microservices.ecom.exception;

import com.microservices.common.response.ApiResponseUtil;
import com.microservices.common.response.ErrorApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(NotificationNotFoundException.class)
    public ResponseEntity<ErrorApiResponse<Void>> handleNotificationNotFoundException(NotificationNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponseUtil.error(HttpStatus.NOT_FOUND, ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorApiResponse<Void>> handleInvalidType(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest().body(ApiResponseUtil.error(HttpStatus.BAD_REQUEST,
                "Invalid notification type. Use ORDER_CONFIRMATION or PAYMENT_CONFIRMATION."));
    }
}
