package com.dbdoctor.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 调用日志实体
 *
 * <p>记录所有 AI 调用的详细信息，用于监控、分析和优化</p>
 *
 * @author DB-Doctor
 * @version 2.3.0
 * @since 2.3.0
 */
@Data
@Entity
@Table(name = "ai_invocation_log", indexes = {
        @Index(name = "idx_trace_id", columnList = "trace_id"),
        @Index(name = "idx_agent_name", columnList = "agent_name"),
        @Index(name = "idx_model_name", columnList = "model_name"),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_created_time", columnList = "created_time"),
        @Index(name = "idx_start_time", columnList = "start_time"),
        @Index(name = "idx_trace_agent", columnList = "trace_id,agent_name")
})
public class AiInvocationLog {

    /**
     * 主键 ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    /**
     * SQL 指纹（关联 slow_query_template.sql_fingerprint）
     */
    @Column(name = "trace_id", nullable = false, length = 64)
    private String traceId;

    /**
     * Agent 角色（DIAGNOSIS/REASONING/CODING）
     */
    @Column(name = "agent_name", nullable = false, length = 20)
    private String agentName;

    /**
     * 模型名称（qwen2.5:7b/deepseek-r1）
     */
    @Column(name = "model_name", nullable = false, length = 50)
    private String modelName;

    /**
     * 供应商（ollama/openai/deepseek）
     */
    @Column(name = "provider", nullable = false, length = 20)
    private String provider;

    /**
     * 输入 Token 数
     */
    @Column(name = "input_tokens", nullable = false)
    private Integer inputTokens;

    /**
     * 输出 Token 数
     */
    @Column(name = "output_tokens", nullable = false)
    private Integer outputTokens;

    /**
     * 总 Token 数
     */
    @Column(name = "total_tokens", nullable = false)
    private Integer totalTokens;

    /**
     * 耗时（毫秒）
     */
    @Column(name = "duration_ms", nullable = false)
    private Long durationMs;

    /**
     * 开始时间
     */
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    /**
     * 状态（SUCCESS/FAILED/TIMEOUT）
     */
    @Column(name = "status", nullable = false, length = 10)
    private String status;

    /**
     * 错误分类（TIMEOUT/API_ERROR/RATE_LIMIT/NETWORK_ERROR/CONFIG_ERROR/AUTH_ERROR/CONTENT_FILTER/TOKEN_LIMIT/UNKNOWN）
     */
    @Column(name = "error_category", length = 30)
    private String errorCategory;

    /**
     * 错误信息
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * 提示词（可选存储）
     */
    @Column(name = "prompt_text", columnDefinition = "MEDIUMTEXT")
    private String promptText;

    /**
     * 响应内容（可选存储）
     */
    @Column(name = "response_text", columnDefinition = "MEDIUMTEXT")
    private String responseText;

    /**
     * 创建时间
     */
    @Column(name = "created_time", nullable = false, updatable = false)
    private LocalDateTime createdTime;

    /**
     * 扩展标签（JSON）
     * <p>可用于存储额外的元数据，如 {"env":"prod","version":"v2.3.0"}</p>
     */
    @Column(name = "tags", columnDefinition = "JSON")
    private String tags;

    /**
     * 判断调用是否成功
     *
     * @return 如果状态为 SUCCESS 返回 true
     */
    public boolean isSuccess() {
        return "SUCCESS".equals(this.status);
    }

    /**
     * 判断调用是否失败
     *
     * @return 如果状态为 FAILED 或 TIMEOUT 返回 true
     */
    public boolean isFailure() {
        return "FAILED".equals(this.status) || "TIMEOUT".equals(this.status);
    }

    /**
     * 计算 Token 使用率（输出/输入）
     *
     * @return Token 使用率，如果输入为 0 返回 0
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
        } else if (durationMs < 60000) {
            return String.format("%.1fs", durationMs / 1000.0);
        } else {
            long minutes = durationMs / 60000;
            long seconds = (durationMs % 60000) / 1000;
            return String.format("%dm%ds", minutes, seconds);
        }
    }
}
