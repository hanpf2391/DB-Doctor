package com.dbdoctor.controller;

import com.dbdoctor.common.Result;
import com.dbdoctor.monitoring.health.HealthCheckService;
import com.dbdoctor.monitoring.metrics.MetricsCollector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 监控 API 控制器
 *
 * <p>提供系统健康监控相关的 REST API</p>
 *
 * @author DB-Doctor
 * @version 3.2.0
 * @since 3.2.0
 */
@Slf4j
@RestController
@RequestMapping("/api/monitoring")
@RequiredArgsConstructor
public class MonitoringController {

    private final HealthCheckService healthCheckService;
    private final MetricsCollector metricsCollector;

    /**
     * 获取系统健康状态
     *
     * GET /api/monitoring/health
     *
     * @return 健康状态
     */
    @GetMapping("/health")
    public Result<Object> getHealthStatus() {
        log.info("[监控API] 查询系统健康状态");

        try {
            var healthCheckResult = healthCheckService.performHealthCheck();
            var systemInfo = healthCheckService.getSystemInfo();

            var data = Map.of(
                "checkTime", healthCheckResult.getCheckTime(),
                "status", healthCheckResult.getStatus(),
                "systemInfo", systemInfo,
                "indicators", healthCheckResult.getIndicators()
            );

            return Result.success("查询成功", data);
        } catch (Exception e) {
            log.error("[监控API] 查询系统健康状态失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 获取系统信息
     *
     * GET /api/monitoring/system-info
     *
     * @return 系统信息
     */
    @GetMapping("/system-info")
    public Result<Object> getSystemInfo() {
        log.info("[监控API] 查询系统信息");

        try {
            var systemInfo = healthCheckService.getSystemInfo();
            return Result.success("查询成功", systemInfo);
        } catch (Exception e) {
            log.error("[监控API] 查询系统信息失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 刷新健康检查
     *
     * POST /api/monitoring/health/refresh
     *
     * @return 最新的健康状态
     */
    @PostMapping("/health/refresh")
    public Result<Object> refreshHealthStatus() {
        log.info("[监控API] 刷新健康检查");

        try {
            var healthCheckResult = healthCheckService.performHealthCheck();
            return Result.success("刷新成功", healthCheckResult);
        } catch (Exception e) {
            log.error("[监控API] 刷新健康检查失败", e);
            return Result.error("刷新失败: " + e.getMessage());
        }
    }

    /**
     * 获取性能指标
     *
     * GET /api/monitoring/metrics
     *
     * @return 性能指标
     */
    @GetMapping("/metrics")
    public Result<Object> getMetrics() {
        log.info("[监控API] 查询性能指标");

        try {
            var metrics = metricsCollector.collectMetrics();

            // 构建慢查询指标数据
            var slowQueryMetrics = Map.of(
                "qps", metrics.getSlowQueryQps() != null ? metrics.getSlowQueryQps() : 0.0
            );

            // 构建 AI 分析指标数据
            var aiAnalysisMetrics = Map.of(
                "avgDuration", metrics.getAiAnalysisDurationAvg() != null ? metrics.getAiAnalysisDurationAvg() : 0.0,
                "maxDuration", metrics.getAiAnalysisDurationMax() != null ? metrics.getAiAnalysisDurationMax() : 0.0,
                "p99Duration", metrics.getAiAnalysisDurationP99() != null ? metrics.getAiAnalysisDurationP99() : 0.0
            );

            // 构建系统资源指标数据
            var systemResourceMetrics = Map.of(
                "cpuUsage", metrics.getCpuUsage() != null ? metrics.getCpuUsage() : 0.0,
                "memoryUsage", metrics.getJvmMemoryUsage() != null ? metrics.getJvmMemoryUsage() : 0.0
            );

            var data = Map.of(
                "slowQueryMetrics", slowQueryMetrics,
                "aiAnalysisMetrics", aiAnalysisMetrics,
                "systemResourceMetrics", systemResourceMetrics,
                "queueBacklog", metrics.getQueueBacklog() != null ? metrics.getQueueBacklog() : 0
            );

            return Result.success("查询成功", data);
        } catch (Exception e) {
            log.error("[监控API] 查询性能指标失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }
}
