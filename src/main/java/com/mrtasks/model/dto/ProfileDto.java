package com.mrtasks.model.dto;

import lombok.Data;

@Data
public class ProfileDto {
    private String username;
    private String companyName;
    private String logoUrl;
    private String email;
    private String phone;
    private String language;
    private boolean emailVerified;
    private String emailVerificationToken;
}