package com.dbdoctor.controller;

import com.dbdoctor.check.MySqlEnvChecker;
import com.dbdoctor.common.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 环境检查 Controller
 *
 * <p>提供手动触发环境检查的 API</p>
 *
 * @author DB-Doctor
 * @version 2.4.0
 * @since 2.4.0
 */
@Slf4j
@RestController
@RequestMapping("/api/environment")
@RequiredArgsConstructor
public class EnvironmentCheckController {

    private final MySqlEnvChecker envChecker;

    /**
     * 手动触发环境检查
     *
     * @return 检查结果
     */
    @PostMapping("/check")
    public Result<Map<String, Object>> checkEnvironment() {
        log.info("[环境检查] 用户手动触发完整环境检查");

        try {
            // 执行完整的环境检查（生成详细报告）
            boolean isHealthy = envChecker.checkFully();

            // 获取诊断信息
            String diagnosticInfo = envChecker.getDiagnosticInfo();

            Map<String, Object> result = Map.of(
                "success", isHealthy,
                "message", isHealthy ? "环境检查通过" : "环境检查未通过，请根据建议进行配置",
                "diagnosticInfo", diagnosticInfo,
                "timestamp", System.currentTimeMillis()
            );

            if (isHealthy) {
                log.info("[环境检查] ✅ 检查通过");
                return Result.success(result);
            } else {
                log.warn("[环境检查] ❌ 检查未通过");
                return Result.<Map<String, Object>>error("环境配置需要优化");
            }

        } catch (Exception e) {
            log.error("[环境检查] 检查失败", e);
            return Result.error("环境检查失败: " + e.getMessage());
        }
    }

    /**
     * 快速检查（不修改配置）
     *
     * @return 检查结果
     */
    @GetMapping("/quick-check")
    public Result<Map<String, Object>> quickCheck() {
        log.info("[环境检查] 快速检查");

        try {
            boolean isHealthy = envChecker.checkQuickly();
            String diagnosticInfo = envChecker.getDiagnosticInfo();

            Map<String, Object> result = Map.of(
                "success", isHealthy,
                "message", isHealthy ? "环境健康" : "环境待优化",
                "diagnosticInfo", diagnosticInfo,
                "timestamp", System.currentTimeMillis()
            );

            return Result.success(result);

        } catch (Exception e) {
            log.error("[环境检查] 快速检查失败", e);
            return Result.error("快速检查失败: " + e.getMessage());
        }
    }
}
