package com.mrtasks.controller;

import com.mrtasks.model.User;
import com.mrtasks.model.UserProfile;
import com.mrtasks.model.UserSubscription;
import com.mrtasks.repository.UserProfileRepository;
import com.mrtasks.repository.UserRepository;
import com.mrtasks.repository.UserSubscriptionRepository;
import com.mrtasks.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Locale;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class ProfileController {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final MessageSource messageSource;
    private final EmailService emailService;

    @GetMapping("/profile")
    public String showProfile(Model model, Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        UserProfile profile = userProfileRepository.findByUser(user)
                .orElseGet(() -> {
                    UserProfile newProfile = new UserProfile();
                    newProfile.setUser(user);
                    newProfile.setLanguage("en");
                    return userProfileRepository.save(newProfile);
                });
        UserSubscription userSubscription = userSubscriptionRepository.findByUser(user)
                .orElse(new UserSubscription());

        model.addAttribute("user", user);
        model.addAttribute("profile", profile);
        model.addAttribute("subscription", userSubscription);
        return "profile";
    }

    @PostMapping("/profile")
    public String updateProfile(
            @ModelAttribute UserProfile profile,
            Authentication auth,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request,
            HttpServletResponse response) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        UserProfile existingProfile = userProfileRepository.findByUser(user)
                .orElseGet(() -> {
                    UserProfile newProfile = new UserProfile();
                    newProfile.setUser(user);
                    newProfile.setLanguage("en");
                    return newProfile;
                });

        // Check if email has changed and is valid
        String oldEmail = existingProfile.getEmail();
        String newEmail = profile.getEmail();
        boolean emailChanged = newEmail != null && !newEmail.trim().isEmpty() &&
                (oldEmail == null || !oldEmail.equals(newEmail));

        existingProfile.setCompanyName(profile.getCompanyName());
        existingProfile.setLogoUrl(profile.getLogoUrl());
        existingProfile.setEmail(newEmail);
        existingProfile.setPhone(profile.getPhone());
        existingProfile.setLanguage(profile.getLanguage() != null ? profile.getLanguage() : "en");

        // Save profile first
        userProfileRepository.save(existingProfile);

        // Handle email verification if email changed
        if (emailChanged) {
            String verificationToken = UUID.randomUUID().toString();
            existingProfile.setEmailVerificationToken(verificationToken);
            existingProfile.setEmailVerified(false);
            userProfileRepository.save(existingProfile);

            emailService.sendVerificationEmail(newEmail, verificationToken, existingProfile.getLanguage());
        }

        // Set the language in a cookie
        String newLanguage = existingProfile.getLanguage();
        Cookie languageCookie = new Cookie("userLanguage", newLanguage);
        languageCookie.setMaxAge(30 * 24 * 60 * 60); // Cookie expires in 30 days
        languageCookie.setPath("/"); // Cookie is available for the entire application
        response.addCookie(languageCookie);

        // Set the locale for the current request
        LocaleContextHolder.setLocale(Locale.forLanguageTag(newLanguage));

        redirectAttributes.addFlashAttribute("message",
                messageSource.getMessage("profile.updated.success", null, LocaleContextHolder.getLocale()));
        redirectAttributes.addFlashAttribute("messageType", "success");

        return "redirect:/profile";
    }
}