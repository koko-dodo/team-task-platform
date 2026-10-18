package com.group15.lab1.service;

import com.group15.lab1.controller.dto.CreateTaskRequest;
import com.group15.lab1.controller.dto.UpdateTaskRequest;
import com.group15.lab1.controller.dto.UpdateTaskStatusRequest;
import com.group15.lab1.controller.dto.TaskOperationLogResponse;
import com.group15.lab1.controller.dto.TaskResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.group15.lab1.entity.Task;
import com.group15.lab1.entity.TaskDependency;
import com.group15.lab1.entity.TaskOperationLog;
import com.group15.lab1.entity.TaskOperationScopeType;
import com.group15.lab1.entity.TaskOperationType;
import com.group15.lab1.entity.Team;
import com.group15.lab1.entity.TeamMember;
import com.group15.lab1.entity.TeamRole;
import com.group15.lab1.entity.TeamStatus;
import com.group15.lab1.entity.User;
import com.group15.lab1.exception.ConflictException;
import com.group15.lab1.exception.ForbiddenOperationException;
import com.group15.lab1.exception.TaskNotFoundException;
import com.group15.lab1.repository.TaskDependencyRepository;
import com.group15.lab1.repository.TaskRepository;
import com.group15.lab1.repository.TeamMemberRepository;
import com.group15.lab1.repository.TeamRepository;
import com.group15.lab1.repository.UserRepository;
import com.group15.lab1.service.TaskOperationLogService;
import com.group15.lab1.service.impl.TaskServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TaskDependencyRepository taskDependencyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private TaskOperationLogService taskOperationLogService;

    @InjectMocks
    private TaskServiceImpl taskService;

    @Test
    void createTask_Success() {
        Long userId = 1L;
        User user = new User();
        user.setId(userId);
        user.setUsername("testUser");

        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("完成测试报告");
        request.setStatus("TODO");
        request.setPriority("medium");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        
        Task savedTask = new Task();
        savedTask.setId(1L);
        savedTask.setTitle(request.getTitle());
        savedTask.setStatus(request.getStatus());
        savedTask.setPriority(request.getPriority());
        savedTask.setCreator(user);
        
        when(taskRepository.save(any(Task.class))).thenReturn(savedTask);

        TaskResponse created = taskService.createTask(userId, request);
        assertNotNull(created);
        assertEquals("完成测试报告", created.getTitle());
        assertEquals("TODO", created.getStatus());
        assertEquals("medium", created.getPriority());
        verify(taskRepository, times(1)).save(any(Task.class));
        verify(taskOperationLogService).recordTaskLog(
                eq(savedTask),
                eq(user),
                eq(TaskOperationType.CREATE_TASK),
                eq("创建任务：" + savedTask.getTitle()),
                isNull(),
                contains("title=完成测试报告"));
    }

    @Test
    void getTaskById_Exists_ReturnsTaskResponse() {
        Long taskId = 1L;
        Long userId = 1L;

        Task mockTask = new Task();
        mockTask.setId(taskId);
        mockTask.setTitle("测试任务");
        mockTask.setStatus("TODO");
        mockTask.setPriority("medium");
        User user = new User();
        user.setId(userId);
        mockTask.setCreator(user); 

        when(taskRepository.findByIdAndCreator_IdAndTeamIsNull(taskId, userId)).thenReturn(Optional.of(mockTask));

        TaskResponse found = taskService.getTaskById(userId, taskId);
        assertNotNull(found);
        assertEquals(taskId, found.getId());
        assertEquals("测试任务", found.getTitle());
    }

    @Test
    void getTaskById_NotExists_ThrowsException() {
        Long taskId = 999L;
        Long userId = 1L;

        when(taskRepository.findByIdAndCreator_IdAndTeamIsNull(taskId, userId)).thenReturn(Optional.empty());

        TaskNotFoundException exception = assertThrows(TaskNotFoundException.class, () -> {
            taskService.getTaskById(userId, taskId);
        });
        assertEquals("Task not found: " + taskId, exception.getMessage());
    }

    @Test
    void createTask_BlankTitle_ThrowsExceptionAndDoesNotSave() {
        Long userId = 1L;
        User user = new User();
        user.setId(userId);

        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("   ");
        request.setStatus("TODO");
        request.setPriority("medium");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> taskService.createTask(userId, request));

        assertEquals("Title cannot be blank", exception.getMessage());
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void deleteTask_WhenTaskIsDependedOn_ThrowsConflictException() {
        Long userId = 1L;
        Long taskId = 10L;

        User user = new User();
        user.setId(userId);

        Task task = new Task();
        task.setId(taskId);
        task.setCreator(user);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(taskRepository.findByIdAndCreator_IdAndTeamIsNull(taskId, userId)).thenReturn(Optional.of(task));
        when(taskDependencyRepository.existsByPredecessorTask_Id(taskId)).thenReturn(true);

        ConflictException exception = assertThrows(ConflictException.class,
                () -> taskService.deleteTask(userId, taskId));

        assertEquals("任务仍被后继任务依赖，无法删除", exception.getMessage());
        verify(taskDependencyRepository, never()).deleteBySuccessorTask_Id(taskId);
        verify(taskRepository, never()).delete(task);
        verifyNoInteractions(taskOperationLogService);
    }

    @Test
    void deleteTask_WhenTaskIsNotDependedOn_CleansOutgoingDependenciesThenDeletesTask() {
        Long userId = 1L;
        Long taskId = 10L;

        User user = new User();
        user.setId(userId);

        Task task = new Task();
        task.setId(taskId);
        task.setCreator(user);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(taskRepository.findByIdAndCreator_IdAndTeamIsNull(taskId, userId)).thenReturn(Optional.of(task));
        when(taskDependencyRepository.existsByPredecessorTask_Id(taskId)).thenReturn(false);

        taskService.deleteTask(userId, taskId);

        verify(taskDependencyRepository).deleteBySuccessorTask_Id(taskId);
        verify(taskRepository).delete(task);
        verify(taskOperationLogService).recordTaskLog(
                eq(task),
                eq(user),
                eq(TaskOperationType.DELETE_TASK),
                eq("删除任务：null"),
                contains("title=null"),
                isNull());
    }

    @Test
    void addTaskDependency_Success() {
        Long userId = 1L;
        Long predecessorId = 10L;
        Long successorId = 20L;

        User user = new User();
        user.setId(userId);

        Task predecessor = new Task();
        predecessor.setId(predecessorId);
        predecessor.setCreator(user);
        predecessor.setStatus("DONE");

        Task successor = new Task();
        successor.setId(successorId);
        successor.setCreator(user);
        successor.setStatus("TODO");

        when(taskRepository.findByIdAndCreator_IdAndTeamIsNull(successorId, userId)).thenReturn(Optional.of(successor));
        when(taskRepository.findByIdAndCreator_IdAndTeamIsNull(predecessorId, userId)).thenReturn(Optional.of(predecessor));
        when(taskDependencyRepository.existsByPredecessorTask_IdAndSuccessorTask_Id(predecessorId, successorId))
                .thenReturn(false);
        when(taskDependencyRepository.findSuccessorTasksByPredecessorTaskId(successorId)).thenReturn(List.of());

        taskService.addTaskDependency(userId, successorId, predecessorId);

        verify(taskDependencyRepository).save(any(TaskDependency.class));
        verify(taskOperationLogService).recordTaskLog(
                eq(successor),
                eq(user),
                eq(TaskOperationType.ADD_DEPENDENCY),
                contains("新增任务依赖"),
                isNull(),
                contains("taskId=10"));
    }

    // ========================================
    // Lab3回归测试：循环依赖检测
    // ========================================

    @Test
    void addTaskDependency_Cycle_ThrowsConflictException() {
        Long userId = 1L;
        Long predecessorId = 10L;
        Long successorId = 20L;

        User user = new User();
        user.setId(userId);

        Task predecessor = new Task();
        predecessor.setId(predecessorId);
        predecessor.setCreator(user);

        Task successor = new Task();
        successor.setId(successorId);
        successor.setCreator(user);

        Task existingSuccessor = new Task();
        existingSuccessor.setId(predecessorId);

        when(taskRepository.findByIdAndCreator_IdAndTeamIsNull(successorId, userId)).thenReturn(Optional.of(successor));
        when(taskRepository.findByIdAndCreator_IdAndTeamIsNull(predecessorId, userId)).thenReturn(Optional.of(predecessor));
        when(taskDependencyRepository.existsByPredecessorTask_IdAndSuccessorTask_Id(predecessorId, successorId))
                .thenReturn(false);
        when(taskDependencyRepository.findSuccessorTasksByPredecessorTaskId(successorId))
                .thenReturn(List.of(existingSuccessor));

        ConflictException exception = assertThrows(ConflictException.class,
                () -> taskService.addTaskDependency(userId, successorId, predecessorId));

        assertEquals("新增依赖会形成循环依赖", exception.getMessage());
        verify(taskDependencyRepository, never()).save(any(TaskDependency.class));
    }

    @Test
    void addTaskDependency_IndirectCycle_ThrowsConflictException() {
        // 回归测试：检测间接循环依赖（A -> B -> C -> A）
        Long userId = 1L;
        Long taskAId = 1L;
        Long taskBId = 2L;
        Long taskCId = 3L;

        User user = new User();
        user.setId(userId);

        Task taskA = new Task();
        taskA.setId(taskAId);
        taskA.setCreator(user);

        Task taskB = new Task();
        taskB.setId(taskBId);
        taskB.setCreator(user);

        Task taskC = new Task();
        taskC.setId(taskCId);
        taskC.setCreator(user);

        // 已存在依赖：A -> B -> C（形成循环的基础）
        when(taskRepository.findByIdAndCreator_IdAndTeamIsNull(taskAId, userId)).thenReturn(Optional.of(taskA));
        when(taskRepository.findByIdAndCreator_IdAndTeamIsNull(taskCId, userId)).thenReturn(Optional.of(taskC));
        
        // 尝试添加 C -> A，会形成循环（A -> B -> C -> A）
        when(taskDependencyRepository.existsByPredecessorTask_IdAndSuccessorTask_Id(taskCId, taskAId))
                .thenReturn(false);
        when(taskDependencyRepository.findSuccessorTasksByPredecessorTaskId(taskAId))
                .thenReturn(List.of(taskB));  // A的后继是B
        when(taskDependencyRepository.findSuccessorTasksByPredecessorTaskId(taskBId))
                .thenReturn(List.of(taskC));  // B的后继是C（循环检测在这一步就会发现taskC=predecessorTaskId）

        ConflictException exception = assertThrows(ConflictException.class,
                () -> taskService.addTaskDependency(userId, taskAId, taskCId));

        assertEquals("新增依赖会形成循环依赖", exception.getMessage());
        verify(taskDependencyRepository, never()).save(any(TaskDependency.class));
    }

    @Test
    void addTaskDependency_SelfDependency_ThrowsIllegalArgumentException() {
        Long userId = 1L;
        Long taskId = 10L;

        User user = new User();
        user.setId(userId);

        Task task = new Task();
        task.setId(taskId);
        task.setCreator(user);

        when(taskRepository.findByIdAndCreator_IdAndTeamIsNull(taskId, userId)).thenReturn(Optional.of(task));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> taskService.addTaskDependency(userId, taskId, taskId));

        assertEquals("任务不能依赖自己", exception.getMessage());
        verify(taskDependencyRepository, never()).save(any(TaskDependency.class));
    }

    @Test
    void addTaskDependency_Duplicate_ThrowsConflictException() {
        Long userId = 1L;
        Long predecessorId = 10L;
        Long successorId = 20L;

        User user = new User();
        user.setId(userId);

        Task predecessor = new Task();
        predecessor.setId(predecessorId);
        predecessor.setCreator(user);

        Task successor = new Task();
        successor.setId(successorId);
        successor.setCreator(user);

        when(taskRepository.findByIdAndCreator_IdAndTeamIsNull(successorId, userId)).thenReturn(Optional.of(successor));
        when(taskRepository.findByIdAndCreator_IdAndTeamIsNull(predecessorId, userId)).thenReturn(Optional.of(predecessor));
        when(taskDependencyRepository.existsByPredecessorTask_IdAndSuccessorTask_Id(predecessorId, successorId))
                .thenReturn(true);

        ConflictException exception = assertThrows(ConflictException.class,
                () -> taskService.addTaskDependency(userId, successorId, predecessorId));

        assertEquals("依赖关系已存在", exception.getMessage());
        verify(taskDependencyRepository, never()).save(any(TaskDependency.class));
    }

    @Test
    void removeTaskDependency_Success() {
        Long userId = 1L;
        Long predecessorId = 10L;
        Long successorId = 20L;

        User user = new User();
        user.setId(userId);

        Task predecessor = new Task();
        predecessor.setId(predecessorId);
        predecessor.setCreator(user);

        Task successor = new Task();
        successor.setId(successorId);
        successor.setCreator(user);

        TaskDependency dependency = new TaskDependency(predecessor, successor);

        when(taskRepository.findByIdAndCreator_IdAndTeamIsNull(successorId, userId)).thenReturn(Optional.of(successor));
        when(taskRepository.findByIdAndCreator_IdAndTeamIsNull(predecessorId, userId)).thenReturn(Optional.of(predecessor));
        when(taskDependencyRepository.findByPredecessorTask_IdAndSuccessorTask_Id(predecessorId, successorId))
                .thenReturn(Optional.of(dependency));

        taskService.removeTaskDependency(userId, successorId, predecessorId);

        verify(taskDependencyRepository).delete(dependency);
        verify(taskOperationLogService).recordTaskLog(
                eq(successor),
                eq(user),
                eq(TaskOperationType.REMOVE_DEPENDENCY),
                contains("删除任务依赖"),
                contains("taskId=10"),
                isNull());
    }

    @Test
    void removeTaskDependency_NotExists_ThrowsTaskNotFoundException() {
        Long userId = 1L;
        Long predecessorId = 10L;
        Long successorId = 20L;

        User user = new User();
        user.setId(userId);

        Task predecessor = new Task();
        predecessor.setId(predecessorId);
        predecessor.setCreator(user);

        Task successor = new Task();
        successor.setId(successorId);
        successor.setCreator(user);

        when(taskRepository.findByIdAndCreator_IdAndTeamIsNull(successorId, userId)).thenReturn(Optional.of(successor));
        when(taskRepository.findByIdAndCreator_IdAndTeamIsNull(predecessorId, userId)).thenReturn(Optional.of(predecessor));
        when(taskDependencyRepository.findByPredecessorTask_IdAndSuccessorTask_Id(predecessorId, successorId))
                .thenReturn(Optional.empty());

        TaskNotFoundException exception = assertThrows(TaskNotFoundException.class,
                () -> taskService.removeTaskDependency(userId, successorId, predecessorId));

        assertEquals("依赖关系不存在", exception.getMessage());
        verify(taskDependencyRepository, never()).delete(any(TaskDependency.class));
    }

    @Test
    void updateTask_SetDoneWithUnfinishedPredecessor_ThrowsConflictException() {
        Long userId = 1L;
        Long taskId = 20L;

        User user = new User();
        user.setId(userId);

        Task task = new Task();
        task.setId(taskId);
        task.setCreator(user);
        task.setTitle("当前任务");
        task.setStatus("TODO");
        task.setPriority("medium");

        Task unfinishedPredecessor = new Task();
        unfinishedPredecessor.setId(10L);
        unfinishedPredecessor.setStatus("IN_PROGRESS");

        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setTitle("当前任务");
        request.setDescription("desc");
        request.setStatus("DONE");
        request.setPriority("medium");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(taskRepository.findByIdAndCreator_IdAndTeamIsNull(taskId, userId)).thenReturn(Optional.of(task));
        when(taskDependencyRepository.findPredecessorTasksBySuccessorTaskId(taskId))
                .thenReturn(List.of(unfinishedPredecessor));

        ConflictException exception = assertThrows(ConflictException.class,
                () -> taskService.updateTask(userId, taskId, request));

        assertEquals("存在未完成的前置任务，不能将任务设为 DONE", exception.getMessage());
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void updateTask_SetDoneWithFinishedPredecessors_SavesTask() {
        Long userId = 1L;
        Long taskId = 20L;

        User user = new User();
        user.setId(userId);

        Task task = new Task();
        task.setId(taskId);
        task.setCreator(user);
        task.setTitle("当前任务");
        task.setStatus("TODO");
        task.setPriority("medium");

        Task finishedPredecessor = new Task();
        finishedPredecessor.setId(10L);
        finishedPredecessor.setStatus("DONE");

        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setTitle("当前任务");
        request.setDescription("desc");
        request.setStatus("DONE");
        request.setPriority("medium");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(taskRepository.findByIdAndCreator_IdAndTeamIsNull(taskId, userId)).thenReturn(Optional.of(task));
        when(taskDependencyRepository.findPredecessorTasksBySuccessorTaskId(taskId))
                .thenReturn(List.of(finishedPredecessor));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse response = taskService.updateTask(userId, taskId, request);

        assertEquals("DONE", response.getStatus());
        verify(taskRepository).save(task);
        verify(taskOperationLogService).recordTaskLog(
                eq(task),
                eq(user),
                eq(TaskOperationType.UPDATE_TASK_STATUS),
                eq("将任务状态从 TODO 修改为 DONE"),
                eq("TODO"),
                eq("DONE"));
    }

    @Test
    void deleteTeamTask_WhenTaskIsDependedOn_ThrowsConflictException() {
        Long userId = 1L;
        Long teamId = 100L;
        Long taskId = 20L;

        User owner = new User();
        owner.setId(userId);

        Team team = new Team();
        team.setId(teamId);
        team.setOwner(owner);

        TeamMember ownerMember = new TeamMember();
        ownerMember.setTeam(team);
        ownerMember.setUser(owner);
        ownerMember.setRole(TeamRole.Owner);

        Task task = new Task();
        task.setId(taskId);
        task.setTeam(team);
        task.setCreator(owner);

        when(userRepository.findById(userId)).thenReturn(Optional.of(owner));
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId)).thenReturn(Optional.of(ownerMember));
        when(taskRepository.findByIdAndTeam_Id(taskId, teamId)).thenReturn(Optional.of(task));
        when(taskDependencyRepository.existsByPredecessorTask_Id(taskId)).thenReturn(true);

        ConflictException exception = assertThrows(ConflictException.class,
                () -> taskService.deleteTeamTask(userId, teamId, taskId));

        assertEquals("任务仍被后继任务依赖，无法删除", exception.getMessage());
        verify(taskDependencyRepository, never()).deleteBySuccessorTask_Id(taskId);
        verify(taskRepository, never()).delete(task);
    }

    @Test
    void deleteTeamTask_WhenTaskIsNotDependedOn_CleansOutgoingDependenciesThenDeletesTask() {
        Long userId = 1L;
        Long teamId = 100L;
        Long taskId = 20L;

        User owner = new User();
        owner.setId(userId);

        Team team = new Team();
        team.setId(teamId);
        team.setOwner(owner);

        TeamMember ownerMember = new TeamMember();
        ownerMember.setTeam(team);
        ownerMember.setUser(owner);
        ownerMember.setRole(TeamRole.Owner);

        Task task = new Task();
        task.setId(taskId);
        task.setTeam(team);
        task.setCreator(owner);

        when(userRepository.findById(userId)).thenReturn(Optional.of(owner));
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId)).thenReturn(Optional.of(ownerMember));
        when(taskRepository.findByIdAndTeam_Id(taskId, teamId)).thenReturn(Optional.of(task));
        when(taskDependencyRepository.existsByPredecessorTask_Id(taskId)).thenReturn(false);

        taskService.deleteTeamTask(userId, teamId, taskId);

        verify(taskDependencyRepository).deleteBySuccessorTask_Id(taskId);
        verify(taskRepository).delete(task);
        verify(taskOperationLogService).recordTaskLog(
                eq(task),
                eq(owner),
                eq(TaskOperationType.DELETE_TASK),
                eq("删除任务：null"),
                contains("title=null"),
                isNull());
    }

    @Test
    void updateTeamTaskStatus_WhenStatusChanges_RecordsStatusLog() {
        Long userId = 2L;
        Long teamId = 100L;
        Long taskId = 20L;

        User assignee = new User();
        assignee.setId(userId);
        assignee.setUsername("memberUser");

        Team team = new Team();
        team.setId(teamId);
        team.setStatus(TeamStatus.ACTIVE.name());

        TeamMember member = new TeamMember();
        member.setTeam(team);
        member.setUser(assignee);
        member.setRole(TeamRole.Member);

        Task task = new Task();
        task.setId(taskId);
        task.setTeam(team);
        task.setCreator(assignee);
        task.setAssignee(assignee);
        task.setTitle("团队任务");
        task.setStatus("TODO");
        task.setPriority("medium");

        UpdateTaskStatusRequest request = new UpdateTaskStatusRequest();
        request.setStatus("DONE");

        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId)).thenReturn(Optional.of(member));
        when(userRepository.findById(userId)).thenReturn(Optional.of(assignee));
        when(taskRepository.findByIdAndTeam_Id(taskId, teamId)).thenReturn(Optional.of(task));
        when(taskDependencyRepository.findPredecessorTasksBySuccessorTaskId(taskId)).thenReturn(List.of());
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse response = taskService.updateTeamTaskStatus(userId, teamId, taskId, request);

        assertEquals("DONE", response.getStatus());
        verify(taskOperationLogService).recordTaskLog(
                eq(task),
                eq(assignee),
                eq(TaskOperationType.UPDATE_TASK_STATUS),
                eq("将任务状态从 TODO 修改为 DONE"),
                eq("TODO"),
                eq("DONE"));
    }

    @Test
    void getTeamTasks_DisbandedTeam_MemberCannotAccess() {
        Long userId = 1L;
        Long teamId = 100L;

        Team team = new Team();
        team.setId(teamId);
        team.setStatus(TeamStatus.DISBANDED.name());

        TeamMember member = new TeamMember();
        member.setRole(TeamRole.Member);

        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId)).thenReturn(Optional.of(member));

        ForbiddenOperationException exception = assertThrows(ForbiddenOperationException.class,
                () -> taskService.getTeamTasks(userId, teamId));

        assertEquals("团队已解散，普通成员不能访问团队任务", exception.getMessage());
        verify(taskRepository, never()).findByTeam_IdOrderByUpdatedAtDesc(teamId);
    }

    @Test
    void getTeamTasks_DisbandedTeam_OwnerCanStillViewHistory() {
        Long userId = 1L;
        Long teamId = 100L;

        Team team = new Team();
        team.setId(teamId);
        team.setStatus(TeamStatus.DISBANDED.name());

        TeamMember ownerMember = new TeamMember();
        ownerMember.setRole(TeamRole.Owner);

        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId)).thenReturn(Optional.of(ownerMember));
        when(taskRepository.findByTeam_IdOrderByUpdatedAtDesc(teamId)).thenReturn(List.of());

        List<TaskResponse> responses = taskService.getTeamTasks(userId, teamId);

        assertNotNull(responses);
        verify(taskRepository).findByTeam_IdOrderByUpdatedAtDesc(teamId);
    }

    @Test
    void createTeamTask_DisbandedTeam_ThrowsForbiddenOperationException() {
        Long userId = 1L;
        Long teamId = 100L;

        User owner = new User();
        owner.setId(userId);

        Team team = new Team();
        team.setId(teamId);
        team.setOwner(owner);
        team.setStatus(TeamStatus.DISBANDED.name());

        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("团队任务");
        request.setStatus("TODO");
        request.setPriority("medium");

        when(userRepository.findById(userId)).thenReturn(Optional.of(owner));
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));

        ForbiddenOperationException exception = assertThrows(ForbiddenOperationException.class,
                () -> taskService.createTeamTask(userId, teamId, request));

        assertEquals("团队已解散，不能执行该操作", exception.getMessage());
        verify(taskRepository, never()).save(any(Task.class));
    }

    // ========================================
    // Lab3: 跨范围依赖合法性校验
    // ========================================

    @Test
    void addTaskDependency_PersonalTaskToTeamTask_ThrowsIllegalArgumentException() {
        Long userId = 1L;
        Long personalTaskId = 10L;
        Long teamTaskId = 20L;

        User user = new User();
        user.setId(userId);

        Team team = new Team();
        team.setId(100L);

        Task personalTask = new Task();
        personalTask.setId(personalTaskId);
        personalTask.setCreator(user);
        personalTask.setTeam(null);

        Task teamTask = new Task();
        teamTask.setId(teamTaskId);
        teamTask.setCreator(user);
        teamTask.setTeam(team);

        when(taskRepository.findByIdAndCreator_IdAndTeamIsNull(personalTaskId, userId))
                .thenReturn(Optional.of(personalTask));
        when(taskRepository.findByIdAndCreator_IdAndTeamIsNull(teamTaskId, userId))
                .thenReturn(Optional.empty());

        assertThrows(TaskNotFoundException.class,
                () -> taskService.addTaskDependency(userId, personalTaskId, teamTaskId));
        verify(taskDependencyRepository, never()).save(any(TaskDependency.class));
    }

    @Test
    void addTaskDependency_TeamTaskToPersonalTask_ThrowsIllegalArgumentException() {
        Long userId = 1L;
        Long teamId = 100L;
        Long teamTaskId = 10L;
        Long personalTaskId = 20L;

        User owner = new User();
        owner.setId(userId);

        Team team = new Team();
        team.setId(teamId);
        team.setOwner(owner);

        TeamMember ownerMember = new TeamMember();
        ownerMember.setTeam(team);
        ownerMember.setUser(owner);
        ownerMember.setRole(TeamRole.Owner);

        Task personalTask = new Task();
        personalTask.setId(personalTaskId);
        personalTask.setCreator(owner);
        personalTask.setTeam(null);

        Task teamTask = new Task();
        teamTask.setId(teamTaskId);
        teamTask.setCreator(owner);
        teamTask.setTeam(team);

        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId)).thenReturn(Optional.of(ownerMember));
        when(taskRepository.findByIdAndTeam_Id(teamTaskId, teamId)).thenReturn(Optional.of(teamTask));
        when(taskRepository.findByIdAndTeam_Id(personalTaskId, teamId)).thenReturn(Optional.empty());

        assertThrows(TaskNotFoundException.class,
                () -> taskService.addTeamTaskDependency(userId, teamId, teamTaskId, personalTaskId));
        verify(taskDependencyRepository, never()).save(any(TaskDependency.class));
    }

    // ========================================
    // Lab3: 创建任务时的输入校验
    // ========================================

    @Test
    void createTask_PastDeadline_ThrowsIllegalArgumentException() {
        Long userId = 1L;
        User user = new User();
        user.setId(userId);

        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("过期的截止日期");
        request.setStatus("TODO");
        request.setPriority("medium");
        request.setDeadline(LocalDate.of(2020, 1, 1));

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> taskService.createTask(userId, request));

        assertEquals("截止日期不能早于今天", exception.getMessage());
        verify(taskRepository, never()).save(any(Task.class));
    }

    // ========================================
    // Lab3: 团队成员权限校验
    // ========================================

    @Test
    void getTeamTasks_NotMember_ThrowsForbiddenOperationException() {
        Long userId = 1L;
        Long teamId = 100L;

        Team team = new Team();
        team.setId(teamId);

        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId)).thenReturn(Optional.empty());

        ForbiddenOperationException exception = assertThrows(ForbiddenOperationException.class,
                () -> taskService.getTeamTasks(userId, teamId));

        assertEquals("仅团队成员可访问团队任务", exception.getMessage());
        verify(taskRepository, never()).findByTeam_IdOrderByUpdatedAtDesc(teamId);
    }

    @Test
    void createTeamTask_NotAdminOrOwner_ThrowsForbiddenOperationException() {
        Long userId = 1L;
        Long teamId = 100L;

        User user = new User();
        user.setId(userId);

        Team team = new Team();
        team.setId(teamId);

        TeamMember member = new TeamMember();
        member.setTeam(team);
        member.setUser(user);
        member.setRole(TeamRole.Member);

        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("测试团队任务");
        request.setStatus("TODO");
        request.setPriority("medium");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId)).thenReturn(Optional.of(member));

        ForbiddenOperationException exception = assertThrows(ForbiddenOperationException.class,
                () -> taskService.createTeamTask(userId, teamId, request));

        assertEquals("仅 Admin 或 Owner 可进行此操作", exception.getMessage());
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void updateTeamTaskStatus_NotAssignee_ThrowsForbiddenOperationException() {
        Long userId = 1L;
        Long assigneeId = 2L;
        Long teamId = 100L;
        Long taskId = 20L;

        User assignee = new User();
        assignee.setId(assigneeId);

        User requester = new User();
        requester.setId(userId);

        Team team = new Team();
        team.setId(teamId);

        TeamMember member = new TeamMember();
        member.setTeam(team);
        member.setUser(requester);
        member.setRole(TeamRole.Member);

        Task task = new Task();
        task.setId(taskId);
        task.setTeam(team);
        task.setAssignee(assignee);
        task.setStatus("TODO");

        UpdateTaskStatusRequest request = new UpdateTaskStatusRequest();
        request.setStatus("DONE");

        when(userRepository.findById(userId)).thenReturn(Optional.of(requester));
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId)).thenReturn(Optional.of(member));
        when(taskRepository.findByIdAndTeam_Id(taskId, teamId)).thenReturn(Optional.of(task));

        ForbiddenOperationException exception = assertThrows(ForbiddenOperationException.class,
                () -> taskService.updateTeamTaskStatus(userId, teamId, taskId, request));

        assertEquals("仅任务负责人可修改该团队任务状态", exception.getMessage());
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void deleteTeamTask_NotAdminOrOwner_ThrowsForbiddenOperationException() {
        Long userId = 1L;
        Long teamId = 100L;

        User user = new User();
        user.setId(userId);

        Team team = new Team();
        team.setId(teamId);

        TeamMember member = new TeamMember();
        member.setTeam(team);
        member.setUser(user);
        member.setRole(TeamRole.Member);

        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId)).thenReturn(Optional.of(member));

        ForbiddenOperationException exception = assertThrows(ForbiddenOperationException.class,
                () -> taskService.deleteTeamTask(userId, teamId, 20L));

        assertEquals("仅 Admin 或 Owner 可进行此操作", exception.getMessage());
        verify(taskDependencyRepository, never()).deleteBySuccessorTask_Id(any());
        verify(taskRepository, never()).delete(any(Task.class));
    }

    // ========================================
    // Lab3: 非法状态值处理
    // ========================================

    @Test
    void createTask_InvalidStatus_ThrowsIllegalArgumentException() {
        Long userId = 1L;
        User user = new User();
        user.setId(userId);

        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("非法状态");
        request.setStatus("INVALID_STATUS");
        request.setPriority("medium");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThrows(IllegalArgumentException.class,
                () -> taskService.createTask(userId, request));
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void createTask_InvalidPriority_ThrowsIllegalArgumentException() {
        Long userId = 1L;
        User user = new User();
        user.setId(userId);

        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("非法优先级");
        request.setStatus("TODO");
        request.setPriority("invalid_priority");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThrows(IllegalArgumentException.class,
                () -> taskService.createTask(userId, request));
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void getPersonalTaskLogs_OwnerCanViewLogsInRepositoryOrder() {
        Long userId = 1L;
        Long taskId = 10L;
        User actor = new User();
        actor.setId(userId);
        actor.setUsername("owner");

        Task task = new Task();
        task.setId(taskId);
        task.setCreator(actor);

        TaskOperationLog newest = createOperationLog(2L, taskId, actor, LocalDateTime.of(2026, 6, 5, 12, 0));
        TaskOperationLog oldest = createOperationLog(1L, taskId, actor, LocalDateTime.of(2026, 6, 5, 11, 0));

        when(taskRepository.findByIdAndCreator_IdAndTeamIsNull(taskId, userId)).thenReturn(Optional.of(task));
        when(taskOperationLogService.getTaskLogs(taskId)).thenReturn(List.of(newest, oldest));

        List<TaskOperationLogResponse> logs = taskService.getPersonalTaskLogs(userId, taskId);

        assertEquals(List.of(2L, 1L), logs.stream().map(TaskOperationLogResponse::getId).toList());
        assertEquals(userId, logs.get(0).getActorId());
        verify(taskOperationLogService).getTaskLogs(taskId);
    }

    @Test
    void getPersonalTaskLogs_DeletedTaskOwnerCanStillViewLogs() {
        Long userId = 1L;
        Long taskId = 10L;
        User actor = new User();
        actor.setId(userId);
        actor.setUsername("owner");

        when(taskRepository.findByIdAndCreator_IdAndTeamIsNull(taskId, userId)).thenReturn(Optional.empty());
        when(taskOperationLogService.getTaskLogs(taskId)).thenReturn(List.of(
                createOperationLog(2L, taskId, actor, LocalDateTime.of(2026, 6, 5, 12, 0))));

        List<TaskOperationLogResponse> logs = taskService.getPersonalTaskLogs(userId, taskId);

        assertEquals(1, logs.size());
        assertEquals(taskId, logs.get(0).getTaskId());
    }

    @Test
    void getPersonalTaskLogs_OtherUserCannotViewLogs() {
        Long userId = 2L;
        Long taskId = 10L;
        when(taskRepository.findByIdAndCreator_IdAndTeamIsNull(taskId, userId)).thenReturn(Optional.empty());
        when(taskOperationLogService.getTaskLogs(taskId)).thenReturn(List.of());

        assertThrows(TaskNotFoundException.class, () -> taskService.getPersonalTaskLogs(userId, taskId));
        verify(taskOperationLogService).getTaskLogs(taskId);
    }

    // ========================================
    // 业务逻辑层单元测试：任务状态变更时生成日志
    // ========================================

    @Test
    void updateTask_StatusChanged_RecordsStatusLog() {
        Long userId = 1L;
        Long taskId = 10L;

        User user = new User();
        user.setId(userId);
        user.setUsername("testUser");

        Task task = new Task();
        task.setId(taskId);
        task.setCreator(user);
        task.setTitle("测试任务");
        task.setStatus("TODO");
        task.setPriority("medium");

        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setTitle("测试任务");
        request.setDescription("更新描述");
        request.setStatus("IN_PROGRESS");
        request.setPriority("medium");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(taskRepository.findByIdAndCreator_IdAndTeamIsNull(taskId, userId)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        taskService.updateTask(userId, taskId, request);

        // 验证状态变更日志被记录
        verify(taskOperationLogService).recordTaskLog(
                eq(task),
                eq(user),
                eq(TaskOperationType.UPDATE_TASK_STATUS),
                eq("将任务状态从 TODO 修改为 IN_PROGRESS"),
                eq("TODO"),
                eq("IN_PROGRESS"));

        // 验证任务信息变更日志也被记录（因为描述发生了变化）
        verify(taskOperationLogService).recordTaskLog(
                eq(task),
                eq(user),
                eq(TaskOperationType.UPDATE_TASK_INFO),
                contains("修改任务基本信息"),
                any(),
                any());
    }

    // ========================================
    // 无权限访问日志场景测试
    // ========================================

    @Test
    void getTeamTaskLogs_NonMemberCannotViewLogs() {
        Long userId = 2L;
        Long teamId = 100L;
        Long taskId = 10L;
        Team team = new Team();
        team.setId(teamId);
        team.setStatus(TeamStatus.ACTIVE.name());

        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId)).thenReturn(Optional.empty());

        ForbiddenOperationException exception = assertThrows(ForbiddenOperationException.class,
                () -> taskService.getTeamTaskLogs(userId, teamId, taskId));
        assertEquals("仅团队成员可访问团队任务", exception.getMessage());

        verify(taskOperationLogService, never()).getTaskLogs(taskId);
    }

    @Test
    void getPersonalTaskLogs_NonOwnerCannotViewDeletedTaskLogs() {
        Long userId = 2L;
        Long taskId = 10L;
        User otherUser = new User();
        otherUser.setId(1L);

        // 任务已删除，但日志属于另一个用户
        TaskOperationLog log = new TaskOperationLog();
        log.setId(1L);
        log.setTaskId(taskId);
        log.setActor(otherUser);
        log.setScopeType(TaskOperationScopeType.PERSONAL);

        when(taskRepository.findByIdAndCreator_IdAndTeamIsNull(taskId, userId)).thenReturn(Optional.empty());
        when(taskOperationLogService.getTaskLogs(taskId)).thenReturn(List.of(log));

        TaskNotFoundException exception = assertThrows(TaskNotFoundException.class,
                () -> taskService.getPersonalTaskLogs(userId, taskId));
        assertEquals("Task not found: " + taskId, exception.getMessage());
    }

    @Test
    void getTeamTaskLogs_DeletedTaskMemberCanStillViewLogs() {
        Long userId = 2L;
        Long teamId = 100L;
        Long taskId = 10L;
        User actor = new User();
        actor.setId(1L);
        actor.setUsername("owner");
        Team team = new Team();
        team.setId(teamId);
        team.setStatus(TeamStatus.ACTIVE.name());
        TeamMember member = new TeamMember();
        member.setRole(TeamRole.Member);

        TaskOperationLog log = createOperationLog(3L, taskId, actor, LocalDateTime.of(2026, 6, 5, 13, 0));
        log.setScopeType(TaskOperationScopeType.TEAM);
        log.setTeamId(teamId);

        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId)).thenReturn(Optional.of(member));
        when(taskRepository.findByIdAndTeam_Id(taskId, teamId)).thenReturn(Optional.empty());
        when(taskOperationLogService.getTaskLogs(taskId)).thenReturn(List.of(log));

        List<TaskOperationLogResponse> logs = taskService.getTeamTaskLogs(userId, teamId, taskId);

        assertEquals(1, logs.size());
        assertEquals(teamId, logs.get(0).getTeamId());
        assertEquals(TaskOperationScopeType.TEAM, logs.get(0).getScopeType());
    }

    @Test
    void assignTeamTask_WhenAssigneeChanges_RecordsAssigneeLog() {
        Long ownerId = 1L;
        Long memberId = 2L;
        Long teamId = 100L;
        Long taskId = 10L;
        User owner = new User();
        owner.setId(ownerId);
        owner.setUsername("owner");
        User member = new User();
        member.setId(memberId);
        member.setUsername("member");
        Team team = new Team();
        team.setId(teamId);
        team.setStatus(TeamStatus.ACTIVE.name());
        TeamMember ownerMember = new TeamMember();
        ownerMember.setRole(TeamRole.Owner);
        Task task = new Task();
        task.setId(taskId);
        task.setTeam(team);

        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, ownerId)).thenReturn(Optional.of(ownerMember));
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(userRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, memberId)).thenReturn(true);
        when(taskRepository.findByIdAndTeam_Id(taskId, teamId)).thenReturn(Optional.of(task));
        when(taskRepository.save(task)).thenReturn(task);

        taskService.assignTeamTask(ownerId, teamId, taskId, memberId);

        verify(taskOperationLogService).recordTaskLog(
                eq(task),
                eq(owner),
                eq(TaskOperationType.UPDATE_ASSIGNEE),
                eq("将任务负责人从 未分配 修改为 member"),
                eq("未分配"),
                eq("member"));
    }

    private TaskOperationLog createOperationLog(Long id, Long taskId, User actor, LocalDateTime createdAt) {
        TaskOperationLog operationLog = new TaskOperationLog();
        operationLog.setId(id);
        operationLog.setTaskId(taskId);
        operationLog.setActor(actor);
        operationLog.setActorName(actor.getUsername());
        operationLog.setOperationType(TaskOperationType.CREATE_TASK);
        operationLog.setScopeType(TaskOperationScopeType.PERSONAL);
        operationLog.setSummary("创建任务");
        operationLog.setCreatedAt(createdAt);
        return operationLog;
    }
}