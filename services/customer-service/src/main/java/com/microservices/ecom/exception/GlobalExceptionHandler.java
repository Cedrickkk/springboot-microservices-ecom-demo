package com.microservices.ecom.exception;

import com.microservices.common.response.ApiResponseUtil;
import com.microservices.common.response.ErrorApiResponse;
import com.microservices.common.response.FieldErrorResponseDetail;
import com.microservices.ecom.mapper.ErrorApiResponseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final ErrorApiResponseMapper errorApiResponseMapper;

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorApiResponse<List<FieldErrorResponseDetail>>> handleValidationException(MethodArgumentNotValidException ex) {
        List<FieldErrorResponseDetail> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(errorApiResponseMapper::toFieldErrorResponseDetail)
                .toList();
        ErrorApiResponse<List<FieldErrorResponseDetail>> response = ErrorApiResponse.<List<FieldErrorResponseDetail>>builder()
                .code(HttpStatus.BAD_REQUEST.value())
                .status(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message("Request failed due to missing or invalid fields.")
                .errors(errors)
                .build();
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(CustomerNotFoundException.class)
    public ResponseEntity<ErrorApiResponse<Void>> handleCustomerNotFoundException(CustomerNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponseUtil.error(HttpStatus.NOT_FOUND, ex.getMessage()));
    }

}
