package com.dbdoctor.model;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 单次分析摘要 DTO
 *
 * <p>用于列表展示，不包含详细信息</p>
 *
 * @author DB-Doctor
 * @version 2.3.1
 * @since 2.3.1
 */
@Data
public class AnalysisTraceSummary {

    /**
     * SQL 指纹
     */
    private String traceId;

    /**
     * 分析开始时间
     */
    private LocalDateTime startTime;

    /**
     * 总调用次数
     */
    private Integer totalCalls;

    /**
     * 总耗时（毫秒）
     */
    private Long totalDurationMs;

    /**
     * 总 Token 消耗
     */
    private Integer totalTokens;

    /**
     * 状态
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
