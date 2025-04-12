package com.mrtasks.repository;

import com.mrtasks.model.User;
import com.mrtasks.model.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
    Optional<UserProfile> findByUser(User user);
    Optional<UserProfile> findByEmailVerificationToken(String token);
    Optional<UserProfile> findByResetPasswordToken(String token);
    List<UserProfile> findByUserIdIn(List<Long> userIds);
}