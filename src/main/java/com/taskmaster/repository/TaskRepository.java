package com.taskmaster.repository;

import com.taskmaster.model.Task;
import com.taskmaster.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByUser(User user);
    List<Task> findByUserAndBillable(User user, boolean billable);
    Task findByIdAndUser(Long id, User user);
}