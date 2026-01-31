package com.dbdoctor.common.enums;

import lombok.Getter;

/**
 * 错误码枚举
 *
 * 命名规则：
 * - ENV_xxx: 环境错误（Environment）
 * - PERM_xxx: 权限错误（Permission）
 * - NET_xxx: 网络错误（Network）
 * - DATA_xxx: 数据错误（Data）
 * - SQL_xxx: SQL 错误（SQL）
 * - AI_xxx: AI 服务错误（AI Service）
 *
 * @author DB-Doctor
 * @version 3.0.0
 */
@Getter
public enum ErrorCode {

    // ==================== 环境错误（阻断性）====================

    /**
     * 数据库不存在
     */
    DB_NOT_FOUND("ENV_001", "数据库不存在",
            "目标数据库 '%s' 不存在或无法连接",
            ErrorCategory.BLOCKING, RecoveryStrategy.ABORT),

    /**
     * 表不存在
     */
    TABLE_NOT_FOUND("ENV_002", "表不存在",
            "表 '%s.%s' 不存在",
            ErrorCategory.BLOCKING, RecoveryStrategy.ABORT),

    /**
     * 列不存在
     */
    COLUMN_NOT_FOUND("ENV_003", "列不存在",
            "列 '%s.%s.%s' 不存在",
            ErrorCategory.BLOCKING, RecoveryStrategy.ABORT),

    /**
     * 慢查询日志未启用
     */
    SLOW_QUERY_LOG_DISABLED("ENV_004", "慢查询日志未启用",
            "目标数据库的慢查询日志未启用",
            ErrorCategory.BLOCKING, RecoveryStrategy.ABORT),

    // ==================== 权限错误（阻断性）====================

    /**
     * 访问被拒绝
     */
    ACCESS_DENIED("PERM_001", "访问被拒绝",
            "无权限访问数据库/表",
            ErrorCategory.BLOCKING, RecoveryStrategy.ABORT),

    /**
     * 权限不足
     */
    PRIVILEGE_NOT_ENOUGH("PERM_002", "权限不足",
            "当前用户权限不足，无法执行操作",
            ErrorCategory.BLOCKING, RecoveryStrategy.ABORT),

    // ==================== 网络错误（临时性，可重试）====================

    /**
     * 连接超时
     */
    CONNECTION_TIMEOUT("NET_001", "连接超时",
            "连接数据库超时",
            ErrorCategory.TRANSIENT, RecoveryStrategy.RETRY),

    /**
     * 连接丢失
     */
    CONNECTION_LOST("NET_002", "连接丢失",
            "数据库连接中断",
            ErrorCategory.TRANSIENT, RecoveryStrategy.RETRY),

    /**
     * 查询超时
     */
    QUERY_TIMEOUT("NET_003", "查询超时",
            "SQL 执行超时",
            ErrorCategory.TRANSIENT, RecoveryStrategy.RETRY),

    // ==================== 数据错误（非阻断性）====================

    /**
     * 查询结果为空
     */
    EMPTY_RESULT("DATA_001", "查询结果为空",
            "查询返回 0 行",
            ErrorCategory.NONE, RecoveryStrategy.CONTINUE),

    /**
     * 主键冲突
     */
    DUPLICATE_KEY("DATA_002", "主键冲突",
            "违反唯一约束",
            ErrorCategory.TRANSIENT, RecoveryStrategy.CONTINUE),

    /**
     * 数据格式错误
     */
    DATA_FORMAT_ERROR("DATA_003", "数据格式错误",
            "数据格式不正确",
            ErrorCategory.PERMANENT, RecoveryStrategy.ABORT),

    // ==================== SQL 错误（永久性）====================

    /**
     * SQL 语法错误
     */
    SYNTAX_ERROR("SQL_001", "SQL 语法错误",
            "SQL 语法不正确: %s",
            ErrorCategory.PERMANENT, RecoveryStrategy.ABORT),

    // ==================== AI 服务错误（阻断性或临时性）====================

    /**
     * AI API 调用受限
     */
    AI_RATE_LIMIT_EXCEEDED("AI_001", "AI API 调用受限",
            "AI API 调用频率超限",
            ErrorCategory.TRANSIENT, RecoveryStrategy.RETRY),

