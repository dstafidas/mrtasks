package com.mrtasks.controller;

import com.mrtasks.model.User;
import com.mrtasks.model.UserProfile;
import com.mrtasks.model.UserSubscription;
import com.mrtasks.model.dto.PageDto;
import com.mrtasks.model.dto.UserDto;
import com.mrtasks.model.dto.mapper.DtoMapper;
import com.mrtasks.repository.UserProfileRepository;
import com.mrtasks.repository.UserRepository;
import com.mrtasks.repository.UserSubscriptionRepository;
import com.mrtasks.service.EmailService;
import com.mrtasks.service.PremiumService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
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
    private final SessionRegistry sessionRegistry;
    private final DtoMapper dtoMapper;
    private final MessageSource messageSource;

    @GetMapping
    public String adminDashboard(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        Pageable pageable = PageRequest.of(page, size);
        Page<User> userPage = userRepository.findAll(pageable);

        PageDto<UserDto> pageDto = createPageDto(userPage, pageable);

        long totalUsers = userRepository.count();
        long premiumUsers = userSubscriptionRepository.countByIsPremiumTrueAndExpiresAtAfter(LocalDateTime.now());
        long recentActivity = userRepository.countByLastLoginAfter(LocalDateTime.now().minusHours(24));
        long usersOnline = sessionRegistry.getAllPrincipals().stream()
                .filter(principal -> !sessionRegistry.getAllSessions(principal, false).isEmpty())
                .count();

        model.addAttribute("users", pageDto);
        model.addAttribute("currentPage", pageDto.getPageNumber());
        model.addAttribute("totalPages", pageDto.getTotalPages());
        model.addAttribute("search", null);
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("premiumUsers", premiumUsers);
        model.addAttribute("recentActivity", recentActivity);
        model.addAttribute("usersOnline", usersOnline);

        return "admin";
    }

    @GetMapping("/search")
    @ResponseBody
    public ResponseEntity<?> searchUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search) {

        Pageable pageable = PageRequest.of(page, size);
        Page<User> userPage;

        if (search != null && !search.trim().isEmpty()) {
            userPage = userRepository.findByUsernameContainingIgnoreCase(search, pageable);
        } else {
            userPage = userRepository.findAll(pageable);
        }

        PageDto<UserDto> pageDto = createPageDto(userPage, pageable);
        return ResponseEntity.ok(pageDto);
    }

    @GetMapping("/profile/{username}")
    public String viewProfile(@PathVariable String username, Model model) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        UserProfile profile = userProfileRepository.findByUser(user).orElseGet(() -> {
            UserProfile newProfile = new UserProfile();
            newProfile.setUser(user);
            return newProfile;
        });
        UserSubscription subscription = userSubscriptionRepository.findByUser(user)
                .orElse(new UserSubscription());

        model.addAttribute("user", dtoMapper.toUserDto(user, profile, subscription));
        return "admin-profile";
    }

    @PostMapping("/profile/{username}")
    @ResponseBody
    public ResponseEntity<?> updateProfile(
            @PathVariable String username,
            @ModelAttribute UserDto userDto,
            Principal principal) {
        try {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
            UserProfile existingProfile = userProfileRepository.findByUser(user)
                    .orElseGet(() -> {
                        UserProfile newProfile = new UserProfile();
                        newProfile.setUser(user);
                        return newProfile;
                    });

            // Validate inputs
            if (userDto.getEmail() != null && !userDto.getEmail().isEmpty() &&
                    !userDto.getEmail().matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(messageSource.getMessage("profile.email.invalid", null, LocaleContextHolder.getLocale()));
            }
            if (userDto.getPhone() != null && !userDto.getPhone().isEmpty() &&
                    !userDto.getPhone().matches("^\\+?[1-9]\\d{1,14}$")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(messageSource.getMessage("profile.phone.invalid", null, LocaleContextHolder.getLocale()));
            }
            if (userDto.getCompanyName() != null && userDto.getCompanyName().length() > 100) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(messageSource.getMessage("profile.companyName.invalid", null, LocaleContextHolder.getLocale()));
            }

            // Update profile
            existingProfile.setCompanyName(userDto.getCompanyName());
            existingProfile.setEmail(userDto.getEmail());
            existingProfile.setPhone(userDto.getPhone());
            existingProfile.setLanguage(userDto.getLanguage() != null ? userDto.getLanguage() : "en");
            existingProfile.setEmailVerified(userDto.isEmailVerified());
            existingProfile.setEmailVerificationToken(existingProfile.getEmailVerificationToken());
            existingProfile.setResetPasswordToken(existingProfile.getResetPasswordToken());

            logUpdate(existingProfile, "Profile updated", principal);
            userProfileRepository.save(existingProfile);

            UserSubscription subscription = userSubscriptionRepository.findByUser(user)
                    .orElse(new UserSubscription());
            return ResponseEntity.ok(dtoMapper.toUserDto(user, existingProfile, subscription));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to update profile: " + e.getMessage());
        }
    }

    @PostMapping("/upgrade")
    @ResponseBody
    public ResponseEntity<?> upgradeUser(
            @RequestParam String username,
            @RequestParam int months,
            Principal principal) {
        try {
            if (months < 1) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Months must be at least 1.");
            }
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
            premiumService.upgradeToPremiumFromAdmin(user, months);
            UserProfile profile = userProfileRepository.findByUser(user).orElseGet(() -> {
                UserProfile newProfile = new UserProfile();
                newProfile.setUser(user);
                return newProfile;
            });

            logUpdate(profile, "Upgraded to Premium for " + months + " months", principal);
            userProfileRepository.save(profile);

            UserSubscription subscription = userSubscriptionRepository.findByUser(user)
                    .orElse(new UserSubscription());
            return ResponseEntity.ok(dtoMapper.toUserDto(user, profile, subscription));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to upgrade user: " + e.getMessage());
        }
    }

    @PostMapping("/downgrade")
    @ResponseBody
    public ResponseEntity<?> downgradeUser(
            @RequestParam String username,
            Principal principal) {
        try {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
            premiumService.downgradeFromPremium(user);
            UserProfile profile = userProfileRepository.findByUser(user).orElseGet(() -> {
                UserProfile newProfile = new UserProfile();
                newProfile.setUser(user);
                return newProfile;
            });

            logUpdate(profile, "Downgraded from Premium", principal);
            userProfileRepository.save(profile);

            UserSubscription subscription = userSubscriptionRepository.findByUser(user)
                    .orElse(new UserSubscription());
            return ResponseEntity.ok(dtoMapper.toUserDto(user, profile, subscription));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to downgrade user: " + e.getMessage());
        }
    }

    @PostMapping("/reset-password")
    @ResponseBody
    public ResponseEntity<?> resetPassword(
            @RequestParam String username,
            Principal principal) {
        try {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
            UserProfile profile = userProfileRepository.findByUser(user).orElseGet(() -> {
                UserProfile newProfile = new UserProfile();
                newProfile.setUser(user);
                return newProfile;
            });

            if (profile.getEmail() == null || profile.getEmail().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("No email available to send reset email");
            }

            String resetPasswordToken = UUID.randomUUID().toString();
            profile.setResetPasswordToken(resetPasswordToken);
            emailService.sendPasswordResetEmail(profile.getEmail(), resetPasswordToken,
                    profile.getLanguage() != null ? profile.getLanguage() : "en");

            logUpdate(profile, "Reset password email sent to " + profile.getEmail(), principal);
            userProfileRepository.save(profile);

            UserSubscription subscription = userSubscriptionRepository.findByUser(user)
                    .orElse(new UserSubscription());
            return ResponseEntity.ok(dtoMapper.toUserDto(user, profile, subscription));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to reset password: " + e.getMessage());
        }
    }

    private PageDto<UserDto> createPageDto(Page<User> userPage, Pageable pageable) {
        List<Long> userIds = userPage.getContent().stream().map(User::getId).collect(Collectors.toList());
        List<UserProfile> profiles = userProfileRepository.findByUserIdIn(userIds);
        List<UserSubscription> subscriptions = userSubscriptionRepository.findByUserIdIn(userIds);

        PageDto<UserDto> pageDto = new PageDto<>();
        pageDto.setContent(userPage.getContent().stream().map(user -> {
            UserProfile profile = profiles.stream()
                    .filter(p -> p.getUser().getId().equals(user.getId()))
                    .findFirst()
                    .orElse(null);
            UserSubscription subscription = subscriptions.stream()
                    .filter(s -> s.getUser().getId().equals(user.getId()))
                    .findFirst()
                    .orElse(null);
            return dtoMapper.toUserDto(user, profile, subscription);
        }).collect(Collectors.toList()));
        pageDto.setPageNumber(userPage.getNumber());
        pageDto.setPageSize(pageable.getPageSize());
        pageDto.setTotalPages(userPage.getTotalPages());
        pageDto.setTotalElements(userPage.getTotalElements());
        return pageDto;
    }

    private void logUpdate(UserProfile profile, String action, Principal principal) {
        String adminUsername = principal != null ? principal.getName() : "unknown";
        String timestamp = LocalDateTime.now().toString();
        String updateEntry = action + " by " + adminUsername + " at " + timestamp;
        profile.setUpdateHistory(profile.getUpdateHistory() != null
                ? updateEntry + "; " + profile.getUpdateHistory()
                : updateEntry);
    }
}