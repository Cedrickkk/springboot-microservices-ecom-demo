package com.microservices.common.response;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ApiResponseUtil {

    public static <T> SuccessApiResponse<T> success(HttpStatus status, T data, String message) {
        return SuccessApiResponse.<T>builder()
                .code(status.value())
                .status(status.getReasonPhrase())
                .message(message)
                .meta(newMeta())
                .data(data)
                .build();
    }

    public static <T> ErrorApiResponse<T> error(HttpStatus status, String message) {
        return ErrorApiResponse.<T>builder()
                .code(status.value())
                .status(status.getReasonPhrase())
                .message(message)
                .meta(newMeta())
                .build();
    }

    private static BaseApiResponse.Meta newMeta() {
        return new BaseApiResponse.Meta(Instant.now(), UUID.randomUUID().toString());
    }
}
