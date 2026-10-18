package com.group15.lab1.repository;

import com.group15.lab1.entity.Task;
import com.group15.lab1.entity.TaskDependency;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskDependencyRepository extends JpaRepository<TaskDependency, Long> {

    /**
     * 查询某任务的所有前置依赖。
     */
    List<TaskDependency> findBySuccessorTask_Id(Long successorTaskId);

    /**
     * 查询某任务的所有后继依赖。
     */
    List<TaskDependency> findByPredecessorTask_Id(Long predecessorTaskId);

    /**
     * 判断一条依赖是否已存在。
     */
    boolean existsByPredecessorTask_IdAndSuccessorTask_Id(Long predecessorTaskId, Long successorTaskId);

    /**
     * 查询指定依赖关系。
     */
    Optional<TaskDependency> findByPredecessorTask_IdAndSuccessorTask_Id(Long predecessorTaskId, Long successorTaskId);

    /**
     * 查询某任务的所有前置任务。
     */
    @Query("SELECT td.predecessorTask FROM TaskDependency td WHERE td.successorTask.id = :successorTaskId")
    List<Task> findPredecessorTasksBySuccessorTaskId(Long successorTaskId);
    
    /**
     * 查询某任务的所有后继任务。
     */
    @Query("SELECT td.successorTask FROM TaskDependency td WHERE td.predecessorTask.id = :predecessorTaskId")
    List<Task> findSuccessorTasksByPredecessorTaskId(Long predecessorTaskId);
    
    /**
     * 删除某任务的所有依赖关系（作为后继任务）。
     */
    void deleteBySuccessorTask_Id(Long successorTaskId);
    
    /**
     * 判断某任务是否有后继依赖。
     */
    boolean existsByPredecessorTask_Id(Long predecessorTaskId);
    
    /**
     * 删除某任务作为前置任务的全部依赖关系。
     */
    void deleteByPredecessorTask_Id(Long predecessorTaskId);

}
