package com.taskmaster.controller;

import com.taskmaster.model.User;
import com.taskmaster.model.UserProfile;
import com.taskmaster.repository.UserProfileRepository;
import com.taskmaster.repository.UserRepository;
import lombok.RequiredArgsConstructor;
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

    @GetMapping("/profile")
    public String showProfile(Model model, Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        UserProfile profile = userProfileRepository.findByUser(user).orElse(new UserProfile());
        model.addAttribute("user", user);
        model.addAttribute("profile", profile);
        return "profile";
    }

    @PostMapping("/profile")
    public String updateProfile(
            @ModelAttribute UserProfile profile,
            Authentication auth,
            Model model) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        Optional<UserProfile> existingProfile = userProfileRepository.findByUser(user);
        UserProfile updatedProfile = existingProfile.orElse(new UserProfile());
        updatedProfile.setUser(user);
        updatedProfile.setCompanyName(profile.getCompanyName());
        updatedProfile.setLogoUrl(profile.getLogoUrl());
        updatedProfile.setEmail(profile.getEmail());
        updatedProfile.setPhone(profile.getPhone());
        userProfileRepository.save(updatedProfile);
        model.addAttribute("message", "Profile updated successfully");
        model.addAttribute("user", user);
        model.addAttribute("profile", updatedProfile);
        return "profile";
    }
}