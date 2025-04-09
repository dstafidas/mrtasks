package com.mrtasks.controller;

import com.mrtasks.model.Task;
import com.mrtasks.model.User;
import com.mrtasks.repository.TaskRepository;
import com.mrtasks.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/reporting")
@RequiredArgsConstructor
public class ReportingController {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    // Render the reporting page
    @GetMapping
    public String showReportingPage() {
        return "reporting";
    }

    // Helper method to get tasks for a given range
    private List<Task> getTasksForRange(User user, String range) {
        LocalDateTime startDate;
        switch (range.toLowerCase()) {
            case "last-month":
                startDate = LocalDateTime.now().minusMonths(1);
                break;
            case "last-3-months":
                startDate = LocalDateTime.now().minusMonths(3);
                break;
            case "last-6-months":
                startDate = LocalDateTime.now().minusMonths(6);
                break;
            case "one-year":
                startDate = LocalDateTime.now().minusYears(1);
                break;
            case "ytd":
                startDate = LocalDateTime.now().withMonth(1).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
                break;
            default:
                startDate = LocalDateTime.now().minusMonths(1); // Default to last month
        }
        return taskRepository.findByUserAndDeadlineAfter(user, startDate);
    }

    // Tasks per Client
    @GetMapping("/tasks-per-client")
    public ResponseEntity<Map<String, Object>> getTasksPerClient(
            Authentication auth,
            @RequestParam(defaultValue = "last-month") String range) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        List<Task> tasks = getTasksForRange(user, range);

        Map<String, Long> tasksPerClient = tasks.stream()
                .filter(task -> task.getClient() != null)
                .collect(Collectors.groupingBy(
                        task -> task.getClient().getName(),
                        Collectors.counting()
                ));

        Map<String, Object> response = new HashMap<>();
        response.put("labels", new ArrayList<>(tasksPerClient.keySet()));
        response.put("values", new ArrayList<>(tasksPerClient.values()));
        return ResponseEntity.ok(response);
    }

    // Revenue per Client
    @GetMapping("/revenue-per-client")
    public ResponseEntity<Map<String, Object>> getRevenuePerClient(
            Authentication auth,
            @RequestParam(defaultValue = "last-month") String range) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        List<Task> tasks = getTasksForRange(user, range);

        Map<String, Double> revenuePerClient = tasks.stream()
                .filter(task -> task.getClient() != null && task.isBillable())
                .collect(Collectors.groupingBy(
                        task -> task.getClient().getName(),
                        Collectors.summingDouble(Task::getTotal)
                ));

        Map<String, Object> response = new HashMap<>();
        response.put("labels", new ArrayList<>(revenuePerClient.keySet()));
        response.put("values", new ArrayList<>(revenuePerClient.values()));
        return ResponseEntity.ok(response);
    }

    // Tasks per Month
    @GetMapping("/tasks-per-month")
    public ResponseEntity<Map<String, Object>> getTasksPerMonth(
            Authentication auth,
            @RequestParam(defaultValue = "last-month") String range) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        List<Task> tasks = getTasksForRange(user, range);

        // Group by month-year
        Map<String, Long> tasksPerMonth = tasks.stream()
                .collect(Collectors.groupingBy(
                        task -> task.getDeadline().getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + task.getDeadline().getYear(),
                        Collectors.counting()
                ));

        // Sort by date
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
        List<Map.Entry<String, Long>> sortedEntries = tasksPerMonth.entrySet().stream()
                .sorted((e1, e2) -> {
                    LocalDate date1 = LocalDate.parse("01 " + e1.getKey(), formatter);
                    LocalDate date2 = LocalDate.parse("01 " + e2.getKey(), formatter);
                    return date1.compareTo(date2);
                })
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("labels", sortedEntries.stream().map(Map.Entry::getKey).collect(Collectors.toList()));
        response.put("values", sortedEntries.stream().map(Map.Entry::getValue).collect(Collectors.toList()));
        return ResponseEntity.ok(response);
    }

    // Revenue per Month
    @GetMapping("/revenue-per-month")
    public ResponseEntity<Map<String, Object>> getRevenuePerMonth(
            Authentication auth,
            @RequestParam(defaultValue = "last-month") String range) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        List<Task> tasks = getTasksForRange(user, range);

        // Group by month-year
        Map<String, Double> revenuePerMonth = tasks.stream()
                .filter(Task::isBillable)
                .collect(Collectors.groupingBy(
                        task -> task.getDeadline().getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + task.getDeadline().getYear(),
                        Collectors.summingDouble(Task::getTotal)
                ));

        // Sort by date
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
        List<Map.Entry<String, Double>> sortedEntries = revenuePerMonth.entrySet().stream()
                .sorted((e1, e2) -> {
                    LocalDate date1 = LocalDate.parse("01 " + e1.getKey(), formatter);
                    LocalDate date2 = LocalDate.parse("01 " + e2.getKey(), formatter);
                    return date1.compareTo(date2);
                })
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("labels", sortedEntries.stream().map(Map.Entry::getKey).collect(Collectors.toList()));
        response.put("values", sortedEntries.stream().map(Map.Entry::getValue).collect(Collectors.toList()));
        return ResponseEntity.ok(response);
    }
}