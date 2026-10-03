package com.microservices.common.response;

import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.UUID;

public final class ApiResponseUtil {

    private ApiResponseUtil() {
    }

    public static <T> SuccessApiResponse<T> success(HttpStatus status, T data, String message) {
        return SuccessApiResponse.<T>builder()
                .code(status.value())
                .status(status.getReasonPhrase())
                .message(message)
                .meta(new BaseApiResponse.Meta(Instant.now(), UUID.randomUUID().toString()))
                .data(data)
                .build();
    }

    public static <T> ErrorApiResponse<T> error(HttpStatus status, String message) {
        return ErrorApiResponse.<T>builder()
                .code(status.value())
                .status(status.getReasonPhrase())
                .message(message)
                .meta(new BaseApiResponse.Meta(Instant.now(), UUID.randomUUID().toString()))
                .build();
    }

}
