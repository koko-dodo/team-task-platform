package com.group15.lab1.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group15.lab1.controller.dto.AddTaskDependencyRequest;
import com.group15.lab1.controller.dto.TaskOperationLogResponse;
import com.group15.lab1.controller.dto.TaskResponse;
import com.group15.lab1.entity.TaskOperationScopeType;
import com.group15.lab1.entity.TaskOperationType;
import com.group15.lab1.exception.ForbiddenOperationException;
import com.group15.lab1.security.AuthConstants;
import com.group15.lab1.security.JwtTokenProvider;
import com.group15.lab1.service.TaskService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TeamTaskController.class)
class TeamTaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TaskService taskService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void getTeamTaskPredecessors_Returns200() throws Exception {
        Long teamId = 1L;
        Long taskId = 10L;
        Long userId = 100L;
        String token = "valid-token";

        mockValidAccessToken(token, userId);

        TaskResponse predecessor = new TaskResponse();
        predecessor.setId(9L);
        predecessor.setTitle("团队前置任务");
        predecessor.setStatus("DONE");
        predecessor.setPriority("high");

        when(taskService.getTeamTaskPredecessors(userId, teamId, taskId))
                .thenReturn(List.of(predecessor));

        mockMvc.perform(get("/api/teams/{teamId}/tasks/{taskId}/dependencies/predecessors", teamId, taskId)
                        .header("Authorization", AuthConstants.BEARER_PREFIX + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(9L))
                .andExpect(jsonPath("$[0].title").value("团队前置任务"));
    }

