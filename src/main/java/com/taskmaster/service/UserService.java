package com.taskmaster.service;

import com.taskmaster.model.User;
import com.taskmaster.model.UserSubscription;
import com.taskmaster.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public void upgradeToPremium(Long userId, int months) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserSubscription subscription = user.getSubscription();
        if (subscription == null) {
            subscription = new UserSubscription();
            subscription.setUser(user);
            user.setSubscription(subscription);
        }

        subscription.setPremium(true);
        subscription.setExpiresAt(LocalDateTime.now().plusMonths(months));
        subscription.setLastUpdated(LocalDateTime.now());

        userRepository.save(user);
    }
}