package com.mrtasks.repository;

import com.mrtasks.model.Client;
import com.mrtasks.model.Task;
import com.mrtasks.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByUser(User user);
    List<Task> findByUserAndBillable(User user, boolean billable);
    Task findByIdAndUser(Long id, User user);
    List<Task> findByUserAndIdIn(User user, List<Long> taskIds);
    List<Task> findByUserAndStatus(User user, Task.TaskStatus status);
    List<Task> findByClient(Client client);
    boolean existsByClient(Client client);
}