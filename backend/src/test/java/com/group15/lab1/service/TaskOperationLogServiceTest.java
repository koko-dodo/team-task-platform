package com.group15.lab1.service;

import com.group15.lab1.entity.Task;
import com.group15.lab1.entity.TaskOperationLog;
import com.group15.lab1.entity.TaskOperationScopeType;
import com.group15.lab1.entity.TaskOperationType;
import com.group15.lab1.entity.Team;
import com.group15.lab1.entity.User;
import com.group15.lab1.repository.TaskOperationLogRepository;
import com.group15.lab1.service.impl.TaskOperationLogServiceImpl;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskOperationLogServiceTest {

    @Mock
    private TaskOperationLogRepository taskOperationLogRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private TaskOperationLogServiceImpl taskOperationLogService;

    @Test
    void recordTaskLog_PublishesImmutableTeamLogEvent() {
        User actor = new User();
        actor.setId(1L);
        actor.setUsername("owner");
        Team team = new Team();
        team.setId(2L);
        Task task = new Task();
        task.setId(3L);
        task.setTeam(team);

        taskOperationLogService.recordTaskLog(
                task, actor, TaskOperationType.UPDATE_TASK_STATUS, "修改状态", "TODO", "DONE");

        ArgumentCaptor<TaskOperationLogEvent> captor = ArgumentCaptor.forClass(TaskOperationLogEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        TaskOperationLogEvent event = captor.getValue();
        assertEquals(3L, event.taskId());
        assertEquals(1L, event.actorId());
        assertEquals(TaskOperationScopeType.TEAM, event.scopeType());
        assertEquals(2L, event.teamId());
    }

    @Test
    void getTaskLogs_DelegatesToDescendingRepositoryQuery() {
        TaskOperationLog operationLog = new TaskOperationLog();
        when(taskOperationLogRepository.findByTaskIdOrderByCreatedAtDescIdDesc(3L)).thenReturn(List.of(operationLog));

        assertEquals(List.of(operationLog), taskOperationLogService.getTaskLogs(3L));
    }
}
