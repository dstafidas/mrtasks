// EmailService.java
package com.mrtasks.service;

import com.mrtasks.utils.UrlUtils;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final MessageSource messageSource;

    public void sendVerificationEmail(String to, String token, String language) {
        language = StringUtils.hasText(language) ? language : "en";
        String baseUrl = UrlUtils.getBaseUrl();

        try {
            String verificationLink = baseUrl + "/email-verify?token=" + token + "&lang=" + language;
            String subject = messageSource.getMessage("email.verification.subject", null, Locale.forLanguageTag(language));
            String htmlBody = "<h3>" + subject + "</h3>" +
                    "<p>" + messageSource.getMessage("email.verification.body", null, Locale.forLanguageTag(language)) + "</p>" +
                    "<p><a href=\"" + verificationLink + "\">" + verificationLink + "</a></p>" +
                    "<p>" + messageSource.getMessage("email.verification.footer", null, Locale.forLanguageTag(language)) + "</p>";

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true); // true for HTML
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true); // true indicates HTML
            helper.setFrom("no-reply@mrtasks.com");
            helper.setReplyTo("support@mrtasks.com");

            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send verification email to " + to, e);
        }
    }

    public void sendPasswordResetEmail(String to, String token, String language) {

        language = StringUtils.hasText(language) ? language : "en";
        String baseUrl = UrlUtils.getBaseUrl();

        try {
            String resetLink = baseUrl + "/reset-password?token=" + token;
            String subject = messageSource.getMessage("email.reset.subject", null, Locale.forLanguageTag(language));
            String htmlBody = "<h3>" + subject + "</h3>" +
                    "<p>" + messageSource.getMessage("email.reset.body", null, Locale.forLanguageTag(language)) + "</p>" +
                    "<p><a href=\"" + resetLink + "\">" + resetLink + "</a></p>" +
                    "<p>" + messageSource.getMessage("email.reset.footer", null, Locale.forLanguageTag(language)) + "</p>";

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            helper.setFrom("no-reply@mrtasks.com");
            helper.setReplyTo("support@mrtasks.com");

            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send password reset email to " + to, e);
        }
    }
}