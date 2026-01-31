package com.dbdoctor.model;

import lombok.Data;

import java.util.Map;

/**
 * AI 监控统计数据 DTO
 *
 * <p>用于返回 AI 监控大盘的统计数据</p>
 *
 * @author DB-Doctor
 * @version 2.3.0
 * @since 2.3.0
 */
@Data
public class AiMonitorStats {

    /**
     * 总调用次数
     */
    private Long totalCalls;

    /**
     * 成功调用次数
     */
    private Long successCount;

    /**
     * 成功率（百分比，0-100）
     */
    private Double successRate;

    /**
     * 平均耗时（毫秒）
     */
    private Long avgDuration;

    /**
     * 最大耗时（毫秒）
     */
    private Long maxDuration;

    /**
     * 最小耗时（毫秒）
     */
    private Long minDuration;

    /**
     * 总 Token 消耗
     */
    private Long totalTokens;

    /**
     * 输入 Token 总数
     */
    private Long inputTokens;

    /**
     * 输出 Token 总数
     */
    private Long outputTokens;

    /**
     * 各 Agent 的 Token 分布
     * Key: Agent 代码（DIAGNOSIS/REASONING/CODING）
     * Value: Token 总数
     */
    private Map<String, Long> agentTokenDistribution;

    /**
     * 各 Agent 的调用次数分布
     * Key: Agent 代码（DIAGNOSIS/REASONING/CODING）
     * Value: 调用次数
     */
    private Map<String, Long> agentCallDistribution;

    /**
     * 按小时的调用次数（用于趋势图）
     * Key: 小时（0-23）
     * Value: 调用次数
     */
    private Map<Integer, Long> hourlyCallCount;

    /**
     * 统计时间范围
     */
    private String timeRange;

    /**
     * 计算 Token 使用率（输出/输入）
     *
     * @return Token 使用率
     */
    public double getTokenUsageRatio() {
        if (inputTokens == null || inputTokens == 0) {
            return 0.0;
        }
        if (outputTokens == null || outputTokens == 0) {
            return 0.0;
        }
        return (double) outputTokens / inputTokens;
    }

    /**
     * 获取格式化的成功率字符串
     *
     * @return 成功率字符串（如 "99.5%"）
     */
    public String getSuccessRateFormatted() {
        if (successRate == null) {
            return "N/A";
        }
        return String.format("%.2f%%", successRate);
    }

    /**
     * 获取格式化的 Token 总数
     *
     * @return 格式化的 Token 数（如 "1.5M"）
     */
    public String getTotalTokensFormatted() {
        if (totalTokens == null) {
            return "N/A";
        }

        if (totalTokens >= 1_000_000) {
            return String.format("%.1fM", totalTokens / 1_000_000.0);
        } else if (totalTokens >= 1_000) {
            return String.format("%.1fK", totalTokens / 1_000.0);
        } else {
            return totalTokens.toString();
        }
    }

    /**
     * 获取格式化的平均耗时
     *
     * @return 格式化的耗时（如 "3.5s"）
     */
    public String getAvgDurationFormatted() {
        if (avgDuration == null) {
            return "N/A";
        }

        if (avgDuration < 1000) {
            return avgDuration + "ms";
        } else if (avgDuration < 60_000) {
            return String.format("%.1fs", avgDuration / 1000.0);
        } else {
            long minutes = avgDuration / 60_000;
            long seconds = (avgDuration % 60_000) / 1000;
            return String.format("%dm%ds", minutes, seconds);
        }
    }
}
