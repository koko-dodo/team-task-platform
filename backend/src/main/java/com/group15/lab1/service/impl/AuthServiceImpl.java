package com.group15.lab1.service.impl;

import com.group15.lab1.dto.UserDto;
import com.group15.lab1.entity.RefreshToken;
import com.group15.lab1.entity.User;
import com.group15.lab1.exception.InvalidCredentialsException;
import com.group15.lab1.exception.ResourceAlreadyExistsException;
import com.group15.lab1.repository.RefreshTokenRepository;
import com.group15.lab1.repository.UserRepository;
import com.group15.lab1.security.AuthConstants;
import com.group15.lab1.security.JwtTokenProvider;
import com.group15.lab1.service.AuthService;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


/**
 * 认证服务实现类，封装注册、登录和令牌刷新等认证流程。
 */
@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private static final long MILLIS_PER_SECOND = 1000L;

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final long accessTokenExpirationMs;

    public AuthServiceImpl(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            @Value("${app.jwt.expiration}") long accessTokenExpirationMs) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.accessTokenExpirationMs = accessTokenExpirationMs;
    }

    @Override
    public void register(@NotNull String username, @NotNull String password, @NotNull String confirmPassword) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("密码不能为空");
        }
        if (!password.equals(confirmPassword)) {
            throw new IllegalArgumentException("两次输入的密码不一致");
        }
        if (userRepository.existsByUsername(username)) {
            throw new ResourceAlreadyExistsException("用户名已存在");
        }

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        userRepository.save(user);
    }

    @Override
    public AuthTokens login(@NotNull String username, @NotNull String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new InvalidCredentialsException("用户名或密码错误"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new InvalidCredentialsException("用户名或密码错误");
        }

        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        // Keep latest refresh token for each user to simplify invalidation.
        refreshTokenRepository.deleteByUserId(user.getId());
        refreshTokenRepository.save(new RefreshToken(
                user.getId(),
                refreshToken,
                toLocalDateTime(jwtTokenProvider.getExpirationFromToken(refreshToken))));

        return new AuthTokens(
                accessToken,
                refreshToken,
                AuthConstants.TOKEN_SCHEME_BEARER,
                accessTokenExpirationMs / MILLIS_PER_SECOND,
                user.getId(),
                user.getUsername());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean validateToken(@NotNull String token) {
        String normalizedToken = normalizeBearerToken(token);
        if (!jwtTokenProvider.validateToken(normalizedToken)) {
            return false;
        }
        return !jwtTokenProvider.isTokenExpired(normalizedToken);
    }

    @Override
    public AuthTokens refreshAccessToken(@NotNull String refreshToken) {
        String normalizedToken = normalizeBearerToken(refreshToken);
        if (!jwtTokenProvider.validateToken(normalizedToken)) {
            throw new InvalidCredentialsException("刷新令牌无效或已过期");
        }
        if (!AuthConstants.TOKEN_TYPE_REFRESH.equals(jwtTokenProvider.getTokenType(normalizedToken))) {
            throw new InvalidCredentialsException("令牌类型错误");
        }

        Optional<RefreshToken> refreshTokenInDb = refreshTokenRepository.findByToken(normalizedToken);
        if (refreshTokenInDb.isEmpty() || refreshTokenInDb.get().isExpired()) {
            throw new InvalidCredentialsException("刷新令牌无效或已过期");
        }

        Long userId = jwtTokenProvider.getUserIdFromToken(normalizedToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        String newAccessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername());

        return new AuthTokens(
                newAccessToken,
                normalizedToken,
                AuthConstants.TOKEN_SCHEME_BEARER,
                accessTokenExpirationMs / MILLIS_PER_SECOND,
                user.getId(),
                user.getUsername());
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getCurrentUser(@NotNull String token) {
        String normalizedToken = normalizeBearerToken(token);
        if (!validateToken(normalizedToken)) {
            throw new InvalidCredentialsException("令牌无效或已过期");
        }

        Long userId = jwtTokenProvider.getUserIdFromToken(normalizedToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        return new UserDto(
                user.getId(),
                user.getUsername(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                null);
    }

    private String normalizeBearerToken(@NotNull String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("令牌不能为空");
        }
        String prefix = AuthConstants.BEARER_PREFIX;
        if (token.startsWith(prefix)) {
            return token.substring(prefix.length());
        }
        return token;
    }

    private LocalDateTime toLocalDateTime(@NotNull java.util.Date date) {
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }
}