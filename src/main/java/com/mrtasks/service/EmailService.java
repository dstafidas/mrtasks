package com.mrtasks.service;

import com.mrtasks.utils.UrlUtils;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.activation.DataSource;
import jakarta.mail.util.ByteArrayDataSource;

import java.nio.charset.StandardCharsets;
import java.util.List;
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

    public void sendLogEmail(List<String> logs, String date) {
        try {
            // Create CSV content
            StringBuilder csvContent = new StringBuilder();
            csvContent.append("Timestamp,Username,IPAddress,Action\n");
            for (String log : logs) {
                // Parse log: "2025-04-14T12:34:56: User testuser, IP 1.2.3.4 hit dashboard limit"
                String[] parts = log.split(": User |, IP | hit ");
                if (parts.length == 4) {
                    String timestamp = parts[0].trim();
                    String username = parts[1].trim();
                    String ipAddress = parts[2].trim();
                    String action = parts[3].replace(" limit", "").trim();
                    csvContent.append(String.format("\"%s\",\"%s\",\"%s\",\"%s\"\n",
                            timestamp, username, ipAddress, action));
                }
            }

            // Prepare email
            String subject = "Rate Limit Violations for " + date;
            String htmlBody = "<h3>" + subject + "</h3>" +
                    "<p>Attached is the CSV file containing rate limit violations for " + date + ".</p>" +
                    "<p>Total violations: " + logs.size() + "</p>" +
                    "<p>Please review the attached file for details.</p>";

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo("support@mrtasks.com");
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            helper.setFrom("limit-violation@mrtasks.com");

            // Attach CSV
            DataSource dataSource = new ByteArrayDataSource(csvContent.toString().getBytes(StandardCharsets.UTF_8), "text/csv");
            helper.addAttachment("rate_limit_violations_" + date + ".csv", dataSource);

            mailSender.send(message);
        } catch (Exception e) {
            // Log error instead of throwing to avoid scheduler crash
            System.err.println("Failed to send log email for " + date + ": " + e.getMessage());
        }
    }

    public void sendInvoiceEmail(String recipientEmail, byte[] invoicePdf, String name, String language, String userEmail) {
        MimeMessage message = mailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(recipientEmail);

            String subject = messageSource.getMessage(
                    "email.invoice.subject",
                    new Object[]{name},
                    Locale.forLanguageTag(language));

            String text = messageSource.getMessage(
                    "email.invoice.text",
                    null,
                    Locale.forLanguageTag(language));

            helper.setSubject(subject);
            helper.setText(text, false);
            helper.setFrom("invoices@mrtasks.com");
            helper.setReplyTo(userEmail);

            // Add the PDF attachment
            ByteArrayDataSource dataSource = new ByteArrayDataSource(invoicePdf, "application/pdf");
            helper.addAttachment("invoice.pdf", dataSource);

            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send invoice email", e);
        }
    }
}