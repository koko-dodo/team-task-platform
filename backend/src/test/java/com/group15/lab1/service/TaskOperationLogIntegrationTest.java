package com.group15.lab1.service;

import com.group15.lab1.entity.Task;
import com.group15.lab1.entity.TaskOperationLog;
import com.group15.lab1.entity.TaskOperationScopeType;
import com.group15.lab1.entity.TaskOperationType;
import com.group15.lab1.entity.Team;
import com.group15.lab1.entity.User;
import com.group15.lab1.repository.TaskOperationLogRepository;
import com.group15.lab1.repository.TaskRepository;
import com.group15.lab1.repository.TeamRepository;
import com.group15.lab1.repository.UserRepository;
import com.group15.lab1.service.impl.TaskOperationLogEventListener;
import com.group15.lab1.service.impl.TaskOperationLogServiceImpl;
import com.group15.lab1.service.impl.TaskOperationLogWriter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 任务操作日志服务集成测试
 * 验证任务状态变更时日志记录的完整流程：
 * 1. recordTaskLog 发布事件
 * 2. TaskOperationLogEventListener 监听事件
 * 3. TaskOperationLogWriter 写入数据库
 */
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
class TaskOperationLogIntegrationTest {

    // 移除 @Transactional 让每个操作在独立事务中执行，确保事件监听器能立即触发

    @Autowired
    private TaskOperationLogService taskOperationLogService;

    @Autowired
    private TaskOperationLogRepository taskOperationLogRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TeamRepository teamRepository;

    // ========================================
    // 业务逻辑层单元测试：任务状态变更时生成日志
    // ========================================

    @Test
    void recordTaskLog_StatusChange_LogsArePersisted() {
        // 1. 准备测试数据
        User actor = new User();
        actor.setUsername("testUser");
        actor.setPasswordHash("testPassword123");
        actor = userRepository.save(actor);

        Task task = new Task();
        task.setTitle("测试任务");
        task.setStatus("TODO");
        task.setPriority("medium");
        task.setCreator(actor);
        task = taskRepository.save(task);

        // 2. 执行状态变更操作（模拟业务逻辑）
        String oldStatus = task.getStatus();
        String newStatus = "IN_PROGRESS";
        task.setStatus(newStatus);
        task = taskRepository.save(task);

        // 3. 记录日志
        taskOperationLogService.recordTaskLog(
                task,
                actor,
                TaskOperationType.UPDATE_TASK_STATUS,
                String.format("将任务状态从 %s 修改为 %s", oldStatus, newStatus),
                oldStatus,
                newStatus);

        
        // 等待异步操作完成
        waitForAsyncOperation();
        
        List<TaskOperationLog> logs = taskOperationLogRepository.findByTaskIdOrderByCreatedAtDescIdDesc(task.getId());
        
        // 使用自动化断言验证结果
        assertFalse(logs.isEmpty(), "日志应该被记录");
        
        TaskOperationLog log = logs.get(0);
        assertEquals(task.getId(), log.getTaskId());
        assertEquals(actor.getId(), log.getActor().getId());
        assertEquals(actor.getUsername(), log.getActorName());
        assertEquals(TaskOperationType.UPDATE_TASK_STATUS, log.getOperationType());
        assertEquals(TaskOperationScopeType.PERSONAL, log.getScopeType());
        assertEquals(String.format("将任务状态从 %s 修改为 %s", oldStatus, newStatus), log.getSummary());
        assertEquals(oldStatus, log.getOldValue());
        assertEquals(newStatus, log.getNewValue());
        assertNotNull(log.getCreatedAt());
    }

    @Test
    void recordTaskLog_CreateTask_LogsArePersisted() {
        // 1. 准备测试数据
        User actor = new User();
        actor.setUsername("creator");
        actor.setPasswordHash("testPassword123");
        actor = userRepository.save(actor);

        Task task = new Task();
        task.setTitle("新建任务");
        task.setStatus("TODO");
        task.setPriority("medium");
        task.setCreator(actor);
        task = taskRepository.save(task);

        // 2. 记录创建日志
        taskOperationLogService.recordTaskLog(
                task,
                actor,
                TaskOperationType.CREATE_TASK,
                "创建任务：" + task.getTitle(),
                null,
                buildTaskSnapshot(task));

        // 3. 验证日志
        waitForAsyncOperation();
        
        List<TaskOperationLog> logs = taskOperationLogRepository.findByTaskIdOrderByCreatedAtDescIdDesc(task.getId());
        
        assertFalse(logs.isEmpty());
        TaskOperationLog log = logs.get(0);
        
        assertEquals(TaskOperationType.CREATE_TASK, log.getOperationType());
        assertEquals("创建任务：新建任务", log.getSummary());
        assertNull(log.getOldValue());
        assertNotNull(log.getNewValue());
        assertTrue(log.getNewValue().contains("title=新建任务"));
    }

