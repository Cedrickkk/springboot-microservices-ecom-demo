package com.microservices.ecom.exception;

import com.microservices.common.response.ApiResponseUtil;
import com.microservices.common.response.ErrorApiResponse;
import com.microservices.common.response.FieldErrorResponseDetail;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorApiResponse<List<FieldErrorResponseDetail>>> handleValidation(
            MethodArgumentNotValidException exception) {
        var errors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldErrorResponseDetail(error.getField(), error.getDefaultMessage())).toList();
        ErrorApiResponse<List<FieldErrorResponseDetail>> response = ApiResponseUtil.error(
                HttpStatus.BAD_REQUEST, "Request failed due to missing or invalid fields.");
        response.setErrors(errors);
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorApiResponse<Void>> handleMalformedRequest(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest().body(ApiResponseUtil.error(
                HttpStatus.BAD_REQUEST, "Request failed due to missing or invalid fields."));
    }
}
