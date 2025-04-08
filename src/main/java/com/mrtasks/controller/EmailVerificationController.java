// EmailVerificationController.java
package com.mrtasks.controller;

import com.mrtasks.model.UserProfile;
import com.mrtasks.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

@RestController
@RequiredArgsConstructor
@RequestMapping()
public class EmailVerificationController {

    private final UserProfileRepository userProfileRepository;
    private final MessageSource messageSource;

    @GetMapping("/email-verify")
    public ResponseEntity<String> verifyEmail(String token, @RequestParam(defaultValue = "en") String lang) {
        UserProfile profile = userProfileRepository.findByEmailVerificationToken(token)
                .orElse(null);

        if (profile != null) {
            profile.setEmailVerified(true);
            profile.setEmailVerificationToken(null);
            userProfileRepository.save(profile);

            String successMessage = messageSource.getMessage("email.verified.success", null, Locale.forLanguageTag(lang));
            return ResponseEntity.ok(successMessage);
        } else {
            String errorMessage = messageSource.getMessage("email.verified.error", null, Locale.forLanguageTag(lang));
            return ResponseEntity.badRequest().body(errorMessage);
        }
    }
}