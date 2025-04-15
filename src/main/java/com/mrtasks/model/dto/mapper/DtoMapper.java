package com.mrtasks.model.dto.mapper;

import com.mrtasks.model.*;
import com.mrtasks.model.dto.ClientDto;
import com.mrtasks.model.dto.ProfileDto;
import com.mrtasks.model.dto.TaskDto;
import com.mrtasks.model.dto.UserDto;
import org.springframework.stereotype.Component;

@Component
public class DtoMapper {

    public TaskDto toTaskDto(Task task) {
        TaskDto dto = new TaskDto();
        dto.setId(task.getId());
        dto.setTitle(task.getTitle());
        dto.setDescription(task.getDescription());
        dto.setDeadline(task.getDeadline());
        dto.setBillable(task.isBillable());
        dto.setHoursWorked(task.getHoursWorked());
        dto.setHourlyRate(task.getHourlyRate());
        dto.setClientName(task.getClientName());
        dto.setAdvancePayment(task.getAdvancePayment());
        dto.setColor(task.getColor());
        dto.setOrderIndex(task.getOrderIndex());
        dto.setHidden(task.isHidden());
        dto.setStatus(task.getStatus().name());
        dto.setClient(task.getClient() != null ? toClientDto(task.getClient()) : null);
        dto.setTotal(task.getTotal());
        dto.setRemainingDue(task.getRemainingDue());
        return dto;
    }

    public ClientDto toClientDto(Client client) {
        ClientDto dto = new ClientDto();
        dto.setId(client.getId());
        dto.setName(client.getName());
        dto.setEmail(client.getEmail());
        dto.setPhone(client.getPhone());
        dto.setAddress(client.getAddress());
        dto.setTaxId(client.getTaxId());
        return dto;
    }

    public Task toTask(TaskDto dto, Task task) {
        task.setTitle(dto.getTitle());
        task.setDescription(dto.getDescription());
        task.setDeadline(dto.getDeadline());
        task.setBillable(dto.isBillable());
        task.setHoursWorked(dto.getHoursWorked());
        task.setHourlyRate(dto.getHourlyRate());
        task.setClientName(dto.getClientName());
        task.setAdvancePayment(dto.getAdvancePayment());
        task.setColor(dto.getColor());
        task.setHidden(dto.isHidden());
        task.setStatus(Task.TaskStatus.valueOf(dto.getStatus()));
        return task;
    }

    public ProfileDto toProfileDto(UserProfile profile) {
        ProfileDto dto = new ProfileDto();
        dto.setUsername(profile.getUser().getUsername());
        dto.setCompanyName(profile.getCompanyName());
        dto.setLogoUrl(profile.getLogoUrl());
        dto.setEmail(profile.getEmail());
        dto.setPhone(profile.getPhone());
        dto.setLanguage(profile.getLanguage());
        dto.setEmailVerified(profile.isEmailVerified());
        dto.setEmailVerificationToken(profile.getEmailVerificationToken());
        return dto;
    }

    public UserProfile toUserProfile(ProfileDto dto, UserProfile profile) {
        profile.setCompanyName(dto.getCompanyName());
        profile.setLogoUrl(dto.getLogoUrl());
        profile.setEmail(dto.getEmail());
        profile.setPhone(dto.getPhone());
        profile.setLanguage(dto.getLanguage() != null ? dto.getLanguage() : "en");
        profile.setEmailVerificationToken(dto.getEmailVerificationToken());
        return profile;
    }

    public UserDto toUserDto(User user, UserProfile profile, UserSubscription subscription) {
        UserDto dto = new UserDto();
        dto.setUsername(user.getUsername());
        dto.setRole(user.getRole());
        dto.setLastLogin(user.getLastLogin());
        if (profile != null) {
            dto.setCompanyName(profile.getCompanyName());
            dto.setEmail(profile.getEmail());
            dto.setPhone(profile.getPhone());
            dto.setLanguage(profile.getLanguage());
            dto.setEmailVerified(profile.isEmailVerified());
            dto.setEmailVerificationToken(profile.getEmailVerificationToken());
            dto.setResetPasswordToken(profile.getResetPasswordToken());
            dto.setUpdateHistory(profile.getUpdateHistory());
        }
        if (subscription != null) {
            dto.setPremium(subscription.isPremium());
            dto.setExpiresAt(subscription.getExpiresAt());
        }
        return dto;
    }

    public UserProfile toUserProfile(UserDto dto, UserProfile profile) {
        profile.setCompanyName(dto.getCompanyName());
        profile.setEmail(dto.getEmail());
        profile.setPhone(dto.getPhone());
        profile.setLanguage(dto.getLanguage());
        profile.setEmailVerified(dto.isEmailVerified());
        profile.setEmailVerificationToken(dto.getEmailVerificationToken());
        profile.setResetPasswordToken(dto.getResetPasswordToken());
        profile.setUpdateHistory(dto.getUpdateHistory());
        return profile;
    }
}