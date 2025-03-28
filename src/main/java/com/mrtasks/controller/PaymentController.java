package com.mrtasks.controller;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.mrtasks.service.StripeService;
import com.mrtasks.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class PaymentController {

    private final StripeService stripeService;

    private final UserService userService;

    private final MessageSource messageSource;

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @GetMapping("/upgrade-to-premium")
    public String upgradeToPremium(@RequestParam Long userId, @RequestParam int months) throws StripeException {
        String checkoutUrl = stripeService.createCheckoutSession(userId, months);
        return "redirect:" + checkoutUrl;
    }

    @GetMapping("/success")
    public String paymentSuccess(@RequestParam("session_id") String sessionId, RedirectAttributes redirectAttributes) throws StripeException {
        Stripe.apiKey = stripeApiKey;
        Session session = Session.retrieve(sessionId);
        String[] parts = session.getClientReferenceId().split("_");
        Long userId = Long.valueOf(parts[0]);
        int months = Integer.parseInt(parts[1]);
        userService.upgradeToPremium(userId, months);
        String message = messageSource.getMessage("profile.payment.success", null, LocaleContextHolder.getLocale());
        redirectAttributes.addFlashAttribute("message", message);
        redirectAttributes.addFlashAttribute("messageType", "success");
        return "redirect:/profile";
    }

    @GetMapping("/cancel")
    public String paymentCanceled(RedirectAttributes redirectAttributes) {
        String message = messageSource.getMessage("profile.payment.failed", null, LocaleContextHolder.getLocale());
        redirectAttributes.addFlashAttribute("message", message);
        redirectAttributes.addFlashAttribute("messageType", "danger");
        return "redirect:/profile";
    }
}