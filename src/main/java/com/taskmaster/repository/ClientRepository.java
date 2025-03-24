package com.taskmaster.repository;

import com.taskmaster.model.Client;
import com.taskmaster.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClientRepository extends JpaRepository<Client, Long> {
    List<Client> findByUser(User user);
    Client findByIdAndUser(Long id, User user);
}