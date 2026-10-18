package com.group15.lab1.service.impl;

import com.group15.lab1.entity.TaskOperationLog;
import com.group15.lab1.repository.TaskOperationLogRepository;
import com.group15.lab1.repository.UserRepository;
import com.group15.lab1.service.TaskOperationLogEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskOperationLogWriter {

    private final TaskOperationLogRepository taskOperationLogRepository;
    private final UserRepository userRepository;

    public TaskOperationLogWriter(
            TaskOperationLogRepository taskOperationLogRepository,
            UserRepository userRepository) {
        this.taskOperationLogRepository = taskOperationLogRepository;
        this.userRepository = userRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void write(TaskOperationLogEvent event) {
        TaskOperationLog operationLog = new TaskOperationLog();
        operationLog.setTaskId(event.taskId());
        operationLog.setActor(userRepository.getReferenceById(event.actorId()));
        operationLog.setActorName(event.actorName());
        operationLog.setOperationType(event.operationType());
        operationLog.setScopeType(event.scopeType());
        operationLog.setTeamId(event.teamId());
        operationLog.setSummary(event.summary());
        operationLog.setOldValue(event.oldValue());
        operationLog.setNewValue(event.newValue());
        taskOperationLogRepository.saveAndFlush(operationLog);
    }
}
