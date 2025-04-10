package com.mrtasks.repository;

import com.mrtasks.model.User;
import com.mrtasks.model.UserSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, Long> {
    Optional<UserSubscription> findByUser(User user);
    List<UserSubscription> findByUserIdIn(List<Long> userIds);
    long countByIsPremiumTrueAndExpiresAtAfter(LocalDateTime localDateTime);
}