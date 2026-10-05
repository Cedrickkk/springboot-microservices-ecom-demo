package com.microservices.ecom.email;

import com.microservices.common.messaging.PaymentMethod;
import com.microservices.ecom.config.EmailProperties;
import com.microservices.ecom.domain.Customer;
import com.microservices.ecom.domain.OrderConfirmation;
import com.microservices.ecom.domain.PaymentConfirmation;
import com.microservices.ecom.domain.Product;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.math.BigDecimal;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmailServiceTests {
    private JavaMailSender sender;
    private MimeMessage message;
    private EmailService service;

    @BeforeEach
    void setUp() {
        sender = mock(JavaMailSender.class);
        message = new MimeMessage(Session.getInstance(new Properties()));
        when(sender.createMimeMessage()).thenReturn(message);
        var resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        resolver.setCharacterEncoding("UTF-8");
        var engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        service = new EmailService(sender, engine, new EmailProperties("shop@example.com"));
    }

    @Test
    void orderEmailRendersProductsAndEscapesCustomerContent() throws Exception {
        service.sendOrderConfirmationEmail(new OrderConfirmation("ORD-10", BigDecimal.TEN, PaymentMethod.VISA,
                new Customer("c1", "<script>alert(1)</script>", "Doe", "jane@example.com"),
                List.of(new Product(3, "Book & pen", "Description", BigDecimal.TEN, 1))));
        message.saveChanges();
        String html = message.getContent().toString();
        assertTrue(html.contains("ORD-10"));
        assertTrue(html.contains("Book &amp; pen"));
        assertFalse(html.contains("<script>"));
        assertEquals("Order confirmation", message.getSubject());
        assertEquals("jane@example.com", message.getAllRecipients()[0].toString());
        assertEquals("shop@example.com", message.getFrom()[0].toString());
        assertTrue(message.isMimeType("text/html"));
        verify(sender).send(message);
    }

    @Test
    void paymentEmailRendersConfirmationDetails() throws Exception {
        service.sendPaymentSuccessEmail(new PaymentConfirmation("ORD-10", BigDecimal.TEN, PaymentMethod.VISA,
                "Jane", "Doe", "jane@example.com"));
        message.saveChanges();
        assertTrue(message.getContent().toString().contains("Jane Doe"));
        assertTrue(message.getContent().toString().contains("ORD-10"));
        assertEquals("Payment confirmation", message.getSubject());
        verify(sender).send(message);
    }
}
