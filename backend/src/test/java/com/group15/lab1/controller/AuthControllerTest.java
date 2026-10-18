package com.group15.lab1.controller;

import com.group15.lab1.controller.dto.LoginRequest;
import com.group15.lab1.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void login_ValidRequest_Returns200() throws Exception {
        String username = "testUser";
        String password = "testPass123";

        AuthService.AuthTokens mockTokens = new AuthService.AuthTokens(
                "mock-access-token",
                "mock-refresh-token",
                "Bearer",
                3600L,
                1L,
                username
        );

        when(authService.login(anyString(), anyString())).thenReturn(mockTokens);

        LoginRequest request = new LoginRequest();
        request.setUsername(username);
        request.setPassword(password);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("mock-access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("mock-refresh-token"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").value(3600L))
                .andExpect(jsonPath("$.message").value("登录成功"))
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void login_EmptyRequest_Returns400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}