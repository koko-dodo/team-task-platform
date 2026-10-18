package com.group15.lab1.controller.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 新增任务依赖请求 DTO。
 */
public class AddTaskDependencyRequest {

    @NotNull(message = "Predecessor task id cannot be null")
    private Long predecessorTaskId;

    public Long getPredecessorTaskId() {
        return predecessorTaskId;
    }

    public void setPredecessorTaskId(Long predecessorTaskId) {
        this.predecessorTaskId = predecessorTaskId;
    }
}
