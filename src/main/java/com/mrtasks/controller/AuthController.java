package com.mrtasks.controller;

import com.mrtasks.model.User;
import com.mrtasks.model.UserProfile;
import com.mrtasks.repository.UserRepository;
import com.mrtasks.repository.UserProfileRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${recaptcha.secret}")
    private String RECAPTCHA_SECRET_KEY;
    private static final String RECAPTCHA_VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";

    @GetMapping("/")
    public String home(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            return "redirect:/dashboard";
        }
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String showLoginPage(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            return "redirect:/dashboard";
        }
        return "login";
    }

    @GetMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        if (authentication != null) {
            new SecurityContextLogoutHandler().logout(request, response, authentication);
        }
        return "redirect:/login?logout=true";
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            return "redirect:/dashboard";
        }
        model.addAttribute("user", new User());
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(
            @ModelAttribute User user,
            @RequestParam("g-recaptcha-response") String recaptchaResponse,
            Model model) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            String verificationUrl = RECAPTCHA_VERIFY_URL + "?secret=" + RECAPTCHA_SECRET_KEY + "&response=" + recaptchaResponse;
            String result = restTemplate.postForObject(verificationUrl, null, String.class);

            if (result == null || !result.contains("\"success\": true")) {
                model.addAttribute("error", "CAPTCHA verification failed. Please try again.");
                return "register";
            }

            if (userRepository.findByUsername(user.getUsername()).isPresent()) {
                model.addAttribute("error", "Username already exists");
                return "register";
            }
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            user.setRole("ROLE_USER");
            user.setProvider("local");
            userRepository.save(user);
            return "redirect:/login?registered=true";
        } catch (Exception e) {
            model.addAttribute("error", "Registration failed: " + e.getMessage());
            return "register";
        }
    }

    @GetMapping("/reset-password")
    public String showResetPasswordForm(@RequestParam("token") String token, Model model) {
        UserProfile profile = userProfileRepository.findByResetPasswordToken(token).orElse(null);
        if (profile == null) {
            return "redirect:/login";
        }
        model.addAttribute("token", token);
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String processResetPassword(
            @RequestParam("token") String token,
            @RequestParam("password") String password,
            @RequestParam("confirmPassword") String confirmPassword,
            Model model) {
        UserProfile profile = userProfileRepository.findByResetPasswordToken(token).orElse(null);
        if (profile == null) {
            model.addAttribute("error", "Invalid or expired reset token.");
            return "redirect:/login";
        }

        // Server-side password validation
        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Passwords do not match.");
            return "reset-password";
        }
        if (password.length() < 8 || !password.matches(".*[A-Z].*") || !password.matches(".*[0-9].*") || !password.matches(".*[^A-Za-z0-9].*")) {
            model.addAttribute("error", "Password must be at least 8 characters long and include uppercase letters, numbers, and special characters.");
            return "reset-password";
        }

        User user = profile.getUser();
        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);

        // Clear the reset token after successful reset
        profile.setResetPasswordToken(null);
        userProfileRepository.save(profile);

        return "redirect:/login?reset=true";
    }
}