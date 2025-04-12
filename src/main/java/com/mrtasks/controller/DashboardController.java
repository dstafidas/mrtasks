package com.mrtasks.controller;

import com.mrtasks.config.RateLimitConfig;
import com.mrtasks.model.dto.ClientDto;
import com.mrtasks.model.dto.mapper.DtoMapper;
import com.mrtasks.model.dto.TaskDto;
import com.mrtasks.exception.RateLimitExceededException;
import com.mrtasks.model.Client;
import com.mrtasks.model.Task;
import com.mrtasks.model.User;
import com.mrtasks.model.UserProfile;
import com.mrtasks.repository.ClientRepository;
import com.mrtasks.repository.TaskRepository;
import com.mrtasks.repository.UserProfileRepository;
import com.mrtasks.repository.UserRepository;
import com.mrtasks.service.TaskService;
import io.github.bucket4j.Bucket;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final TaskService taskService;
    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final TaskRepository taskRepository;
    private final RateLimitConfig rateLimitConfig;
    private final UserProfileRepository userProfileRepository;
    private final DtoMapper dtoMapper;

    @GetMapping("/dashboard")
    public String listTasks(Model model, Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();

        // Rate limiting
        Bucket bucket = rateLimitConfig.getTaskSearchBucket(auth.getName());
        boolean canAccess = bucket.tryConsume(1);

        List<TaskDto> tasks;
        List<ClientDto> clients;
        String rateLimitMessage = null;

        if (canAccess) {
            tasks = taskService.getTasksForUser(user).stream()
                    .filter(task -> !task.isHidden())
                    .sorted(Comparator.comparingInt(Task::getOrderIndex))
                    .map(dtoMapper::toTaskDto)
                    .collect(Collectors.toList());
            clients = clientRepository.findByUser(user).stream()
                    .map(dtoMapper::toClientDto)
                    .collect(Collectors.toList());
        } else {
            tasks = List.of();
            clients = List.of();
            rateLimitMessage = "error.rate.limit.list";
        }

        model.addAttribute("tasks", tasks);
        model.addAttribute("clients", clients);
        model.addAttribute("newTask", new TaskDto());
        model.addAttribute("totalTaskCount", taskService.getTasksForUser(user).size());
        model.addAttribute("rateLimitMessage", rateLimitMessage);

        return "dashboard";
    }

    @PostMapping("/dashboard")
    @ResponseBody
    public ResponseEntity<?> addTask(@ModelAttribute TaskDto taskDto, Authentication auth) {
        // Rate limiting
        Bucket bucket = rateLimitConfig.getTaskCreationBucket(auth.getName());
        if (!bucket.tryConsume(1)) {
            throw new RateLimitExceededException("error.rate.limit.task");
        }
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        boolean isEmailVerified = userProfileRepository.findByUser(user)
                .map(UserProfile::isEmailVerified)
                .orElse(false);

        // Check email verification and task limit
        if (!isEmailVerified) {
            long taskCount = taskRepository.countByUser(user);
            if (taskCount >= 10) {
                return ResponseEntity.status(403).body("error.task.limit.unverified");
            }
        }

        Task task = new Task();
        task.setUser(user);
        if (taskDto.getClient() != null && taskDto.getClient().getId() != null) {
            Client client = clientRepository.findByIdAndUser(taskDto.getClient().getId(), user);
            task.setClient(client);
            task.setClientName(client != null ? client.getName() : null);
        } else {
            task.setClient(null);
        }
        task.setOrderIndex(taskService.getMaxOrderIndex(user, Task.TaskStatus.valueOf(taskDto.getStatus())) + 1);
        dtoMapper.toTask(taskDto, task);
        taskService.saveTask(task);
        return ResponseEntity.ok(dtoMapper.toTaskDto(task));
    }

    @GetMapping("/dashboard/task/{id}")
    @ResponseBody
    public ResponseEntity<TaskDto> getTask(@PathVariable Long id, Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        Task task = taskService.getTaskByIdAndUser(id, user);
        if (task == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(dtoMapper.toTaskDto(task));
    }

    @PutMapping("/dashboard/task/{id}")
    @ResponseBody
    public ResponseEntity<TaskDto> updateTask(@PathVariable Long id, @ModelAttribute TaskDto taskDto, Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        Task existingTask = taskService.getTaskByIdAndUser(id, user);
        if (existingTask == null) {
            return ResponseEntity.notFound().build();
        }
        if (taskDto.getClient() != null && taskDto.getClient().getId() != null) {
            Client client = clientRepository.findByIdAndUser(taskDto.getClient().getId(), user);
            existingTask.setClient(client);
            existingTask.setClientName(client != null ? client.getName() : null);
        } else {
            existingTask.setClient(null);
            existingTask.setClientName(null);
        }
        dtoMapper.toTask(taskDto, existingTask);
        taskService.updateTask(existingTask);
        return ResponseEntity.ok(dtoMapper.toTaskDto(existingTask));
    }

    @DeleteMapping("/dashboard/task/{id}")
    @ResponseBody
    public ResponseEntity<Void> deleteTask(@PathVariable Long id, Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        Task task = taskService.getTaskByIdAndUser(id, user);
        if (task == null) {
            return ResponseEntity.notFound().build();
        }
        taskService.deleteTask(id, user);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/dashboard/task/{id}/hide")
    @ResponseBody
    public ResponseEntity<String> hideTask(@PathVariable Long id, Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        Task task = taskService.getTaskByIdAndUser(id, user);
        if (task == null) {
            return ResponseEntity.status(403).body("error.task.notfound");
        }
        task.setHidden(true);
        taskService.updateTask(task);
        return ResponseEntity.ok("success.task.hidden");
    }

    @PostMapping("/dashboard/task/{id}/unhide")
    @ResponseBody
    public ResponseEntity<String> unhideTask(@PathVariable Long id, Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        Task task = taskService.getTaskByIdAndUser(id, user);
        if (task == null) {
            return ResponseEntity.status(403).body("error.task.notfound");
        }
        task.setHidden(false);
        taskService.updateTask(task);
        return ResponseEntity.ok("success.task.unhidden");
    }

    @PostMapping("/dashboard/color/{id}")
    @ResponseBody
    public ResponseEntity<String> changeTaskColor(@PathVariable Long id, @RequestParam("color") String color, Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        Task task = taskService.getTaskByIdAndUser(id, user);
        if (task == null) {
            return ResponseEntity.status(403).body("error.task.notfound");
        }
        task.setColor(color);
        taskService.updateTask(task);
        return ResponseEntity.ok("success.color.updated");
    }

    @PostMapping("/dashboard/move")
    @ResponseBody
    public ResponseEntity<TaskDto> moveTask(
            @RequestParam("taskId") Long taskId,
            @RequestParam("status") String status,
            @RequestBody List<Long> taskIds,
            Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        Task task = taskService.getTaskByIdAndUser(taskId, user);
        if (task == null) {
            return ResponseEntity.status(403).body(null);
        }
        task.setStatus(Task.TaskStatus.valueOf(status));
        taskService.updateTask(task);
        taskService.reorderTasks(taskIds, user);
        return ResponseEntity.ok(dtoMapper.toTaskDto(task));
    }
}