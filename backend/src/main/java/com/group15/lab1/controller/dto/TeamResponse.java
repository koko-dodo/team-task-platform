package com.group15.lab1.controller.dto;

import com.group15.lab1.entity.TeamRole;
import java.time.LocalDate;

/**
 * 团队详情响应 DTO。
 */
public class TeamResponse {

    private Long id;
    private String name;
    private Long ownerId;
    private String ownerUsername;
    private String status;
    private TeamRole currentUserRole;
    private LocalDate createdAt;
    private LocalDate updatedAt;
    private Integer memberCount;    

    public TeamResponse() {
    }

    public TeamResponse(
            Long id,
            String name,
            Long ownerId,
            String ownerUsername,
            String status,
            TeamRole currentUserRole,
            LocalDate createdAt,
            LocalDate updatedAt) {
        this.id = id;
        this.name = name;
        this.ownerId = ownerId;
        this.ownerUsername = ownerUsername;
        this.status = status;
        this.currentUserRole = currentUserRole;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
    public TeamResponse(
            Long id,
            String name,
            Long ownerId,
            String ownerUsername,
            String status,
            TeamRole currentUserRole,
            LocalDate createdAt,
            LocalDate updatedAt,
            Integer memberCount) {
        this.id = id;
        this.name = name;
        this.ownerId = ownerId;
        this.ownerUsername = ownerUsername;
        this.status = status;
        this.currentUserRole = currentUserRole;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.memberCount = memberCount;
    }

    // ========== Getters & Setters ==========

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public String getOwnerUsername() {
        return ownerUsername;
    }

    public void setOwnerUsername(String ownerUsername) {
        this.ownerUsername = ownerUsername;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public TeamRole getCurrentUserRole() {
        return currentUserRole;
    }

    public void setCurrentUserRole(TeamRole currentUserRole) {
        this.currentUserRole = currentUserRole;
    }

    public LocalDate getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDate createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDate getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDate updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(Integer memberCount) {
        this.memberCount = memberCount;
    }
}
