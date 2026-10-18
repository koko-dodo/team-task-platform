package com.group15.lab1.entity;

/**
 * 任务操作日志类型。
 */
public enum TaskOperationType {
    CREATE_TASK,
    UPDATE_TASK_INFO,
    UPDATE_TASK_STATUS,
    UPDATE_ASSIGNEE,
    ADD_DEPENDENCY,
    REMOVE_DEPENDENCY,
    DELETE_TASK,
    MEMBER_LEAVE_TASK_CHANGE,
    TEAM_DISBANDED_TASK_CHANGE
}
