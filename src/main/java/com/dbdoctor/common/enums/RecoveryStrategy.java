package com.dbdoctor.common.enums;

/**
 * 恢复策略枚举
 *
 * 定义错误发生后的恢复策略：
 * - CONTINUE: 继续执行（无错误）
 * - RETRY: 重试（临时性错误）
 * - FALLBACK: 降级（部分功能不可用）
 * - ABORT: 中止（阻断性或永久性错误）
 *
 * @author DB-Doctor
 * @version 3.0.0
 */
public enum RecoveryStrategy {

    /**
     * 继续
     *
     * 正常情况，无错误发生，继续执行后续流程。
     */
    CONTINUE(false, 0, "继续执行"),

    /**
     * 重试
     *
     * 临时性错误（如网络超时），可以进行重试。
     * 使用指数退避策略，最多重试 3 次。
     */
    RETRY(true, 3, "重试（指数退避）"),

    /**
     * 降级
     *
     * 部分功能不可用时，使用降级方案继续执行。
     * 例如：无法获取索引选择性时，继续分析但不提供该部分建议。
     */
    FALLBACK(true, 0, "降级处理"),

    /**
     * 中止
     *
     * 阻断性或永久性错误，必须中止执行。
     * 例如：数据库不存在、表不存在、SQL 语法错误等。
     */
    ABORT(true, 0, "中止执行");

    private final boolean isError;
    private final int maxRetries;
    private final String description;

    RecoveryStrategy(boolean isError, int maxRetries, String description) {
        this.isError = isError;
        this.maxRetries = maxRetries;
        this.description = description;
    }

    public boolean isError() {
        return isError;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 判断是否应该重试
     *
     * @return true=应该重试, false=不重试
     */
    public boolean shouldRetry() {
        return this == RETRY;
    }

    /**
     * 判断是否应该中止
     *
     * @return true=应该中止, false=不中止
     */
    public boolean shouldAbort() {
        return this == ABORT;
    }

    /**
     * 判断是否应该降级
     *
     * @return true=应该降级, false=不降级
     */
    public boolean shouldFallback() {
        return this == FALLBACK;
    }
}
