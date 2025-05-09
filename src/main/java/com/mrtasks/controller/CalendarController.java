package com.mrtasks.controller;

import com.mrtasks.model.Task;
import com.mrtasks.model.User;
import com.mrtasks.model.UserProfile;
import com.mrtasks.model.dto.TaskDto; // Assuming you'll convert to DTOs
import com.mrtasks.model.dto.mapper.DtoMapper;
import com.mrtasks.repository.UserProfileRepository;
import com.mrtasks.repository.UserRepository;
import com.mrtasks.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.stream.Collectors;
import java.time.LocalDateTime; // For filtering tasks with deadlines

@Controller
@RequiredArgsConstructor
public class CalendarController {

    private final TaskService taskService;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final DtoMapper dtoMapper;

    @GetMapping("/calendar")
    public String showCalendar(Model model, Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        List<Task> allUserTasks = taskService.getTasksForUser(user);

        List<TaskDto> tasksWithDeadlines = allUserTasks.stream()
                .filter(task -> task.getDeadline() != null && !task.isHidden())
                .map(dtoMapper::toTaskDto)
                .collect(Collectors.toList());

        long tasksWithoutDeadlinesCount = allUserTasks.stream()
                .filter(task -> task.getDeadline() == null && !task.isHidden())
                .count();

        // Fetch user profile for language and other settings if needed for the calendar page
        UserProfile userProfile = userProfileRepository.findByUser(user)
                .orElseGet(() -> {
                    UserProfile newProfile = new UserProfile();
                    newProfile.setUser(user);
                    newProfile.setLanguage("en"); // Default language
                    return userProfileRepository.save(newProfile);
                });

        model.addAttribute("calendarTasks", tasksWithDeadlines);
        model.addAttribute("tasksWithoutDeadlineCount", tasksWithoutDeadlinesCount);
        model.addAttribute("userProfile", dtoMapper.toProfileDto(userProfile)); // For translations, currency etc.

        return "calendar"; // Name of your new Thymeleaf template
    }
}