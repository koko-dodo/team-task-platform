package com.group15.lab1.controller.dto;

import java.time.LocalDateTime;

/**
 * 用户响应DTO
 *
 * 参考：
 * - 添加字段：id, username, email, fullName, avatarUrl, createdAt
 * - 不包含敏感信息（如密码）
 */
public class UserResponse {

    private Long id;
    private String username;
    private LocalDateTime createdAt;

    public UserResponse() {
    }

    public UserResponse(Long id, String username, LocalDateTime createdAt) {
        this.id = id;
        this.username = username;
        this.createdAt = createdAt;
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
}