package com.group15.lab1.service;

import com.group15.lab1.entity.Task;
import com.group15.lab1.entity.TaskOperationLog;
import com.group15.lab1.entity.TaskOperationType;
import com.group15.lab1.entity.User;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public interface TaskOperationLogService {

    /**
     * 记录一条任务操作日志。
     * 采用弱一致策略：日志写入失败不影响主业务提交。
     */
    void recordTaskLog(
            @NotNull Task task,
            @NotNull User actor,
            @NotNull TaskOperationType operationType,
            @NotNull String summary,
            String oldValue,
            String newValue);

    List<TaskOperationLog> getTaskLogs(@NotNull Long taskId);
}
