package com.dbdoctor.common.enums;

/**
 * 错误分类枚举
 *
 * 用于对错误进行分类，决定错误的处理策略：
 * - BLOCKING: 阻断性错误，必须修复才能继续
 * - TRANSIENT: 临时性错误，可能自动恢复
 * - PERMANENT: 永久性错误，需要人工介入
 * - NONE: 无错误，正常情况
 *
 * @author DB-Doctor
 * @version 3.0.0
 */
public enum ErrorCategory {

    /**
     * 阻断性错误
     *
     * 环境/配置/权限问题导致的错误，必须修复后才能继续执行。
     * 例如：数据库不存在、表不存在、权限不足、慢查询日志未启用等。
     *
     * 恢复策略：ABORT（中止执行，通知用户修复环境）
     */
    BLOCKING("阻断性错误", "环境/配置/权限问题，必须修复才能继续"),

    /**
     * 临时性错误
     *
     * 网络/超时等临时性问题，可能自动恢复或通过重试解决。
     * 例如：连接超时、连接丢失、查询超时、AI API 限流等。
     *
     * 恢复策略：RETRY（重试，最多3次，指数退避）
     */
    TRANSIENT("临时性错误", "网络/超时等临时性问题，可以重试"),

    /**
     * 永久性错误
     *
     * SQL 语法错误等永久性问题，无法通过重试解决，需要人工介入修改。
     * 例如：SQL 语法错误、主键冲突等。
     *
     * 恢复策略：ABORT（中止执行，需要人工介入）
     */
    PERMANENT("永久性错误", "SQL 语法错误等，需要人工介入"),

    /**
     * 无错误
     *
     * 正常情况，无错误发生。
     *
     * 恢复策略：CONTINUE（继续执行）
     */
    NONE("无错误", "正常情况");

    private final String displayName;
    private final String description;

    ErrorCategory(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 判断是否应该熔断（阻止后续流程）
     *
     * @return true=应该熔断, false=不熔断
     */
    public boolean shouldCircuitBreak() {
        return this == BLOCKING;
    }

    /**
     * 判断是否可以重试
     *
     * @return true=可以重试, false=不可重试
     */
    public boolean isRetryable() {
        return this == TRANSIENT;
    }
}
