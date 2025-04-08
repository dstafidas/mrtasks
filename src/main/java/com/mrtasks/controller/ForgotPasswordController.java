// ForgotPasswordController.java
package com.mrtasks.controller;

import com.mrtasks.model.User;
import com.mrtasks.model.UserProfile;
import com.mrtasks.repository.UserRepository;
import com.mrtasks.repository.UserProfileRepository;
import com.mrtasks.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class ForgotPasswordController {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final EmailService emailService;

    @GetMapping("/forgot-password")
    public String showForgotPasswordForm() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(
            @RequestParam String username,
            @RequestParam String email,
            RedirectAttributes redirectAttributes) {

        User user = userRepository.findByUsername(username).orElse(null);
        if (user != null) {
            UserProfile profile = userProfileRepository.findByUser(user).orElse(null);
            if (profile != null && email.equals(profile.getEmail())) {
                // Generate reset token and send email
                String resetToken = UUID.randomUUID().toString();
                profile.setResetPasswordToken(resetToken);
                userProfileRepository.save(profile);

                emailService.sendPasswordResetEmail(profile.getEmail(), resetToken, profile.getLanguage());
            }
        }

        // Always redirect with a generic message, regardless of success or failure
        redirectAttributes.addFlashAttribute("sent", "true");
        return "redirect:/login?sent";
    }
}