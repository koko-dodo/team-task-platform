package com.group15.lab1.controller;

import com.group15.lab1.controller.dto.LoginRequest;
import com.group15.lab1.controller.dto.LoginResponse;
import com.group15.lab1.controller.dto.RegisterRequest;
import com.group15.lab1.controller.dto.UserResponse;
import com.group15.lab1.dto.ApiResponse;
import com.group15.lab1.dto.UserDto;
import com.group15.lab1.service.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证控制器，负责注册、登录与令牌相关接口。
 */
@Validated
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 注册新用户账号。
     *
     * @param request 用户注册信息
     * @return 注册结果
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request.getUsername(), request.getPassword(), request.getConfirmPassword());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("注册成功", null));
    }

    /**
     * 使用用户名和密码登录，返回访问令牌和刷新令牌。
     *
     * @param request 登录请求
     * @return 登录结果与令牌信息
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthService.AuthTokens tokens = authService.login(request.getUsername(), request.getPassword());
        LoginResponse response = toLoginResponse(tokens, "登录成功");
        return ResponseEntity.ok(ApiResponse.success("登录成功", response));
    }

    /**
     * 刷新访问令牌，优先使用 body 中的刷新令牌，否则回退到 Authorization 请求头。
     *
     * @param request       刷新请求体
     * @param authorization 可选 Authorization 头
     * @return 新的访问令牌信息
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(
            @RequestBody TokenRequest request,
            @RequestHeader(name = "Authorization", required = false) String authorization) {
        String refreshToken = request != null ? request.getRefreshToken() : null;
        if ((refreshToken == null || refreshToken.isBlank()) && authorization != null && !authorization.isBlank()) {
            refreshToken = authorization;
        }

        AuthService.AuthTokens tokens = authService.refreshAccessToken(refreshToken);
        LoginResponse response = toLoginResponse(tokens, "令牌刷新成功");
        return ResponseEntity.ok(ApiResponse.success("令牌刷新成功", response));
    }

    /**
     * 校验访问令牌是否有效。
     *
     * @param authorization Authorization 请求头
     * @return 令牌有效性结果
     */
    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<Boolean>> validate(
            @RequestHeader("Authorization") @NotBlank String authorization) {
        boolean valid = authService.validateToken(authorization);
        if (!valid) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.fail(HttpStatus.UNAUTHORIZED.value(), "令牌无效或已过期"));
        }
        return ResponseEntity.ok(ApiResponse.success("令牌有效", true));
    }

    /**
     * 获取当前登录用户基本信息。
     *
     * @param authorization Authorization 请求头
     * @return 当前用户信息
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> me(
            @RequestHeader("Authorization") @NotBlank String authorization) {
        UserDto currentUser = authService.getCurrentUser(authorization);
        UserResponse response = new UserResponse(
                currentUser.getId(),
                currentUser.getUsername(),
                currentUser.getCreatedAt());
        return ResponseEntity.ok(ApiResponse.success("获取当前用户成功", response));
    }

    private LoginResponse toLoginResponse(AuthService.AuthTokens tokens, String message) {
        UserResponse user = new UserResponse(tokens.userId(), tokens.username(), null);
        return new LoginResponse(
                tokens.accessToken(),
                tokens.refreshToken(),
                tokens.tokenType(),
                tokens.expiresIn(),
                user,
                message);
    }

    public static class TokenRequest {
        private String refreshToken;

        public String getRefreshToken() {
            return refreshToken;
        }

        public void setRefreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
        }
    }
}