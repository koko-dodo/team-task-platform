package com.group15.lab1.service.impl;

import com.group15.lab1.entity.Task;
import com.group15.lab1.entity.TaskOperationLog;
import com.group15.lab1.entity.TaskOperationScopeType;
import com.group15.lab1.entity.TaskOperationType;
import com.group15.lab1.entity.Team;
import com.group15.lab1.entity.User;
import com.group15.lab1.repository.TaskOperationLogRepository;
import com.group15.lab1.service.TaskOperationLogEvent;
import com.group15.lab1.service.TaskOperationLogService;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskOperationLogServiceImpl implements TaskOperationLogService {

    private final TaskOperationLogRepository taskOperationLogRepository;
    private final ApplicationEventPublisher eventPublisher;

    public TaskOperationLogServiceImpl(
            TaskOperationLogRepository taskOperationLogRepository,
            ApplicationEventPublisher eventPublisher) {
        this.taskOperationLogRepository = taskOperationLogRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void recordTaskLog(
            @NotNull Task task,
            @NotNull User actor,
            @NotNull TaskOperationType operationType,
            @NotNull String summary,
            String oldValue,
            String newValue) {
        Team team = task.getTeam();
        eventPublisher.publishEvent(new TaskOperationLogEvent(
                task.getId(),
                actor.getId(),
                actor.getUsername(),
                operationType,
                resolveScopeType(team),
                team == null ? null : team.getId(),
                summary,
                oldValue,
                newValue));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskOperationLog> getTaskLogs(@NotNull Long taskId) {
        return taskOperationLogRepository.findByTaskIdOrderByCreatedAtDescIdDesc(taskId);
    }

    private TaskOperationScopeType resolveScopeType(Team team) {
        return team == null ? TaskOperationScopeType.PERSONAL : TaskOperationScopeType.TEAM;
    }
}
