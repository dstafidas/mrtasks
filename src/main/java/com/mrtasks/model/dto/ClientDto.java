package com.mrtasks.model.dto;

import lombok.Data;

@Data
public class ClientDto {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String address;
    private String taxId;
}