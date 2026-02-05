package com.dbdoctor.integration;

import com.dbdoctor.dto.ChangePasswordRequest;
import com.dbdoctor.dto.LoginRequest;
import com.dbdoctor.dto.LoginResponse;
import com.dbdoctor.entity.User;
import com.dbdoctor.repository.UserRepository;
import com.dbdoctor.service.AuthenticationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.*;

/**
 * 认证功能集成测试
 *
 * <p>测试完整的登录、修改密码流程</p>
 *
 * @author DB-Doctor
 * @version 3.1.0
 * @since 3.1.0
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("认证功能集成测试")
class AuthenticationIntegrationTest {

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        // 清理数据库
        userRepository.deleteAll();

        // 初始化默认用户
        authenticationService.initializeDefaultUser("admin", "admin123");
    }

    @Test
    @DisplayName("完整的登录流程 - 成功")
    void testCompleteLoginFlow() {
        // Given
        LoginRequest request = new LoginRequest("admin", "admin123");

        // When
        LoginResponse response = authenticationService.login(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getUsername()).isEqualTo("admin");
        assertThat(response.getToken()).isNotEmpty();
        assertThat(response.getLoginTime()).isNotNull();
    }

    @Test
    @DisplayName("完整的修改密码流程 - 成功")
    void testCompleteChangePasswordFlow() {
        // Given
        ChangePasswordRequest request = new ChangePasswordRequest(
            "admin123",
            "newpass123",
            "newpass123"
        );

        // When
        authenticationService.changePassword("admin", request);

        // Then - 验证旧密码无法登录
        LoginRequest oldLoginRequest = new LoginRequest("admin", "admin123");
        assertThatThrownBy(() -> authenticationService.login(oldLoginRequest))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("用户名或密码错误");

        // 验证新密码可以登录
        LoginRequest newLoginRequest = new LoginRequest("admin", "newpass123");
        LoginResponse response = authenticationService.login(newLoginRequest);
        assertThat(response).isNotNull();
        assertThat(response.getUsername()).isEqualTo("admin");
    }

    @Test
    @DisplayName("用户存在性检查 - 存在")
    void testUserExists() {
        // When
        boolean exists = authenticationService.userExists("admin");

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("用户存在性检查 - 不存在")
    void testUserNotExists() {
        // When
        boolean exists = authenticationService.userExists("notexist");

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("初始化默认用户 - 已存在时跳过")
    void testInitializeDefaultUserWhenExists() {
        // Given - 用户已在 setUp 中创建

        // When - 再次初始化
        authenticationService.initializeDefaultUser("admin", "differentpassword");

        // Then - 密码不应改变（因为用户已存在）
        LoginRequest request = new LoginRequest("admin", "admin123");
        LoginResponse response = authenticationService.login(request);
        assertThat(response).isNotNull();

        // 使用新密码应该登录失败
        LoginRequest wrongRequest = new LoginRequest("admin", "differentpassword");
        assertThatThrownBy(() -> authenticationService.login(wrongRequest))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
