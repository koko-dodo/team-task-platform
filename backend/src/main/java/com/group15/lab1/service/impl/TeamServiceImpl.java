package com.group15.lab1.service.impl;

import com.group15.lab1.entity.Team;
import com.group15.lab1.entity.TeamMember;
import com.group15.lab1.entity.TeamRole;
import com.group15.lab1.entity.TeamStatus;
import com.group15.lab1.entity.User;
import com.group15.lab1.exception.ConflictException;
import com.group15.lab1.exception.ForbiddenOperationException;
import com.group15.lab1.exception.MemberNotFoundException;
import com.group15.lab1.exception.TaskNotFoundException;
import com.group15.lab1.exception.TeamNotFoundException;
import com.group15.lab1.repository.TaskRepository;
import com.group15.lab1.repository.TeamMemberRepository;
import com.group15.lab1.repository.TeamRepository;
import com.group15.lab1.repository.UserRepository;
import com.group15.lab1.service.TaskService;
import com.group15.lab1.service.TeamService;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.lang.Nullable;
import org.springframework.transaction.annotation.Transactional;

/**
 * 团队服务实现。
 */
@Service
@Transactional
public class TeamServiceImpl implements TeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final TaskService taskService;

    public TeamServiceImpl(
            TeamRepository teamRepository,
            TeamMemberRepository teamMemberRepository,
            TaskRepository taskRepository,
            UserRepository userRepository,
            TaskService taskService) {
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.taskService = taskService;
    }

    @Override
    public Team createTeam(@NotNull Long userId, @NotNull String teamName) {
        // 1. 校验参数
        if (teamName == null || teamName.isBlank()) {
            throw new IllegalArgumentException("团队名称不能为空");
        }
        if (teamName.length() > 50) {
            throw new IllegalArgumentException("团队名称长度不能超过 50 个字符");
        }

        // 2. 获取创建者用户信息
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new TaskNotFoundException("用户不存在: " + userId));

        // 3. 创建团队（事务内）
        Team team = new Team(teamName.trim(), owner);
        Team savedTeam = teamRepository.save(team);

        // 4. 创建 Owner 成员关系（同事务）
        TeamMember ownerMember = new TeamMember(savedTeam, owner, TeamRole.Owner);
        teamMemberRepository.save(ownerMember);

        return savedTeam;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Team> getTeamsByUserId(@NotNull Long userId) {
        // 获取用户实体
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new TaskNotFoundException("用户不存在: " + userId));

        return teamRepository.findByUserRelated(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Team getTeamById(@NotNull Long teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new TeamNotFoundException("团队不存在: " + teamId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeamMember> getTeamMembers(@NotNull Long teamId) {
        // 验证团队存在
        getTeamById(teamId);
        // 获取该团队所有成员
        return teamMemberRepository.findAllByTeamId(teamId);
    }

    @Override
    public TeamMember addTeamMember(@NotNull Long teamId, @NotNull Long userId, @Nullable TeamRole role,
            @NotNull Long operatorId) {
        // 1. 验证团队存在
        Team team = getTeamById(teamId);
        assertTeamActive(team);

        if (role == null) {
            role = TeamRole.Member;
        }
        if (role == TeamRole.Owner) {
            throw new IllegalArgumentException("不能通过该接口添加 Owner 角色");
        }

        // 2. 验证操作者是 Owner
        assertOwner(teamId, operatorId);

        // 3. 验证用户存在
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new TaskNotFoundException("用户不存在: " + userId));

        // 4. 检查是否已是团队成员
        if (teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)) {
            throw new ConflictException("用户已是该团队成员");
        }

        // 5. 添加成员（事务内）
        TeamMember member = new TeamMember(team, user, role);
        return teamMemberRepository.save(member);
    }

    @Override
    public TeamMember updateMemberRole(@NotNull Long teamId, @NotNull Long memberId, @NotNull TeamRole newRole,
            @NotNull Long operatorId) {
        // 1. 验证团队存在
        Team team = getTeamById(teamId);
        assertTeamActive(team);

        if (newRole == null) {
            throw new IllegalArgumentException("新角色不能为空");
        }
        if (newRole == TeamRole.Owner) {
            throw new ForbiddenOperationException("不能将成员角色修改为 Owner");
        }

        // 2. 验证操作者是 Owner
        assertOwner(teamId, operatorId);

        // 3. 验证成员存在
        TeamMember member = teamMemberRepository.findByTeamIdAndUserId(teamId, memberId)
                .orElseThrow(() -> new MemberNotFoundException("成员不存在: " + memberId));

        // 4. 不允许修改 Owner 的角色
        if (member.getRole() == TeamRole.Owner && newRole != TeamRole.Owner) {
            throw new ForbiddenOperationException("不能修改 Owner 的角色");
        }

        // 5. 修改角色（事务内）
        member.setRole(newRole);
        return teamMemberRepository.save(member);
    }

    @Override
    public void removeTeamMember(@NotNull Long teamId, @NotNull Long memberId, @NotNull Long operatorId) {
        // 1. 验证团队存在
        Team team = getTeamById(teamId);
        assertTeamActive(team);

        // 2. 验证操作者是 Owner
        assertOwner(teamId, operatorId);

        // 3. 验证成员存在
        TeamMember member = teamMemberRepository.findByTeamIdAndUserId(teamId, memberId)
                .orElseThrow(() -> new MemberNotFoundException("成员不存在: " + memberId));

        // 4. 不允许移除 Owner 自身
        if (member.getRole() == TeamRole.Owner) {
            throw new ForbiddenOperationException("不能移除团队 Owner");
        }

        // 5. 解除该成员负责的团队任务，避免任务继续指向非团队成员
        taskService.clearAssignmentsForDepartedMember(teamId, memberId, operatorId);

        // 6. 删除成员（事务内）
        teamMemberRepository.delete(member);
    }

    @Override
    public void leaveTeam(@NotNull Long teamId, @NotNull Long userId) {
        Team team = getTeamById(teamId);
        assertTeamActive(team);

        TeamMember member = teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new ForbiddenOperationException("仅团队成员可执行离队操作"));

        if (member.getRole() == TeamRole.Owner) {
            throw new ForbiddenOperationException("Owner 不能直接离开团队，请先转让所有权或解散团队");
        }

        taskService.clearAssignmentsForDepartedMember(teamId, userId, userId);
        teamMemberRepository.delete(member);
    }

    @Override
    public Team transferOwnership(@NotNull Long teamId, @NotNull Long newOwnerId, @NotNull Long operatorId) {
        Team team = getTeamById(teamId);
        assertTeamActive(team);
        assertOwner(teamId, operatorId);

        if (operatorId.equals(newOwnerId)) {
            throw new ConflictException("新 Owner 不能是当前 Owner 自己");
        }

        TeamMember currentOwnerMember = teamMemberRepository.findByTeamIdAndUserId(teamId, operatorId)
                .orElseThrow(() -> new ForbiddenOperationException("仅 Owner 可进行此操作"));
        TeamMember newOwnerMember = teamMemberRepository.findByTeamIdAndUserId(teamId, newOwnerId)
                .orElseThrow(() -> new MemberNotFoundException("成员不存在: " + newOwnerId));

        team.setOwner(newOwnerMember.getUser());
        currentOwnerMember.setRole(TeamRole.Admin);
        newOwnerMember.setRole(TeamRole.Owner);

        teamMemberRepository.save(currentOwnerMember);
        teamMemberRepository.save(newOwnerMember);
        return teamRepository.save(team);
    }

    @Override
    public void disbandTeam(@NotNull Long teamId, @NotNull Long operatorId) {
        Team team = getTeamById(teamId);
        assertOwner(teamId, operatorId);

        if (TeamStatus.DISBANDED.name().equals(team.getStatus())) {
            throw new ConflictException("团队已解散");
        }

        team.setStatus(TeamStatus.DISBANDED.name());
        teamRepository.save(team);
        taskService.recordTeamDisbandedTaskChanges(teamId, operatorId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TeamRole> getUserRoleInTeam(@NotNull Long teamId, @NotNull Long userId) {
        return teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
                .map(TeamMember::getRole);
    }

    // ========== 私有权限校验方法 ==========

    /**
     * 检查用户是否是团队 Owner。
     * 
     * @param teamId 团队 ID
     * @param userId 用户 ID
     * @throws ForbiddenOperationException 如果用户不是 Owner
     */
    private void assertOwner(@NotNull Long teamId, @NotNull Long userId) {
        TeamRole role = getUserRoleInTeam(teamId, userId)
                .orElseThrow(() -> new ForbiddenOperationException("仅 Owner 可进行此操作"));
        if (role != TeamRole.Owner) {
            throw new ForbiddenOperationException("仅 Owner 可进行此操作");
        }
    }

    private void assertTeamActive(@NotNull Team team) {
        if (TeamStatus.DISBANDED.name().equals(team.getStatus())) {
            throw new ForbiddenOperationException("团队已解散，不能执行该操作");
        }
    }
}
