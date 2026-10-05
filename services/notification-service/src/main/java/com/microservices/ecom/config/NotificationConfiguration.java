package com.microservices.ecom.config;

import com.microservices.common.messaging.EventCodec;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(EmailProperties.class)
public class NotificationConfiguration {
    @Bean
    public EventCodec eventCodec(JsonMapper mapper) {
        return new EventCodec(mapper);
    }
}
