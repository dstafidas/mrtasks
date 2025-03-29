package com.mrtasks.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "user_profile")
@Data
public class UserProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    private String companyName;
    private String logoUrl;
    private String email;
    private String phone;

    @Column(name = "language", nullable = false, columnDefinition = "varchar(5) default 'en'")
    private String language = "en";
}