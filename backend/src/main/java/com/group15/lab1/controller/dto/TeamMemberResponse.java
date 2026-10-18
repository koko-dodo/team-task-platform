package com.group15.lab1.controller.dto;

import com.group15.lab1.entity.TeamRole;
import java.time.LocalDateTime;

/**
 * 团队成员响应 DTO。
 */
public class TeamMemberResponse {

    private Long userId;
    private String username;
    private TeamRole role;
    private LocalDateTime joinedAt;

    public TeamMemberResponse() {
    }

    public TeamMemberResponse(Long userId, String username, TeamRole role, LocalDateTime joinedAt) {
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.joinedAt = joinedAt;
    }

    // ========== Getters & Setters ==========

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public TeamRole getRole() {
        return role;
    }

    public void setRole(TeamRole role) {
        this.role = role;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }
}
