package com.microservices.ecom.email;

import com.microservices.ecom.config.EmailProperties;
import com.microservices.ecom.domain.OrderConfirmation;
import com.microservices.ecom.domain.PaymentConfirmation;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final EmailProperties properties;

    public void sendOrderConfirmationEmail(OrderConfirmation confirmation) throws MessagingException {
        var context = new Context();
        context.setVariable("customerName", confirmation.customer().firstname() + " " + confirmation.customer().lastname());
        context.setVariable("confirmation", confirmation);
        send(confirmation.customer().email(), EmailTemplates.ORDER_CONFIRMATION, context);
    }

    public void sendPaymentSuccessEmail(PaymentConfirmation confirmation) throws MessagingException {
        var context = new Context();
        context.setVariable("customerName", confirmation.customerFirstname() + " " + confirmation.customerLastname());
        context.setVariable("confirmation", confirmation);
        send(confirmation.customerEmail(), EmailTemplates.PAYMENT_CONFIRMATION, context);
    }

    private void send(String recipient, EmailTemplates template, Context context) throws MessagingException {
        var message = mailSender.createMimeMessage();
        var helper = new MimeMessageHelper(message, StandardCharsets.UTF_8.name());
        helper.setFrom(properties.from());
        helper.setTo(recipient);
        helper.setSubject(template.getSubject());
        helper.setText(templateEngine.process(template.getTemplate(), context), true);
        mailSender.send(message);
    }
}
