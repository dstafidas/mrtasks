package com.mrtasks.controller;

import com.mrtasks.service.StripeService;
import com.stripe.exception.StripeException;
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
    private final MessageSource messageSource;

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @GetMapping("/upgrade-to-premium")
    public String upgradeToPremium(@RequestParam Long userId, @RequestParam int months) throws StripeException {
        String checkoutUrl = stripeService.createCheckoutSession(userId, months);
        return "redirect:" + checkoutUrl;
    }

    @GetMapping("/success")
    public String paymentSuccess(RedirectAttributes redirectAttributes) {
        // Add success message and redirect to profile
        String message = messageSource.getMessage("profile.payment.success", null, LocaleContextHolder.getLocale());
        redirectAttributes.addFlashAttribute("message", message);
        redirectAttributes.addFlashAttribute("messageType", "success");
        return "redirect:/profile";
    }

    @GetMapping("/cancel")
    public String paymentCanceled(RedirectAttributes redirectAttributes) {
        // Add failure message and redirect to profile
        String message = messageSource.getMessage("profile.payment.failed", null, LocaleContextHolder.getLocale());
        redirectAttributes.addFlashAttribute("message", message);
        redirectAttributes.addFlashAttribute("messageType", "danger");
        return "redirect:/profile";
    }
}