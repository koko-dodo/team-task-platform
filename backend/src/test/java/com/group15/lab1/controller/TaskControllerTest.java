package com.group15.lab1.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group15.lab1.controller.dto.AddTaskDependencyRequest;
import com.group15.lab1.controller.dto.TaskOperationLogResponse;
import com.group15.lab1.controller.dto.TaskResponse;
import com.group15.lab1.entity.TaskOperationScopeType;
import com.group15.lab1.entity.TaskOperationType;
import com.group15.lab1.exception.ConflictException;
import com.group15.lab1.exception.ForbiddenOperationException;
import com.group15.lab1.exception.TaskNotFoundException;
import com.group15.lab1.security.AuthConstants;
import com.group15.lab1.security.JwtTokenProvider;
import com.group15.lab1.service.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskController.class)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TaskService taskService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void getTaskById_ExistingTask_Returns200() throws Exception {
        Long taskId = 1L;
        Long userId = 1L;
        String token = "valid-token";

        mockValidAccessToken(token, userId);

        TaskResponse response = new TaskResponse();
        response.setId(taskId);
        response.setTitle("控制器测试任务");
        response.setStatus("TODO");
        response.setPriority("high");

        when(taskService.getTaskById(userId, taskId)).thenReturn(response);

        mockMvc.perform(get("/api/tasks/{id}", taskId)
                        .header("Authorization", AuthConstants.BEARER_PREFIX + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(taskId))
                .andExpect(jsonPath("$.title").value("控制器测试任务"))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.priority").value("high"));
    }

    @Test
    void getTaskById_NonExistentTask_Returns404() throws Exception {
        Long taskId = 999L;
        Long userId = 1L;
        String token = "valid-token";

        mockValidAccessToken(token, userId);
        when(taskService.getTaskById(userId, taskId))
                .thenThrow(new TaskNotFoundException("Task not found: " + taskId));

        mockMvc.perform(get("/api/tasks/{id}", taskId)
                        .header("Authorization", AuthConstants.BEARER_PREFIX + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Task not found: " + taskId));
    }

    @Test
    void getTaskById_InvalidToken_Returns401() throws Exception {
        Long taskId = 1L;
        String token = "invalid-token";

        when(jwtTokenProvider.validateToken(token)).thenReturn(false);

        mockMvc.perform(get("/api/tasks/{id}", taskId)
                        .header("Authorization", AuthConstants.BEARER_PREFIX + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("令牌无效或已过期"));

        verifyNoInteractions(taskService);
    }

    @Test
    void getTaskById_MissingToken_Returns401() throws Exception {
        mockMvc.perform(get("/api/tasks/{id}", 1L))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("缺少认证令牌"));

        verifyNoInteractions(taskService);
    }

    // ========================================
    // API端点接口测试：查看任务日志接口
    // ========================================

    @Test
    void getTaskLogs_Returns200_WithMultipleLogs() throws Exception {
        Long taskId = 1L;
        Long userId = 1L;
        String token = "valid-token";
        mockValidAccessToken(token, userId);

        TaskOperationLogResponse log1 = new TaskOperationLogResponse();
        log1.setId(10L);
        log1.setTaskId(taskId);
        log1.setActorId(userId);
        log1.setActorName("testUser");
        log1.setOperationType(TaskOperationType.CREATE_TASK);
        log1.setScopeType(TaskOperationScopeType.PERSONAL);
        log1.setSummary("创建任务：测试任务");
        log1.setCreatedAt(LocalDateTime.of(2026, 6, 5, 12, 0));

        TaskOperationLogResponse log2 = new TaskOperationLogResponse();
        log2.setId(11L);
        log2.setTaskId(taskId);
        log2.setActorId(userId);
        log2.setActorName("testUser");
        log2.setOperationType(TaskOperationType.UPDATE_TASK_STATUS);
        log2.setScopeType(TaskOperationScopeType.PERSONAL);
        log2.setSummary("将任务状态从 TODO 修改为 IN_PROGRESS");
        log2.setOldValue("TODO");
        log2.setNewValue("IN_PROGRESS");
        log2.setCreatedAt(LocalDateTime.of(2026, 6, 5, 14, 0));

        when(taskService.getPersonalTaskLogs(userId, taskId)).thenReturn(List.of(log2, log1));

        mockMvc.perform(get("/api/tasks/{id}/logs", taskId)
                        .header("Authorization", AuthConstants.BEARER_PREFIX + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(11L))
                .andExpect(jsonPath("$[0].operationType").value("UPDATE_TASK_STATUS"))
                .andExpect(jsonPath("$[0].oldValue").value("TODO"))
                .andExpect(jsonPath("$[0].newValue").value("IN_PROGRESS"))
                .andExpect(jsonPath("$[1].id").value(10L))
                .andExpect(jsonPath("$[1].operationType").value("CREATE_TASK"));
    }

    // ========================================
    // 无权限访问日志的异常场景
    // ========================================

    @Test
    void getTaskLogs_NoPermission_Throws403() throws Exception {
        Long taskId = 1L;
        Long userId = 1L;
        String token = "valid-token";
        mockValidAccessToken(token, userId);

        when(taskService.getPersonalTaskLogs(userId, taskId))
                .thenThrow(new ForbiddenOperationException("无权访问该任务日志"));

        mockMvc.perform(get("/api/tasks/{id}/logs", taskId)
                        .header("Authorization", AuthConstants.BEARER_PREFIX + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("无权访问该任务日志"));
    }

    @Test
    void getTaskLogs_TaskNotFound_Throws404() throws Exception {
        Long taskId = 999L;
        Long userId = 1L;
        String token = "valid-token";
        mockValidAccessToken(token, userId);

        when(taskService.getPersonalTaskLogs(userId, taskId))
                .thenThrow(new TaskNotFoundException("Task not found: " + taskId));

        mockMvc.perform(get("/api/tasks/{id}/logs", taskId)
                        .header("Authorization", AuthConstants.BEARER_PREFIX + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Task not found: " + taskId));
    }

    @Test
    void getTaskLogs_InvalidToken_Throws401() throws Exception {
        Long taskId = 1L;
        String token = "invalid-token";

        when(jwtTokenProvider.validateToken(token)).thenReturn(false);

        mockMvc.perform(get("/api/tasks/{id}/logs", taskId)
                        .header("Authorization", AuthConstants.BEARER_PREFIX + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("令牌无效或已过期"));

        verifyNoInteractions(taskService);
    }

    @Test
    void getTaskPredecessors_Returns200() throws Exception {
        Long taskId = 1L;
        Long userId = 1L;
        String token = "valid-token";

        mockValidAccessToken(token, userId);

        TaskResponse predecessor = new TaskResponse();
        predecessor.setId(2L);
        predecessor.setTitle("前置任务");
        predecessor.setStatus("DONE");
        predecessor.setPriority("medium");

        when(taskService.getTaskPredecessors(userId, taskId)).thenReturn(List.of(predecessor));

        mockMvc.perform(get("/api/tasks/{id}/dependencies/predecessors", taskId)
                        .header("Authorization", AuthConstants.BEARER_PREFIX + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2L))
                .andExpect(jsonPath("$[0].title").value("前置任务"));
    }

    @Test
    void addTaskDependency_Returns201() throws Exception {
        Long taskId = 1L;
        Long userId = 1L;
        String token = "valid-token";

        mockValidAccessToken(token, userId);

        AddTaskDependencyRequest request = new AddTaskDependencyRequest();
        request.setPredecessorTaskId(2L);

        mockMvc.perform(post("/api/tasks/{id}/dependencies", taskId)
                        .header("Authorization", AuthConstants.BEARER_PREFIX + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void removeTaskDependency_Returns204() throws Exception {
        Long taskId = 1L;
        Long predecessorTaskId = 2L;
        Long userId = 1L;
        String token = "valid-token";

        mockValidAccessToken(token, userId);

        mockMvc.perform(delete("/api/tasks/{id}/dependencies/{predecessorTaskId}", taskId, predecessorTaskId)
                        .header("Authorization", AuthConstants.BEARER_PREFIX + token))
                .andExpect(status().isNoContent());

        verify(taskService).removeTaskDependency(userId, taskId, predecessorTaskId);
    }

    @Test
    void addTaskDependency_Cycle_Returns409() throws Exception {
        Long taskId = 1L;
        Long predecessorTaskId = 2L;
        Long userId = 1L;
        String token = "valid-token";

        mockValidAccessToken(token, userId);
        doThrow(new ConflictException("新增依赖会形成循环依赖"))
                .when(taskService).addTaskDependency(userId, taskId, predecessorTaskId);

        AddTaskDependencyRequest request = new AddTaskDependencyRequest();
        request.setPredecessorTaskId(predecessorTaskId);

        mockMvc.perform(post("/api/tasks/{id}/dependencies", taskId)
                        .header("Authorization", AuthConstants.BEARER_PREFIX + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("新增依赖会形成循环依赖"));
    }

    @Test
    void addTaskDependency_Duplicate_Returns409() throws Exception {
        Long taskId = 1L;
        Long predecessorTaskId = 2L;
        Long userId = 1L;
        String token = "valid-token";

        mockValidAccessToken(token, userId);
        doThrow(new ConflictException("依赖关系已存在"))
                .when(taskService).addTaskDependency(userId, taskId, predecessorTaskId);

        AddTaskDependencyRequest request = new AddTaskDependencyRequest();
        request.setPredecessorTaskId(predecessorTaskId);

        mockMvc.perform(post("/api/tasks/{id}/dependencies", taskId)
                        .header("Authorization", AuthConstants.BEARER_PREFIX + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("依赖关系已存在"));
    }

    @Test
    void deleteTask_WithDependents_Returns409() throws Exception {
        Long taskId = 1L;
        Long userId = 1L;
        String token = "valid-token";

        mockValidAccessToken(token, userId);
        doThrow(new ConflictException("任务仍被后继任务依赖，无法删除"))
                .when(taskService).deleteTask(userId, taskId);

        mockMvc.perform(delete("/api/tasks/{id}", taskId)
                        .header("Authorization", AuthConstants.BEARER_PREFIX + token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("任务仍被后继任务依赖，无法删除"));
    }

    private void mockValidAccessToken(String token, Long userId) {
        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.isTokenExpired(token)).thenReturn(false);
        when(jwtTokenProvider.getTokenType(token)).thenReturn(AuthConstants.TOKEN_TYPE_ACCESS);
        when(jwtTokenProvider.getUserIdFromToken(token)).thenReturn(userId);
    }
}