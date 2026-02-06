package com.dbdoctor.monitoring.alert;

import com.dbdoctor.entity.AlertHistory;
import com.dbdoctor.entity.AlertRule;
import com.dbdoctor.monitoring.websocket.AlertWebSocketHandler;
import com.dbdoctor.repository.AlertRuleRepository;
import com.dbdoctor.service.AlertHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 告警规则引擎
 *
 * <p>负责评估告警规则，触发告警，管理冷却期</p>
 *
 * @author DB-Doctor
 * @version 3.2.0
 * @since 3.2.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertRuleEngine {

    private final AlertRuleRepository alertRuleRepository;
    private final AlertHistoryService alertHistoryService;
    private final AlertWebSocketHandler webSocketHandler;

    /**
     * 最后告警时间缓存（规则ID -> 最后告警时间）
     */
    private final Map<Long, LocalDateTime> lastAlertTimeCache = new ConcurrentHashMap<>();

    /**
     * 评估告警规则
     *
     * @param metrics 性能指标数据
     */
    @Async("monitoringExecutor")
    public void evaluateRules(MetricsData metrics) {
        try {
            // 查询所有启用的告警规则
            List<AlertRule> rules = alertRuleRepository.findByEnabled(true);

            log.debug("[告警引擎] 开始评估告警规则，共 {} 条启用规则", rules.size());

            for (AlertRule rule : rules) {
                try {
                    if (shouldTriggerAlert(rule, metrics)) {
                        // 触发告警
                        triggerAlert(rule, metrics);
                    }
                } catch (Exception e) {
                    log.error("[告警引擎] 规则评估失败: {}", rule.getName(), e);
                }
            }
        } catch (Exception e) {
            log.error("[告警引擎] 告警规则评估失败", e);
        }
    }

    /**
     * 判断是否应该触发告警
     */
    private boolean shouldTriggerAlert(AlertRule rule, MetricsData metrics) {
        // 检查冷却期（避免告警风暴）
        if (isInCoolDownPeriod(rule)) {
            log.debug("[告警引擎] 规则在冷却期内，跳过: {}", rule.getName());
            return false;
        }

        // 获取指标值
        Double metricValue = metrics.getMetricValue(rule.getMetricName());
        if (metricValue == null) {
            log.debug("[告警引擎] 指标值不存在，跳过规则: {}, metric={}", rule.getName(), rule.getMetricName());
            return false;
        }

        // 根据规则类型判断
        return switch (rule.getType()) {
            case "THRESHOLD" -> evaluateThresholdRule(rule, metricValue);
            case "ANOMALY" -> evaluateAnomalyRule(rule, metricValue);
            case "TREND" -> evaluateTrendRule(rule, metricValue);
            default -> {
                log.warn("[告警引擎] 未知规则类型: {}", rule.getType());
                yield false;
            }
        };
    }

    /**
     * 评估阈值告警规则
     */
    private boolean evaluateThresholdRule(AlertRule rule, Double metricValue) {
        Double threshold = rule.getThresholdValue();
        if (threshold == null) {
            log.warn("[告警引擎] 阈值规则未配置阈值: {}", rule.getName());
            return false;
        }

        String operator = rule.getConditionOperator();
        boolean triggered = switch (operator) {
            case ">" -> metricValue > threshold;
            case ">=" -> metricValue >= threshold;
            case "<" -> metricValue < threshold;
            case "<=" -> metricValue <= threshold;
            case "=" -> metricValue.equals(threshold);
            default -> {
                log.warn("[告警引擎] 未知的比较操作符: {}", operator);
                yield false;
            }
        };

        if (triggered) {
            log.info("[告警引擎] 阈值告警触发: rule={}, metric={}, value={}, threshold={}, operator={}",
                    rule.getName(), rule.getMetricName(), metricValue, threshold, operator);
        }

        return triggered;
    }

    /**
     * 评估异常告警规则
     */
    private boolean evaluateAnomalyRule(AlertRule rule, Double metricValue) {
        // 异常检测：指标值等于预设的异常值（如 datasourceStatus = 1 表示连接失败）
        Double anomalyValue = rule.getThresholdValue();
        if (anomalyValue == null) {
            // 对于没有明确阈值的异常规则，任何非零值都视为异常
            boolean triggered = metricValue != 0;
            if (triggered) {
                log.info("[告警引擎] 异常告警触发: rule={}, metric={}, value={}",
                        rule.getName(), rule.getMetricName(), metricValue);
            }
            return triggered;
        }

        boolean triggered = metricValue.equals(anomalyValue);
        if (triggered) {
            log.info("[告警引擎] 异常告警触发: rule={}, metric={}, value={}, expected={}",
                    rule.getName(), rule.getMetricName(), metricValue, anomalyValue);
        }

        return triggered;
    }

    /**
     * 评估趋势告警规则
     */
    private boolean evaluateTrendRule(AlertRule rule, Double metricValue) {
        // TODO: 实现趋势检测逻辑
        // 趋势检测需要历史数据支持，可以通过查询 alert_history 表来实现
        log.debug("[告警引擎] 趋势告警规则暂未实现: {}", rule.getName());
        return false;
    }

    /**
     * 触发告警
     */
    private void triggerAlert(AlertRule rule, MetricsData metrics) {
        try {
            // 构建告警消息
            String message = buildAlertMessage(rule, metrics);

            // 构建指标数据（JSON格式）
            String metricsJson = buildMetricsJson(metrics);

            // 创建告警历史记录
            AlertHistory alert = AlertHistory.builder()
                .ruleId(rule.getId())
                .ruleName(rule.getDisplayName())
                .severity(rule.getSeverity())
                .status("FIRING")
                .message(message)
                .metrics(metricsJson)
                .triggeredAt(LocalDateTime.now())
                .build();

            alertHistoryService.saveAsync(alert);

            // WebSocket 实时推送告警
            try {
                webSocketHandler.broadcastAlert(alert);
            } catch (Exception e) {
                log.error("[告警引擎] WebSocket 广播失败: alertId={}", alert.getId(), e);
            }

            // 更新最后告警时间（用于冷却期判断）
            updateLastAlertTime(rule);

            log.warn("[告警引擎] 告警已触发: rule={}, severity={}, message={}",
                    rule.getName(), rule.getSeverity(), message);
        } catch (Exception e) {
            log.error("[告警引擎] 触发告警失败: rule={}", rule.getName(), e);
        }
    }

    /**
     * 构建告警消息
     */
    private String buildAlertMessage(AlertRule rule, MetricsData metrics) {
        Double metricValue = metrics.getMetricValue(rule.getMetricName());

        StringBuilder message = new StringBuilder();
        message.append("告警规则 [").append(rule.getDisplayName()).append("] 被触发\n");

        if (metricValue != null) {
            message.append("指标: ").append(rule.getMetricName())
                   .append(" = ").append(String.format("%.2f", metricValue));

            if (rule.getThresholdValue() != null) {
                message.append(" (阈值: ").append(rule.getThresholdValue())
                       .append(" ").append(rule.getConditionOperator()).append(")");
            }
        }

        if (rule.getDescription() != null) {
            message.append("\n说明: ").append(rule.getDescription());
        }

        return message.toString();
    }

    /**
     * 构建指标数据 JSON
     */
    private String buildMetricsJson(MetricsData metrics) {
        Map<String, Object> metricsMap = new HashMap<>();

        if (metrics.getSlowQueryQps() != null) {
            metricsMap.put("slowQueryQps", metrics.getSlowQueryQps());
        }
        if (metrics.getAiAnalysisDurationAvg() != null) {
            metricsMap.put("aiAnalysisDurationAvg", metrics.getAiAnalysisDurationAvg());
        }
        if (metrics.getAiAnalysisDurationMax() != null) {
            metricsMap.put("aiAnalysisDurationMax", metrics.getAiAnalysisDurationMax());
        }
        if (metrics.getQueueBacklog() != null) {
            metricsMap.put("queueBacklog", metrics.getQueueBacklog());
        }
        if (metrics.getDatasourceStatus() != null) {
            metricsMap.put("datasourceStatus", metrics.getDatasourceStatus());
        }
        if (metrics.getJvmMemoryUsage() != null) {
            metricsMap.put("jvmMemoryUsage", metrics.getJvmMemoryUsage());
        }

        // 使用简单的 JSON 格式
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : metricsMap.entrySet()) {
            if (!first) {
                json.append(",");
            }
            json.append("\"").append(entry.getKey()).append("\":")
                .append(entry.getValue());
            first = false;
        }
        json.append("}");

        return json.toString();
    }

    /**
     * 判断是否在冷却期内
     */
    private boolean isInCoolDownPeriod(AlertRule rule) {
        LocalDateTime lastAlertTime = lastAlertTimeCache.get(rule.getId());
        if (lastAlertTime == null) {
            return false;
        }

        int coolDownMinutes = rule.getCoolDownMinutes();
        LocalDateTime coolDownEndTime = lastAlertTime.plusMinutes(coolDownMinutes);

        return LocalDateTime.now().isBefore(coolDownEndTime);
    }

    /**
     * 更新最后告警时间
     */
    private void updateLastAlertTime(AlertRule rule) {
        lastAlertTimeCache.put(rule.getId(), LocalDateTime.now());
    }

    /**
     * 清理过期的最后告警时间缓存
     */
    public void cleanupLastAlertTimeCache() {
        LocalDateTime now = LocalDateTime.now();
        lastAlertTimeCache.entrySet().removeIf(entry -> {
            AlertRule rule = alertRuleRepository.findById(entry.getKey()).orElse(null);
            if (rule == null) {
                return true;
            }
            LocalDateTime coolDownEndTime = entry.getValue().plusMinutes(rule.getCoolDownMinutes());
            return now.isAfter(coolDownEndTime);
        });

        log.debug("[告警引擎] 清理最后告警时间缓存完成");
    }
}
