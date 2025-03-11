package com.taskmaster.controller;

import com.taskmaster.model.Task;
import com.taskmaster.model.User;
import com.taskmaster.repository.UserRepository;
import com.taskmaster.service.InvoiceService;
import com.taskmaster.service.PremiumService;
import com.taskmaster.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final InvoiceService invoiceService;
    private final PremiumService premiumService;
    private final UserRepository userRepository;

    @GetMapping("/tasks")
    public String listTasks(Model model, Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        model.addAttribute("tasks", taskService.getTasksForUser(user));
        model.addAttribute("newTask", new Task());
        model.addAttribute("isPremium", premiumService.isPremiumUser(user));
        model.addAttribute("expiresAt", premiumService.getExpirationDate(user));
        return "tasks";
    }

    @PostMapping("/tasks")
    @ResponseBody
    public ResponseEntity<Task> addTask(@ModelAttribute Task task, Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        task.setUser(user);
        task.setOrderIndex(taskService.getMaxOrderIndex(user) + 1);
        taskService.saveTask(task);
        return ResponseEntity.ok(task);
    }

    @GetMapping("/tasks/{id}")
    @ResponseBody
    public ResponseEntity<Task> getTask(@PathVariable Long id, Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        Task task = taskService.getTaskByIdAndUser(id, user);
        if (task == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(task);
    }

    @PutMapping("/tasks/{id}")
    @ResponseBody
    public ResponseEntity<Task> updateTask(@PathVariable Long id, @ModelAttribute Task task, Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        Task existingTask = taskService.getTaskByIdAndUser(id, user);
        if (existingTask == null) {
            return ResponseEntity.notFound().build();
        }
        existingTask.setTitle(task.getTitle());
        existingTask.setDescription(task.getDescription());
        existingTask.setDeadline(task.getDeadline());
        existingTask.setBillable(task.isBillable());
        existingTask.setHoursWorked(task.getHoursWorked());
        existingTask.setHourlyRate(task.getHourlyRate());
        existingTask.setClientName(task.getClientName());
        existingTask.setAdvancePayment(task.getAdvancePayment());
        existingTask.setColor(task.getColor());
        existingTask.setOrderIndex(existingTask.getOrderIndex());
        taskService.updateTask(existingTask);
        return ResponseEntity.ok(existingTask);
    }

    @DeleteMapping("/tasks/{id}")
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

    @PostMapping("/tasks/color/{id}")
    @ResponseBody
    public ResponseEntity<String> changeTaskColor(@PathVariable Long id, @RequestParam("color") String color, Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        Task task = taskService.getTaskByIdAndUser(id, user);
        if (task == null) {
            return ResponseEntity.status(403).body("Task not found or not authorized");
        }
        task.setColor(color);
        taskService.updateTask(task);
        return ResponseEntity.ok("Color updated");
    }

    @PostMapping("/tasks/reorder")
    @ResponseBody
    public ResponseEntity<String> reorderTasks(@RequestBody List<Long> taskIds, Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        taskService.reorderTasks(taskIds, user);
        return ResponseEntity.ok("Order updated");
    }

    @PostMapping("/invoice")
    public ResponseEntity<byte[]> downloadInvoice(
            @RequestParam("taskIds") List<Long> taskIds,
            Authentication auth) throws Exception {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        if (!premiumService.isPremiumUser(user)) {
            return ResponseEntity.status(403).body("Premium subscription required".getBytes());
        }
        byte[] invoice = invoiceService.generateInvoice(user, taskIds);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=invoice_" + user.getUsername() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(invoice);
    }
}