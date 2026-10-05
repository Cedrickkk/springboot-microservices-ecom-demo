package com.microservices.ecom;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false", "spring.config.import=", "eureka.client.enabled=false",
        "spring.kafka.listener.auto-startup=false", "spring.kafka.admin.auto-create=false",
        "application.config.customer-url=http://localhost:8090/api/v1/customers"
})
class NotificationServiceApplicationTests {

    @Autowired
    private JavaMailSender mailSender;
    @Autowired
    private Environment environment;

    @Test
    void contextLoads() {
    }

    @Test
    void registersMailSenderWithoutConfigServer() {
        var sender = assertInstanceOf(JavaMailSenderImpl.class, mailSender);
        assertEquals(environment.getProperty("spring.mail.host"), sender.getHost());
        assertEquals(environment.getProperty("spring.mail.port", Integer.class), sender.getPort());
        assertEquals("5000", sender.getJavaMailProperties().getProperty("mail.smtp.connectiontimeout"));
    }

}
