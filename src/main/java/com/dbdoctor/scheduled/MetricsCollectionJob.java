package com.dbdoctor.scheduled;

import com.dbdoctor.config.MonitoringProperties;
import com.dbdoctor.monitoring.alert.AlertRuleEngine;
import com.dbdoctor.monitoring.alert.MetricsData;
import com.dbdoctor.monitoring.metrics.MetricsCollector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 性能指标采集定时任务
 *
 * <p>定期采集系统性能指标并触发告警评估</p>
 *
 * @author DB-Doctor
 * @version 3.2.0
 * @since 3.2.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "db-doctor.monitoring.metrics-collection.slow-query.enabled", havingValue = "true", matchIfMissing = true)
public class MetricsCollectionJob {

    private final MetricsCollector metricsCollector;
    private final AlertRuleEngine alertRuleEngine;
    private final MonitoringProperties monitoringProperties;

    /**
     * 定时采集慢查询指标
     *
     * 默认每 30 秒执行一次
     */
    @Scheduled(fixedDelayString = "${db-doctor.monitoring.metrics-collection.slow-query.interval-seconds:30}000")
    public void collectSlowQueryMetrics() {
        try {
            log.debug("[定时采集] 开始采集慢查询指标");

            // 采集性能指标
            MetricsData metrics = metricsCollector.collectMetrics();

            // 触发告警规则评估
            alertRuleEngine.evaluateRules(metrics);

            log.debug("[定时采集] 慢查询指标采集完成");
        } catch (Exception e) {
            log.error("[定时采集] 慢查询指标采集失败", e);
        }
    }

    /**
     * 定时采集 AI 分析指标
     *
     * 默认每 60 秒执行一次
     */
    @Scheduled(fixedDelayString = "${db-doctor.monitoring.metrics-collection.ai-analysis.interval-seconds:60}000")
    public void collectAiAnalysisMetrics() {
        try {
            log.debug("[定时采集] 开始采集 AI 分析指标");

            // 采集性能指标
            MetricsData metrics = metricsCollector.collectMetrics();

            // 触发告警规则评估
            alertRuleEngine.evaluateRules(metrics);

            log.debug("[定时采集] AI 分析指标采集完成");
        } catch (Exception e) {
            log.error("[定时采集] AI 分析指标采集失败", e);
        }
    }

    /**
     * 清理告警引擎的缓存
     *
     * 每小时执行一次
     */
    @Scheduled(fixedRate = 3600000)  // 1 小时
    public void cleanupAlertEngineCache() {
        try {
            log.debug("[定时采集] 清理告警引擎缓存");
            alertRuleEngine.cleanupLastAlertTimeCache();
        } catch (Exception e) {
            log.error("[定时采集] 清理告警引擎缓存失败", e);
        }
    }
}
