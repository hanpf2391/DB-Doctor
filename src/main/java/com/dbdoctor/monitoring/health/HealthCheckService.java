package com.dbdoctor.monitoring.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 健康检查服务
 *
 * <p>协调所有健康指标检查，提供系统整体健康状态</p>
 *
 * @author DB-Doctor
 * @version 3.2.0
 * @since 3.2.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HealthCheckService {

    private final List<HealthIndicator> healthIndicators;

    /**
     * 执行健康检查
     *
     * @return 健康检查结果
     */
    public HealthCheckResult performHealthCheck() {
        HealthCheckResult result = new HealthCheckResult();
        result.setCheckTime(LocalDateTime.now());
        result.setStatus("UP");

        Map<String, IndicatorStatus> indicators = new HashMap<>();

        // 执行所有健康指标检查
        for (HealthIndicator indicator : healthIndicators) {
            try {
                IndicatorStatus status = indicator.check();
                indicators.put(indicator.getName(), status);

                // 如果有指标不健康，整体状态降级
                if (!status.isHealthy()) {
                    result.setStatus("DEGRADED");
                }
            } catch (Exception e) {
                log.error("[健康检查] 指标检查失败: {}", indicator.getName(), e);

                // 创建失败状态
                IndicatorStatus failedStatus = IndicatorStatus.unhealthy(
                    "检查失败: " + e.getMessage()
                );
                indicators.put(indicator.getName(), failedStatus);

                // 有指标检查失败，整体状态降级
                result.setStatus("DOWN");
            }
        }

        result.setIndicators(indicators);
        return result;
    }

    /**
     * 获取系统信息
     *
     * @return 系统信息
     */
    public SystemInfo getSystemInfo() {
        SystemInfo info = new SystemInfo();

        // 获取运行时信息
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        info.setStartTime(java.util.Date.from(java.time.Instant.ofEpochMilli(runtimeMXBean.getStartTime())));
        info.setUptime(formatDuration(runtimeMXBean.getUptime()));
        info.setJavaVersion(System.getProperty("java.version"));
        info.setJavaVendor(System.getProperty("java.vendor"));
        info.setJvmName(runtimeMXBean.getVmName());
        info.setJvmVersion(runtimeMXBean.getVmVersion());

        // 获取 JVM 内存信息
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        JvmMemoryInfo memoryInfo = new JvmMemoryInfo();
        memoryInfo.setHeapMemoryUsed(formatBytes(memoryMXBean.getHeapMemoryUsage().getUsed()));
        memoryInfo.setHeapMemoryMax(formatBytes(memoryMXBean.getHeapMemoryUsage().getMax()));
        memoryInfo.setHeapMemoryCommitted(formatBytes(memoryMXBean.getHeapMemoryUsage().getCommitted()));
        memoryInfo.setNonHeapMemoryUsed(formatBytes(memoryMXBean.getNonHeapMemoryUsage().getUsed()));

        // 计算堆内存使用率
        long max = memoryMXBean.getHeapMemoryUsage().getMax();
        long used = memoryMXBean.getHeapMemoryUsage().getUsed();
        double heapUsagePercent = max > 0 ? (double) used / max * 100 : 0;
        memoryInfo.setHeapUsagePercent(String.format("%.2f%%", heapUsagePercent));

        info.setJvmMemory(memoryInfo);

        // 系统属性
        info.setOsName(System.getProperty("os.name"));
        info.setOsVersion(System.getProperty("os.version"));
        info.setOsArch(System.getProperty("os.arch"));
        info.setProcessors(Runtime.getRuntime().availableProcessors());

        return info;
    }

    /**
     * 格式化持续时间
     */
    private String formatDuration(long millis) {
        Duration duration = Duration.ofMillis(millis);
        long days = duration.toDays();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();

        if (days > 0) {
            return String.format("%d天%d小时%d分钟", days, hours, minutes);
        } else if (hours > 0) {
            return String.format("%d小时%d分钟", hours, minutes);
        } else if (minutes > 0) {
            return String.format("%d分钟%d秒", minutes, seconds);
        } else {
            return String.format("%d秒", seconds);
        }
    }

    /**
     * 格式化字节数
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }

    /**
     * 健康检查结果
     */
    @lombok.Data
    public static class HealthCheckResult {
        private LocalDateTime checkTime;
        private String status;  // UP, DEGRADED, DOWN
        private Map<String, IndicatorStatus> indicators;
    }

    /**
     * 系统信息
     */
    @lombok.Data
    public static class SystemInfo {
        private java.util.Date startTime;
        private String uptime;
        private String javaVersion;
        private String javaVendor;
        private String jvmName;
        private String jvmVersion;
        private JvmMemoryInfo jvmMemory;
        private String osName;
        private String osVersion;
        private String osArch;
        private Integer processors;
    }

    /**
     * JVM 内存信息
     */
    @lombok.Data
    public static class JvmMemoryInfo {
        private String heapMemoryUsed;
        private String heapMemoryMax;
        private String heapMemoryCommitted;
        private String nonHeapMemoryUsed;
        private String heapUsagePercent;
    }
}
