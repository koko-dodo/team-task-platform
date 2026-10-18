package com.group15.lab1.service;

import com.group15.lab1.entity.TaskOperationScopeType;
import com.group15.lab1.entity.TaskOperationType;

public record TaskOperationLogEvent(
        Long taskId,
        Long actorId,
        String actorName,
        TaskOperationType operationType,
        TaskOperationScopeType scopeType,
        Long teamId,
        String summary,
        String oldValue,
        String newValue) {
}
