package com.group15.lab1.repository;
 
import com.group15.lab1.entity.Team;
import com.group15.lab1.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
 
import java.util.List;
import java.util.Optional;
 
@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {
    
    // 根据团队名称查找团队
    Optional<Team> findByName(String name);
    
    // 查找用户创建的所有团队
    List<Team> findByOwner(User owner);
    
    // 查找用户参与的所有团队（包括作为成员）
    @Query("SELECT t FROM Team t WHERE t.id IN (SELECT tm.team.id FROM TeamMember tm WHERE tm.user = :user)")
    List<Team> findByMembersContaining(@Param("user") User user);
    
    // 查找用户相关的所有团队（创建或参与）
    @Query("SELECT t FROM Team t WHERE t.owner = :user OR t.id IN (SELECT tm.team.id FROM TeamMember tm WHERE tm.user = :user)")
    List<Team> findByUserRelated(@Param("user") User user);
    
    // 检查团队名称是否已存在（排除当前团队）
    @Query("SELECT COUNT(t) > 0 FROM Team t WHERE t.name = :name AND t.id != :excludeId")
    boolean existsByNameAndIdNot(@Param("name") String name, @Param("excludeId") Long excludeId);
    
    // 检查用户是否是团队成员
    @Query("SELECT COUNT(tm) > 0 FROM TeamMember tm WHERE tm.team.id = :teamId AND tm.user = :user")
    boolean isUserMemberOfTeam(@Param("teamId") Long teamId, @Param("user") User user);
    
    // 检查用户是否是团队所有者
    @Query("SELECT COUNT(t) > 0 FROM Team t WHERE t.id = :teamId AND t.owner = :user")
    boolean isUserOwnerOfTeam(@Param("teamId") Long teamId, @Param("user") User user);
}