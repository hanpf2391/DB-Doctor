package com.dbdoctor.monitoring.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池健康指标检查器
 *
 * <p>检查AI分析和监控线程池的状态</p>
 *
 * @author DB-Doctor
 * @version 3.2.0
 * @since 3.2.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ThreadPoolHealthIndicator implements HealthIndicator {

    @Qualifier("analysisExecutor")
    private final Executor analysisExecutor;

    @Qualifier("monitoringExecutor")
    private final Executor monitoringExecutor;

    @Override
    public String getName() {
        return "threadPool";
    }

    @Override
    public String getDisplayName() {
        return "线程池";
    }

    @Override
    public IndicatorStatus check() {
        try {
            Map<String, Object> details = new HashMap<>();

            // 检查分析线程池
            Map<String, Object> analysisDetails = checkThreadPool(analysisExecutor, "AI分析");
            details.put("analysis", analysisDetails);

            // 检查监控线程池
            Map<String, Object> monitoringDetails = checkThreadPool(monitoringExecutor, "监控");
            details.put("monitoring", monitoringDetails);

            // 判断整体健康状态
            boolean analysisHealthy = (Boolean) analysisDetails.get("healthy");
            boolean monitoringHealthy = (Boolean) monitoringDetails.get("healthy");

            if (analysisHealthy && monitoringHealthy) {
                return IndicatorStatus.healthy("线程池运行正常", details);
            } else {
                return IndicatorStatus.unhealthy("部分线程池异常", details);
            }
        } catch (Exception e) {
            log.error("[健康检查] 线程池健康检查失败", e);

            Map<String, Object> details = new HashMap<>();
            details.put("error", e.getMessage());

            return IndicatorStatus.unhealthy("线程池检查失败: " + e.getMessage(), details);
        }
    }

    /**
     * 检查单个线程池
     */
    private Map<String, Object> checkThreadPool(Executor executor, String name) {
        Map<String, Object> details = new HashMap<>();

        try {
            ThreadPoolTaskExecutor taskExecutor = (ThreadPoolTaskExecutor) executor;
            ThreadPoolExecutor threadPool = taskExecutor.getThreadPoolExecutor();

            int activeCount = threadPool.getActiveCount();
            long completedTaskCount = threadPool.getCompletedTaskCount();
            int corePoolSize = threadPool.getCorePoolSize();
            int maximumPoolSize = threadPool.getMaximumPoolSize();
            int poolSize = threadPool.getPoolSize();
            int queueSize = threadPool.getQueue().size();
            int queueRemainingCapacity = threadPool.getQueue().remainingCapacity();

            // 计算队列使用率
            int queueCapacity = queueSize + queueRemainingCapacity;
            double queueUsageRate = queueCapacity > 0 ? (double) queueSize / queueCapacity * 100 : 0;

            // 计算活跃线程使用率
            double threadUsageRate = maximumPoolSize > 0 ? (double) activeCount / maximumPoolSize * 100 : 0;

            details.put("activeCount", activeCount);
            details.put("completedTaskCount", completedTaskCount);
            details.put("corePoolSize", corePoolSize);
            details.put("maxPoolSize", maximumPoolSize);
            details.put("poolSize", poolSize);
            details.put("queueSize", queueSize);
            details.put("queueCapacity", queueCapacity);
            details.put("queueUsageRate", String.format("%.2f%%", queueUsageRate));
            details.put("threadUsageRate", String.format("%.2f%%", threadUsageRate));

            // 判断健康状态
            boolean healthy = queueUsageRate < 90 && threadUsageRate < 90;
            details.put("healthy", healthy);
            details.put("status", healthy ? "UP" : "HIGH_LOAD");

            if (!healthy) {
                details.put("warning", queueUsageRate >= 90 ? "队列使用率过高" : "线程使用率过高");
            }

        } catch (Exception e) {
            log.error("[健康检查] {}线程池检查失败", name, e);
            details.put("healthy", false);
            details.put("status", "ERROR");
            details.put("error", e.getMessage());
        }

        return details;
    }
}
