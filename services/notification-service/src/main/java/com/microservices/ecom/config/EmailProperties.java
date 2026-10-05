package com.microservices.ecom.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("application.email")
public record EmailProperties(@DefaultValue("notifications@ecommerce.local") String from) {
    public EmailProperties {
        if (from == null || from.isBlank()) {
            throw new IllegalArgumentException("Email sender must not be blank");
        }
    }
}
