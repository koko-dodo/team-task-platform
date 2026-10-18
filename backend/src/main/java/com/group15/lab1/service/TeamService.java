package com.group15.lab1.service;

import com.group15.lab1.entity.Team;
import com.group15.lab1.entity.TeamMember;
import com.group15.lab1.entity.TeamRole;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;
import org.springframework.lang.Nullable;

/**
 * 团队服务接口。
 */
public interface TeamService {

    /**
     * 创建团队。
     * 创建者自动成为 Owner。
     * 
     * @param userId   创建者 ID
     * @param teamName 团队名称
     * @return 创建后的团队
     */
    Team createTeam(@NotNull Long userId, @NotNull String teamName);

    /**
     * 获取当前用户所属的所有团队。
     * 
     * @param userId 用户 ID
     * @return 团队列表
     */
    List<Team> getTeamsByUserId(@NotNull Long userId);

    /**
     * 获取团队详情。
     * 
     * @param teamId 团队 ID
     * @return 团队信息
     * @throws TeamNotFoundException 如果团队不存在
     */
    Team getTeamById(@NotNull Long teamId);

    /**
     * 获取团队的所有成员。
     * 权限：团队内所有成员可查看
     * 
     * @param teamId 团队 ID
     * @return 成员列表
     * @throws TeamNotFoundException 如果团队不存在
     */
    List<TeamMember> getTeamMembers(@NotNull Long teamId);

    /**
     * 添加成员到团队。
     * 权限：仅 Owner 可操作
     * 
     * @param teamId     团队 ID
     * @param userId     被添加的用户 ID
     * @param role       成员角色（通常为 Member）
     * @param operatorId 操作者 ID（必须是 Owner）
     * @return 添加后的成员关系
     * @throws TeamNotFoundException       如果团队不存在
     * @throws ForbiddenOperationException 如果操作者不是 Owner
     * @throws ConflictException           如果用户已是团队成员
     */
    TeamMember addTeamMember(@NotNull Long teamId, @NotNull Long userId, @Nullable TeamRole role,
            @NotNull Long operatorId);

    /**
     * 修改团队成员的角色。
     * 权限：仅 Owner 可操作
     * 
     * @param teamId     团队 ID
     * @param memberId   被修改的成员用户 ID
     * @param newRole    新角色
     * @param operatorId 操作者 ID（必须是 Owner）
     * @return 修改后的成员关系
     * @throws TeamNotFoundException       如果团队不存在
     * @throws MemberNotFoundException     如果成员不存在
     * @throws ForbiddenOperationException 如果操作者不是 Owner 或尝试修改 Owner 角色
     */
    TeamMember updateMemberRole(@NotNull Long teamId, @NotNull Long memberId, @NotNull TeamRole newRole,
            @NotNull Long operatorId);

    /**
     * 移除团队成员。
     * 权限：仅 Owner 可操作，且不能移除 Owner 自身。
     *
     * @param teamId     团队 ID
     * @param memberId   被移除的成员用户 ID
     * @param operatorId 操作者 ID（必须是 Owner）
     * @throws TeamNotFoundException       如果团队不存在
     * @throws MemberNotFoundException     如果成员不存在
     * @throws ForbiddenOperationException 如果操作者不是 Owner 或尝试移除 Owner 自身
     */
    void removeTeamMember(@NotNull Long teamId, @NotNull Long memberId, @NotNull Long operatorId);

    /**
     * 成员主动离开团队。
     * 权限：普通成员或 Admin 可离开，Owner 不允许直接离开。
     *
     * @param teamId 团队 ID
     * @param userId 当前用户 ID
     */
    void leaveTeam(@NotNull Long teamId, @NotNull Long userId);

    /**
     * 转让团队所有权。
     * 权限：仅当前 Owner 可操作，目标必须是当前团队成员。
     *
     * @param teamId     团队 ID
     * @param newOwnerId 新 Owner 用户 ID
     * @param operatorId 当前 Owner 用户 ID
     * @return 更新后的团队
     */
    Team transferOwnership(@NotNull Long teamId, @NotNull Long newOwnerId, @NotNull Long operatorId);

    /**
     * 解散团队。
     * 仅 Owner 可操作，采用状态标记，不物理删除历史数据。
     *
     * @param teamId     团队 ID
     * @param operatorId 操作者 ID
     */
    void disbandTeam(@NotNull Long teamId, @NotNull Long operatorId);

    /**
     * 获取用户在某个团队中的角色（内部方法，Service 层权限检查用）。
     * 
     * @param teamId 团队 ID
     * @param userId 用户 ID
     * @return 用户的角色；若用户不是该团队成员则返回 {@code Optional.empty()}
     */
    Optional<TeamRole> getUserRoleInTeam(@NotNull Long teamId, @NotNull Long userId);
}
