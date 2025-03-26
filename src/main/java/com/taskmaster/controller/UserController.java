package com.taskmaster.controller;

import com.taskmaster.model.User;
import com.taskmaster.model.UserProfile;
import com.taskmaster.model.UserSubscription;
import com.taskmaster.repository.UserProfileRepository;
import com.taskmaster.repository.UserRepository;
import com.taskmaster.repository.UserSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final MessageSource messageSource;

    @GetMapping("/profile")
    public String showProfile(Model model, Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        UserProfile profile = userProfileRepository.findByUser(user).orElse(new UserProfile());
        UserSubscription userSubscription = userSubscriptionRepository.findByUser(user).orElse(new UserSubscription());
        model.addAttribute("user", user);
        model.addAttribute("profile", profile);
        model.addAttribute("subscription", userSubscription);
        return "profile";
    }

    @PostMapping("/profile")
    public String updateProfile(
            @ModelAttribute UserProfile profile,
            Authentication auth,
            Model model) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        UserSubscription userSubscription = userSubscriptionRepository.findByUser(user).orElse(new UserSubscription());
        Optional<UserProfile> existingProfile = userProfileRepository.findByUser(user);
        UserProfile updatedProfile = existingProfile.orElse(new UserProfile());
        updatedProfile.setUser(user);
        updatedProfile.setCompanyName(profile.getCompanyName());
        updatedProfile.setLogoUrl(profile.getLogoUrl());
        updatedProfile.setEmail(profile.getEmail());
        updatedProfile.setPhone(profile.getPhone());
        userProfileRepository.save(updatedProfile);
        String message = messageSource.getMessage("profile.updated.success", null, LocaleContextHolder.getLocale());
        model.addAttribute("message", message);
        model.addAttribute("messageType", "success");
        model.addAttribute("user", user);
        model.addAttribute("profile", updatedProfile);
        model.addAttribute("subscription", userSubscription);
        return "profile";
    }
}