package com.taskmaster.controller;

import com.taskmaster.model.Task;
import com.taskmaster.model.User;
import com.taskmaster.repository.UserRepository;
import com.taskmaster.service.InvoiceService;
import com.taskmaster.service.PremiumService;
import com.taskmaster.service.TaskService;
import lombok.Getter;
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
        return ResponseEntity.ok(task); // Return the saved task
    }

    @GetMapping("/tasks/edit/{id}")
    public String showEditForm(@PathVariable Long id, Authentication auth, Model model) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        Task task = taskService.getTaskByIdAndUser(id, user);
        if (task == null) {
            model.addAttribute("error", "Task not found or not authorized");
            return listTasks(model, auth);
        }
        model.addAttribute("task", task);
        return "edit-task";
    }

    @PostMapping("/tasks/edit/{id}")
    public String updateTask(@PathVariable Long id, @ModelAttribute Task task, Authentication auth, Model model) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        Task existingTask = taskService.getTaskByIdAndUser(id, user);
        if (existingTask == null) {
            model.addAttribute("error", "Task not found or not authorized");
            return listTasks(model, auth);
        }
        task.setId(id);
        task.setUser(user);
        task.setOrderIndex(existingTask.getOrderIndex()); // Preserve order
        taskService.updateTask(task);
        return "redirect:/tasks?message=Task+updated+successfully";
    }

    @PostMapping("/tasks/delete/{id}")
    public String deleteTask(@PathVariable Long id, Authentication auth, Model model) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        Task task = taskService.getTaskByIdAndUser(id, user);
        if (task == null) {
            model.addAttribute("error", "Task not found or not authorized");
            return listTasks(model, auth);
        }
        taskService.deleteTask(id, user);
        return "redirect:/tasks?message=Task+deleted+successfully";
    }

    @PostMapping("/tasks/color/{id}")
    @ResponseBody // Indicates this returns a response body, not a view
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

    @GetMapping("/invoice")
    public ResponseEntity<byte[]> downloadInvoice(
            @RequestParam(required = false) String clientName,
            Authentication auth) throws Exception {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        if (!premiumService.isPremiumUser(user)) {
            return ResponseEntity.status(403).body("Premium subscription required".getBytes());
        }
        byte[] invoice = invoiceService.generateInvoice(user, clientName);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=invoice_" + user.getUsername() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(invoice);
    }

    @PostMapping("/invoice/email")
    public String sendInvoiceEmail(
            @RequestParam(required = false) String clientName,
            @RequestParam String clientEmail,
            Authentication auth,
            Model model) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        try {
            if (!premiumService.isPremiumUser(user)) {
                model.addAttribute("error", "Premium subscription required or expired");
                return "tasks";
            }
            invoiceService.sendInvoiceEmail(user, clientName, clientEmail);
            model.addAttribute("message", "Invoice emailed successfully to " + clientEmail);
        } catch (Exception e) {
            model.addAttribute("error", "Failed to send invoice: " + e.getMessage());
        }
        return "redirect:/tasks";
    }
}