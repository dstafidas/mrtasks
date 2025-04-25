package com.mrtasks.controller;

import com.mrtasks.config.RateLimitConfig;
import com.mrtasks.model.Task;
import com.mrtasks.model.Task.TaskStatus;
import com.mrtasks.model.User;
import com.mrtasks.model.dto.ClientDto;
import com.mrtasks.model.dto.mapper.DtoMapper;
import com.mrtasks.model.dto.PageDto;
import com.mrtasks.model.dto.TaskDto;
import com.mrtasks.repository.ClientRepository;
import com.mrtasks.repository.TaskRepository;
import com.mrtasks.repository.UserRepository;
import com.mrtasks.service.TaskService;
import com.mrtasks.utils.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping
@RequiredArgsConstructor
public class TasksController {

    private final TaskService taskService;
    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final TaskRepository taskRepository;
    private final RateLimitConfig rateLimitConfig;
    private final DtoMapper dtoMapper;

    @GetMapping("/tasks")
    public String listAllTasks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) String status,
            Model model,
            Authentication auth,
            HttpServletRequest request) {
        // Validate page and size
        if (page < 0) {
            page = 0;
        }
        // Rate limiting
        boolean canSearchTasks = rateLimitConfig.canSearchTasks(auth.getName(), RequestUtils.getClientIp(request));
        if (!canSearchTasks) {
            User user = userRepository.findByUsername(auth.getName()).orElseThrow();
            model.addAttribute("error", "limit.error.rate.task.search");
            model.addAttribute("tasks", List.of());
            model.addAttribute("clients", clientRepository.findByUser(user).stream()
                    .map(dtoMapper::toClientDto)
                    .collect(Collectors.toList()));
            model.addAttribute("currentPage", 0);
            model.addAttribute("totalPages", 0);
            model.addAttribute("pageSize", size);
            model.addAttribute("search", search);
            model.addAttribute("clientId", clientId);
            model.addAttribute("status", status);
            return "tasks";
        }

        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        Pageable pageable = PageRequest.of(page, size);


        // Convert String status to TaskStatus enum if provided
        TaskStatus taskStatus = null;
        if (status != null && !status.trim().isEmpty()) {
            try {
                taskStatus = TaskStatus.valueOf(status.trim());
            } catch (IllegalArgumentException e) {
                taskStatus = null;
            }
        }

        Page<Task> taskPage = queryTasks(user, search, clientId, taskStatus, pageable);
        List<TaskDto> taskDtos = taskPage.getContent().stream()
                .sorted(Comparator.comparingInt(Task::getOrderIndex))
                .map(dtoMapper::toTaskDto)
                .collect(Collectors.toList());
        List<ClientDto> clientDtos = clientRepository.findByUser(user).stream()
                .map(dtoMapper::toClientDto)
                .collect(Collectors.toList());

        model.addAttribute("tasks", taskDtos);
        model.addAttribute("clients", clientDtos);
        model.addAttribute("currentPage", taskPage.getNumber());
        model.addAttribute("totalPages", taskPage.getTotalPages());
        model.addAttribute("pageSize", size);
        model.addAttribute("search", search);
        model.addAttribute("clientId", clientId);
        model.addAttribute("status", status);
        return "tasks";
    }

    @GetMapping("/tasks/search")
    @ResponseBody
    public ResponseEntity<?> searchTasks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) String status,
            Authentication auth,
            HttpServletRequest request) {
        // Rate limiting
        boolean canSearchTasks = rateLimitConfig.canSearchTasks(auth.getName(), RequestUtils.getClientIp(request));
        if (!canSearchTasks) {
            return ResponseEntity.status(429).body("limit.error.rate.task.search");
        }
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        // Validate page and size
        if (page < 0) {
            page = 0;
        }

        Pageable pageable = PageRequest.of(page, size);


        // Convert String status to TaskStatus enum if provided
        TaskStatus taskStatus = null;
        if (status != null && !status.trim().isEmpty()) {
            try {
                taskStatus = TaskStatus.valueOf(status.trim());
            } catch (IllegalArgumentException e) {
                taskStatus = null;
            }
        }

        Page<Task> taskPage = queryTasks(user, search, clientId, taskStatus, pageable);
        PageDto<TaskDto> pageDto = new PageDto<>();
        pageDto.setContent(taskPage.getContent().stream()
                .sorted(Comparator.comparingInt(Task::getOrderIndex))
                .map(dtoMapper::toTaskDto)
                .collect(Collectors.toList()));
        pageDto.setPageNumber(taskPage.getNumber());
        pageDto.setPageSize(taskPage.getSize());
        pageDto.setTotalPages(taskPage.getTotalPages());
        pageDto.setTotalElements(taskPage.getTotalElements());

        return ResponseEntity.ok(pageDto);
    }

    private Page<Task> queryTasks(User user, String search, Long clientId, TaskStatus taskStatus, Pageable pageable) {
        if (search != null && !search.trim().isEmpty()) {
            if (clientId != null && taskStatus != null) {
                return taskRepository.findByUserAndTitleContainingIgnoreCaseAndClientIdAndStatus(
                        user, search, clientId, taskStatus, pageable);
            } else if (clientId != null) {
                return taskRepository.findByUserAndTitleContainingIgnoreCaseAndClientId(
                        user, search, clientId, pageable);
            } else if (taskStatus != null) {
                return taskRepository.findByUserAndTitleContainingIgnoreCaseAndStatus(
                        user, search, taskStatus, pageable);
            } else {
                return taskRepository.findByUserAndTitleContainingIgnoreCase(user, search, pageable);
            }
        } else {
            if (clientId != null && taskStatus != null) {
                return taskRepository.findByUserAndClientIdAndStatus(user, clientId, taskStatus, pageable);
            } else if (clientId != null) {
                return taskRepository.findByUserAndClientId(user, clientId, pageable);
            } else if (taskStatus != null) {
                return taskRepository.findByUserAndStatus(user, taskStatus, pageable);
            } else {
                return taskRepository.findByUser(user, pageable);
            }
        }
    }
}