package com.group15.lab1.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * 更新任务请求DTO。
 */
public class UpdateTaskRequest {

    @NotBlank(message = "Title cannot be blank")
    @Size(max = 100, message = "Title must be at most 100 characters")
    private String title;

    @Size(max = 500, message = "Description must be at most 500 characters")
    private String description;

    @NotBlank(message = "Status cannot be blank")
    @Size(max = 20, message = "Status must be at most 20 characters")
    @Pattern(regexp = "(?i)TODO|IN_PROGRESS|DONE", message = "Status must be one of: TODO, IN_PROGRESS, DONE")
    private String status;

    @NotBlank(message = "Priority cannot be blank")
    @Size(max = 20, message = "Priority must be at most 20 characters")
    @Pattern(regexp = "(?i)low|medium|high|urgent", message = "Priority must be one of: low, medium, high, urgent")
    private String priority;

    private LocalDate deadline;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }
}
