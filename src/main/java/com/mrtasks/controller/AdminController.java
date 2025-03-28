package com.mrtasks.controller;

import com.mrtasks.model.User;
import com.mrtasks.model.UserSubscription;
import com.mrtasks.repository.UserRepository;
import com.mrtasks.repository.UserSubscriptionRepository;
import com.mrtasks.service.PremiumService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final PremiumService premiumService;
    private final UserRepository userRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;

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

        // Fetch subscriptions for the users in the current page
        List<Long> userIds = userPage.getContent().stream().map(User::getId).toList();
        List<UserSubscription> subscriptions = userSubscriptionRepository.findByUserIdIn(userIds);
        Map<Long, UserSubscription> subscriptionMap = subscriptions.stream()
                .collect(Collectors.toMap(sub -> sub.getUser().getId(), sub -> sub));

        // Pass data to the model
        model.addAttribute("users", userPage.getContent());
        model.addAttribute("subscriptions", subscriptionMap); // Map of user ID to subscription
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
        return adminDashboard(page, size, search, model);
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
        return adminDashboard(page, size, search, model);
    }
}