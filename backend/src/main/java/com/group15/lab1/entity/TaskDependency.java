package com.group15.lab1.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

/**
 * 任务依赖关系实体。
 *
 * predecessorTask -> successorTask 表示：
 * successorTask 依赖 predecessorTask，只有 predecessorTask 完成后，
 * successorTask 才允许进入 DONE 状态。
 */
@Entity
@Table(name = "task_dependency", uniqueConstraints = {
        @UniqueConstraint(name = "uk_task_dependency_pair", columnNames = { "predecessor_task_id", "successor_task_id" })
})
public class TaskDependency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 前置任务。
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "predecessor_task_id", nullable = false)
    private Task predecessorTask;

    /**
     * 后继任务。
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "successor_task_id", nullable = false)
    private Task successorTask;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public TaskDependency() {
    }

    public TaskDependency(Task predecessorTask, Task successorTask) {
        this.predecessorTask = predecessorTask;
        this.successorTask = successorTask;
    }

    @PrePersist
    public void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Task getPredecessorTask() {
        return predecessorTask;
    }

    public void setPredecessorTask(Task predecessorTask) {
        this.predecessorTask = predecessorTask;
    }

    public Task getSuccessorTask() {
        return successorTask;
    }

    public void setSuccessorTask(Task successorTask) {
        this.successorTask = successorTask;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
