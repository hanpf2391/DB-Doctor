package com.dbdoctor.model;

import com.dbdoctor.common.enums.ErrorCode;
import com.dbdoctor.common.enums.ErrorCategory;
import com.dbdoctor.common.enums.RecoveryStrategy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 工具执行结果（统一封装）
 *
 * 设计原则：
 * 1. 成功和失败都返回 ToolResult
 * 2. AI 能够理解和解析 ToolResult
 * 3. 代码能够根据 ToolResult 做判断
 *
 * 使用场景：
 * - 所有诊断工具的返回值（如 getTableSchema、getExecutionPlan 等）
 * - Agent 可以根据 ToolResult 的 success 字段判断是否成功
 * - Agent 可以根据 errorCode 和 userMessage 生成错误报告
 *
 * @author DB-Doctor
 * @version 3.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolResult {

    /**
     * 执行是否成功
     */
    private boolean success;

    /**
     * 错误码（失败时有值）
     */
    private ErrorCode errorCode;

    /**
     * 机器可读的错误信息（供代码解析）
     */
    private String errorMessage;

    /**
     * 人类可读的错误描述（供 AI 和用户阅读）
     */
    private String userMessage;

    /**
     * 错误分类
     */
    private ErrorCategory category;

    /**
     * 建议的恢复策略
     */
    private RecoveryStrategy recoveryStrategy;

    /**
     * 成功时的数据（JSON 格式）
     */
    private String data;

    /**
     * 工具执行耗时（毫秒）
     */
    private long executionTimeMs;

    /**
     * 重试建议次数
     */
    private int suggestedRetries;

    // ==================== 静态工厂方法 ====================

    /**
     * 创建成功结果
     *
     * @param data 返回的数据（JSON 格式）
     * @return ToolResult
     */
    public static ToolResult success(String data) {
        return ToolResult.builder()
                .success(true)
                .data(data)
                .category(ErrorCategory.NONE)
                .recoveryStrategy(RecoveryStrategy.CONTINUE)
                .suggestedRetries(0)
                .build();
    }

    /**
     * 创建成功结果（带执行时间）
     *
     * @param data            返回的数据（JSON 格式）
     * @param executionTimeMs 执行耗时（毫秒）
     * @return ToolResult
     */
    public static ToolResult success(String data, long executionTimeMs) {
        return ToolResult.builder()
                .success(true)
                .data(data)
                .category(ErrorCategory.NONE)
                .recoveryStrategy(RecoveryStrategy.CONTINUE)
                .suggestedRetries(0)
                .executionTimeMs(executionTimeMs)
                .build();
    }

    /**
     * 创建失败结果
     *
     * @param errorCode    错误码
     * @param errorMessage 错误信息
     * @return ToolResult
     */
    public static ToolResult failure(ErrorCode errorCode, String errorMessage) {
        ErrorCategory category = errorCode.getCategory();
        RecoveryStrategy strategy = errorCode.getRecoveryStrategy();

        return ToolResult.builder()
                .success(false)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .userMessage(errorCode.getUserMessage())
                .category(category)
                .recoveryStrategy(strategy)
                .suggestedRetries(strategy.getMaxRetries())
                .build();
    }

    /**
     * 创建失败结果（带参数化消息）
     *
     * @param errorCode    错误码
     * @param errorMessage 错误信息
     * @param args         用户消息参数
     * @return ToolResult
     */
    public static ToolResult failure(ErrorCode errorCode, String errorMessage, Object... args) {
        ErrorCategory category = errorCode.getCategory();
        RecoveryStrategy strategy = errorCode.getRecoveryStrategy();

        return ToolResult.builder()
                .success(false)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .userMessage(errorCode.formatUserMessage(args))
                .category(category)
                .recoveryStrategy(strategy)
                .suggestedRetries(strategy.getMaxRetries())
                .build();
    }

    /**
     * 创建失败结果（完整参数）
     *
     * @param errorCode       错误码
     * @param errorMessage    错误信息
     * @param userMessage     用户消息
     * @param executionTimeMs 执行耗时
     * @return ToolResult
     */
    public static ToolResult failure(ErrorCode errorCode, String errorMessage,
                                     String userMessage, long executionTimeMs) {
        return ToolResult.builder()
                .success(false)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .userMessage(userMessage)
                .category(errorCode.getCategory())
                .recoveryStrategy(errorCode.getRecoveryStrategy())
                .suggestedRetries(errorCode.getRecoveryStrategy().getMaxRetries())
                .executionTimeMs(executionTimeMs)
                .build();
    }

    // ==================== 判断方法 ====================

    /**
     * 判断是否应该重试
     *
     * @return true=应该重试, false=不可重试
     */
    public boolean shouldRetry() {
        return !success && recoveryStrategy == RecoveryStrategy.RETRY;
    }

    /**
     * 判断是否应该熔断（阻止后续流程）
     *
     * @return true=应该熔断, false=不熔断
     */
    public boolean shouldCircuitBreak() {
        return !success && category == ErrorCategory.BLOCKING;
    }

    /**
     * 判断是否应该中止
     *
     * @return true=应该中止, false=不中止
     */
    public boolean shouldAbort() {
        return !success && recoveryStrategy == RecoveryStrategy.ABORT;
    }

    /**
     * 判断是否应该降级
     *
     * @return true=应该降级, false=不降级
     */
    public boolean shouldFallback() {
        return !success && recoveryStrategy == RecoveryStrategy.FALLBACK;
    }

    /**
     * 判断是否是阻断性错误
     *
     * @return true=阻断性错误, false=非阻断性错误
     */
    public boolean isBlockingError() {
        return !success && category == ErrorCategory.BLOCKING;
    }

    /**
     * 判断是否是临时性错误
     *
     * @return true=临时性错误, false=非临时性错误
     */
    public boolean isTransientError() {
        return !success && category == ErrorCategory.TRANSIENT;
    }

    /**
     * 判断是否是永久性错误
     *
     * @return true=永久性错误, false=非永久性错误
     */
    public boolean isPermanentError() {
        return !success && category == ErrorCategory.PERMANENT;
    }

    // ==================== 格式化方法 ====================

    /**
     * 格式化为 JSON 字符串（供 AI 解析）
     *
     * @return JSON 字符串
     */
    public String toJsonString() {
        if (success) {
            return String.format(
                    "{\"success\":true,\"data\":%s,\"category\":\"NONE\",\"recoveryStrategy\":\"CONTINUE\"}",
                    data != null ? data : "null"
            );
        } else {
            return String.format(
                    "{\"success\":false,\"errorCode\":\"%s\",\"errorMessage\":\"%s\",\"userMessage\":\"%s\",\"category\":\"%s\",\"recoveryStrategy\":\"%s\",\"suggestedRetries\":%d}",
                    errorCode != null ? errorCode.getCode() : "UNKNOWN",
                    escapeJson(errorMessage),
                    escapeJson(userMessage),
                    category != null ? category.name() : "UNKNOWN",
                    recoveryStrategy != null ? recoveryStrategy.name() : "UNKNOWN",
                    suggestedRetries
            );
        }
    }

    /**
     * 转义 JSON 字符串中的特殊字符
     *
     * @param str 原始字符串
     * @return 转义后的字符串
     */
    private String escapeJson(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * 格式化为人类可读的报告（供 AI 参考）
     *
     * @return Markdown 格式的报告
     */
    public String toReport() {
        if (success) {
            return "✅ 工具执行成功";
        } else {
            StringBuilder report = new StringBuilder();
            report.append("❌ 工具执行失败\n\n");
            report.append("**错误码**: ").append(errorCode != null ? errorCode.getCode() : "UNKNOWN").append("\n");
            report.append("**错误类型**: ").append(category != null ? category.getDisplayName() : "UNKNOWN").append("\n");
            report.append("**错误描述**: ").append(userMessage != null ? userMessage : errorMessage).append("\n");
            report.append("**建议操作**: ").append(recoveryStrategy != null ? recoveryStrategy.getDescription() : "UNKNOWN").append("\n");
            if (suggestedRetries > 0) {
                report.append("**建议重试次数**: ").append(suggestedRetries).append("\n");
            }
            return report.toString();
        }
    }
}
