package com.mrtasks.controller;

import com.mrtasks.config.RateLimitConfig;
import com.mrtasks.model.User;
import com.mrtasks.model.UserProfile;
import com.mrtasks.model.UserSubscription;
import com.mrtasks.model.dto.ProfileDto;
import com.mrtasks.model.dto.mapper.DtoMapper;
import com.mrtasks.repository.UserProfileRepository;
import com.mrtasks.repository.UserRepository;
import com.mrtasks.repository.UserSubscriptionRepository;
import com.mrtasks.service.EmailService;
import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Locale;
import java.util.UUID;

@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final MessageSource messageSource;
    private final EmailService emailService;
    private final RateLimitConfig rateLimitConfig;
    private final DtoMapper dtoMapper;

    @GetMapping
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

        model.addAttribute("profile", dtoMapper.toProfileDto(profile));
        model.addAttribute("subscription", userSubscription);
        return "profile";
    }

    @PostMapping
    @ResponseBody
    public ResponseEntity<?> updateProfile(
            @ModelAttribute ProfileDto profileDto,
            Authentication auth,
            HttpServletResponse response,
            HttpServletRequest request) {
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
        String newEmail = profileDto.getEmail();
        boolean emailChanged = newEmail != null && !newEmail.trim().isEmpty() &&
                (oldEmail == null || !oldEmail.equals(newEmail));

        // Rate limiting for email changes
        if (emailChanged) {
            boolean canChangeEmail = rateLimitConfig.canChangeEmail(user.getUsername(), request.getRemoteAddr());
            if (!canChangeEmail) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(messageSource.getMessage("error.rate.limit.email.change", null, LocaleContextHolder.getLocale()));
            }
        }

        // Validate email format
        if (newEmail != null && !newEmail.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(messageSource.getMessage("profile.email.invalid", null, LocaleContextHolder.getLocale()));
        }

        // Validate phone format
        String phone = profileDto.getPhone();
        if (phone != null && !phone.matches("^\\+?[1-9]\\d{1,14}$")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(messageSource.getMessage("profile.phone.invalid", null, LocaleContextHolder.getLocale()));
        }

        // Validate logo URL
        String logoUrl = profileDto.getLogoUrl();
        if (logoUrl != null && !logoUrl.matches("^(https?:\\/\\/)?([\\w-]+\\.)+[\\w-]+(\\/[\\w-.\\/?%&=]*)?$")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(messageSource.getMessage("profile.logoUrl.invalid", null, LocaleContextHolder.getLocale()));
        }

        // Update profile
        dtoMapper.toUserProfile(profileDto, existingProfile);

        // Handle email verification if changed
        if (emailChanged) {
            String verificationToken = UUID.randomUUID().toString();
            existingProfile.setEmailVerificationToken(verificationToken);
            existingProfile.setEmailVerified(false);
            emailService.sendVerificationEmail(newEmail, verificationToken, existingProfile.getLanguage());
        }

        userProfileRepository.save(existingProfile);

        // Set language cookie
        String newLanguage = existingProfile.getLanguage();
        Cookie languageCookie = new Cookie("userLanguage", newLanguage);
        languageCookie.setMaxAge(30 * 24 * 60 * 60); // 30 days
        languageCookie.setPath("/");
        response.addCookie(languageCookie);

        // Set locale
        LocaleContextHolder.setLocale(Locale.forLanguageTag(newLanguage));

        return ResponseEntity.ok(dtoMapper.toProfileDto(existingProfile));
    }
}