package com.group15.lab1.controller.dto;

import com.group15.lab1.entity.TaskOperationScopeType;
import com.group15.lab1.entity.TaskOperationType;
import java.time.LocalDateTime;

public class TaskOperationLogResponse {

    private Long id;
    private Long taskId;
    private Long actorId;
    private String actorName;
    private TaskOperationType operationType;
    private TaskOperationScopeType scopeType;
    private Long teamId;
    private String summary;
    private String oldValue;
    private String newValue;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public Long getActorId() {
        return actorId;
    }

    public void setActorId(Long actorId) {
        this.actorId = actorId;
    }

    public String getActorName() {
        return actorName;
    }

    public void setActorName(String actorName) {
        this.actorName = actorName;
    }

    public TaskOperationType getOperationType() {
        return operationType;
    }

    public void setOperationType(TaskOperationType operationType) {
        this.operationType = operationType;
    }

    public TaskOperationScopeType getScopeType() {
        return scopeType;
    }

    public void setScopeType(TaskOperationScopeType scopeType) {
        this.scopeType = scopeType;
    }

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getOldValue() {
        return oldValue;
    }

    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
