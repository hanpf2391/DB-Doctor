package com.dbdoctor.monitoring.alert;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * 性能指标数据
 *
 * <p>封装各类性能指标数据，用于告警规则评估</p>
 *
 * @author DB-Doctor
 * @version 3.2.0
 * @since 3.2.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricsData {

    /**
     * 慢查询处理速率（QPS）
     */
    private Double slowQueryQps;

    /**
     * AI 分析平均耗时（秒）
     */
    private Double aiAnalysisDurationAvg;

    /**
     * AI 分析最大耗时（秒）
     */
    private Double aiAnalysisDurationMax;

    /**
     * AI 分析 P99 耗时（秒）
     */
    private Double aiAnalysisDurationP99;

    /**
     * 队列积压数量
     */
    private Integer queueBacklog;

    /**
     * 数据源连接状态（0=正常，1=失败）
     */
    private Integer datasourceStatus;

    /**
     * JVM 内存使用率（0-1）
     */
    private Double jvmMemoryUsage;

    /**
     * CPU 使用率（0-1）
     */
    private Double cpuUsage;

    /**
     * 其他自定义指标
     */
    @Builder.Default
    private Map<String, Object> customMetrics = new HashMap<>();

    /**
     * 获取指标值
     */
    public Double getMetricValue(String metricName) {
        return switch (metricName) {
            case "slowQueryQps" -> slowQueryQps;
            case "aiAnalysisDurationAvg" -> aiAnalysisDurationAvg;
            case "aiAnalysisDurationMax" -> aiAnalysisDurationMax;
            case "aiAnalysisDurationP99" -> aiAnalysisDurationP99;
            case "queueBacklog" -> queueBacklog != null ? queueBacklog.doubleValue() : null;
            case "datasourceStatus" -> datasourceStatus != null ? datasourceStatus.doubleValue() : null;
            case "jvmMemoryUsage" -> jvmMemoryUsage;
            case "cpuUsage" -> cpuUsage;
            default -> {
                Object value = customMetrics.get(metricName);
                yield value != null ? value instanceof Double ? (Double) value : Double.parseDouble(value.toString()) : null;
            }
        };
    }

    /**
     * 添加自定义指标
     */
    public void addCustomMetric(String name, Object value) {
        if (this.customMetrics == null) {
            this.customMetrics = new HashMap<>();
        }
        this.customMetrics.put(name, value);
    }

    /**
     * 判断指标是否存在
     */
    public boolean hasMetric(String metricName) {
        return getMetricValue(metricName) != null;
    }
}
