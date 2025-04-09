package com.mrtasks.controller;

import com.mrtasks.model.UserProfile;
import com.mrtasks.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Locale;

@Controller
@RequiredArgsConstructor
@RequestMapping()
public class EmailVerificationController {

    private final UserProfileRepository userProfileRepository;
    private final MessageSource messageSource;

    @GetMapping("/email-verify")
    public String verifyEmail(@RequestParam String token,
                              @RequestParam(defaultValue = "en") String lang,
                              Model model) {
        UserProfile profile = userProfileRepository.findByEmailVerificationToken(token)
                .orElse(null);

        Locale locale = Locale.forLanguageTag(lang);

        if (profile != null) {
            profile.setEmailVerified(true);
            profile.setEmailVerificationToken(null);
            userProfileRepository.save(profile);
            model.addAttribute("success", true);
            model.addAttribute("message", messageSource.getMessage("email.verified.success", null, locale));
        } else {
            model.addAttribute("success", false);
            model.addAttribute("message", messageSource.getMessage("email.verified.error", null, locale));
        }

        model.addAttribute("title", messageSource.getMessage("email.verification.title", null, locale));
        model.addAttribute("returnButton", messageSource.getMessage("email.verification.return", null, locale));

        return "email-verification-result";
    }
}