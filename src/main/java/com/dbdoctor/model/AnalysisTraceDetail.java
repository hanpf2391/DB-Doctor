package com.dbdoctor.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 单次分析详情 DTO
 *
 * <p>表示一个 SQL 指纹（traceId）的完整分析过程</p>
 *
 * <p>聚合内容：</p>
 * <ul>
 *   <li>基本信息：traceId、开始/结束时间、状态</li>
 *   <li>统计信息：总耗时、总 Token、总调用次数、成功率</li>
 *   <li>调用详情：完整的 AI 调用链（DIAGNOSIS → REASONING → CODING）</li>
 * </ul>
 *
 * @author DB-Doctor
 * @version 2.3.1
 * @since 2.3.1
 */
@Data
public class AnalysisTraceDetail {

    /**
     * SQL 指纹
     */
    private String traceId;

    /**
     * SQL 示例（用于展示）
     */
    private String sampleSql;

    /**
     * 数据库名称
     */
    private String dbName;

    /**
     * 分析开始时间
     */
    private LocalDateTime startTime;

    /**
     * 分析结束时间
     */
    private LocalDateTime endTime;

    /**
     * 总耗时（毫秒）
     */
    private Long totalDurationMs;

    /**
     * 总 Token 消耗
     */
    private Integer totalTokens;

    /**
     * 总调用次数
     */
    private Integer totalCalls;

    /**
     * 成功率（0-100）
     */
    private Double successRate;

    /**
     * AI 调用详情列表（按时间顺序）
     */
    private List<AiInvocationDetail> invocations;

    /**
     * 状态：SUCCESS（全部成功）/ PARTIAL_FAILURE（部分失败）/ FAILED（全部失败）
     */
    private String status;

    /**
     * 获取格式化的耗时描述
     */
    public String getDurationDescription() {
        if (totalDurationMs == null) {
            return "N/A";
        }
        if (totalDurationMs < 1000) {
            return totalDurationMs + "ms";
        } else if (totalDurationMs < 60000) {
            return String.format("%.1fs", totalDurationMs / 1000.0);
        } else {
            long minutes = totalDurationMs / 60000;
            long seconds = (totalDurationMs % 60000) / 1000;
            return String.format("%dm%ds", minutes, seconds);
        }
    }

    /**
     * 判断是否全部成功
     */
    public boolean isAllSuccess() {
        return "SUCCESS".equals(this.status);
    }

    /**
     * 获取状态显示文本
     */
    public String getStatusText() {
        if ("SUCCESS".equals(this.status)) {
            return "成功";
        } else if ("PARTIAL_FAILURE".equals(this.status)) {
            return "部分失败";
        } else if ("FAILED".equals(this.status)) {
            return "失败";
        }
        return "未知";
    }
}
