package com.group15.lab1.service.impl;

import com.group15.lab1.controller.dto.CreateTaskRequest;
import com.group15.lab1.controller.dto.TaskOperationLogResponse;
import com.group15.lab1.controller.dto.TaskResponse;
import com.group15.lab1.controller.dto.UpdateTaskRequest;
import com.group15.lab1.controller.dto.UpdateTaskStatusRequest;
import com.group15.lab1.entity.Task;
import com.group15.lab1.entity.TaskDependency;
import com.group15.lab1.entity.TaskOperationLog;
import com.group15.lab1.entity.TaskOperationScopeType;
import com.group15.lab1.entity.TaskOperationType;
import com.group15.lab1.entity.TaskPriority;
import com.group15.lab1.entity.TaskStatus;
import com.group15.lab1.entity.Team;
import com.group15.lab1.entity.TeamMember;
import com.group15.lab1.entity.TeamRole;
import com.group15.lab1.entity.TeamStatus;
import com.group15.lab1.entity.User;
import com.group15.lab1.exception.ConflictException;
import com.group15.lab1.exception.ForbiddenOperationException;
import com.group15.lab1.exception.TaskNotFoundException;
import com.group15.lab1.exception.TeamNotFoundException;
import com.group15.lab1.repository.TaskDependencyRepository;
import com.group15.lab1.repository.TaskRepository;
import com.group15.lab1.repository.TeamRepository;
import com.group15.lab1.repository.TeamMemberRepository;
import com.group15.lab1.repository.UserRepository;
import com.group15.lab1.service.TaskOperationLogService;
import com.group15.lab1.service.TaskService;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.HashSet;
import jakarta.validation.constraints.NotNull;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.lang.Nullable;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final TaskDependencyRepository taskDependencyRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final TaskOperationLogService taskOperationLogService;

    public TaskServiceImpl(
            TaskRepository taskRepository,
            TaskDependencyRepository taskDependencyRepository,
            TeamRepository teamRepository,
            TeamMemberRepository teamMemberRepository,
            UserRepository userRepository,
            TaskOperationLogService taskOperationLogService) {
        this.taskRepository = taskRepository;
        this.taskDependencyRepository = taskDependencyRepository;
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.userRepository = userRepository;
        this.taskOperationLogService = taskOperationLogService;
    }

    @Override
    public TaskResponse createTask(@NotNull Long userId, @NotNull CreateTaskRequest request) {
        User user = getUserOrThrow(userId);

        Task task = new Task();
        task.setCreator(user);
        task.setTitle(normalizeTitle(request.getTitle()));
        task.setDescription(normalizeDescription(request.getDescription()));
        task.setStatus(TaskStatus.normalize(request.getStatus()));
        task.setPriority(TaskPriority.normalize(request.getPriority()));
        task.setDeadline(validateDeadlineForCreate(request.getDeadline()));
        Task savedTask = taskRepository.save(task);
        taskOperationLogService.recordTaskLog(
                savedTask,
                user,
                TaskOperationType.CREATE_TASK,
                "创建任务：" + savedTask.getTitle(),
                null,
                buildTaskSnapshot(savedTask));
        return toTaskResponse(savedTask);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksByUserId(@NotNull Long userId) {
        return taskRepository.findByCreator_IdAndTeamIsNullOrderByUpdatedAtDesc(userId)
                .stream()
                .map(this::toTaskResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskResponse> getMyTasks(@NotNull Long userId) {
        return taskRepository.findMyTasks(userId)
                .stream()
                .map(this::toTaskResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TaskResponse getTaskById(@NotNull Long userId, @NotNull Long taskId) {
        return toTaskResponse(getPersonalTaskOrThrow(userId, taskId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskOperationLogResponse> getPersonalTaskLogs(@NotNull Long userId, @NotNull Long taskId) {
        List<TaskOperationLog> logs = taskOperationLogService.getTaskLogs(taskId);
        if (!taskRepository.findByIdAndCreator_IdAndTeamIsNull(taskId, userId).isPresent()) {
            assertDeletedPersonalTaskLogReadable(userId, taskId, logs);
        }

        return logs.stream()
                .map(this::toTaskOperationLogResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskResponse> getTaskPredecessors(@NotNull Long userId, @NotNull Long taskId) {
        Task task = getPersonalTaskOrThrow(userId, taskId);
        return taskDependencyRepository.findPredecessorTasksBySuccessorTaskId(task.getId())
                .stream()
                .map(this::toTaskResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskResponse> getTaskSuccessors(@NotNull Long userId, @NotNull Long taskId) {
        Task task = getPersonalTaskOrThrow(userId, taskId);
        return taskDependencyRepository.findSuccessorTasksByPredecessorTaskId(task.getId())
                .stream()
                .map(this::toTaskResponse)
                .toList();
    }

    @Override
    public void addTaskDependency(@NotNull Long userId, @NotNull Long taskId, @NotNull Long predecessorTaskId) {
        Task successorTask = getPersonalTaskOrThrow(userId, taskId);
        Task predecessorTask = getPersonalTaskOrThrow(userId, predecessorTaskId);
        createDependency(predecessorTask, successorTask);
        recordDependencyLog(successorTask, predecessorTask, successorTask.getCreator(), TaskOperationType.ADD_DEPENDENCY);
    }

    @Override
    public void removeTaskDependency(@NotNull Long userId, @NotNull Long taskId, @NotNull Long predecessorTaskId) {
        Task successorTask = getPersonalTaskOrThrow(userId, taskId);
        Task predecessorTask = getPersonalTaskOrThrow(userId, predecessorTaskId);
        deleteDependency(predecessorTask, successorTask);
        recordDependencyLog(successorTask, predecessorTask, successorTask.getCreator(),
                TaskOperationType.REMOVE_DEPENDENCY);
    }

    @Override
    public TaskResponse updateTask(@NotNull Long userId, @NotNull Long taskId, @NotNull UpdateTaskRequest request) {
        User actor = getUserOrThrow(userId);
        Task task = getPersonalTaskOrThrow(userId, taskId);
        String oldTaskInfoSnapshot = buildTaskInfoSnapshot(task);
        String oldStatus = task.getStatus();
        String normalizedStatus = TaskStatus.normalize(request.getStatus());
        assertCanSetStatus(task, normalizedStatus);
        String normalizedTitle = normalizeTitle(request.getTitle());
        String normalizedDescription = normalizeDescription(request.getDescription());
        String normalizedPriority = TaskPriority.normalize(request.getPriority());
        LocalDate normalizedDeadline = validateDeadlineForUpdate(request.getDeadline(), task.getDeadline());

        task.setTitle(normalizedTitle);
        task.setDescription(normalizedDescription);
        task.setStatus(normalizedStatus);
        task.setPriority(normalizedPriority);
        task.setDeadline(normalizedDeadline);

        Task savedTask = taskRepository.save(task);
        recordTaskUpdateLogs(savedTask, actor, oldTaskInfoSnapshot, oldStatus);
        return toTaskResponse(savedTask);
    }

    @Override
    public void deleteTask(@NotNull Long userId, @NotNull Long taskId) {
        User actor = getUserOrThrow(userId);
        Task task = getPersonalTaskOrThrow(userId, taskId);
        String oldTaskSnapshot = buildTaskSnapshot(task);
        String summary = "删除任务：" + task.getTitle();
        deleteTaskWithDependencyRules(task);
        taskOperationLogService.recordTaskLog(
                task,
                actor,
                TaskOperationType.DELETE_TASK,
                summary,
                oldTaskSnapshot,
                null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskResponse> getTeamTasks(@NotNull Long userId, @NotNull Long teamId) {
        assertTeamTaskReadable(teamId, userId);
        return taskRepository.findByTeam_IdOrderByUpdatedAtDesc(teamId)
                .stream()
                .map(this::toTaskResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TaskResponse getTeamTask(@NotNull Long userId, @NotNull Long teamId, @NotNull Long taskId) {
        assertTeamTaskReadable(teamId, userId);
        return toTaskResponse(getTeamTaskOrThrow(teamId, taskId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskOperationLogResponse> getTeamTaskLogs(@NotNull Long userId, @NotNull Long teamId,
            @NotNull Long taskId) {
        assertTeamTaskReadable(teamId, userId);
        List<TaskOperationLog> logs = taskOperationLogService.getTaskLogs(taskId);
        if (!taskRepository.findByIdAndTeam_Id(taskId, teamId).isPresent()) {
            assertDeletedTeamTaskLogReadable(teamId, taskId, logs);
        }

        return logs.stream()
                .map(this::toTaskOperationLogResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskResponse> getTeamTaskPredecessors(@NotNull Long userId, @NotNull Long teamId, @NotNull Long taskId) {
        assertTeamTaskReadable(teamId, userId);
        Task task = getTeamTaskOrThrow(teamId, taskId);
        return taskDependencyRepository.findPredecessorTasksBySuccessorTaskId(task.getId())
                .stream()
                .map(this::toTaskResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskResponse> getTeamTaskSuccessors(@NotNull Long userId, @NotNull Long teamId, @NotNull Long taskId) {
        assertTeamTaskReadable(teamId, userId);
        Task task = getTeamTaskOrThrow(teamId, taskId);
        return taskDependencyRepository.findSuccessorTasksByPredecessorTaskId(task.getId())
                .stream()
                .map(this::toTaskResponse)
                .toList();
    }

    @Override
    public void addTeamTaskDependency(@NotNull Long userId, @NotNull Long teamId, @NotNull Long taskId,
            @NotNull Long predecessorTaskId) {
        assertTeamTaskWritable(teamId, userId, true);
        Task successorTask = getTeamTaskOrThrow(teamId, taskId);
        Task predecessorTask = getTeamTaskOrThrow(teamId, predecessorTaskId);
        createDependency(predecessorTask, successorTask);
        recordDependencyLog(successorTask, predecessorTask, getUserOrThrow(userId), TaskOperationType.ADD_DEPENDENCY);
    }

    @Override
    public void removeTeamTaskDependency(@NotNull Long userId, @NotNull Long teamId, @NotNull Long taskId,
            @NotNull Long predecessorTaskId) {
        assertTeamTaskWritable(teamId, userId, true);
        Task successorTask = getTeamTaskOrThrow(teamId, taskId);
        Task predecessorTask = getTeamTaskOrThrow(teamId, predecessorTaskId);
        deleteDependency(predecessorTask, successorTask);
        recordDependencyLog(successorTask, predecessorTask, getUserOrThrow(userId), TaskOperationType.REMOVE_DEPENDENCY);
    }

    @Override
    public TaskResponse createTeamTask(@NotNull Long userId, @NotNull Long teamId, @NotNull CreateTaskRequest request) {
        User creator = getUserOrThrow(userId);
        Team team = getTeamOrThrow(teamId);
        assertTeamTaskWritable(teamId, userId, true);

        Task task = new Task();
        task.setCreator(creator);
        task.setTeam(team);
        task.setTitle(normalizeTitle(request.getTitle()));
        task.setDescription(normalizeDescription(request.getDescription()));
        task.setStatus(TaskStatus.normalize(request.getStatus()));
        task.setPriority(TaskPriority.normalize(request.getPriority()));
        task.setDeadline(validateDeadlineForCreate(request.getDeadline()));

        Task savedTask = taskRepository.save(task);
        taskOperationLogService.recordTaskLog(
                savedTask,
                creator,
                TaskOperationType.CREATE_TASK,
                "创建任务：" + savedTask.getTitle(),
                null,
                buildTaskSnapshot(savedTask));
        return toTaskResponse(savedTask);
    }

    @Override
    public TaskResponse updateTeamTask(@NotNull Long userId, @NotNull Long teamId, @NotNull Long taskId,
            @NotNull UpdateTaskRequest request) {
        assertTeamTaskWritable(teamId, userId, true);
        User actor = getUserOrThrow(userId);
        Task task = getTeamTaskOrThrow(teamId, taskId);
        String oldTaskInfoSnapshot = buildTaskInfoSnapshot(task);
        String oldStatus = task.getStatus();

        String normalizedTitle = normalizeTitle(request.getTitle());
        String normalizedDescription = normalizeDescription(request.getDescription());
        String normalizedStatus = TaskStatus.normalize(request.getStatus());
        String normalizedPriority = TaskPriority.normalize(request.getPriority());
        assertCanSetStatus(task, normalizedStatus);

        task.setTitle(normalizedTitle);
        task.setDescription(normalizedDescription);
        task.setStatus(normalizedStatus);
        task.setPriority(normalizedPriority);
        task.setDeadline(validateDeadlineForUpdate(request.getDeadline(), task.getDeadline()));

        Task savedTask = taskRepository.save(task);
        recordTaskUpdateLogs(savedTask, actor, oldTaskInfoSnapshot, oldStatus);
        return toTaskResponse(savedTask);
    }

    @Override
    public TaskResponse updateTeamTaskStatus(@NotNull Long userId, @NotNull Long teamId, @NotNull Long taskId,
            @NotNull UpdateTaskStatusRequest request) {
        assertTeamTaskWritable(teamId, userId, false);
        User actor = getUserOrThrow(userId);
        Task task = getTeamTaskOrThrow(teamId, taskId);
        String oldStatus = task.getStatus();
        String normalizedStatus = TaskStatus.normalize(request.getStatus());

        assertAssignee(task, userId);
        assertCanSetStatus(task, normalizedStatus);
        task.setStatus(normalizedStatus);
        Task savedTask = taskRepository.save(task);
        if (!oldStatus.equals(savedTask.getStatus())) {
            taskOperationLogService.recordTaskLog(
                    savedTask,
                    actor,
                    TaskOperationType.UPDATE_TASK_STATUS,
                    String.format("将任务状态从 %s 修改为 %s", oldStatus, savedTask.getStatus()),
                    oldStatus,
                    savedTask.getStatus());
        }
        return toTaskResponse(savedTask);
    }

    @Override
    public void deleteTeamTask(@NotNull Long userId, @NotNull Long teamId, @NotNull Long taskId) {
        assertTeamTaskWritable(teamId, userId, true);
        User actor = getUserOrThrow(userId);
        Task task = getTeamTaskOrThrow(teamId, taskId);
        String oldTaskSnapshot = buildTaskSnapshot(task);
        String summary = "删除任务：" + task.getTitle();
        deleteTaskWithDependencyRules(task);
        taskOperationLogService.recordTaskLog(
                task,
                actor,
                TaskOperationType.DELETE_TASK,
                summary,
                oldTaskSnapshot,
                null);
    }

    @Override
    public TaskResponse assignTeamTask(@NotNull Long userId, @NotNull Long teamId, @NotNull Long taskId,
            @Nullable Long assigneeId) {
        assertTeamTaskWritable(teamId, userId, true);
        User actor = getUserOrThrow(userId);
        Task task = getTeamTaskOrThrow(teamId, taskId);
        User oldAssignee = task.getAssignee();

        if (assigneeId == null) {
            if (oldAssignee == null) {
                return toTaskResponse(task);
            }
            rememberFormerAssignee(task);
            task.setAssignee(null);
            Task savedTask = taskRepository.save(task);
            recordAssigneeLog(savedTask, actor, oldAssignee, null);
            return toTaskResponse(savedTask);
        }

        User assignee = getUserOrThrow(assigneeId);
        if (!teamMemberRepository.existsByTeamIdAndUserId(teamId, assigneeId)) {
            throw new IllegalArgumentException("负责人必须是该团队成员");
        }

        if (oldAssignee != null && oldAssignee.getId().equals(assigneeId)) {
            return toTaskResponse(task);
        }

        task.setAssignee(assignee);
        task.setFormerAssigneeId(null);
        task.setFormerAssigneeName(null);
        Task savedTask = taskRepository.save(task);
        recordAssigneeLog(savedTask, actor, oldAssignee, assignee);
        return toTaskResponse(savedTask);
    }

    @Override
    public void clearAssignmentsForDepartedMember(@NotNull Long teamId, @NotNull Long memberId,
            @NotNull Long actorId) {
        User actor = getUserOrThrow(actorId);
        taskRepository.findByTeam_IdAndAssignee_Id(teamId, memberId)
                .forEach(task -> {
                    User oldAssignee = task.getAssignee();
                    rememberFormerAssignee(task);
                    task.setAssignee(null);
                    Task savedTask = taskRepository.save(task);
                    taskOperationLogService.recordTaskLog(
                            savedTask,
                            actor,
                            TaskOperationType.MEMBER_LEAVE_TASK_CHANGE,
                            "成员离队或被移除，已取消任务负责人：" + displayUser(oldAssignee),
                            displayUser(oldAssignee),
                            "未分配");
                });
    }

    @Override
    public void recordTeamDisbandedTaskChanges(@NotNull Long teamId, @NotNull Long actorId) {
        User actor = getUserOrThrow(actorId);
        taskRepository.findByTeam_IdOrderByUpdatedAtDesc(teamId)
                .forEach(task -> taskOperationLogService.recordTaskLog(
                        task,
                        actor,
                        TaskOperationType.TEAM_DISBANDED_TASK_CHANGE,
                        "团队解散，任务访问方式发生变化：" + task.getTitle(),
                        TeamStatus.ACTIVE.name(),
                        TeamStatus.DISBANDED.name()));
    }

    private Task getPersonalTaskOrThrow(@NotNull Long userId, @NotNull Long taskId) {
        return taskRepository.findByIdAndCreator_IdAndTeamIsNull(taskId, userId)
                .orElseThrow(() -> new TaskNotFoundException("Task not found: " + taskId));
    }

    private Task getTeamTaskOrThrow(@NotNull Long teamId, @NotNull Long taskId) {
        return taskRepository.findByIdAndTeam_Id(taskId, teamId)
                .orElseThrow(() -> new TaskNotFoundException("团队任务不存在: " + taskId));
    }

    private Team getTeamOrThrow(@NotNull Long teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new TeamNotFoundException("团队不存在: " + teamId));
    }

    private User getUserOrThrow(@NotNull Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new TaskNotFoundException("User not found: " + userId));
    }

    private TeamRole getTeamRoleOrThrow(@NotNull Long teamId, @NotNull Long userId) {
        getTeamOrThrow(teamId);
        return teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
                .map(TeamMember::getRole)
                .orElseThrow(() -> new ForbiddenOperationException("仅团队成员可访问团队任务"));
    }

    private void assertTeamMember(@NotNull Long teamId, @NotNull Long userId) {
        getTeamRoleOrThrow(teamId, userId);
    }

    private void assertAdminOrOwner(@NotNull Long teamId, @NotNull Long userId) {
        TeamRole role = getTeamRoleOrThrow(teamId, userId);
        if (role != TeamRole.Admin && role != TeamRole.Owner) {
            throw new ForbiddenOperationException("仅 Admin 或 Owner 可进行此操作");
        }
    }

    private void assertTeamTaskReadable(@NotNull Long teamId, @NotNull Long userId) {
        Team team = getTeamOrThrow(teamId);
        TeamRole role = getTeamRoleOrThrow(teamId, userId);
        if (TeamStatus.DISBANDED.name().equals(team.getStatus()) && role == TeamRole.Member) {
            throw new ForbiddenOperationException("团队已解散，普通成员不能访问团队任务");
        }
    }

    private void assertTeamTaskWritable(@NotNull Long teamId, @NotNull Long userId, boolean requireAdminOrOwner) {
        Team team = getTeamOrThrow(teamId);
        if (TeamStatus.DISBANDED.name().equals(team.getStatus())) {
            throw new ForbiddenOperationException("团队已解散，不能执行该操作");
        }

        if (requireAdminOrOwner) {
            assertAdminOrOwner(teamId, userId);
            return;
        }

        assertTeamMember(teamId, userId);
    }

    private void assertAssignee(@NotNull Task task, @NotNull Long userId) {
        if (task.getAssignee() == null || !task.getAssignee().getId().equals(userId)) {
            throw new ForbiddenOperationException("仅任务负责人可修改该团队任务状态");
        }
    }

    private void assertDeletedPersonalTaskLogReadable(@NotNull Long userId, @NotNull Long taskId,
            @NotNull List<TaskOperationLog> logs) {
        if (logs.isEmpty()) {
            throw new TaskNotFoundException("Task not found: " + taskId);
        }

        boolean readable = logs.stream()
                .anyMatch(log -> log.getScopeType() == TaskOperationScopeType.PERSONAL
                        && log.getActor() != null
                        && log.getActor().getId().equals(userId));
        if (!readable) {
            throw new TaskNotFoundException("Task not found: " + taskId);
        }
    }

    private void assertDeletedTeamTaskLogReadable(@NotNull Long teamId, @NotNull Long taskId,
            @NotNull List<TaskOperationLog> logs) {
        if (logs.isEmpty()) {
            throw new TaskNotFoundException("团队任务不存在: " + taskId);
        }

        boolean readable = logs.stream()
                .anyMatch(log -> log.getScopeType() == TaskOperationScopeType.TEAM
                        && log.getTeamId() != null
                        && log.getTeamId().equals(teamId));
        if (!readable) {
            throw new TaskNotFoundException("团队任务不存在: " + taskId);
        }
    }

    private void createDependency(@NotNull Task predecessorTask, @NotNull Task successorTask) {
        validateDependencyCreation(predecessorTask, successorTask);
        taskDependencyRepository.save(new TaskDependency(predecessorTask, successorTask));
    }

    private void deleteTaskWithDependencyRules(@NotNull Task task) {
        if (taskDependencyRepository.existsByPredecessorTask_Id(task.getId())) {
            throw new ConflictException("任务仍被后继任务依赖，无法删除");
        }

        taskDependencyRepository.deleteBySuccessorTask_Id(task.getId());
        taskRepository.delete(task);
    }

    private void deleteDependency(@NotNull Task predecessorTask, @NotNull Task successorTask) {
        TaskDependency dependency = taskDependencyRepository.findByPredecessorTask_IdAndSuccessorTask_Id(
                predecessorTask.getId(), successorTask.getId())
                .orElseThrow(() -> new TaskNotFoundException("依赖关系不存在"));
        taskDependencyRepository.delete(dependency);
    }

    private void validateDependencyCreation(@NotNull Task predecessorTask, @NotNull Task successorTask) {
        if (predecessorTask.getId().equals(successorTask.getId())) {
            throw new IllegalArgumentException("任务不能依赖自己");
        }

        assertDependencyScopeCompatible(predecessorTask, successorTask);

        if (taskDependencyRepository.existsByPredecessorTask_IdAndSuccessorTask_Id(
                predecessorTask.getId(), successorTask.getId())) {
            throw new ConflictException("依赖关系已存在");
        }

        if (wouldCreateCycle(predecessorTask.getId(), successorTask.getId())) {
            throw new ConflictException("新增依赖会形成循环依赖");
        }
    }

    private void assertDependencyScopeCompatible(@NotNull Task predecessorTask, @NotNull Task successorTask) {
        Team predecessorTeam = predecessorTask.getTeam();
        Team successorTeam = successorTask.getTeam();

        if (predecessorTeam == null && successorTeam == null) {
            return;
        }

        if (predecessorTeam == null || successorTeam == null) {
            throw new IllegalArgumentException("个人任务和团队任务之间不能建立依赖");
        }

        if (!predecessorTeam.getId().equals(successorTeam.getId())) {
            throw new IllegalArgumentException("团队任务只能依赖同一团队内的任务");
        }
    }

    private boolean wouldCreateCycle(@NotNull Long predecessorTaskId, @NotNull Long successorTaskId) {
        Deque<Long> queue = new ArrayDeque<>();
        Set<Long> visited = new HashSet<>();
        queue.add(successorTaskId);

        while (!queue.isEmpty()) {
            Long currentTaskId = queue.removeFirst();
            if (!visited.add(currentTaskId)) {
                continue;
            }

            List<Task> successors = taskDependencyRepository.findSuccessorTasksByPredecessorTaskId(currentTaskId);
            for (Task successor : successors) {
                if (successor.getId().equals(predecessorTaskId)) {
                    return true;
                }
                queue.addLast(successor.getId());
            }
        }

        return false;
    }

    private void assertCanSetStatus(@NotNull Task task, @NotNull String normalizedStatus) {
        if (!TaskStatus.DONE.name().equals(normalizedStatus)) {
            return;
        }

        boolean hasUnfinishedPredecessor = taskDependencyRepository.findPredecessorTasksBySuccessorTaskId(task.getId())
                .stream()
                .anyMatch(predecessor -> !TaskStatus.DONE.name().equals(predecessor.getStatus()));

        if (hasUnfinishedPredecessor) {
            throw new ConflictException("存在未完成的前置任务，不能将任务设为 DONE");
        }
    }

    private TaskResponse toTaskResponse(@NotNull Task task) {
        TaskResponse response = new TaskResponse();
        response.setId(task.getId());
        response.setTitle(task.getTitle());
        response.setDescription(task.getDescription());
        response.setStatus(task.getStatus());
        response.setPriority(task.getPriority());
        
        LocalDate deadlineDate = task.getDeadline();
        if (deadlineDate != null) {
            response.setDeadline(deadlineDate);
        }
        response.setCreatedAt(task.getCreatedAt());
        response.setUpdatedAt(task.getUpdatedAt());

        if (task.getCreator() != null) {
            response.setCreatorId(task.getCreator().getId());
            response.setCreatorName(task.getCreator().getUsername());
        }
        if (task.getTeam() != null) {
            response.setTeamId(task.getTeam().getId());
            response.setTeamName(task.getTeam().getName());
        }
        if (task.getAssignee() != null) {
            response.setAssigneeId(task.getAssignee().getId());
            response.setAssigneeName(task.getAssignee().getUsername());
        }
        response.setFormerAssigneeId(task.getFormerAssigneeId());
        response.setFormerAssigneeName(task.getFormerAssigneeName());

        return response;
    }

    private TaskOperationLogResponse toTaskOperationLogResponse(@NotNull TaskOperationLog operationLog) {
        TaskOperationLogResponse response = new TaskOperationLogResponse();
        response.setId(operationLog.getId());
        response.setTaskId(operationLog.getTaskId());
        response.setActorId(operationLog.getActor().getId());
        response.setActorName(operationLog.getActorName());
        response.setOperationType(operationLog.getOperationType());
        response.setScopeType(operationLog.getScopeType());
        response.setTeamId(operationLog.getTeamId());
        response.setSummary(operationLog.getSummary());
        response.setOldValue(operationLog.getOldValue());
        response.setNewValue(operationLog.getNewValue());
        response.setCreatedAt(operationLog.getCreatedAt());
        return response;
    }

    private void rememberFormerAssignee(@NotNull Task task) {
        if (task.getAssignee() == null) {
            return;
        }
        task.setFormerAssigneeId(task.getAssignee().getId());
        task.setFormerAssigneeName(task.getAssignee().getUsername());
    }

    private void recordTaskUpdateLogs(@NotNull Task task, @NotNull User actor, @NotNull String oldTaskInfoSnapshot,
            @NotNull String oldStatus) {
        String newTaskInfoSnapshot = buildTaskInfoSnapshot(task);

        if (!oldTaskInfoSnapshot.equals(newTaskInfoSnapshot)) {
            taskOperationLogService.recordTaskLog(
                    task,
                    actor,
                    TaskOperationType.UPDATE_TASK_INFO,
                    "修改任务基本信息：" + task.getTitle(),
                    oldTaskInfoSnapshot,
                    newTaskInfoSnapshot);
        }

        if (!oldStatus.equals(task.getStatus())) {
            taskOperationLogService.recordTaskLog(
                    task,
                    actor,
                    TaskOperationType.UPDATE_TASK_STATUS,
                    String.format("将任务状态从 %s 修改为 %s", oldStatus, task.getStatus()),
                    oldStatus,
                    task.getStatus());
        }
    }

    private void recordDependencyLog(@NotNull Task successorTask, @NotNull Task predecessorTask, @NotNull User actor,
            @NotNull TaskOperationType operationType) {
        boolean added = operationType == TaskOperationType.ADD_DEPENDENCY;
        taskOperationLogService.recordTaskLog(
                successorTask,
                actor,
                operationType,
                String.format("%s任务依赖：%s", added ? "新增" : "删除", predecessorTask.getTitle()),
                added ? null : dependencySnapshot(predecessorTask),
                added ? dependencySnapshot(predecessorTask) : null);
    }

    private void recordAssigneeLog(@NotNull Task task, @NotNull User actor, @Nullable User oldAssignee,
            @Nullable User newAssignee) {
        taskOperationLogService.recordTaskLog(
                task,
                actor,
                TaskOperationType.UPDATE_ASSIGNEE,
                String.format("将任务负责人从 %s 修改为 %s", displayUser(oldAssignee), displayUser(newAssignee)),
                displayUser(oldAssignee),
                displayUser(newAssignee));
    }

    private String dependencySnapshot(@NotNull Task task) {
        return String.format("taskId=%s; title=%s", task.getId(), nullSafe(task.getTitle()));
    }

    private String displayUser(@Nullable User user) {
        return user == null ? "未分配" : nullSafe(user.getUsername());
    }

    private String buildTaskSnapshot(@NotNull Task task) {
        return String.format(
                "title=%s; description=%s; status=%s; priority=%s; deadline=%s",
                nullSafe(task.getTitle()),
                nullSafe(task.getDescription()),
                nullSafe(task.getStatus()),
                nullSafe(task.getPriority()),
                task.getDeadline() == null ? "null" : task.getDeadline().toString());
    }

    private String buildTaskInfoSnapshot(@NotNull Task task) {
        return String.format(
                "title=%s; description=%s; priority=%s; deadline=%s",
                nullSafe(task.getTitle()),
                nullSafe(task.getDescription()),
                nullSafe(task.getPriority()),
                task.getDeadline() == null ? "null" : task.getDeadline().toString());
    }

    private String nullSafe(String value) {
        return value == null ? "null" : value;
    }

    private String normalizeTitle(@NotNull String title) {
        String normalized = title == null ? null : title.trim();
        if (normalized == null || normalized.isEmpty()) {
            throw new IllegalArgumentException("Title cannot be blank");
        }
        return normalized;
    }

    @Nullable
    private LocalDate validateDeadlineForCreate(@Nullable LocalDate deadline) {
        if (deadline == null) {
            return null;
        }

        if (deadline.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("截止日期不能早于今天");
        }

        return deadline;
    }

    @Nullable
    private LocalDate validateDeadlineForUpdate(@Nullable LocalDate deadline, @Nullable LocalDate currentDeadline) {
        if (deadline == null) {
            return null;
        }

        if (deadline.equals(currentDeadline)) {
            return deadline;
        }

        return validateDeadlineForCreate(deadline);
    }

    @Nullable
    private String normalizeDescription(@Nullable String description) {
        if (description == null) {
            return null;
        }

        String normalized = description.trim();
        return normalized.isEmpty() ? null : normalized;
    }

}
