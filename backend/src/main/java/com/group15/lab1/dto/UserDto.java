package com.group15.lab1.dto;

import java.time.LocalDateTime;

/**
 * 响应层：用户数据传输对象
 */
public class UserDto {

    private Long id;
    private String username;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UserProfileDto profile;

    public UserDto() {
    }

    public UserDto(Long id, String username, LocalDateTime createdAt, LocalDateTime updatedAt, UserProfileDto profile) {
        this.id = id;
        this.username = username;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.profile = profile;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public UserProfileDto getProfile() {
        return profile;
    }

    public void setProfile(UserProfileDto profile) {
        this.profile = profile;
    }
}
