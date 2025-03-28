package com.mrtasks.repository;

import com.mrtasks.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Page<User> findByUsernameContainingIgnoreCase(String username, Pageable pageable);
    Optional<User> findByUsername(String username);
}