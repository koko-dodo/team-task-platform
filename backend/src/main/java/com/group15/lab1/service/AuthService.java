package com.group15.lab1.service;

import com.group15.lab1.dto.UserDto;
import jakarta.validation.constraints.NotNull;

/**
 * 认证服务接口
 * 处理用户注册和登录业务
 */
public interface AuthService {

    void register(@NotNull String username, @NotNull String password, @NotNull String confirmPassword);

    AuthTokens login(@NotNull String username, @NotNull String password);

    boolean validateToken(@NotNull String token);

    AuthTokens refreshAccessToken(@NotNull String refreshToken);

    UserDto getCurrentUser(@NotNull String token);

    /**
     * Service layer return model for auth-related token response.
     */
    record AuthTokens(
            String accessToken,
            String refreshToken,
            String tokenType,
            long expiresIn,
            Long userId,
            String username) {
    }
}