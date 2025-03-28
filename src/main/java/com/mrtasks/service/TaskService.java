package com.mrtasks.service;

import com.mrtasks.model.Task;
import com.mrtasks.model.User;
import com.mrtasks.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;

    public List<Task> getTasksForUser(User user) {
        return taskRepository.findByUser(user).stream()
                .sorted(Comparator.comparingInt(Task::getOrderIndex))
                .toList();
    }

    public void saveTask(Task task) {
        taskRepository.save(task);
    }

    public Task getTaskByIdAndUser(Long id, User user) {
        return taskRepository.findByIdAndUser(id, user);
    }

    public void updateTask(Task task) {
        taskRepository.save(task);
    }

    public void deleteTask(Long id, User user) {
        Task task = getTaskByIdAndUser(id, user);
        if (task != null) {
            taskRepository.delete(task);
        }
    }

    public int getMaxOrderIndex(User user, Task.TaskStatus status) {
        return taskRepository.findByUserAndStatus(user, status).stream()
                .mapToInt(Task::getOrderIndex)
                .max()
                .orElse(-1); // Start at -1 so first task in column gets 0
    }

    public void reorderTasks(List<Long> taskIds, User user) {
        // Fetch all tasks for the user
        List<Task> userTasks = taskRepository.findByUser(user);

        // Update orderIndex for tasks in the provided list
        for (int i = 0; i < taskIds.size(); i++) {
            Long taskId = taskIds.get(i);
            int finalI = i;
            userTasks.stream()
                    .filter(task -> task.getId().equals(taskId))
                    .findFirst()
                    .ifPresent(task -> {
                        task.setOrderIndex(finalI);
                        taskRepository.save(task);
                    });
        }
    }
}