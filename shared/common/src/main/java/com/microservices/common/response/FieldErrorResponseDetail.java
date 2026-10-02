package com.microservices.common.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public final class FieldErrorResponseDetail {
    private String field;
    private String message;
}
