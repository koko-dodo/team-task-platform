package com.group15.lab1.controller;

import com.group15.lab1.controller.dto.AddTaskDependencyRequest;
import com.group15.lab1.controller.dto.CreateTaskRequest;
import com.group15.lab1.controller.dto.TaskResponse;
import com.group15.lab1.controller.dto.TaskOperationLogResponse;
import com.group15.lab1.controller.dto.UpdateTaskRequest;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;
    private final JwtTokenProvider jwtTokenProvider;

    public TaskController(TaskService taskService, JwtTokenProvider jwtTokenProvider) {
        this.taskService = taskService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * 创建当前登录用户的任务。
     *
     * @param authorization Authorization 请求头
     * @param request       任务创建参数
     * @return 创建后的任务
     */
    @PostMapping
    public ResponseEntity<TaskResponse> createTask(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @Valid @RequestBody CreateTaskRequest request) {
        Long userId = extractUserIdFromAuthorizationHeader(authorization);
        TaskResponse response = taskService.createTask(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 查询当前登录用户的任务列表。
     *
     * @param authorization Authorization 请求头
     * @return 当前用户任务列表
     */
    @GetMapping
    public ResponseEntity<List<TaskResponse>> getTasksByUserId(
            @RequestHeader(name = "Authorization", required = false) String authorization) {
        Long userId = extractUserIdFromAuthorizationHeader(authorization);
        return ResponseEntity.ok(taskService.getTasksByUserId(userId));
    }

    /**
     * 查询“我的任务”列表。
     * 包括个人任务和分配给当前用户的团队任务。
     *
     * @param authorization Authorization 请求头
     * @return 我的任务列表
     */
    @GetMapping("/my")
    public ResponseEntity<List<TaskResponse>> getMyTasks(
            @RequestHeader(name = "Authorization", required = false) String authorization) {
        Long userId = extractUserIdFromAuthorizationHeader(authorization);
        return ResponseEntity.ok(taskService.getMyTasks(userId));
    }

    /**
     * 查询当前登录用户的单个任务详情。
     *
     * @param authorization Authorization 请求头
     * @param id            任务 ID
     * @return 任务详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> getTaskById(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable Long id) {
        Long userId = extractUserIdFromAuthorizationHeader(authorization);
        return ResponseEntity.ok(taskService.getTaskById(userId, id));
    }

    @GetMapping("/{id}/logs")
    public ResponseEntity<List<TaskOperationLogResponse>> getTaskLogs(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable Long id) {
        Long userId = extractUserIdFromAuthorizationHeader(authorization);
        return ResponseEntity.ok(taskService.getPersonalTaskLogs(userId, id));
    }

    @GetMapping("/{id}/dependencies/predecessors")
    public ResponseEntity<List<TaskResponse>> getTaskPredecessors(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable Long id) {
        Long userId = extractUserIdFromAuthorizationHeader(authorization);
        return ResponseEntity.ok(taskService.getTaskPredecessors(userId, id));
    }

    @GetMapping("/{id}/dependencies/successors")
    public ResponseEntity<List<TaskResponse>> getTaskSuccessors(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable Long id) {
        Long userId = extractUserIdFromAuthorizationHeader(authorization);
        return ResponseEntity.ok(taskService.getTaskSuccessors(userId, id));
    }

    @PostMapping("/{id}/dependencies")
    public ResponseEntity<Void> addTaskDependency(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable Long id,
            @Valid @RequestBody AddTaskDependencyRequest request) {
        Long userId = extractUserIdFromAuthorizationHeader(authorization);
        taskService.addTaskDependency(userId, id, request.getPredecessorTaskId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{id}/dependencies/{predecessorTaskId}")
    public ResponseEntity<Void> removeTaskDependency(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable Long id,
            @PathVariable Long predecessorTaskId) {
        Long userId = extractUserIdFromAuthorizationHeader(authorization);
        taskService.removeTaskDependency(userId, id, predecessorTaskId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 更新当前登录用户的任务。
     *
     * @param authorization Authorization 请求头
     * @param id            任务 ID
     * @param request       任务更新参数
     * @return 更新后的任务
     */
    @PutMapping("/{id}")
    public ResponseEntity<TaskResponse> updateTask(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable Long id,
            @Valid @RequestBody UpdateTaskRequest request) {
        Long userId = extractUserIdFromAuthorizationHeader(authorization);
        return ResponseEntity.ok(taskService.updateTask(userId, id, request));
    }

    /**
     * 删除当前登录用户的任务。
     *
     * @param authorization Authorization 请求头
     * @param id            任务 ID
     * @return 无内容响应
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable Long id) {
        Long userId = extractUserIdFromAuthorizationHeader(authorization);
        taskService.deleteTask(userId, id);
        return ResponseEntity.noContent().build();
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