    @Test
    void recordTaskLog_TeamTask_LogsWithTeamScope() {
        // 1. 准备测试数据
        User actor = new User();
        actor.setUsername("teamOwner");
        actor.setPasswordHash("testPassword123");
        actor = userRepository.save(actor);

        Team team = new Team();
        team.setName("测试团队");
        team.setOwner(actor);
        team = teamRepository.save(team);

        Task task = new Task();
        task.setTitle("团队任务");
        task.setStatus("TODO");
        task.setPriority("medium");
        task.setCreator(actor);
        task.setTeam(team);
        task = taskRepository.save(task);

        // 2. 记录日志
        taskOperationLogService.recordTaskLog(
                task,
                actor,
                TaskOperationType.CREATE_TASK,
                "创建团队任务：" + task.getTitle(),
                null,
                buildTaskSnapshot(task));

        // 3. 验证日志
        waitForAsyncOperation();
        
        List<TaskOperationLog> logs = taskOperationLogRepository.findByTaskIdOrderByCreatedAtDescIdDesc(task.getId());
        
        assertFalse(logs.isEmpty());
        TaskOperationLog log = logs.get(0);
        
        assertEquals(TaskOperationScopeType.TEAM, log.getScopeType());
        assertEquals(team.getId(), log.getTeamId());
    }

    // ========================================
    // 无权限访问日志场景
    // ========================================

    @Test
    @Transactional(readOnly = true)
    void getTaskLogs_NonExistentTask_ReturnsEmpty() {
        List<TaskOperationLog> logs = taskOperationLogService.getTaskLogs(99999L);
        assertTrue(logs.isEmpty());
    }

    // ========================================
    // 删除任务后仍能保留日志信息
    // ========================================

    @Test
    void recordTaskLog_DeleteTask_LogsPersistedAfterDeletion() {
        // 1. 准备测试数据
        User actor = new User();
        actor.setUsername("deleter");
        actor.setPasswordHash("testPassword123");
        actor = userRepository.save(actor);

        Task task = new Task();
        task.setTitle("待删除任务");
        task.setStatus("TODO");
        task.setPriority("medium");
        task.setCreator(actor);
        task = taskRepository.save(task);
        Long taskId = task.getId();

        // 2. 先记录一些操作日志
        taskOperationLogService.recordTaskLog(
                task, actor, TaskOperationType.CREATE_TASK, "创建任务", null, buildTaskSnapshot(task));
        
        task.setStatus("IN_PROGRESS");
        task = taskRepository.save(task);
        taskOperationLogService.recordTaskLog(
                task, actor, TaskOperationType.UPDATE_TASK_STATUS, "状态变更", "TODO", "IN_PROGRESS");

        waitForAsyncOperation();
        
        // 3. 验证删除前有两条日志
        List<TaskOperationLog> logsBeforeDelete = taskOperationLogRepository.findByTaskIdOrderByCreatedAtDescIdDesc(taskId);
        assertEquals(2, logsBeforeDelete.size());

        // 4. 删除任务（这里模拟删除，实际应调用 service.deleteTask）
        taskRepository.delete(task);

        // 5. 验证日志仍然存在（级联删除应被禁止或日志应保留）
        List<TaskOperationLog> logsAfterDelete = taskOperationLogRepository.findByTaskIdOrderByCreatedAtDescIdDesc(taskId);
        assertFalse(logsAfterDelete.isEmpty(), "删除任务后日志应保留");
        assertEquals(2, logsAfterDelete.size(), "删除任务后日志数量应保持不变");
    }

    // ========================================
    // Lab3回归测试：日志完整性验证
    // ========================================

    @Test
    void recordTaskLog_MultipleOperations_LogsInCorrectOrder() {
        // 准备数据
        User actor = new User();
        actor.setUsername("multiOpUser");
        actor.setPasswordHash("testPassword123");
        actor = userRepository.save(actor);

        Task task = new Task();
        task.setTitle("多操作测试任务");
        task.setStatus("TODO");
        task.setPriority("medium");
        task.setCreator(actor);
        task = taskRepository.save(task);

        // 记录多个操作日志
        taskOperationLogService.recordTaskLog(
                task, actor, TaskOperationType.CREATE_TASK, "创建任务", null, "snapshot1");
        
        task.setStatus("IN_PROGRESS");
        task = taskRepository.save(task);
        taskOperationLogService.recordTaskLog(
                task, actor, TaskOperationType.UPDATE_TASK_STATUS, "状态变更", "TODO", "IN_PROGRESS");
        
        task.setPriority("high");
        task = taskRepository.save(task);
        taskOperationLogService.recordTaskLog(
                task, actor, TaskOperationType.UPDATE_TASK_INFO, "优先级变更", "medium", "high");

        // 验证日志顺序（最新的在前）
        waitForAsyncOperation();
        
        List<TaskOperationLog> logs = taskOperationLogService.getTaskLogs(task.getId());
        
        assertEquals(3, logs.size());
        assertEquals(TaskOperationType.UPDATE_TASK_INFO, logs.get(0).getOperationType()); // 最新
        assertEquals(TaskOperationType.UPDATE_TASK_STATUS, logs.get(1).getOperationType());
        assertEquals(TaskOperationType.CREATE_TASK, logs.get(2).getOperationType()); // 最早
    }

    private String buildTaskSnapshot(Task task) {
        return String.format(
                "title=%s; description=%s; status=%s; priority=%s; deadline=%s",
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getDeadline());
    }

    /**
     * 等待异步操作完成
     * 由于 @TransactionalEventListener 使用 AFTER_COMMIT 阶段，日志会在事务提交后异步写入
     */
    private void waitForAsyncOperation() {
        try {
            TimeUnit.MILLISECONDS.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}