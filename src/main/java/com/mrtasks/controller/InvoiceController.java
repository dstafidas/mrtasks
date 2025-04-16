package com.mrtasks.controller;

import com.mrtasks.config.RateLimitConfig;
import com.mrtasks.exception.RateLimitExceededException;
import com.mrtasks.model.Client;
import com.mrtasks.model.User;
import com.mrtasks.model.UserProfile;
import com.mrtasks.repository.ClientRepository;
import com.mrtasks.repository.UserProfileRepository;
import com.mrtasks.repository.UserRepository;
import com.mrtasks.service.EmailService;
import com.mrtasks.service.InvoiceService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class InvoiceController {
    private final InvoiceService invoiceService;
    private final UserRepository userRepository;
    private final RateLimitConfig rateLimitConfig;
    private final EmailService emailService;
    private final ClientRepository clientRepository;
    private final UserProfileRepository userProfileRepository;

    @PostMapping("/invoice")
    public ResponseEntity<byte[]> downloadInvoice(
            @RequestParam("taskIds") List<Long> taskIds,
            Authentication auth,
            HttpServletRequest request) throws Exception {
        // Rate limiting
        boolean canDownloadInvoice = rateLimitConfig.canDownloadInvoice(auth.getName(), request.getRemoteAddr());
        if (!canDownloadInvoice) {
            throw new RateLimitExceededException("limit.error.rate.invoice");
        }
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();


        byte[] invoice = invoiceService.generateInvoice(user, taskIds);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=invoice_" + user.getUsername() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(invoice);
    }

    @PostMapping("/invoice/send")
    @ResponseBody
    public ResponseEntity<String> sendInvoice(
            @RequestParam Long clientId,
            @RequestParam List<Long> taskIds,
            Authentication auth,
            HttpServletRequest request) {



        // Rate limiting check
        if (!rateLimitConfig.canSendInvoice(auth.getName(), request.getRemoteAddr())) {
            throw new RateLimitExceededException("limit.error.rate.invoice.send");
        }

        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        UserProfile userProfile = userProfileRepository.findByUser(user).orElseThrow();
        Client client = clientRepository.findByIdAndUser(clientId, user);

        if (!userProfile.isEmailVerified())
            return ResponseEntity.badRequest().body("error.email.not.verified");

        if (client == null || client.getEmail() == null || client.getEmail().isEmpty()) {
            return ResponseEntity.badRequest().body("error.client.invalid");
        }

        try {
            byte[] invoice = invoiceService.generateInvoice(user, taskIds);
            emailService.sendInvoiceEmail(client.getEmail(), invoice, userProfile.getCompanyName(), userProfile.getLanguage(), userProfile.getEmail());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("error.invoice.failed");
        }
    }
}