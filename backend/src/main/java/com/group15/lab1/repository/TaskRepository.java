package com.group15.lab1.repository;

import com.group15.lab1.entity.Task;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskRepository extends JpaRepository<Task, Long> {

    /**
     * 查询某用户创建的个人任务。
     */
    List<Task> findByCreator_IdAndTeamIsNullOrderByUpdatedAtDesc(Long userId);

    /**
     * 查询“我的任务”列表：
     * 1. 我创建的个人任务
     * 2. 分配给我的活跃团队任务（已解散团队的任务不显示）
     */
    @Query("""
            SELECT t
            FROM Task t
            LEFT JOIN t.team team
            WHERE (t.team IS NULL AND t.creator.id = :userId)
               OR (team IS NOT NULL AND team.status = 'ACTIVE' AND t.assignee IS NOT NULL AND t.assignee.id = :userId)
            ORDER BY t.updatedAt DESC
            """)
    List<Task> findMyTasks(@Param("userId") Long userId);

    /**
     * 查询某用户的单个个人任务。
     */
    Optional<Task> findByIdAndCreator_IdAndTeamIsNull(Long id, Long userId);

    /**
     * 查询某团队下的全部任务。
     */
    List<Task> findByTeam_IdOrderByUpdatedAtDesc(Long teamId);

    /**
     * 查询某团队下的指定任务。
     */
    Optional<Task> findByIdAndTeam_Id(Long id, Long teamId);

    /**
     * 查询某团队内分配给指定成员的任务。
     */
    List<Task> findByTeam_IdAndAssignee_Id(Long teamId, Long assigneeId);

    /**
     * 检查某任务是否属于某团队。
     */
    boolean existsByIdAndTeam_Id(Long id, Long teamId);
}
