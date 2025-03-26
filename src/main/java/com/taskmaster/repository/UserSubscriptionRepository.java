package com.taskmaster.repository;

import com.taskmaster.model.User;
import com.taskmaster.model.UserSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, Long> {
    Optional<UserSubscription> findByUser(User user);
    List<UserSubscription> findByUserIdIn(List<Long> userIds);
}