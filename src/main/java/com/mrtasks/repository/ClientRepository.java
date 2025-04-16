package com.mrtasks.repository;

import com.mrtasks.model.Client;
import com.mrtasks.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClientRepository extends JpaRepository<Client, Long> {
    Page<Client> findByUser(User user, Pageable pageable);
    Page<Client> findByUserAndNameContainingIgnoreCase(User user, String name, Pageable pageable);
    Client findByIdAndUser(Long id, User user);
    List<Client> findByUser(User user);
    long countByUser(User user);

}