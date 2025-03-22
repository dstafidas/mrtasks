package com.taskmaster.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import com.taskmaster.utils.UrlUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class StripeService {

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @Value("${app.host:http://localhost:8080}") // Fallback host
    private String fallbackHost;

    public String createCheckoutSession(Long userId, int months) throws StripeException {
        Stripe.apiKey = stripeApiKey;

        // Calculate price based on months (in cents)
        long unitAmount;
        if (months == 1) unitAmount = 1000L; // €10
        else if (months == 3) unitAmount = 2500L; // €25
        else if (months == 6) unitAmount = 4500L; // €45
        else throw new IllegalArgumentException("Invalid subscription duration");

        // Get dynamic base URL
        String baseUrl = UrlUtils.getBaseUrl();

        // Construct dynamic success and cancel URLs
        String successUrl = baseUrl + "/success?session_id={CHECKOUT_SESSION_ID}";
        String cancelUrl = baseUrl + "/cancel";

        SessionCreateParams params = SessionCreateParams.builder()
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency("eur")
                                                .setUnitAmount(unitAmount)
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName(months + " Month Premium Subscription")
                                                                .build()
                                                )
                                                .build()
                                )
                                .setQuantity(1L)
                                .build()
                )
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .setClientReferenceId(userId + "_" + months)
                .build();

        Session session = Session.create(params);
        return session.getUrl();
    }
}