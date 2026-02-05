package com.dbdoctor.service;

import com.dbdoctor.dto.ChangePasswordRequest;
import com.dbdoctor.dto.LoginRequest;
import com.dbdoctor.dto.LoginResponse;
import com.dbdoctor.entity.User;
import com.dbdoctor.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 认证服务测试类
 *
 * <p>TDD 红色阶段：先编写测试，验证失败，然后实现</p>
 *
 * @author DB-Doctor
 * @version 3.1.0
 * @since 3.1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("认证服务测试")
class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;

    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        // 注意：这里会抛出异常，因为 AuthenticationServiceImpl 还不存在
        // 这是 TDD 红色阶段的预期行为
        try {
            authenticationService = new AuthenticationServiceImpl(userRepository);
        } catch (Exception e) {
            // 类不存在是正常的，TDD 红色阶段
        }
    }

    @Test
    @DisplayName("登录成功 - 正确的用户名和密码")
    void testLoginSuccess() {
        // Given
        LoginRequest request = new LoginRequest("admin", "admin123");
        User user = new User();
        user.setId(1L);
        user.setUsername("admin");
        user.setPassword("0192023a7bbd73250516f069df18b500"); // MD5 of "admin123"

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));

        // When
        LoginResponse response = authenticationService.login(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getUsername()).isEqualTo("admin");
        assertThat(response.getToken()).isNotEmpty();

        verify(userRepository, times(1)).findByUsername("admin");
    }

    @Test
    @DisplayName("登录失败 - 用户名不存在")
    void testLoginUserNotFound() {
        // Given
        LoginRequest request = new LoginRequest("notexist", "password");
        when(userRepository.findByUsername("notexist")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authenticationService.login(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("用户名或密码错误");

        verify(userRepository, times(1)).findByUsername("notexist");
    }

    @Test
    @DisplayName("登录失败 - 密码错误")
    void testLoginWrongPassword() {
        // Given
        LoginRequest request = new LoginRequest("admin", "wrongpassword");
        User user = new User();
        user.setId(1L);
        user.setUsername("admin");
        user.setPassword("0192023a7bbd73250516f069df18b500"); // MD5 of "admin123"

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));

        // When & Then
        assertThatThrownBy(() -> authenticationService.login(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("用户名或密码错误");

        verify(userRepository, times(1)).findByUsername("admin");
    }

    @Test
    @DisplayName("修改密码成功 - 正确的旧密码")
    void testChangePasswordSuccess() {
        // Given
        String username = "admin";
        ChangePasswordRequest request = new ChangePasswordRequest(
            "admin123",
            "newpass123",
            "newpass123"
        );

        User user = new User();
        user.setId(1L);
        user.setUsername("admin");
        user.setPassword("0192023a7bbd73250516f069df18b500"); // MD5 of "admin123"

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

        // When
        authenticationService.changePassword(username, request);

        // Then
        verify(userRepository, times(1)).findByUsername(username);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("修改密码失败 - 旧密码错误")
    void testChangePasswordWrongOldPassword() {
        // Given
        String username = "admin";
        ChangePasswordRequest request = new ChangePasswordRequest(
            "wrongold",
            "newpass123",
            "newpass123"
        );

        User user = new User();
        user.setId(1L);
        user.setUsername("admin");
        user.setPassword("0192023a7bbd73250516f069df18b500"); // MD5 of "admin123"

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

        // When & Then
        assertThatThrownBy(() -> authenticationService.changePassword(username, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("旧密码错误");

        verify(userRepository, times(1)).findByUsername(username);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("修改密码失败 - 新密码与确认密码不一致")
    void testChangePasswordMismatch() {
        // Given
        String username = "admin";
        ChangePasswordRequest request = new ChangePasswordRequest(
            "admin123",
            "newpass123",
            "different"
        );

        // When & Then
        assertThatThrownBy(() -> authenticationService.changePassword(username, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("新密码与确认密码不一致");

        verify(userRepository, never()).findByUsername(any());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("初始化默认用户 - 用户不存在时创建")
    void testInitializeDefaultUserWhenNotExists() {
        // Given
        when(userRepository.existsByUsername("admin")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        // When
        authenticationService.initializeDefaultUser("admin", "admin123");

        // Then
        verify(userRepository, times(1)).existsByUsername("admin");
        verify(userRepository, times(1)).save(argThat(u ->
            u.getUsername().equals("admin") &&
            u.getPassword().equals("0192023a7bbd73250516f069df18b500") // MD5 of "admin123"
        ));
    }

    @Test
    @DisplayName("初始化默认用户 - 用户已存在时不创建")
    void testInitializeDefaultUserWhenExists() {
        // Given
        when(userRepository.existsByUsername("admin")).thenReturn(true);

        // When
        authenticationService.initializeDefaultUser("admin", "admin123");

        // Then
        verify(userRepository, times(1)).existsByUsername("admin");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("检查用户是否存在 - 存在")
    void testUserExistsTrue() {
        // Given
        when(userRepository.existsByUsername("admin")).thenReturn(true);

        // When
        boolean exists = authenticationService.userExists("admin");

        // Then
        assertThat(exists).isTrue();
        verify(userRepository, times(1)).existsByUsername("admin");
    }

    @Test
    @DisplayName("检查用户是否存在 - 不存在")
    void testUserExistsFalse() {
        // Given
        when(userRepository.existsByUsername("notexist")).thenReturn(false);

        // When
        boolean exists = authenticationService.userExists("notexist");

        // Then
        assertThat(exists).isFalse();
        verify(userRepository, times(1)).existsByUsername("notexist");
    }

    /**
     * 测试辅助类：MD5 加密（用于测试数据准备）
     * 实际实现会在 AuthenticationServiceImpl 中提供
     */
    private String md5Encrypt(String plainText) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(plainText.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
