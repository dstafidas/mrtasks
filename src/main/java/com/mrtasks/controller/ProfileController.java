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
import com.mrtasks.utils.RequestUtils;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

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
            boolean canChangeEmail = rateLimitConfig.canChangeEmail(user.getUsername(), RequestUtils.getClientIp(request));
            if (!canChangeEmail) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(messageSource.getMessage("limit.error.rate.email.change", null, Locale.forLanguageTag(existingProfile.getLanguage())));
            }
        }

        // Validate email format
        if (StringUtils.hasText(newEmail) && !newEmail.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(messageSource.getMessage("profile.email.invalid", null, Locale.forLanguageTag(existingProfile.getLanguage())));
        }

        // Validate phone format
        String phone = profileDto.getPhone();
        if (StringUtils.hasText(phone) && !phone.matches("^\\+?[1-9]\\d{1,14}$")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(messageSource.getMessage("profile.phone.invalid", null, Locale.forLanguageTag(existingProfile.getLanguage())));
        }

        // Validate logo URL
        String logoUrl = profileDto.getLogoUrl();
        if (StringUtils.hasText(logoUrl) && !logoUrl.matches("^(https?:\\/\\/)?([\\w-]+\\.)+[\\w-]+(\\/[\\w-.\\/?%&=]*)?$")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(messageSource.getMessage("profile.logoUrl.invalid", null, Locale.forLanguageTag(existingProfile.getLanguage())));
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

        return ResponseEntity.ok(dtoMapper.toProfileDto(existingProfile));
    }

    @PostMapping("/language")
    @ResponseBody
    public ResponseEntity<?> updateLanguage(
            @RequestParam("language") String language,
            Authentication auth,
            HttpServletResponse response) {
        // Validate language code
        if (!language.matches("^[a-z]{2}$")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(messageSource.getMessage("profile.language.invalid", null, LocaleContextHolder.getLocale()));
        }

        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        UserProfile profile = userProfileRepository.findByUser(user)
                .orElseGet(() -> {
                    UserProfile newProfile = new UserProfile();
                    newProfile.setUser(user);
                    newProfile.setLanguage("en");
                    return newProfile;
                });

        // Update language
        profile.setLanguage(language);
        userProfileRepository.save(profile);

        // Set language cookie
        Cookie languageCookie = new Cookie("userLanguage", language);
        languageCookie.setMaxAge(30 * 24 * 60 * 60); // 30 days
        languageCookie.setPath("/");
        response.addCookie(languageCookie);

        // Set locale
        LocaleContextHolder.setLocale(Locale.forLanguageTag(language));

        return ResponseEntity.ok().build();
    }

    @PostMapping("/currency")
    @ResponseBody
    public ResponseEntity<?> updateCurrency(
            @RequestParam("currency") String currency,
            Authentication auth) {
        // Validate currency code
        if (!currency.matches("^[A-Z]{3}$")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(messageSource.getMessage("profile.currency.invalid", null, LocaleContextHolder.getLocale()));
        }

        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        UserProfile profile = userProfileRepository.findByUser(user)
                .orElseGet(() -> {
                    UserProfile newProfile = new UserProfile();
                    newProfile.setUser(user);
                    return newProfile;
                });

        // Update currency
        profile.setCurrency(currency);
        userProfileRepository.save(profile);

        return ResponseEntity.ok().build();
    }
}