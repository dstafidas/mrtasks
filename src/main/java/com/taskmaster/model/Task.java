package com.taskmaster.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "tasks")
@Data
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String title;
    private String description;
    private LocalDateTime deadline;
    private boolean billable;
    private double hoursWorked;
    private double hourlyRate;
    private String clientName;
    private double advancePayment;
    private String color;
    private int orderIndex;

    public double getTotal() {
        return billable ? hoursWorked * hourlyRate : 0;
    }

    public double getRemainingDue() {
        return billable ? (hoursWorked * hourlyRate - advancePayment) : 0;
    }
}