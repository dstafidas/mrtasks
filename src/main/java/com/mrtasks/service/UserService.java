package com.mrtasks.service;

import com.mrtasks.model.User;
import com.mrtasks.model.UserSubscription;
import com.mrtasks.repository.UserRepository;
import com.mrtasks.repository.UserSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;

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

        userRepository.save(user);
    }
}