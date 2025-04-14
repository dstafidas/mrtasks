package com.mrtasks.controller;

import com.mrtasks.config.RateLimitConfig;
import com.mrtasks.exception.RateLimitExceededException;
import com.mrtasks.model.User;
import com.mrtasks.repository.UserRepository;
import com.mrtasks.service.InvoiceService;
import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class InvoiceController {
    private final InvoiceService invoiceService;
    private final UserRepository userRepository;
    private final RateLimitConfig rateLimitConfig;

    @PostMapping("/invoice")
    public ResponseEntity<byte[]> downloadInvoice(
            @RequestParam("taskIds") List<Long> taskIds,
            Authentication auth,
            HttpServletRequest request) throws Exception {
        // Rate limiting
        boolean canDownloadInvoice = rateLimitConfig.canDownloadInvoice(auth.getName(), request.getRemoteAddr());
        if (!canDownloadInvoice) {
            throw new RateLimitExceededException("error.rate.limit.invoice");
        }
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();


        byte[] invoice = invoiceService.generateInvoice(user, taskIds);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=invoice_" + user.getUsername() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(invoice);
    }
}