package com.group15.lab1.controller;

import com.group15.lab1.controller.dto.AddTeamMemberRequest;
import com.group15.lab1.controller.dto.CreateTeamRequest;
import com.group15.lab1.controller.dto.TeamMemberResponse;
import com.group15.lab1.controller.dto.TeamResponse;
import com.group15.lab1.controller.dto.TransferOwnershipRequest;
import com.group15.lab1.controller.dto.UpdateMemberRoleRequest;
import com.group15.lab1.dto.ApiResponse;
import com.group15.lab1.entity.Team;
import com.group15.lab1.entity.TeamMember;
import com.group15.lab1.entity.TeamRole;
import com.group15.lab1.exception.ForbiddenOperationException;
import com.group15.lab1.exception.InvalidCredentialsException;
import com.group15.lab1.exception.TaskNotFoundException;
import com.group15.lab1.repository.UserRepository;
import com.group15.lab1.security.AuthConstants;
import com.group15.lab1.security.JwtTokenProvider;
import com.group15.lab1.service.TeamService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 团队管理控制器。
 * 
 * 处理团队创建、查询、成员管理等操作。
 * 所有请求需要有效的 JWT 认证令牌。
 */
@RestController
@RequestMapping("/api/teams")
public class TeamController {

    private final TeamService teamService;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    public TeamController(TeamService teamService, JwtTokenProvider jwtTokenProvider, UserRepository userRepository) {
        this.teamService = teamService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
    }

