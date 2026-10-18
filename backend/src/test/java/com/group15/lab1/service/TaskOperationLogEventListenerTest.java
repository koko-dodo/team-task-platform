package com.group15.lab1.service;

import com.group15.lab1.entity.TaskOperationScopeType;
import com.group15.lab1.entity.TaskOperationType;
import com.group15.lab1.service.impl.TaskOperationLogEventListener;
import com.group15.lab1.service.impl.TaskOperationLogWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TaskOperationLogEventListenerTest {

    @Mock
    private TaskOperationLogWriter taskOperationLogWriter;

    @InjectMocks
    private TaskOperationLogEventListener listener;

    @Test
    void handleTaskOperationLog_DelegatesToIndependentWriter() {
        TaskOperationLogEvent event = createEvent();

        listener.handleTaskOperationLog(event);

        verify(taskOperationLogWriter).write(event);
    }

    @Test
    void handleTaskOperationLog_WhenPersistenceFails_DoesNotPropagate() {
        doThrow(new RuntimeException("database unavailable"))
                .when(taskOperationLogWriter).write(any(TaskOperationLogEvent.class));

        assertDoesNotThrow(() -> listener.handleTaskOperationLog(createEvent()));
    }

    private TaskOperationLogEvent createEvent() {
        return new TaskOperationLogEvent(
                3L,
                1L,
                "owner",
                TaskOperationType.UPDATE_TASK_STATUS,
                TaskOperationScopeType.TEAM,
                2L,
                "修改状态",
                "TODO",
                "DONE");
    }
}
