package com.taskmaster.controller;

import com.taskmaster.model.User;
import com.taskmaster.repository.UserRepository;
import com.taskmaster.service.PremiumService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final PremiumService premiumService;
    private final UserRepository userRepository;

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

        model.addAttribute("users", userPage.getContent());
        model.addAttribute("currentPage", userPage.getNumber());
        model.addAttribute("totalPages", userPage.getTotalPages());
        model.addAttribute("search", search); // Preserve search term in UI
        return "admin";
    }

    @PostMapping("/upgrade")
    public String upgradeUser(
            @RequestParam String username,
            @RequestParam int months,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            Model model) {
        try {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
            premiumService.upgradeToPremium(user, months);
            model.addAttribute("message", "User " + username + " upgraded to Premium for " + months + " months");
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
        }
        return adminDashboard(page, size, search, model); // Reuse pagination logic
    }

    @PostMapping("/downgrade")
    public String downgradeUser(
            @RequestParam String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            Model model) {
        try {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
            premiumService.downgradeFromPremium(user);
            model.addAttribute("message", "User " + username + " downgraded from Premium");
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
        }
        return adminDashboard(page, size, search, model); // Reuse pagination logic
    }
}