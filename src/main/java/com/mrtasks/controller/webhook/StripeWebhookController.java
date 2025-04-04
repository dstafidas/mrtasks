package com.mrtasks.controller.webhook;

import com.mrtasks.service.PremiumService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Log4j2
public class StripeWebhookController {

    private final PremiumService premiumService;

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    @PostMapping("/stripe/webhook")
    public ResponseEntity<String> handleWebhook(@RequestBody String payload,
                                                @RequestHeader("Stripe-Signature") String sigHeader) {
        try {
            // Verify the webhook signature for security
            Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret);

            // Handle the checkout.session.completed event
            if ("checkout.session.completed".equals(event.getType())) {
                Session session = (Session) event.getDataObjectDeserializer().getObject()
                        .orElseThrow(() -> new IllegalStateException("Unable to deserialize session"));

                // Extract userId and months from client_reference_id
                String[] parts = session.getClientReferenceId().split("_");
                Long userId = Long.valueOf(parts[0]);
                int months = Integer.parseInt(parts[1]);

                // Upgrade the user to premium
                premiumService.upgradeToPremium(userId, months);

                // Retrieve transaction details for receipt generation
                String transactionId = session.getPaymentIntent(); // Payment ID
                long amount = session.getAmountTotal(); // Amount in cents
                String currency = session.getCurrency(); // Currency (e.g., "eur")
                String customerEmail = session.getCustomerDetails().getEmail(); // Customer email

                // TODO: Store the transaction data in a database for myData integration,
                // TODO: use session key or transaction id as idepodency key
                log.info("Transaction: {}, Amount: {} {}, UserId: {}, Months: {}, Email: {}",
                        transactionId, amount, currency, userId, months, customerEmail);
            }

            return ResponseEntity.ok("Webhook handled successfully");
        } catch (SignatureVerificationException e) {
            return ResponseEntity.status(400).body("Invalid signature");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error processing webhook: " + e.getMessage());
        }
    }
}