package com.taskmaster.service;

import com.taskmaster.model.Task;
import com.taskmaster.model.User;
import com.taskmaster.repository.TaskRepository;
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

    public int getMaxOrderIndex(User user) {
        return taskRepository.findByUser(user).stream()
                .mapToInt(Task::getOrderIndex)
                .max().orElse(0);
    }

    public void reorderTasks(List<Long> taskIds, User user) {
        List<Task> tasks = taskRepository.findByUser(user);
        for (int i = 0; i < taskIds.size(); i++) {
            Long taskId = taskIds.get(i);
            int finalI = i;
            tasks.stream()
                    .filter(task -> task.getId().equals(taskId))
                    .findFirst()
                    .ifPresent(task -> {
                        task.setOrderIndex(finalI);
                        taskRepository.save(task);
                    });
        }
    }
}