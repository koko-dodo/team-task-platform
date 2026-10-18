package com.group15.lab1.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 更新任务状态请求 DTO。
 */
public class UpdateTaskStatusRequest {

    @NotBlank(message = "Status cannot be blank")
    @Size(max = 20, message = "Status must be at most 20 characters")
    @Pattern(
            regexp = "(?i)TODO|IN_PROGRESS|DONE",
            message = "Status must be one of: TODO, IN_PROGRESS, DONE")
    private String status;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
