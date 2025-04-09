package com.mrtasks.controller;

import com.mrtasks.model.User;
import com.mrtasks.model.UserProfile;
import com.mrtasks.model.UserSubscription;
import com.mrtasks.repository.UserProfileRepository;
import com.mrtasks.repository.UserRepository;
import com.mrtasks.repository.UserSubscriptionRepository;
import com.mrtasks.service.EmailService;
import com.mrtasks.service.PremiumService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final PremiumService premiumService;
    private final UserRepository userRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final UserProfileRepository userProfileRepository;
    private final EmailService emailService;

    @GetMapping
    public String adminDashboard(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            Model model) {
        Pageable pageable = PageRequest.of(page, size);
        Page<User> userPage;

        if (search != null && !search.trim().isEmpty()) {
            userPage = userRepository.findByUsernameContainingIgnoreCase(search, pageable);
        } else {
            userPage = userRepository.findAll(pageable);
        }

        List<Long> userIds = userPage.getContent().stream().map(User::getId).toList();
        List<UserSubscription> subscriptions = userSubscriptionRepository.findByUserIdIn(userIds);
        Map<Long, UserSubscription> subscriptionMap = subscriptions.stream()
                .collect(Collectors.toMap(sub -> sub.getUser().getId(), sub -> sub));

        model.addAttribute("users", userPage.getContent());
        model.addAttribute("subscriptions", subscriptionMap);
        model.addAttribute("currentPage", userPage.getNumber());
        model.addAttribute("totalPages", userPage.getTotalPages());
        model.addAttribute("search", search);
        return "admin";
    }

    @GetMapping("/profile/{username}")
    public String viewProfile(@PathVariable String username, Model model) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        UserProfile profile = userProfileRepository.findByUser(user)
                .orElse(new UserProfile()); // Default to empty profile if not found

        model.addAttribute("user", user);
        model.addAttribute("profile", profile);
        return "admin-profile";
    }

    @PostMapping("/profile/{username}")
    public String updateProfile(
            @PathVariable String username,
            @ModelAttribute UserProfile profile,
            Model model,
            Principal principal) { // Add Principal to get the admin user
        try {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
            UserProfile existingProfile = userProfileRepository.findByUser(user)
                    .orElseGet(() -> {
                        UserProfile newProfile = new UserProfile();
                        newProfile.setUser(user); // Set user for new profiles
                        return newProfile;
                    });

            // Set the user relationship and update fields
            existingProfile.setUser(user);
            existingProfile.setCompanyName(profile.getCompanyName());
            existingProfile.setEmail(profile.getEmail());
            existingProfile.setPhone(profile.getPhone());
            existingProfile.setLanguage(profile.getLanguage());
            existingProfile.setEmailVerified(profile.isEmailVerified());

            // Preserve tokens if not part of the form update
            existingProfile.setResetPasswordToken(existingProfile.getResetPasswordToken());
            existingProfile.setEmailVerificationToken(existingProfile.getEmailVerificationToken());

            // Log the update with the admin's username
            logUpdate(existingProfile, "Profile updated", principal);

            model.addAttribute("message", "Profile for " + username + " updated successfully");
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
        }

        // Reload the user and profile to ensure the view reflects the latest data
        User updatedUser = userRepository.findByUsername(username).get();
        UserProfile updatedProfile = userProfileRepository.findByUser(updatedUser).orElse(new UserProfile());
        model.addAttribute("user", updatedUser);
        model.addAttribute("profile", updatedProfile);
        return "admin-profile";
    }

    @PostMapping("/reset-password")
    public String resetPassword(
            @RequestParam String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            Model model,
            Principal principal) {
        try {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

            UserProfile profile = userProfileRepository.findByUser(user).orElseGet(() -> {
                UserProfile newProfile = new UserProfile();
                newProfile.setUser(user); // Set user for new profiles
                return newProfile;
            });

            if (StringUtils.hasText(profile.getEmail())) {
                String resetPasswordToken = UUID.randomUUID().toString();
                profile.setResetPasswordToken(resetPasswordToken);
                emailService.sendPasswordResetEmail(profile.getEmail(), resetPasswordToken, profile.getLanguage());
                model.addAttribute("message", "Password reset link sent to " + profile.getEmail());
                // Log the update with the admin's username
                logUpdate(profile, "Reset password email sent to " + profile.getEmail(), principal);
            } else {
                model.addAttribute("error", "No email available to send reset email");
            }
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
        }
        return adminDashboard(page, size, search, model);
    }

    @PostMapping("/upgrade")
    public String upgradeUser(
            @RequestParam String username,
            @RequestParam int months,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            Model model,
            Principal principal) {
        try {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
            premiumService.upgradeToPremiumFromAdmin(user, months);

            UserProfile userProfile = userProfileRepository.findByUser(user).orElseGet(() -> {
                UserProfile newProfile = new UserProfile();
                newProfile.setUser(user); // Explicitly set the user
                return newProfile;
            });

            // Log the update with the admin's username
            logUpdate(userProfile, "Upgraded to Premium for " + months + " months", principal);
            model.addAttribute("message", "User " + username + " upgraded to Premium for " + months + " months");
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
        }
        return adminDashboard(page, size, search, model);
    }

    @PostMapping("/downgrade")
    public String downgradeUser(
            @RequestParam String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            Model model,
            Principal principal) {
        try {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
            premiumService.downgradeFromPremium(user);

            UserProfile userProfile = userProfileRepository.findByUser(user).orElseGet(() -> {
                UserProfile newProfile = new UserProfile();
                newProfile.setUser(user); // Explicitly set the user
                return newProfile;
            });

            // Log the update with the admin's username
            logUpdate(userProfile, "Downgraded from Premium", principal);
            model.addAttribute("message", "User " + username + " downgraded from Premium");
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
        }
        return adminDashboard(page, size, search, model);
    }

    private void logUpdate(UserProfile profile, String action, Principal principal) {
        String adminUsername = principal != null ? principal.getName() : "";
        String timestamp = LocalDateTime.now().toString();
        String updateEntry = action + " by " + adminUsername + " at " + timestamp;
        profile.setUpdateHistory(profile.getUpdateHistory() != null
                ? updateEntry + "; " + profile.getUpdateHistory()
                : updateEntry);
        userProfileRepository.save(profile);
    }
}