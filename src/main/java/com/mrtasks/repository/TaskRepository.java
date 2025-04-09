package com.mrtasks.repository;

import com.mrtasks.model.Client;
import com.mrtasks.model.Task;
import com.mrtasks.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByUser(User user);
    Task findByIdAndUser(Long id, User user);
    List<Task> findByUserAndIdIn(User user, List<Long> taskIds);
    List<Task> findByUserAndStatus(User user, Task.TaskStatus status);
    Page<Task> findByUser(User user, Pageable pageable);
    Page<Task> findByUserAndTitleContainingIgnoreCase(User user, String title, Pageable pageable);
    Page<Task> findByUserAndClientId(User user, Long clientId, Pageable pageable);
    Page<Task> findByUserAndStatus(User user, Task.TaskStatus status, Pageable pageable);
    Page<Task> findByUserAndTitleContainingIgnoreCaseAndClientId(User user, String title, Long clientId, Pageable pageable);
    Page<Task> findByUserAndTitleContainingIgnoreCaseAndStatus(User user, String title, Task.TaskStatus status, Pageable pageable);
    Page<Task> findByUserAndClientIdAndStatus(User user, Long clientId, Task.TaskStatus status, Pageable pageable);
    Page<Task> findByUserAndTitleContainingIgnoreCaseAndClientIdAndStatus(User user, String title, Long clientId, Task.TaskStatus status, Pageable pageable);
    boolean existsByClient(Client client);
    List<Task> findByUserAndDeadlineAfter(User user, LocalDateTime deadline);
}