    @Test
    void getTeamTaskSuccessors_Returns200() throws Exception {
        Long teamId = 1L;
        Long taskId = 10L;
        Long userId = 100L;
        String token = "valid-token";

        mockValidAccessToken(token, userId);

        TaskResponse successor = new TaskResponse();
        successor.setId(11L);
        successor.setTitle("团队后继任务");
        successor.setStatus("TODO");
        successor.setPriority("medium");

        when(taskService.getTeamTaskSuccessors(userId, teamId, taskId))
                .thenReturn(List.of(successor));

        mockMvc.perform(get("/api/teams/{teamId}/tasks/{taskId}/dependencies/successors", teamId, taskId)
                        .header("Authorization", AuthConstants.BEARER_PREFIX + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(11L))
                .andExpect(jsonPath("$[0].title").value("团队后继任务"));
    }

    @Test
    void addTeamTaskDependency_Returns201() throws Exception {
        Long teamId = 1L;
        Long taskId = 10L;
        Long userId = 100L;
        String token = "valid-token";

        mockValidAccessToken(token, userId);

        AddTaskDependencyRequest request = new AddTaskDependencyRequest();
        request.setPredecessorTaskId(9L);

        mockMvc.perform(post("/api/teams/{teamId}/tasks/{taskId}/dependencies", teamId, taskId)
                        .header("Authorization", AuthConstants.BEARER_PREFIX + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(taskService).addTeamTaskDependency(userId, teamId, taskId, 9L);
    }

    @Test
    void removeTeamTaskDependency_Returns204() throws Exception {
        Long teamId = 1L;
        Long taskId = 10L;
        Long predecessorTaskId = 9L;
        Long userId = 100L;
        String token = "valid-token";

        mockValidAccessToken(token, userId);

        mockMvc.perform(delete("/api/teams/{teamId}/tasks/{taskId}/dependencies/{predecessorTaskId}",
                        teamId, taskId, predecessorTaskId)
                        .header("Authorization", AuthConstants.BEARER_PREFIX + token))
                .andExpect(status().isNoContent());

        verify(taskService).removeTeamTaskDependency(userId, teamId, taskId, predecessorTaskId);
    }

    @Test
    void getTeamTasks_DisbandedTeam_Member_Returns403() throws Exception {
        Long teamId = 1L;
        Long userId = 100L;
        String token = "valid-token";

        mockValidAccessToken(token, userId);
        doThrow(new ForbiddenOperationException("团队已解散，普通成员不能访问团队任务"))
                .when(taskService).getTeamTasks(userId, teamId);

        mockMvc.perform(get("/api/teams/{teamId}/tasks", teamId)
                        .header("Authorization", AuthConstants.BEARER_PREFIX + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("团队已解散，普通成员不能访问团队任务"));
    }

    @Test
    void getTeamTaskLogs_Returns200() throws Exception {
        Long teamId = 1L;
        Long taskId = 10L;
        Long userId = 100L;
        String token = "valid-token";
        mockValidAccessToken(token, userId);

        TaskOperationLogResponse log = new TaskOperationLogResponse();
        log.setId(20L);
        log.setTaskId(taskId);
        log.setActorId(userId);
        log.setActorName("owner");
        log.setOperationType(TaskOperationType.UPDATE_ASSIGNEE);
        log.setScopeType(TaskOperationScopeType.TEAM);
        log.setTeamId(teamId);
        log.setSummary("将任务负责人从 未分配 修改为 member");
        when(taskService.getTeamTaskLogs(userId, teamId, taskId)).thenReturn(List.of(log));

        mockMvc.perform(get("/api/teams/{teamId}/tasks/{taskId}/logs", teamId, taskId)
                        .header("Authorization", AuthConstants.BEARER_PREFIX + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].scopeType").value("TEAM"))
                .andExpect(jsonPath("$[0].teamId").value(teamId))
                .andExpect(jsonPath("$[0].operationType").value("UPDATE_ASSIGNEE"));
    }

    // ========================================
    // 无权限访问日志的异常场景
    // ========================================

    @Test
    void getTeamTaskLogs_NonMember_Returns403() throws Exception {
        Long teamId = 1L;
        Long taskId = 10L;
        Long userId = 999L;
        String token = "valid-token";
        mockValidAccessToken(token, userId);
        doThrow(new ForbiddenOperationException("仅团队成员可访问团队任务"))
                .when(taskService).getTeamTaskLogs(userId, teamId, taskId);

        mockMvc.perform(get("/api/teams/{teamId}/tasks/{taskId}/logs", teamId, taskId)
                        .header("Authorization", AuthConstants.BEARER_PREFIX + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("仅团队成员可访问团队任务"));
    }

    @Test
    void getTeamTaskLogs_InvalidToken_Returns401() throws Exception {
        Long teamId = 1L;
        Long taskId = 10L;
        String token = "invalid-token";

        when(jwtTokenProvider.validateToken(token)).thenReturn(false);

        mockMvc.perform(get("/api/teams/{teamId}/tasks/{taskId}/logs", teamId, taskId)
                        .header("Authorization", AuthConstants.BEARER_PREFIX + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("令牌无效或已过期"));

        verifyNoInteractions(taskService);
    }

    // ========================================
    // Lab3回归测试：团队任务状态变更日志
    // ========================================

    @Test
    void getTeamTaskLogs_DeletedTaskMemberCanViewLogs() throws Exception {
        Long teamId = 1L;
        Long taskId = 10L;
        Long userId = 100L;
        String token = "valid-token";
        mockValidAccessToken(token, userId);

        TaskOperationLogResponse log = new TaskOperationLogResponse();
        log.setId(20L);
        log.setTaskId(taskId);
        log.setActorId(1L);
        log.setActorName("owner");
        log.setOperationType(TaskOperationType.DELETE_TASK);
        log.setScopeType(TaskOperationScopeType.TEAM);
        log.setTeamId(teamId);
        log.setSummary("删除任务：已删除的任务");
        
        when(taskService.getTeamTaskLogs(userId, teamId, taskId)).thenReturn(List.of(log));

        mockMvc.perform(get("/api/teams/{teamId}/tasks/{taskId}/logs", teamId, taskId)
                        .header("Authorization", AuthConstants.BEARER_PREFIX + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].operationType").value("DELETE_TASK"))
                .andExpect(jsonPath("$[0].scopeType").value("TEAM"));
    }

    private void mockValidAccessToken(String token, Long userId) {
        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.isTokenExpired(token)).thenReturn(false);
        when(jwtTokenProvider.getTokenType(token)).thenReturn(AuthConstants.TOKEN_TYPE_ACCESS);
        when(jwtTokenProvider.getUserIdFromToken(token)).thenReturn(userId);
    }
}