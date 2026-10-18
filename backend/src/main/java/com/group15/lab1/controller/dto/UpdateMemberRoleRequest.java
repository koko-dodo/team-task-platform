package com.group15.lab1.controller.dto;

import com.group15.lab1.entity.TeamRole;
import jakarta.validation.constraints.NotNull;

/**
 * 更新团队成员角色的请求 DTO。
 */
public class UpdateMemberRoleRequest {

    @NotNull(message = "角色不能为空")
    private TeamRole role;

    public UpdateMemberRoleRequest() {
    }

    public UpdateMemberRoleRequest(TeamRole role) {
        this.role = role;
    }

    public TeamRole getRole() {
        return role;
    }

    public void setRole(TeamRole role) {
        this.role = role;
    }
}
