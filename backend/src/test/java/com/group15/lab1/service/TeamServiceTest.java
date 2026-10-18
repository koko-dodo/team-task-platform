package com.group15.lab1.service;

import com.group15.lab1.entity.Team;
import com.group15.lab1.entity.TeamMember;
import com.group15.lab1.entity.TeamRole;
import com.group15.lab1.entity.TeamStatus;
import com.group15.lab1.entity.User;
import com.group15.lab1.exception.ConflictException;
import com.group15.lab1.exception.ForbiddenOperationException;
import com.group15.lab1.repository.TaskRepository;
import com.group15.lab1.repository.TeamMemberRepository;
import com.group15.lab1.repository.TeamRepository;
import com.group15.lab1.repository.UserRepository;
import com.group15.lab1.service.impl.TeamServiceImpl;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TaskService taskService;

    @InjectMocks
    private TeamServiceImpl teamServiceImpl;

    @Test
    void removeTeamMember_ClearsAssignmentsThroughTaskService() {
        Long teamId = 1L;
        Long memberId = 2L;
        Long ownerId = 3L;

        Team team = createTeam(teamId, ownerId);
        TeamMember ownerMember = createMember(team, ownerId, TeamRole.Owner);
        TeamMember targetMember = createMember(team, memberId, TeamRole.Member);

        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, ownerId)).thenReturn(Optional.of(ownerMember));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, memberId)).thenReturn(Optional.of(targetMember));

        teamServiceImpl.removeTeamMember(teamId, memberId, ownerId);

        verify(taskService).clearAssignmentsForDepartedMember(teamId, memberId, ownerId);
        verify(teamMemberRepository).delete(targetMember);
    }

    @Test
    void leaveTeam_NonOwner_SucceedsAndClearsAssignments() {
        Long teamId = 1L;
        Long userId = 2L;

        Team team = createTeam(teamId, 99L);
        TeamMember member = createMember(team, userId, TeamRole.Member);

        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId)).thenReturn(Optional.of(member));

        teamServiceImpl.leaveTeam(teamId, userId);

        verify(taskService).clearAssignmentsForDepartedMember(teamId, userId, userId);
        verify(teamMemberRepository).delete(member);
    }

    @Test
    void leaveTeam_Owner_ThrowsForbiddenOperationException() {
        Long teamId = 1L;
        Long ownerId = 2L;

        Team team = createTeam(teamId, ownerId);
        TeamMember ownerMember = createMember(team, ownerId, TeamRole.Owner);

        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, ownerId)).thenReturn(Optional.of(ownerMember));

        ForbiddenOperationException exception = assertThrows(ForbiddenOperationException.class,
                () -> teamServiceImpl.leaveTeam(teamId, ownerId));

        assertEquals("Owner 不能直接离开团队，请先转让所有权或解散团队", exception.getMessage());
        verify(taskService, never()).clearAssignmentsForDepartedMember(teamId, ownerId, ownerId);
        verify(teamMemberRepository, never()).delete(ownerMember);
    }

    @Test
    void disbandTeam_Owner_SetsStatusToDisbanded() {
        Long teamId = 1L;
        Long ownerId = 2L;

        Team team = createTeam(teamId, ownerId);
        TeamMember ownerMember = createMember(team, ownerId, TeamRole.Owner);

        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, ownerId)).thenReturn(Optional.of(ownerMember));

        teamServiceImpl.disbandTeam(teamId, ownerId);

        assertEquals(TeamStatus.DISBANDED.name(), team.getStatus());
        verify(teamRepository).save(team);
        verify(taskService).recordTeamDisbandedTaskChanges(teamId, ownerId);
    }

    @Test
    void disbandTeam_AlreadyDisbanded_ThrowsConflictException() {
        Long teamId = 1L;
        Long ownerId = 2L;

        Team team = createTeam(teamId, ownerId);
        team.setStatus(TeamStatus.DISBANDED.name());
        TeamMember ownerMember = createMember(team, ownerId, TeamRole.Owner);

        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, ownerId)).thenReturn(Optional.of(ownerMember));

        ConflictException exception = assertThrows(ConflictException.class,
                () -> teamServiceImpl.disbandTeam(teamId, ownerId));

        assertEquals("团队已解散", exception.getMessage());
        verify(teamRepository, never()).save(team);
    }

    @Test
    void transferOwnership_SucceedsAndUpdatesRoles() {
        Long teamId = 1L;
        Long ownerId = 2L;
        Long newOwnerId = 3L;

        Team team = createTeam(teamId, ownerId);
        TeamMember ownerMember = createMember(team, ownerId, TeamRole.Owner);
        TeamMember newOwnerMember = createMember(team, newOwnerId, TeamRole.Member);

        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, ownerId)).thenReturn(Optional.of(ownerMember));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, newOwnerId)).thenReturn(Optional.of(newOwnerMember));
        when(teamRepository.save(team)).thenReturn(team);

        Team updated = teamServiceImpl.transferOwnership(teamId, newOwnerId, ownerId);

        assertEquals(newOwnerId, updated.getOwner().getId());
        assertEquals(TeamRole.Admin, ownerMember.getRole());
        assertEquals(TeamRole.Owner, newOwnerMember.getRole());
        verify(teamMemberRepository).save(ownerMember);
        verify(teamMemberRepository).save(newOwnerMember);
        verify(teamRepository).save(team);
    }

    @Test
    void transferOwnership_ToSelf_ThrowsConflictException() {
        Long teamId = 1L;
        Long ownerId = 2L;

        Team team = createTeam(teamId, ownerId);
        TeamMember ownerMember = createMember(team, ownerId, TeamRole.Owner);

        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, ownerId)).thenReturn(Optional.of(ownerMember));

        ConflictException exception = assertThrows(ConflictException.class,
                () -> teamServiceImpl.transferOwnership(teamId, ownerId, ownerId));

        assertEquals("新 Owner 不能是当前 Owner 自己", exception.getMessage());
        verify(teamRepository, never()).save(team);
    }

    // ========================================
    // Lab3: 非 Owner 越权操作校验
    // ========================================

    @Test
    void removeTeamMember_NotOwner_ThrowsForbiddenOperationException() {
        Long teamId = 1L;
        Long memberId = 2L;
        Long nonOwnerId = 3L;

        Team team = createTeam(teamId, 99L);
        TeamMember nonOwnerMember = createMember(team, nonOwnerId, TeamRole.Member);

        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, nonOwnerId)).thenReturn(Optional.of(nonOwnerMember));

        ForbiddenOperationException exception = assertThrows(ForbiddenOperationException.class,
                () -> teamServiceImpl.removeTeamMember(teamId, memberId, nonOwnerId));

        assertEquals("仅 Owner 可进行此操作", exception.getMessage());
        verify(taskService, never()).clearAssignmentsForDepartedMember(teamId, memberId, nonOwnerId);
    }

    @Test
    void disbandTeam_NotOwner_ThrowsForbiddenOperationException() {
        Long teamId = 1L;
        Long nonOwnerId = 3L;

        Team team = createTeam(teamId, 99L);
        TeamMember nonOwnerMember = createMember(team, nonOwnerId, TeamRole.Admin);

        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, nonOwnerId)).thenReturn(Optional.of(nonOwnerMember));

        ForbiddenOperationException exception = assertThrows(ForbiddenOperationException.class,
                () -> teamServiceImpl.disbandTeam(teamId, nonOwnerId));

        assertEquals("仅 Owner 可进行此操作", exception.getMessage());
        verify(teamRepository, never()).save(team);
    }

    @Test
    void removeTeamMember_TryRemoveOwner_ThrowsForbiddenOperationException() {
        Long teamId = 1L;
        Long ownerId = 2L;

        Team team = createTeam(teamId, ownerId);
        TeamMember ownerMember = createMember(team, ownerId, TeamRole.Owner);

        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, ownerId)).thenReturn(Optional.of(ownerMember));

        ForbiddenOperationException exception = assertThrows(ForbiddenOperationException.class,
                () -> teamServiceImpl.removeTeamMember(teamId, ownerId, ownerId));

        assertEquals("不能移除团队 Owner", exception.getMessage());
        verify(taskService, never()).clearAssignmentsForDepartedMember(teamId, ownerId, ownerId);
    }

    @Test
    void leaveTeam_NotMember_ThrowsForbiddenOperationException() {
        Long teamId = 1L;
        Long userId = 999L;

        Team team = createTeam(teamId, 99L);

        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId)).thenReturn(Optional.empty());

        ForbiddenOperationException exception = assertThrows(ForbiddenOperationException.class,
                () -> teamServiceImpl.leaveTeam(teamId, userId));

        assertEquals("仅团队成员可执行离队操作", exception.getMessage());
        verify(taskService, never()).clearAssignmentsForDepartedMember(teamId, userId, userId);
    }

    @Test
    void transferOwnership_NotOwner_ThrowsForbiddenOperationException() {
        Long teamId = 1L;
        Long nonOwnerId = 3L;
        Long newOwnerId = 4L;

        Team team = createTeam(teamId, 99L);
        TeamMember nonOwnerMember = createMember(team, nonOwnerId, TeamRole.Member);

        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, nonOwnerId)).thenReturn(Optional.of(nonOwnerMember));

        ForbiddenOperationException exception = assertThrows(ForbiddenOperationException.class,
                () -> teamServiceImpl.transferOwnership(teamId, newOwnerId, nonOwnerId));

        assertEquals("仅 Owner 可进行此操作", exception.getMessage());
        verify(teamRepository, never()).save(team);
    }

    private Team createTeam(Long teamId, Long ownerId) {
        User owner = new User();
        owner.setId(ownerId);

        Team team = new Team();
        team.setId(teamId);
        team.setOwner(owner);
        return team;
    }

    private TeamMember createMember(Team team, Long userId, TeamRole role) {
        User user = new User();
        user.setId(userId);

        TeamMember member = new TeamMember();
        member.setTeam(team);
        member.setUser(user);
        member.setRole(role);
        return member;
    }
}
