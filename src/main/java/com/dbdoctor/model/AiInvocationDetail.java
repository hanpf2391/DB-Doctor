package com.dbdoctor.model;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 调用详情 DTO
 *
 * <p>用于返回单次 AI 调用的详细信息</p>
 *
 * @author DB-Doctor
 * @version 2.3.0
 * @since 2.3.0
 */
@Data
public class AiInvocationDetail {

    /**
     * 主键 ID
     */
    private Long id;

    /**
     * SQL 指纹
     */
    private String traceId;

    /**
     * Agent 角色名称（中文）
     */
    private String agentDisplayName;

    /**
     * Agent 角色代码
     */
    private String agentCode;

    /**
     * 模型名称
     */
    private String modelName;

    /**
     * 供应商
     */
    private String provider;

    /**
     * 输入 Token 数
     */
    private Integer inputTokens;

    /**
     * 输出 Token 数
     */
    private Integer outputTokens;

    /**
     * 总 Token 数
     */
    private Integer totalTokens;

    /**
     * 耗时（毫秒）
     */
    private Long durationMs;

    /**
     * 耗时描述（人类可读）
     */
    private String durationDescription;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 状态代码（SUCCESS/FAILED/TIMEOUT）
     */
    private String statusCode;

    /**
     * 状态显示名称（中文）
     */
    private String statusDisplayName;

    /**
     * 错误分类
     */
    private String errorCategory;

    /**
     * 错误分类显示名称（中文）
     */
    private String errorCategoryDisplayName;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 提示词（可选）
     */
    private String promptText;

    /**
     * 响应内容（可选）
     */
    private String responseText;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;

    /**
     * 判断调用是否成功
     *
     * @return 如果状态为 SUCCESS 返回 true
     */
    public boolean isSuccess() {
        return "SUCCESS".equals(this.statusCode);
    }

    /**
     * 判断调用是否失败
     *
     * @return 如果状态为 FAILED 或 TIMEOUT 返回 true
     */
    public boolean isFailure() {
        return "FAILED".equals(this.statusCode) || "TIMEOUT".equals(this.statusCode);
    }

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
     * 获取耗时描述（人类可读）
     *
     * @return 耗时描述，如 "3.5s"
     */
    public String getDurationDescription() {
        if (durationMs == null) {
            return "N/A";
        }

        if (durationMs < 1000) {
            return durationMs + "ms";
        } else if (durationMs < 60_000) {
            return String.format("%.1fs", durationMs / 1000.0);
        } else {
            long minutes = durationMs / 60_000;
            long seconds = (durationMs % 60_000) / 1000;
            return String.format("%dm%ds", minutes, seconds);
        }
    }

    /**
     * 获取状态标签类型（用于前端）
     *
     * @return success/danger/warning/info
     */
    public String getStatusType() {
        if ("SUCCESS".equals(this.statusCode)) {
            return "success";
        } else if ("TIMEOUT".equals(this.statusCode)) {
            return "warning";
        } else if ("FAILED".equals(this.statusCode)) {
            return "danger";
        } else {
            return "info";
        }
    }
}
