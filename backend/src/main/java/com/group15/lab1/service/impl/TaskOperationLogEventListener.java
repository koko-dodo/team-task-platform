package com.group15.lab1.service.impl;

import com.group15.lab1.service.TaskOperationLogEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class TaskOperationLogEventListener {

    private static final Logger log = LoggerFactory.getLogger(TaskOperationLogEventListener.class);

    private final TaskOperationLogWriter taskOperationLogWriter;

    public TaskOperationLogEventListener(TaskOperationLogWriter taskOperationLogWriter) {
        this.taskOperationLogWriter = taskOperationLogWriter;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleTaskOperationLog(TaskOperationLogEvent event) {
        try {
            taskOperationLogWriter.write(event);
        } catch (Exception exception) {
            log.warn("Failed to record task operation log for taskId={}", event.taskId(), exception);
        }
    }
}
