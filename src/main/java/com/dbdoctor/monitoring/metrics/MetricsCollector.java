package com.dbdoctor.monitoring.metrics;

import com.dbdoctor.monitoring.alert.MetricsData;
import com.dbdoctor.repository.SlowQuerySampleRepository;
import com.dbdoctor.repository.SlowQueryTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.time.LocalDateTime;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 性能指标采集器
 *
 * <p>采集系统各类性能指标，用于监控和告警</p>
 *
 * @author DB-Doctor
 * @version 3.2.0
 * @since 3.2.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MetricsCollector {

    private final SlowQueryTemplateRepository templateRepository;
    private final SlowQuerySampleRepository sampleRepository;

    // 线程池（通过注入获取）
    private final org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor analysisExecutor;
    private final org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor monitoringExecutor;

    /**
     * 采集所有性能指标
     *
     * @return 指标数据
     */
    public MetricsData collectMetrics() {
        try {
            MetricsData metrics = new MetricsData();

            // 采集慢查询指标
            collectSlowQueryMetrics(metrics);

            // 采集 AI 分析指标
            collectAiAnalysisMetrics(metrics);

            // 采集系统资源指标
            collectSystemResourceMetrics(metrics);

            // 采集队列指标
            collectQueueMetrics(metrics);

            log.debug("[指标采集] 性能指标采集完成: {}", metrics);

            return metrics;
        } catch (Exception e) {
            log.error("[指标采集] 性能指标采集失败", e);
            return new MetricsData();
        }
    }

    /**
     * 采集慢查询指标
     */
    private void collectSlowQueryMetrics(MetricsData metrics) {
        try {
            // 统计最近 1 分钟的慢查询数量
            LocalDateTime oneMinuteAgo = LocalDateTime.now().minusMinutes(1);

            // 计算每秒慢查询数量（QPS）
            // 这里简化处理，实际应该使用时间窗口统计
            long totalCount = templateRepository.count();
            long recentCount = templateRepository.findAll().stream()
                .filter(t -> t.getFirstSeenTime() != null && t.getFirstSeenTime().isAfter(oneMinuteAgo))
                .count();

            double qps = recentCount / 60.0;  // 每秒慢查询数

            metrics.setSlowQueryQps(qps);

            log.debug("[指标采集] 慢查询指标: qps={}", qps);
        } catch (Exception e) {
            log.error("[指标采集] 慢查询指标采集失败", e);
        }
    }

    /**
     * 采集 AI 分析指标
     */
    private void collectAiAnalysisMetrics(MetricsData metrics) {
        try {
            // TODO: 从 AiInvocationLog 表统计 AI 分析指标
            // 这里先返回默认值
            metrics.setAiAnalysisDurationAvg(0.0);
            metrics.setAiAnalysisDurationMax(0.0);
            metrics.setAiAnalysisDurationP99(0.0);

            log.debug("[指标采集] AI 分析指标: avg=0, max=0, p99=0");
        } catch (Exception e) {
            log.error("[指标采集] AI 分析指标采集失败", e);
        }
    }

    /**
     * 采集系统资源指标
     */
    private void collectSystemResourceMetrics(MetricsData metrics) {
        try {
            // JVM 内存使用率
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            double memoryUsage = maxMemory > 0 ? (double) usedMemory / maxMemory : 0;

            metrics.setJvmMemoryUsage(memoryUsage);

            // CPU 使用率
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
                double cpuUsage = ((com.sun.management.OperatingSystemMXBean) osBean).getSystemCpuLoad();
                if (cpuUsage < 0) {
                    cpuUsage = ((com.sun.management.OperatingSystemMXBean) osBean).getProcessCpuLoad();
                }
                metrics.setCpuUsage(cpuUsage >= 0 ? cpuUsage : 0);
            } else {
                metrics.setCpuUsage(0.0);
            }

            log.debug("[指标采集] 系统资源指标: memoryUsage={}, cpuUsage={}", memoryUsage, metrics.getCpuUsage());
        } catch (Exception e) {
            log.error("[指标采集] 系统资源指标采集失败", e);
        }
    }

    /**
     * 采集队列指标
     */
    private void collectQueueMetrics(MetricsData metrics) {
        try {
            // 获取分析线程池队列大小
            if (analysisExecutor != null) {
                ThreadPoolExecutor threadPool = analysisExecutor.getThreadPoolExecutor();
                int queueSize = threadPool.getQueue().size();
                metrics.setQueueBacklog(queueSize);
            } else {
                metrics.setQueueBacklog(0);
            }

            log.debug("[指标采集] 队列指标: backlog={}", metrics.getQueueBacklog());
        } catch (Exception e) {
            log.error("[指标采集] 队列指标采集失败", e);
        }
    }
}
