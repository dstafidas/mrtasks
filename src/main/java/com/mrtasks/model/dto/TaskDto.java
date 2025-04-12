package com.mrtasks.model.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TaskDto {
    private Long id;
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
    private boolean hidden;
    private String status;
    private ClientDto client;
    private double total;
    private double remainingDue;
}