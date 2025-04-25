package com.mrtasks.model.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserDto {
    private String username;
    private String role;
    private LocalDateTime lastLogin;
    private String companyName;
    private String email;
    private String phone;
    private String language;
    private boolean emailVerified;
    private String emailVerificationToken;
    private String resetPasswordToken;
    private String updateHistory;
    private boolean isPremium;
    private LocalDateTime expiresAt;
    private String status;
}