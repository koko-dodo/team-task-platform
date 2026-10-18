package com.group15.lab1.service;

import com.group15.lab1.controller.dto.CreateTaskRequest;
import com.group15.lab1.controller.dto.TaskResponse;
import com.group15.lab1.controller.dto.TaskOperationLogResponse;
import com.group15.lab1.controller.dto.UpdateTaskRequest;
import com.group15.lab1.controller.dto.UpdateTaskStatusRequest;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.lang.Nullable;

public interface TaskService {

    TaskResponse createTask(@NotNull Long userId, @NotNull CreateTaskRequest request);

    List<TaskResponse> getTasksByUserId(@NotNull Long userId);

    List<TaskResponse> getMyTasks(@NotNull Long userId);

    TaskResponse getTaskById(@NotNull Long userId, @NotNull Long taskId);

    List<TaskOperationLogResponse> getPersonalTaskLogs(@NotNull Long userId, @NotNull Long taskId);

    List<TaskResponse> getTaskPredecessors(@NotNull Long userId, @NotNull Long taskId);

    List<TaskResponse> getTaskSuccessors(@NotNull Long userId, @NotNull Long taskId);

    void addTaskDependency(@NotNull Long userId, @NotNull Long taskId, @NotNull Long predecessorTaskId);

    void removeTaskDependency(@NotNull Long userId, @NotNull Long taskId, @NotNull Long predecessorTaskId);

    TaskResponse updateTask(@NotNull Long userId, @NotNull Long taskId, @NotNull UpdateTaskRequest request);

    void deleteTask(@NotNull Long userId, @NotNull Long taskId);

    List<TaskResponse> getTeamTasks(@NotNull Long userId, @NotNull Long teamId);

    TaskResponse getTeamTask(@NotNull Long userId, @NotNull Long teamId, @NotNull Long taskId);

    List<TaskOperationLogResponse> getTeamTaskLogs(@NotNull Long userId, @NotNull Long teamId, @NotNull Long taskId);

    List<TaskResponse> getTeamTaskPredecessors(@NotNull Long userId, @NotNull Long teamId, @NotNull Long taskId);

    List<TaskResponse> getTeamTaskSuccessors(@NotNull Long userId, @NotNull Long teamId, @NotNull Long taskId);

    void addTeamTaskDependency(@NotNull Long userId, @NotNull Long teamId, @NotNull Long taskId,
            @NotNull Long predecessorTaskId);

    void removeTeamTaskDependency(@NotNull Long userId, @NotNull Long teamId, @NotNull Long taskId,
            @NotNull Long predecessorTaskId);

    TaskResponse createTeamTask(@NotNull Long userId, @NotNull Long teamId, @NotNull CreateTaskRequest request);

    TaskResponse updateTeamTask(@NotNull Long userId, @NotNull Long teamId, @NotNull Long taskId,
            @NotNull UpdateTaskRequest request);

    TaskResponse updateTeamTaskStatus(@NotNull Long userId, @NotNull Long teamId, @NotNull Long taskId,
            @NotNull UpdateTaskStatusRequest request);

    void deleteTeamTask(@NotNull Long userId, @NotNull Long teamId, @NotNull Long taskId);

    TaskResponse assignTeamTask(@NotNull Long userId, @NotNull Long teamId, @NotNull Long taskId,
            @Nullable Long assigneeId);

    void clearAssignmentsForDepartedMember(@NotNull Long teamId, @NotNull Long memberId, @NotNull Long actorId);

    void recordTeamDisbandedTaskChanges(@NotNull Long teamId, @NotNull Long actorId);
}