    /**
     * 创建团队。
     * 创建者自动成为 Owner。
     * 
     * @param authorization Authorization 请求头
     * @param request       团队创建请求
     * @return 创建后的团队信息
     */
    @PostMapping
    public ResponseEntity<ApiResponse<TeamResponse>> createTeam(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @Valid @RequestBody CreateTeamRequest request) {
        Long userId = extractUserIdFromAuthorizationHeader(authorization);
        Team team = teamService.createTeam(userId, request.getName());

        TeamResponse response = new TeamResponse(
                team.getId(),
                team.getName(),
                team.getOwner().getId(),
                team.getOwner().getUsername(),
                team.getStatus(),
                TeamRole.Owner, // 创建者自动为 Owner
                team.getCreatedAt(),
                team.getUpdatedAt());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("团队创建成功", response));
    }

    /**
     * 获取当前用户所属的所有团队列表。
     * 
     * @param authorization Authorization 请求头
     * @return 团队列表
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<TeamResponse>>> getTeamsByCurrentUser(
            @RequestHeader(name = "Authorization", required = false) String authorization) {
        Long userId = extractUserIdFromAuthorizationHeader(authorization);
        List<Team> teams = teamService.getTeamsByUserId(userId);

        List<TeamResponse> responses = teams.stream()
                .map(team -> {
                    TeamRole currentUserRole = resolveCurrentUserRole(team, userId);
                    int memberCount = teamService.getTeamMembers(team.getId()).size();
                    return new TeamResponse(
                            team.getId(),
                            team.getName(),
                            team.getOwner().getId(),
                            team.getOwner().getUsername(),
                            team.getStatus(),
                            currentUserRole,
                            team.getCreatedAt(),
                            team.getUpdatedAt(),
                            memberCount);
                })
                .toList();

        return ResponseEntity.ok(ApiResponse.success("获取用户团队列表成功", responses));
    }

    /**
     * 获取团队详情。
     * 权限：团队成员可查看
     * 
     * @param authorization Authorization 请求头
     * @param teamId        团队 ID
     * @return 团队信息
     */
    @GetMapping("/{teamId}")
    public ResponseEntity<ApiResponse<TeamResponse>> getTeamById(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable Long teamId) {
        Long userId = extractUserIdFromAuthorizationHeader(authorization);
        Team team = teamService.getTeamById(teamId);
        TeamRole currentUserRole = resolveCurrentUserRole(team, userId);
        if (currentUserRole == null) {
            throw new ForbiddenOperationException("仅团队成员可查看团队详情");
        }
        int memberCount = teamService.getTeamMembers(teamId).size();

        TeamResponse response = new TeamResponse(
                team.getId(),
                team.getName(),
                team.getOwner().getId(),
                team.getOwner().getUsername(),
                team.getStatus(),
                currentUserRole,
                team.getCreatedAt(),
                team.getUpdatedAt(),
                memberCount);

        return ResponseEntity.ok(ApiResponse.success("获取团队详情成功", response));
    }

    /**
     * 获取团队的所有成员列表。
     * 权限：团队成员可查看
     * 
     * @param authorization Authorization 请求头
     * @param teamId        团队 ID
     * @return 成员列表
     */
    @GetMapping("/{teamId}/members")
    public ResponseEntity<ApiResponse<List<TeamMemberResponse>>> getTeamMembers(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable Long teamId) {
        Long userId = extractUserIdFromAuthorizationHeader(authorization);
        Team team = teamService.getTeamById(teamId);
        TeamRole currentUserRole = resolveCurrentUserRole(team, userId);

        // 权限：仅团队成员可查看成员列表
        if (currentUserRole == null) {
            throw new ForbiddenOperationException("仅团队成员可查看成员列表");
        }

        List<TeamMember> members = teamService.getTeamMembers(teamId);

        List<TeamMemberResponse> responses = members.stream()
                .map(member -> new TeamMemberResponse(
                        member.getUser().getId(),
                        member.getUser().getUsername(),
                        member.getRole(),
                        member.getJoinedAt()))
                .toList();

        return ResponseEntity.ok(ApiResponse.success("获取团队成员列表成功", responses));
    }

    /**
     * 向团队添加成员。
     * 权限：仅 Owner 可操作
     * 
     * @param authorization Authorization 请求头
     * @param teamId        团队 ID
     * @param request       添加成员请求
     * @return 新添加的成员信息
     */
    @PostMapping("/{teamId}/members")
    public ResponseEntity<ApiResponse<TeamMemberResponse>> addTeamMember(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable Long teamId,
            @Valid @RequestBody AddTeamMemberRequest request) {
        Long operatorId = extractUserIdFromAuthorizationHeader(authorization);
        Long targetUserId = resolveTargetUserId(request);
        TeamRole role = request.getRole() == null ? TeamRole.Member : request.getRole();
        TeamMember member = teamService.addTeamMember(teamId, targetUserId, role, operatorId);

        TeamMemberResponse response = new TeamMemberResponse(
                member.getUser().getId(),
                member.getUser().getUsername(),
                member.getRole(),
                member.getJoinedAt());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("成员添加成功", response));
    }

    private TeamRole resolveCurrentUserRole(Team team, Long userId) {
        if (team.getOwner() != null && team.getOwner().getId().equals(userId)) {
            return TeamRole.Owner;
        }
        return teamService.getUserRoleInTeam(team.getId(), userId).orElse(null);
    }

    private Long resolveTargetUserId(AddTeamMemberRequest request) {
        Long userId = request.getUserId();
        String username = request.getUsername();

        if (userId == null && (username == null || username.isBlank())) {
            throw new IllegalArgumentException("userId 或 username 至少提供一个");
        }

        if (userId != null && (username == null || username.isBlank())) {
            return userRepository.findById(userId)
                    .map(user -> user.getId())
                    .orElseThrow(() -> new TaskNotFoundException("用户不存在: " + userId));
        }

        String normalizedUsername = username.trim();
        Long resolvedByUsername = userRepository.findByUsername(normalizedUsername)
                .map(user -> user.getId())
                .orElseThrow(() -> new TaskNotFoundException("用户不存在: " + normalizedUsername));

        if (userId == null) {
            return resolvedByUsername;
        }

        if (!userId.equals(resolvedByUsername)) {
            throw new IllegalArgumentException("userId 与 username 不匹配");
        }

        return resolvedByUsername;
    }

    /**
     * 修改团队成员的角色。
     * 权限：仅 Owner 可操作
     * 
     * @param authorization Authorization 请求头
     * @param teamId        团队 ID
     * @param memberId      成员用户 ID
     * @param request       修改角色请求
     * @return 更新后的成员信息
     */
    @PatchMapping("/{teamId}/members/{memberId}/role")
    public ResponseEntity<ApiResponse<TeamMemberResponse>> updateMemberRole(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable Long teamId,
            @PathVariable Long memberId,
            @Valid @RequestBody UpdateMemberRoleRequest request) {
        Long operatorId = extractUserIdFromAuthorizationHeader(authorization);
        TeamMember member = teamService.updateMemberRole(teamId, memberId, request.getRole(), operatorId);

        TeamMemberResponse response = new TeamMemberResponse(
                member.getUser().getId(),
                member.getUser().getUsername(),
                member.getRole(),
                member.getJoinedAt());

        return ResponseEntity.ok(ApiResponse.success("成员角色修改成功", response));
    }

    /**
     * 移除团队成员。
     * 权限：仅 Owner 可操作，且不能移除 Owner 自身。
     *
     * @param authorization Authorization 请求头
     * @param teamId        团队 ID
     * @param memberId      被移除的成员用户 ID
     * @return 204 No Content
     */
    @DeleteMapping("/{teamId}/members/{memberId}")
    public ResponseEntity<Void> removeTeamMember(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable Long teamId,
            @PathVariable Long memberId) {
        Long operatorId = extractUserIdFromAuthorizationHeader(authorization);
        teamService.removeTeamMember(teamId, memberId, operatorId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 当前成员主动离开团队。
     * Owner 不允许直接离开。
     *
     * @param authorization Authorization 请求头
     * @param teamId        团队 ID
     * @return 204 No Content
     */
    @DeleteMapping("/{teamId}/members/me")
    public ResponseEntity<Void> leaveTeam(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable Long teamId) {
        Long userId = extractUserIdFromAuthorizationHeader(authorization);
        teamService.leaveTeam(teamId, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 转让团队所有权。
     * 权限：仅当前 Owner 可操作。
     *
     * @param authorization Authorization 请求头
     * @param teamId        团队 ID
     * @param request       转让所有权请求
     * @return 更新后的团队信息
     */
    @PatchMapping("/{teamId}/ownership")
    public ResponseEntity<ApiResponse<TeamResponse>> transferOwnership(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable Long teamId,
            @Valid @RequestBody TransferOwnershipRequest request) {
        Long operatorId = extractUserIdFromAuthorizationHeader(authorization);
        Team team = teamService.transferOwnership(teamId, request.getNewOwnerId(), operatorId);
        TeamRole currentUserRole = resolveCurrentUserRole(team, operatorId);
        int memberCount = teamService.getTeamMembers(teamId).size();

        TeamResponse response = new TeamResponse(
                team.getId(),
                team.getName(),
                team.getOwner().getId(),
                team.getOwner().getUsername(),
                team.getStatus(),
                currentUserRole,
                team.getCreatedAt(),
                team.getUpdatedAt(),
                memberCount);

        return ResponseEntity.ok(ApiResponse.success("团队所有权转让成功", response));
    }

    /**
     * 解散团队。
     * 权限：仅 Owner 可操作。
     *
     * @param authorization Authorization 请求头
     * @param teamId        团队 ID
     * @return 204 No Content
     */
    @PatchMapping("/{teamId}/disband")
    public ResponseEntity<Void> disbandTeam(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable Long teamId) {
        Long userId = extractUserIdFromAuthorizationHeader(authorization);
        teamService.disbandTeam(teamId, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 从 Authorization 请求头中提取用户 ID。
     * 
     * @param authorization Authorization 请求头值
     * @return 用户 ID
     * @throws InvalidCredentialsException 如果令牌无效或过期
     */
    private Long extractUserIdFromAuthorizationHeader(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            throw new InvalidCredentialsException("缺少认证令牌");
        }

        String token = authorization;
        String prefix = AuthConstants.BEARER_PREFIX;
        if (authorization.startsWith(prefix)) {
            token = authorization.substring(prefix.length());
        }

        if (!jwtTokenProvider.validateToken(token) || jwtTokenProvider.isTokenExpired(token)) {
            throw new InvalidCredentialsException("令牌无效或已过期");
        }
        if (!AuthConstants.TOKEN_TYPE_ACCESS.equals(jwtTokenProvider.getTokenType(token))) {
            throw new InvalidCredentialsException("令牌类型错误");
        }

        return jwtTokenProvider.getUserIdFromToken(token);
    }
}
