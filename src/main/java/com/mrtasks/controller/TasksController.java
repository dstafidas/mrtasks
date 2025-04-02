package com.mrtasks.controller;

import com.mrtasks.model.Client;
import com.mrtasks.model.Task;
import com.mrtasks.model.Task.TaskStatus;
import com.mrtasks.model.User;
import com.mrtasks.repository.ClientRepository;
import com.mrtasks.repository.TaskRepository;
import com.mrtasks.repository.UserRepository;
import com.mrtasks.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Comparator;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class TasksController {

    private final TaskService taskService;
    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final TaskRepository taskRepository;

    @GetMapping("/tasks")
    public String listAllTasks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) String status,
            Model model,
            Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        Pageable pageable = PageRequest.of(page, size);
        Page<Task> taskPage;

        // Convert String status to TaskStatus enum if provided
        TaskStatus taskStatus = null;
        if (status != null && !status.trim().isEmpty()) {
            try {
                taskStatus = TaskStatus.valueOf(status.trim());
            } catch (IllegalArgumentException e) {
                // Handle invalid status gracefully, e.g., ignore or default to null
                taskStatus = null;
            }
        }

        if (search != null && !search.trim().isEmpty()) {
            if (clientId != null && taskStatus != null) {
                taskPage = taskRepository.findByUserAndTitleContainingIgnoreCaseAndClientIdAndStatus(
                        user, search, clientId, taskStatus, pageable);
            } else if (clientId != null) {
                taskPage = taskRepository.findByUserAndTitleContainingIgnoreCaseAndClientId(
                        user, search, clientId, pageable);
            } else if (taskStatus != null) {
                taskPage = taskRepository.findByUserAndTitleContainingIgnoreCaseAndStatus(
                        user, search, taskStatus, pageable);
            } else {
                taskPage = taskRepository.findByUserAndTitleContainingIgnoreCase(user, search, pageable);
            }
        } else {
            if (clientId != null && taskStatus != null) {
                taskPage = taskRepository.findByUserAndClientIdAndStatus(user, clientId, taskStatus, pageable);
            } else if (clientId != null) {
                taskPage = taskRepository.findByUserAndClientId(user, clientId, pageable);
            } else if (taskStatus != null) {
                taskPage = taskRepository.findByUserAndStatus(user, taskStatus, pageable);
            } else {
                taskPage = taskRepository.findByUser(user, pageable);
            }
        }

        List<Client> clients = clientRepository.findByUser(user);
        model.addAttribute("tasks", taskPage.getContent().stream()
                .sorted(Comparator.comparingInt(Task::getOrderIndex))
                .toList());
        model.addAttribute("clients", clients);
        model.addAttribute("currentPage", taskPage.getNumber());
        model.addAttribute("totalPages", taskPage.getTotalPages());
        model.addAttribute("pageSize", size);
        model.addAttribute("search", search);
        model.addAttribute("clientId", clientId);
        model.addAttribute("status", status); // Keep original String value for the form
        return "tasks";
    }
}