package com.group15.lab1.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group15.lab1.controller.dto.CreateTeamRequest;
import com.group15.lab1.controller.dto.TransferOwnershipRequest;
import com.group15.lab1.entity.Team;
import com.group15.lab1.entity.TeamRole;
import com.group15.lab1.entity.TeamStatus;
import com.group15.lab1.entity.User;
import com.group15.lab1.exception.ConflictException;
import com.group15.lab1.exception.ForbiddenOperationException;
import com.group15.lab1.repository.UserRepository;
import com.group15.lab1.security.AuthConstants;
import com.group15.lab1.security.JwtTokenProvider;
import com.group15.lab1.service.TeamService;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TeamController.class)
class TeamControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TeamService teamService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void createTeam_ReturnsCreatedWithStatus() throws Exception {
        Long userId = 1L;
        String token = "valid-token";

        mockValidAccessToken(token, userId);

        User owner = new User();
        owner.setId(userId);
        owner.setUsername("owner");

        Team team = new Team();
        team.setId(10L);
        team.setName("Dev Team");
        team.setOwner(owner);
        team.setStatus(TeamStatus.ACTIVE.name());
        team.setCreatedAt(LocalDate.of(2026, 5, 14));
        team.setUpdatedAt(LocalDate.of(2026, 5, 14));

        CreateTeamRequest request = new CreateTeamRequest();
        request.setName("Dev Team");

        when(teamService.createTeam(userId, "Dev Team")).thenReturn(team);

        mockMvc.perform(post("/api/teams")
                        .header("Authorization", AuthConstants.BEARER_PREFIX + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(10L))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.currentUserRole").value(TeamRole.Owner.name()));
    }

    @Test
    void leaveTeam_Returns204() throws Exception {
        Long teamId = 10L;
        Long userId = 1L;
        String token = "valid-token";

        mockValidAccessToken(token, userId);

        mockMvc.perform(delete("/api/teams/{teamId}/members/me", teamId)
                        .header("Authorization", AuthConstants.BEARER_PREFIX + token))
                .andExpect(status().isNoContent());

        verify(teamService).leaveTeam(teamId, userId);
    }

    @Test
    void disbandTeam_Returns204() throws Exception {
        Long teamId = 10L;
        Long userId = 1L;
        String token = "valid-token";

        mockValidAccessToken(token, userId);

        mockMvc.perform(patch("/api/teams/{teamId}/disband", teamId)
                        .header("Authorization", AuthConstants.BEARER_PREFIX + token))
                .andExpect(status().isNoContent());

        verify(teamService).disbandTeam(teamId, userId);
    }

    @Test
    void transferOwnership_Returns200() throws Exception {
        Long teamId = 10L;
        Long userId = 1L;
        Long newOwnerId = 2L;
        String token = "valid-token";

        mockValidAccessToken(token, userId);

        User newOwner = new User();
        newOwner.setId(newOwnerId);
        newOwner.setUsername("new-owner");

        Team team = new Team();
        team.setId(teamId);
        team.setName("Dev Team");
        team.setOwner(newOwner);
        team.setStatus(TeamStatus.ACTIVE.name());
        team.setCreatedAt(LocalDate.of(2026, 5, 14));
        team.setUpdatedAt(LocalDate.of(2026, 5, 19));

        TransferOwnershipRequest request = new TransferOwnershipRequest();
        request.setNewOwnerId(newOwnerId);

        when(teamService.transferOwnership(teamId, newOwnerId, userId)).thenReturn(team);
        when(teamService.getTeamMembers(teamId)).thenReturn(java.util.List.of());

        mockMvc.perform(patch("/api/teams/{teamId}/ownership", teamId)
                        .header("Authorization", AuthConstants.BEARER_PREFIX + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ownerId").value(newOwnerId))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void removeTeamMember_NotOwner_Returns403() throws Exception {
        Long teamId = 10L;
        Long targetMemberId = 2L;
        Long nonOwnerId = 3L;
        String token = "valid-token";

        mockValidAccessToken(token, nonOwnerId);
        doThrow(new ForbiddenOperationException("仅 Owner 可进行此操作"))
                .when(teamService).removeTeamMember(teamId, targetMemberId, nonOwnerId);

        mockMvc.perform(delete("/api/teams/{teamId}/members/{memberId}", teamId, targetMemberId)
                        .header("Authorization", AuthConstants.BEARER_PREFIX + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("仅 Owner 可进行此操作"));
    }

    @Test
    void disbandTeam_AlreadyDisbanded_Returns409() throws Exception {
        Long teamId = 10L;
        Long ownerId = 1L;
        String token = "valid-token";

        mockValidAccessToken(token, ownerId);
        doThrow(new ConflictException("团队已解散"))
                .when(teamService).disbandTeam(teamId, ownerId);

        mockMvc.perform(patch("/api/teams/{teamId}/disband", teamId)
                        .header("Authorization", AuthConstants.BEARER_PREFIX + token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("团队已解散"));
    }

    @Test
    void removeTeamMember_Success_Returns204() throws Exception {
        Long teamId = 10L;
        Long ownerId = 1L;
        Long targetMemberId = 2L;
        String token = "valid-token";

        mockValidAccessToken(token, ownerId);

        mockMvc.perform(delete("/api/teams/{teamId}/members/{memberId}", teamId, targetMemberId)
                        .header("Authorization", AuthConstants.BEARER_PREFIX + token))
                .andExpect(status().isNoContent());

        verify(teamService).removeTeamMember(teamId, targetMemberId, ownerId);
    }

    private void mockValidAccessToken(String token, Long userId) {
        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.isTokenExpired(token)).thenReturn(false);
        when(jwtTokenProvider.getTokenType(token)).thenReturn(AuthConstants.TOKEN_TYPE_ACCESS);
        when(jwtTokenProvider.getUserIdFromToken(token)).thenReturn(userId);
    }
}
