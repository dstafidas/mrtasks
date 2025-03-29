package com.mrtasks.service;

import com.mrtasks.model.User;
import com.mrtasks.model.UserSubscription;
import com.mrtasks.repository.UserRepository;
import com.mrtasks.repository.UserSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PremiumService {

    private final UserSubscriptionRepository userSubscriptionRepository;
    private final UserRepository userRepository;

    public boolean isPremiumUser(User user) {
        Optional<UserSubscription> subscriptionOpt = userSubscriptionRepository.findByUser(user);
        return subscriptionOpt.map(sub -> sub.isPremium() && (sub.getExpiresAt() == null || sub.getExpiresAt().isAfter(LocalDateTime.now())))
                .orElse(false);
    }

    public void upgradeToPremiumFromAdmin(User user, int months) {
        Optional<UserSubscription> existing = userSubscriptionRepository.findByUser(user);
        UserSubscription subscription = existing.orElse(new UserSubscription());
        subscription.setUser(user);
        subscription.setPremium(true);
        subscription.setExpiresAt(LocalDateTime.now().plusMonths(months));
        subscription.setLastUpdated(LocalDateTime.now());
        userSubscriptionRepository.save(subscription);
    }

    public void upgradeToPremium(Long userId, int months) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserSubscription subscription = userSubscriptionRepository.findByUser(user).orElse(null);
        if (subscription == null) {
            subscription = new UserSubscription();
            subscription.setUser(user);
        }

        subscription.setPremium(true);
        subscription.setExpiresAt(LocalDateTime.now().plusMonths(months));
        subscription.setLastUpdated(LocalDateTime.now());

        userSubscriptionRepository.save(subscription);
    }

    public void downgradeFromPremium(User user) {
        Optional<UserSubscription> existing = userSubscriptionRepository.findByUser(user);
        existing.ifPresent(subscription -> {
            subscription.setPremium(false);
            subscription.setExpiresAt(null);
            subscription.setLastUpdated(LocalDateTime.now());
            userSubscriptionRepository.save(subscription);
        });
    }

    public LocalDateTime getExpirationDate(User user) {
        Optional<UserSubscription> subscription = userSubscriptionRepository.findByUser(user);
        return subscription.map(UserSubscription::getExpiresAt).orElse(null);
    }
}