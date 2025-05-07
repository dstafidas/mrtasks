package com.mrtasks.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Entity
@Table(name = "tasks")
@Data
@EqualsAndHashCode(callSuper = true)
public class Task extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "client_id")
    private Client client;

    private String title;
    @Column(columnDefinition = "TEXT")
    private String description;
    private LocalDateTime deadline;
    private boolean billable;
    private double hoursWorked;
    private double hourlyRate;
    private String clientName;
    private double advancePayment;
    private String color;
    private int orderIndex;
    private boolean hidden = false;
    @Column(columnDefinition = "float default 0")
    private double fixedAmount;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "varchar(10) default 'HOURLY'")
    private BillingType billingType = BillingType.HOURLY;

    @Enumerated(EnumType.STRING)
    private TaskStatus status = TaskStatus.TODO;

    public double getTotal() {
        if (!billable) return 0;
        return billingType == BillingType.FIXED ? fixedAmount : hoursWorked * hourlyRate;
    }

    public double getRemainingDue() {
        if (!billable) return 0;
        return billingType == BillingType.FIXED ? (fixedAmount - advancePayment) : (hoursWorked * hourlyRate - advancePayment);
    }

    public enum TaskStatus {
        TODO,
        IN_PROGRESS,
        COMPLETED
    }

    public enum BillingType {
        HOURLY,
        FIXED
    }
}