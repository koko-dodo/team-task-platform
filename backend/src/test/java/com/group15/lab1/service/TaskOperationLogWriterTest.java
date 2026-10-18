package com.group15.lab1.service;

import com.group15.lab1.entity.TaskOperationLog;
import com.group15.lab1.entity.TaskOperationScopeType;
import com.group15.lab1.entity.TaskOperationType;
import com.group15.lab1.entity.User;
import com.group15.lab1.repository.TaskOperationLogRepository;
import com.group15.lab1.repository.UserRepository;
import com.group15.lab1.service.impl.TaskOperationLogWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskOperationLogWriterTest {

    @Mock
    private TaskOperationLogRepository taskOperationLogRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TaskOperationLogWriter writer;

    @Test
    void write_PersistsCompleteEventSnapshot() {
        User actor = new User();
        actor.setId(1L);
        when(userRepository.getReferenceById(1L)).thenReturn(actor);
        TaskOperationLogEvent event = createEvent();

        writer.write(event);

        ArgumentCaptor<TaskOperationLog> captor = ArgumentCaptor.forClass(TaskOperationLog.class);
        verify(taskOperationLogRepository).saveAndFlush(captor.capture());
        TaskOperationLog operationLog = captor.getValue();
        assertEquals(3L, operationLog.getTaskId());
        assertEquals(actor, operationLog.getActor());
        assertEquals(TaskOperationType.UPDATE_TASK_STATUS, operationLog.getOperationType());
        assertEquals("TODO", operationLog.getOldValue());
        assertEquals("DONE", operationLog.getNewValue());
    }

    @Test
    void write_UsesIndependentTransaction() throws Exception {
        Transactional transactional = TaskOperationLogWriter.class
                .getMethod("write", TaskOperationLogEvent.class)
                .getAnnotation(Transactional.class);

        assertEquals(Propagation.REQUIRES_NEW, transactional.propagation());
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
