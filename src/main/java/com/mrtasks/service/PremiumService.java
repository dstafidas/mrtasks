package com.mrtasks.service;

import com.mrtasks.model.User;
import com.mrtasks.model.UserSubscription;
import com.mrtasks.repository.UserSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PremiumService {

    private final UserSubscriptionRepository subscriptionRepository;

    public boolean isPremiumUser(User user) {
        Optional<UserSubscription> subscriptionOpt = subscriptionRepository.findByUser(user);
        return subscriptionOpt.map(sub -> sub.isPremium() && (sub.getExpiresAt() == null || sub.getExpiresAt().isAfter(LocalDateTime.now())))
                .orElse(false);
    }

    public void upgradeToPremium(User user, int months) {
        Optional<UserSubscription> existing = subscriptionRepository.findByUser(user);
        UserSubscription subscription = existing.orElse(new UserSubscription());
        subscription.setUser(user);
        subscription.setPremium(true);
        subscription.setExpiresAt(LocalDateTime.now().plusMonths(months));
        subscription.setLastUpdated(LocalDateTime.now());
        subscriptionRepository.save(subscription);
    }

    public void downgradeFromPremium(User user) {
        Optional<UserSubscription> existing = subscriptionRepository.findByUser(user);
        existing.ifPresent(subscription -> {
            subscription.setPremium(false);
            subscription.setExpiresAt(null);
            subscription.setLastUpdated(LocalDateTime.now());
            subscriptionRepository.save(subscription);
        });
    }

    public LocalDateTime getExpirationDate(User user) {
        Optional<UserSubscription> subscription = subscriptionRepository.findByUser(user);
        return subscription.map(UserSubscription::getExpiresAt).orElse(null);
    }
}