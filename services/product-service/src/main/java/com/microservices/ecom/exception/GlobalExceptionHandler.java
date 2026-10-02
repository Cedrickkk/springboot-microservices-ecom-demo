package com.microservices.ecom.exception;

import com.microservices.common.response.ApiResponseUtil;
import com.microservices.common.response.ErrorApiResponse;
import com.microservices.common.response.FieldErrorResponseDetail;
import com.microservices.ecom.mapper.ErrorApiResponseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {
    private final ErrorApiResponseMapper errorApiResponseMapper;

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorApiResponse<List<FieldErrorResponseDetail>>> handleValidationException(
            MethodArgumentNotValidException ex) {
        var errors = ex.getBindingResult().getFieldErrors().stream()
                .map(errorApiResponseMapper::toFieldErrorResponseDetail).toList();
        ErrorApiResponse<List<FieldErrorResponseDetail>> response = ApiResponseUtil.error(
                HttpStatus.BAD_REQUEST, "Request failed due to missing or invalid fields.");
        response.setErrors(errors);
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler({ProductNotFoundException.class, CategoryNotFoundException.class})
    public ResponseEntity<ErrorApiResponse<Void>> handleNotFoundException(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponseUtil.error(HttpStatus.NOT_FOUND, ex.getMessage()));
    }

    @ExceptionHandler(ProductPurchaseException.class)
    public ResponseEntity<ErrorApiResponse<Void>> handlePurchaseException(ProductPurchaseException ex) {
        return ResponseEntity.badRequest().body(ApiResponseUtil.error(HttpStatus.BAD_REQUEST, ex.getMessage()));
    }

    @ExceptionHandler({HandlerMethodValidationException.class, HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorApiResponse<Void>> handleInvalidRequest(Exception ex) {
        return ResponseEntity.badRequest().body(ApiResponseUtil.error(
                HttpStatus.BAD_REQUEST, "Request failed due to missing or invalid fields."));
    }
}
