package com.group15.lab1.controller;

import com.group15.lab1.controller.dto.AddTaskDependencyRequest;
import com.group15.lab1.controller.dto.AssignTaskRequest;
import com.group15.lab1.controller.dto.CreateTaskRequest;
import com.group15.lab1.controller.dto.TaskResponse;
import com.group15.lab1.controller.dto.TaskOperationLogResponse;
import com.group15.lab1.controller.dto.UpdateTaskRequest;
import com.group15.lab1.controller.dto.UpdateTaskStatusRequest;
import com.group15.lab1.exception.InvalidCredentialsException;
import com.group15.lab1.security.AuthConstants;
import com.group15.lab1.security.JwtTokenProvider;
import com.group15.lab1.service.TaskService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 团队任务控制器。
 */
@RestController
@RequestMapping("/api/teams/{teamId}/tasks")
public class TeamTaskController {

    private final TaskService taskService;
    private final JwtTokenProvider jwtTokenProvider;

    public TeamTaskController(TaskService taskService, JwtTokenProvider jwtTokenProvider) {
        this.taskService = taskService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * 获取某团队下的全部任务。
     */
    @GetMapping
    public ResponseEntity<List<TaskResponse>> getTeamTasks(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable Long teamId) {
        Long userId = extractUserIdFromAuthorizationHeader(authorization);
        return ResponseEntity.ok(taskService.getTeamTasks(userId, teamId));
    }

    /**
     * 获取某团队下的单个任务详情。
     */
    @GetMapping("/{taskId}")
    public ResponseEntity<TaskResponse> getTeamTask(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable Long teamId,
            @PathVariable Long taskId) {
        Long userId = extractUserIdFromAuthorizationHeader(authorization);
        return ResponseEntity.ok(taskService.getTeamTask(userId, teamId, taskId));
    }

    @GetMapping("/{taskId}/logs")
    public ResponseEntity<List<TaskOperationLogResponse>> getTeamTaskLogs(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable Long teamId,
            @PathVariable Long taskId) {
        Long userId = extractUserIdFromAuthorizationHeader(authorization);
        return ResponseEntity.ok(taskService.getTeamTaskLogs(userId, teamId, taskId));
    }

    @GetMapping("/{taskId}/dependencies/predecessors")
    public ResponseEntity<List<TaskResponse>> getTeamTaskPredecessors(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable Long teamId,
            @PathVariable Long taskId) {
        Long userId = extractUserIdFromAuthorizationHeader(authorization);
        return ResponseEntity.ok(taskService.getTeamTaskPredecessors(userId, teamId, taskId));
    }

    @GetMapping("/{taskId}/dependencies/successors")
    public ResponseEntity<List<TaskResponse>> getTeamTaskSuccessors(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable Long teamId,
            @PathVariable Long taskId) {
        Long userId = extractUserIdFromAuthorizationHeader(authorization);
        return ResponseEntity.ok(taskService.getTeamTaskSuccessors(userId, teamId, taskId));
    }

    @PostMapping("/{taskId}/dependencies")
    public ResponseEntity<Void> addTeamTaskDependency(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable Long teamId,
            @PathVariable Long taskId,
            @Valid @RequestBody AddTaskDependencyRequest request) {
        Long userId = extractUserIdFromAuthorizationHeader(authorization);
        taskService.addTeamTaskDependency(userId, teamId, taskId, request.getPredecessorTaskId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{taskId}/dependencies/{predecessorTaskId}")
    public ResponseEntity<Void> removeTeamTaskDependency(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable Long teamId,
            @PathVariable Long taskId,
            @PathVariable Long predecessorTaskId) {
        Long userId = extractUserIdFromAuthorizationHeader(authorization);
        taskService.removeTeamTaskDependency(userId, teamId, taskId, predecessorTaskId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 创建团队任务。
     */
    @PostMapping
    public ResponseEntity<TaskResponse> createTeamTask(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable Long teamId,
            @Valid @RequestBody CreateTaskRequest request) {
        Long userId = extractUserIdFromAuthorizationHeader(authorization);
        TaskResponse response = taskService.createTeamTask(userId, teamId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 修改团队任务。
     */
    @PutMapping("/{taskId}")
    public ResponseEntity<TaskResponse> updateTeamTask(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable Long teamId,
            @PathVariable Long taskId,
            @Valid @RequestBody UpdateTaskRequest request) {
        Long userId = extractUserIdFromAuthorizationHeader(authorization);
        return ResponseEntity.ok(taskService.updateTeamTask(userId, teamId, taskId, request));
    }

    /**
     * Member 更新自己负责的团队任务状态。
     */
    @PatchMapping("/{taskId}/status")
    public ResponseEntity<TaskResponse> updateTeamTaskStatus(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable Long teamId,
            @PathVariable Long taskId,
            @Valid @RequestBody UpdateTaskStatusRequest request) {
        Long userId = extractUserIdFromAuthorizationHeader(authorization);
        return ResponseEntity.ok(taskService.updateTeamTaskStatus(userId, teamId, taskId, request));
    }

    /**
     * 删除团队任务。
     */
    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> deleteTeamTask(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable Long teamId,
            @PathVariable Long taskId) {
        Long userId = extractUserIdFromAuthorizationHeader(authorization);
        taskService.deleteTeamTask(userId, teamId, taskId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 分配团队任务负责人。
     */
    @PutMapping("/{taskId}/assign")
    public ResponseEntity<TaskResponse> assignTeamTask(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable Long teamId,
            @PathVariable Long taskId,
            @RequestBody AssignTaskRequest request) {
        Long userId = extractUserIdFromAuthorizationHeader(authorization);
        Long assigneeId = request == null ? null : request.getAssigneeId();
        return ResponseEntity.ok(taskService.assignTeamTask(userId, teamId, taskId, assigneeId));
    }

    private Long extractUserIdFromAuthorizationHeader(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            throw new InvalidCredentialsException("缺少认证令牌");
        }

        String token = authorization;
        String prefix = AuthConstants.BEARER_PREFIX;
        if (authorization.startsWith(prefix)) {
            token = authorization.substring(prefix.length());
        }

        if (!jwtTokenProvider.validateToken(token) || jwtTokenProvider.isTokenExpired(token)) {
            throw new InvalidCredentialsException("令牌无效或已过期");
        }
        if (!AuthConstants.TOKEN_TYPE_ACCESS.equals(jwtTokenProvider.getTokenType(token))) {
            throw new InvalidCredentialsException("令牌类型错误");
        }

        return jwtTokenProvider.getUserIdFromToken(token);
    }
}