    /**
     * AI 模型不可用
     */
    AI_MODEL_NOT_AVAILABLE("AI_002", "AI 模型不可用",
            "AI 模型当前不可用",
            ErrorCategory.BLOCKING, RecoveryStrategy.ABORT),

    /**
     * AI API 超时
     */
    AI_TIMEOUT("AI_003", "AI API 超时",
            "AI API 调用超时",
            ErrorCategory.TRANSIENT, RecoveryStrategy.RETRY);

    // ==================== 字段定义 ====================

    /**
     * 错误码（如 "ENV_001"）
     */
    private final String code;

    /**
     * 错误名称（简短描述）
     */
    private final String name;

    /**
     * 用户消息模板（支持参数化，使用 String.format）
     */
    private final String userMessageTemplate;

    /**
     * 错误分类
     */
    private final ErrorCategory category;

    /**
     * 恢复策略
     */
    private final RecoveryStrategy recoveryStrategy;

    // ==================== 构造方法 ====================

    ErrorCode(String code, String name, String userMessageTemplate,
              ErrorCategory category, RecoveryStrategy recoveryStrategy) {
        this.code = code;
        this.name = name;
        this.userMessageTemplate = userMessageTemplate;
        this.category = category;
        this.recoveryStrategy = recoveryStrategy;
    }

    // ==================== 工具方法 ====================

    /**
     * 格式化用户消息（支持参数化）
     *
     * @param args 参数列表
     * @return 格式化后的用户消息
     */
    public String formatUserMessage(Object... args) {
        try {
            return String.format(userMessageTemplate, args);
        } catch (Exception e) {
            // 格式化失败时返回原始模板
            return userMessageTemplate;
        }
    }

    /**
     * 获取默认的用户消息（无参数）
     *
     * @return 用户消息
     */
    public String getUserMessage() {
        return userMessageTemplate;
    }

    /**
     * 判断是否应该重试
     *
     * @return true=应该重试, false=不可重试
     */
    public boolean isRetryable() {
        return recoveryStrategy.shouldRetry();
    }

    /**
     * 判断是否应该熔断
     *
     * @return true=应该熔断, false=不熔断
     */
    public boolean shouldCircuitBreak() {
        return category.shouldCircuitBreak();
    }

    /**
     * 根据错误码查找枚举值
     *
     * @param code 错误码（如 "ENV_001"）
     * @return ErrorCode 枚举值，找不到返回 null
     */
    public static ErrorCode fromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }
        for (ErrorCode errorCode : values()) {
            if (errorCode.code.equals(code)) {
                return errorCode;
            }
        }
        return null;
    }

    /**
     * 根据异常信息推断错误码（用于 SQLException 等数据库异常）
     *
     * @param sqlState SQLState 标准错误码
     * @param message  异常消息
     * @return 推断的错误码，无法推断返回 null
     */
    public static ErrorCode fromDatabaseError(String sqlState, String message) {
        if (message == null) {
            return null;
        }

        String msg = message.toLowerCase();

        // 数据库不存在
        if ("42000".equals(sqlState) || msg.contains("unknown database")) {
            return DB_NOT_FOUND;
        }

        // 表不存在
        if ("42S02".equals(sqlState) ||
            (msg.contains("table") && msg.contains("doesn't exist")) ||
            msg.contains("unknown table")) {
            return TABLE_NOT_FOUND;
        }

        // 列不存在
        if ("42S22".equals(sqlState) || msg.contains("unknown column")) {
            return COLUMN_NOT_FOUND;
        }

        // 权限不足
        if ("42000".equals(sqlState) || msg.contains("access denied")) {
            return ACCESS_DENIED;
        }

        // 语法错误
        if ("42000".equals(sqlState) || msg.contains("syntax error")) {
            return SYNTAX_ERROR;
        }

        // 连接超时
        if ("08S01".equals(sqlState) || msg.contains("timeout") || msg.contains("timed out")) {
            return QUERY_TIMEOUT;
        }

        // 主键冲突
        if ("23000".equals(sqlState) || msg.contains("duplicate entry")) {
            return DUPLICATE_KEY;
        }

        return null;
    }
}
