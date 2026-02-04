package com.dbdoctor.controller;

import com.dbdoctor.common.Result;
import com.dbdoctor.model.EnvCheckReport;
import com.dbdoctor.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 环境检查 Controller
 *
 * <p>提供测试连接和环境检查的 API</p>
 *
 * @author DB-Doctor
 * @version 3.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/environment")
@RequiredArgsConstructor
public class EnvironmentCheckController {

    private final com.dbdoctor.check.MySqlEnvChecker envChecker;
    private final SystemConfigService configService;

    /**
     * 测试数据库连接（含完整环境检查）
     *
     * 前端传递配置参数进行测试
     *
     * @param config 配置参数
     * @return 环境检查报告
     */
    @PostMapping("/test-connection")
    public Result<EnvCheckReport> testConnection(@RequestBody Map<String, String> config) {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🔍 [测试连接] 收到测试请求");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        try {
            String url = config.get("url");
            String username = config.get("username");
            String password = config.get("password");

            if (url == null || url.trim().isEmpty()) {
                return Result.error("JDBC URL 不能为空");
            }
            if (username == null || username.trim().isEmpty()) {
                return Result.error("用户名不能为空");
            }
            if (password == null || password.trim().isEmpty()) {
                return Result.error("密码不能为空");
            }

            // 执行完整的环境检查
            EnvCheckReport report = envChecker.checkFully(url, username, password);

            // 根据检查结果返回
            if (report.isOverallPassed()) {
                log.info("✅ [测试连接] 环境检查全部通过");
                return Result.success(report);
            } else {
                log.warn("❌ [测试连接] 环境检查未通过");
                return Result.error("环境检查未通过，请修复问题后重试", report);
            }

        } catch (Exception e) {
            log.error("❌ [测试连接] 测试失败", e);
            return Result.error("测试连接失败: " + e.getMessage());
        }
    }

    /**
     * 使用当前 H2 中的配置进行环境检查
     *
     * @return 环境检查报告
     */
    @PostMapping("/check-current")
    public Result<EnvCheckReport> checkCurrentConfig() {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🔍 [环境检查] 检查当前配置");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        try {
            // 从 H2 读取配置
            String url = configService.getDecryptedValue("database.url");
            String username = configService.getDecryptedValue("database.username");
            String password = configService.getDecryptedValue("database.password");

            if (url == null || url.trim().isEmpty()) {
                return Result.error("H2 数据库中未找到数据库配置");
            }

            // 执行环境检查
            EnvCheckReport report = envChecker.checkFully(url, username, password);

            if (report.isOverallPassed()) {
                log.info("✅ [环境检查] 当前配置检查通过");
                return Result.success(report);
            } else {
                log.warn("❌ [环境检查] 当前配置检查未通过");
                return Result.error("环境检查未通过", report);
            }

        } catch (Exception e) {
            log.error("❌ [环境检查] 检查失败", e);
            return Result.error("环境检查失败: " + e.getMessage());
        }
    }
}
