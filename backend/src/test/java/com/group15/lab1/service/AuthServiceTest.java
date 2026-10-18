package com.group15.lab1.service;

import com.group15.lab1.entity.RefreshToken;
import com.group15.lab1.entity.User;
import com.group15.lab1.exception.InvalidCredentialsException;
import com.group15.lab1.exception.ResourceAlreadyExistsException;
import com.group15.lab1.repository.RefreshTokenRepository;
import com.group15.lab1.repository.UserRepository;
import com.group15.lab1.security.JwtTokenProvider;
import com.group15.lab1.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    private final long accessTokenExpirationMs = 3600000L;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(
                userRepository,
                refreshTokenRepository,
                passwordEncoder,
                jwtTokenProvider,
                accessTokenExpirationMs
        );
    }

    @Test
    void login_ValidCredentials_ReturnsTokens() {
        String username = "testUser";
        String password = "testPass123";
        Long userId = 1L;
        String mockAccessToken = "mock-access-token";
        String mockRefreshToken = "mock-refresh-token";

        User mockUser = new User();
        mockUser.setId(userId);
        mockUser.setUsername(username);
        mockUser.setPasswordHash("encoded-password");

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches(password, mockUser.getPasswordHash())).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(userId, username)).thenReturn(mockAccessToken);
        when(jwtTokenProvider.generateRefreshToken(userId)).thenReturn(mockRefreshToken);
        when(jwtTokenProvider.getExpirationFromToken(mockRefreshToken)).thenReturn(new java.util.Date());
        doNothing().when(refreshTokenRepository).deleteByUserId(userId);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(new RefreshToken());

        AuthService.AuthTokens tokens = authService.login(username, password);

        assertNotNull(tokens);
        assertEquals(mockAccessToken, tokens.accessToken());
        assertEquals(mockRefreshToken, tokens.refreshToken());
        assertEquals(userId, tokens.userId());
        assertEquals(username, tokens.username());

        verify(userRepository).findByUsername(username);
        verify(passwordEncoder).matches(password, mockUser.getPasswordHash());
        verify(refreshTokenRepository).deleteByUserId(userId);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void login_WrongPassword_ThrowsException() {
        String username = "testUser";
        String inputPassword = "wrongPass";
        String correctEncodedPassword = "encoded-password";

        User mockUser = new User();
        mockUser.setUsername(username);
        mockUser.setPasswordHash(correctEncodedPassword);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches(inputPassword, correctEncodedPassword)).thenReturn(false);

        InvalidCredentialsException exception = assertThrows(InvalidCredentialsException.class, () -> {
            authService.login(username, inputPassword);
        });
        assertEquals("用户名或密码错误", exception.getMessage());
    }

    @Test
    void login_UserNotFound_ThrowsException() {
        String username = "notExists";

        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        InvalidCredentialsException exception = assertThrows(InvalidCredentialsException.class, () -> {
            authService.login(username, "any");
        });
        assertEquals("用户名或密码错误", exception.getMessage());
    }

    @Test
    void register_DuplicateUsername_ThrowsResourceAlreadyExistsException() {
        String username = "existingUser";
        String password = "TestPass123";
        String confirmPassword = "TestPass123";

        when(userRepository.existsByUsername(username)).thenReturn(true);

        ResourceAlreadyExistsException exception = assertThrows(ResourceAlreadyExistsException.class,
                () -> authService.register(username, password, confirmPassword));

        assertEquals("用户名已存在", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_PasswordMismatch_ThrowsIllegalArgumentException() {
        String username = "newUser";
        String password = "TestPass123";
        String confirmPassword = "DifferentPass456";

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> authService.register(username, password, confirmPassword));

        assertEquals("两次输入的密码不一致", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_BlankUsername_ThrowsIllegalArgumentException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> authService.register("   ", "TestPass123", "TestPass123"));

        assertEquals("用户名不能为空", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_Success() {
        String username = "newUser";
        String password = "TestPass123";
        String confirmPassword = "TestPass123";

        when(userRepository.existsByUsername(username)).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn("encoded-password");

        authService.register(username, password, confirmPassword);

        verify(userRepository).save(any(User.class));
    }
}