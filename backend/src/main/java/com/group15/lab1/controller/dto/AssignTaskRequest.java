package com.group15.lab1.controller.dto;

/**
 * 分配团队任务负责人的请求 DTO。
 */
public class AssignTaskRequest {

    private Long assigneeId;

    public AssignTaskRequest() {
    }

    public AssignTaskRequest(Long assigneeId) {
        this.assigneeId = assigneeId;
    }

    public Long getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(Long assigneeId) {
        this.assigneeId = assigneeId;
    }
}
