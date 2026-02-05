package com.dbdoctor.config;

import com.dbdoctor.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 用户初始化配置
 *
 * <p>在应用启动时自动初始化默认用户</p>
 *
 * @author DB-Doctor
 * @version 3.1.0
 * @since 3.1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserInitializationConfig {

    private final AuthenticationService authenticationService;

    /**
     * 默认用户名（从配置文件读取）
     */
    @org.springframework.beans.factory.annotation.Value("${db-doctor.auth.default-username:admin}")
    private String defaultUsername;

    /**
     * 默认密码（从配置文件读取）
     */
    @org.springframework.beans.factory.annotation.Value("${db-doctor.auth.default-password:admin123}")
    private String defaultPassword;

    /**
     * 应用启动完成后初始化默认用户
     */
    @EventListener(ApplicationReadyEvent.class)
    @Order(1) // 确保在其他初始化之前执行
    public void initializeDefaultUser() {
        log.info("========================================");
        log.info("[用户初始化] 开始初始化默认用户...");
        log.info("[用户初始化] 默认用户名: {}", defaultUsername);
        log.info("[用户初始化] 默认密码: {}", defaultPassword);
        log.info("========================================");

        try {
            authenticationService.initializeDefaultUser(defaultUsername, defaultPassword);
            log.info("✅ [用户初始化] 默认用户初始化完成");
        } catch (Exception e) {
            log.error("❌ [用户初始化] 默认用户初始化失败", e);
        }
    }
}
