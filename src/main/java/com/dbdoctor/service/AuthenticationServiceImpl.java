package com.dbdoctor.service;

import com.dbdoctor.dto.ChangePasswordRequest;
import com.dbdoctor.dto.LoginRequest;
import com.dbdoctor.dto.LoginResponse;
import com.dbdoctor.entity.User;
import com.dbdoctor.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.util.UUID;

/**
 * 认证服务实现类
 *
 * <p>提供用户登录、密码修改、默认用户初始化等功能</p>
 *
 * @author DB-Doctor
 * @version 3.1.0
 * @since 3.1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private final UserRepository userRepository;

    @Override
    public LoginResponse login(LoginRequest request) {
        // 参数校验
        if (request == null || request.getUsername() == null || request.getPassword() == null) {
            throw new IllegalArgumentException("用户名或密码不能为空");
        }

        // 查询用户
        User user = userRepository.findByUsername(request.getUsername())
            .orElseThrow(() -> new IllegalArgumentException("用户名或密码错误"));

        // 验证密码
        String encryptedPassword = md5Encrypt(request.getPassword());
        if (!user.getPassword().equals(encryptedPassword)) {
            log.warn("[认证服务] 用户 {} 登录失败：密码错误", request.getUsername());
            throw new IllegalArgumentException("用户名或密码错误");
        }

        // 生成 token（简单实现，使用 UUID）
        String token = UUID.randomUUID().toString().replace("-", "");

        log.info("[认证服务] 用户 {} 登录成功", request.getUsername());

        return new LoginResponse(user.getUsername(), token);
    }

    @Override
    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        // 参数校验
        if (request == null ||
            request.getOldPassword() == null ||
            request.getNewPassword() == null ||
            request.getConfirmPassword() == null) {
            throw new IllegalArgumentException("密码不能为空");
        }

        // 检查新密码与确认密码是否一致
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("新密码与确认密码不一致");
        }

        // 查询用户
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        // 验证旧密码
        String encryptedOldPassword = md5Encrypt(request.getOldPassword());
        if (!user.getPassword().equals(encryptedOldPassword)) {
            log.warn("[认证服务] 用户 {} 修改密码失败：旧密码错误", username);
            throw new IllegalArgumentException("旧密码错误");
        }

        // 更新密码
        String encryptedNewPassword = md5Encrypt(request.getNewPassword());
        user.setPassword(encryptedNewPassword);

        // 如果提供了新用户名，则更新用户名
        String newUsername = request.getNewUsername();
        if (newUsername != null && !newUsername.trim().isEmpty()) {
            // 检查新用户名是否已被占用
            if (!newUsername.equals(username) && userRepository.existsByUsername(newUsername)) {
                throw new IllegalArgumentException("用户名已存在");
            }
            user.setUsername(newUsername.trim());
            log.info("[认证服务] 用户 {} 修改用户名为 {}", username, newUsername.trim());
        }

        userRepository.save(user);

        log.info("[认证服务] 用户 {} 密码修改成功", username);
    }

    @Override
    @Transactional
    public void initializeDefaultUser(String defaultUsername, String defaultPassword) {
        // 检查用户是否已存在
        if (userRepository.existsByUsername(defaultUsername)) {
            log.info("[认证服务] 默认用户 {} 已存在，跳过初始化", defaultUsername);
            return;
        }

        // 创建默认用户
        User user = new User();
        user.setUsername(defaultUsername);
        user.setPassword(md5Encrypt(defaultPassword));
        userRepository.save(user);

        log.info("[认证服务] 默认用户 {} 初始化成功（密码：{}）", defaultUsername, defaultPassword);
    }

    @Override
    public boolean userExists(String username) {
        return userRepository.existsByUsername(username);
    }

    /**
     * MD5 加密
     *
     * @param plainText 明文
     * @return MD5 密文（32位小写十六进制）
     */
    private String md5Encrypt(String plainText) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(plainText.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("[认证服务] MD5 加密失败", e);
            throw new RuntimeException("密码加密失败", e);
        }
    }
}
