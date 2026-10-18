package com.group15.lab1.repository;

import com.group15.lab1.entity.TeamMember;
import com.group15.lab1.entity.TeamRole;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * 团队成员数据访问接口。
 */
@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {

    /**
     * 查询某用户在某团队中的成员关系。
     * 
     * @param teamId 团队 ID
     * @param userId 用户 ID
     * @return 成员关系，如不存在则返回 Optional.empty()
     */
    Optional<TeamMember> findByTeamIdAndUserId(Long teamId, Long userId);

    /**
     * 查询某团队的所有成员。
     * 
     * @param teamId 团队 ID
     * @return 成员列表
     */
    List<TeamMember> findAllByTeamId(Long teamId);

    /**
     * 查询某团队中具有特定角色的所有成员。
     * 
     * @param teamId 团队 ID
     * @param role   角色（Owner / Admin / Member）
     * @return 成员列表
     */
    List<TeamMember> findAllByTeamIdAndRole(Long teamId, TeamRole role);

    /**
     * 检查某用户是否是某团队的成员。
     * 
     * @param teamId 团队 ID
     * @param userId 用户 ID
     * @return true 如果是成员，false 否则
     */
    boolean existsByTeamIdAndUserId(Long teamId, Long userId);

    /**
     * 查询某用户所属的所有团队的成员关系。
     * 
     * @param userId 用户 ID
     * @return 成员关系列表
     */
    @Query("SELECT tm FROM TeamMember tm WHERE tm.user.id = :userId ORDER BY tm.joinedAt DESC")
    List<TeamMember> findAllByUserId(@Param("userId") Long userId);

    /**
     * 统计某团队的成员数。
     * 
     * @param teamId 团队 ID
     * @return 成员数
     */
    long countByTeamId(Long teamId);
}
