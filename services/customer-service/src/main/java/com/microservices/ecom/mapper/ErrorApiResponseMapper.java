package com.microservices.ecom.mapper;

import com.microservices.common.response.FieldErrorResponseDetail;
import org.springframework.stereotype.Component;
import org.springframework.validation.FieldError;

@Component
public class ErrorApiResponseMapper {

    public FieldErrorResponseDetail toFieldErrorResponseDetail(FieldError fieldError) {
        return FieldErrorResponseDetail.builder()
                .field(fieldError.getField())
                .message(fieldError.getDefaultMessage())
                .build();
    }

}
