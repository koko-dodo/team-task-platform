package com.group15.lab1.controller.dto;

import com.group15.lab1.entity.TeamRole;
import jakarta.validation.constraints.Size;

/**
 * 添加团队成员的请求 DTO。
 */
public class AddTeamMemberRequest {

    private Long userId;

    @Size(max = 20, message = "用户名长度不能超过 20")
    private String username;

    private TeamRole role;

    public AddTeamMemberRequest() {
    }

    public AddTeamMemberRequest(Long userId, String username, TeamRole role) {
        this.userId = userId;
        this.username = username;
        this.role = role;
    }

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
}
