package com.dbdoctor.service;

import com.dbdoctor.model.ToolResult;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 熔断器管理器
 *
 * 功能：
 * - 记录各工具的失败次数
 * - 判断是否应该熔断
 * - 在熔断状态下阻止工具调用
 *
 * 状态机：
 * CLOSED → OPEN → HALF_OPEN → CLOSED
 *
 * @author DB-Doctor
 * @version 3.0.0
 */
@Slf4j
@Component
public class CircuitBreaker {

    /**
     * 每个工具的失败统计
     */
    private final Map<String, FailureStats> failureStats = new ConcurrentHashMap<>();

    // ==================== 配置参数 ====================

    /**
     * 连续失败多少次触发熔断
     */
    @Value("${db-doctor.circuit-breaker.failure-threshold:3}")
    private int failureThreshold;

    /**
     * 熔断持续时间（秒）
     */
    @Value("${db-doctor.circuit-breaker.timeout-seconds:60}")
    private long timeoutSeconds;

    /**
     * 半开状态最多允许的调用次数
     */
    @Value("${db-doctor.circuit-breaker.half-open-max-calls:1}")
    private int halfOpenMaxCalls;

    // ==================== 核心方法 ====================

    /**
     * 判断工具调用是否允许执行（熔断器检查）
     *
     * @param toolName 工具名称
     * @return true=允许执行, false=被熔断
     */
    public boolean allowExecution(String toolName) {
        FailureStats stats = failureStats.computeIfAbsent(toolName, k -> new FailureStats());

        // 检查是否在熔断状态
        if (stats.isCircuitOpen()) {
            long timeSinceLastFailure = System.currentTimeMillis() - stats.getLastFailureTime();

            if (timeSinceLastFailure > timeoutSeconds * 1000L) {
                // 超时熔断时间，尝试恢复到半开状态
                stats.transitionToHalfOpen();
                log.info("🔓 熔断器恢复: toolName={}, 状态=HALF_OPEN", toolName);
                return true;
            } else {
                log.warn("⛔ 熔断器阻止: toolName={}, 状态=OPEN, 剩余时间={}s",
                        toolName,
                        (timeoutSeconds * 1000L - timeSinceLastFailure) / 1000);
                return false;
            }
        }

        // 检查是否在半开状态
        if (stats.isHalfOpen()) {
            if (stats.getHalfOpenCalls() >= halfOpenMaxCalls) {
                log.warn("⛔ 熔断器阻止: toolName={}, 半开状态调用次数已达上限", toolName);
                return false;
            }
            log.info("🟡 半开状态: toolName={}, 允许尝试 ({}/{})",
                    toolName, stats.getHalfOpenCalls() + 1, halfOpenMaxCalls);
            return true;
        }

        // 关闭状态，正常执行
        return true;
    }

    /**
     * 记录工具调用结果
     *
     * @param toolName 工具名称
     * @param result   工具执行结果
     */
    public void recordResult(String toolName, ToolResult result) {
        FailureStats stats = failureStats.computeIfAbsent(toolName, k -> new FailureStats());

        if (result.isSuccess()) {
            // 成功：重置失败计数
            if (stats.getFailureCount() > 0 || stats.isCircuitOpen() || stats.isHalfOpen()) {
                log.info("✅ 工具恢复: toolName={}, 失败次数重置, 状态=CLOSED", toolName);
            }
            stats.reset();

        } else {
            // 失败：增加失败计数，检查是否需要熔断
            stats.incrementFailure();
            stats.setLastFailureTime(System.currentTimeMillis());

            // 检查是否是阻断性错误
            if (result.shouldCircuitBreak()) {
                stats.incrementCircuitBreakFailure();
            }

            if (stats.getFailureCount() >= failureThreshold) {
                log.warn("⛔ 触发熔断: toolName={}, 失败次数={}, 阈值={}",
                        toolName, stats.getFailureCount(), failureThreshold);
                stats.transitionToOpen();
            } else {
                log.warn("⚠️ 工具失败: toolName={}, 失败次数={}/{}",
                        toolName, stats.getFailureCount(), failureThreshold);
            }
        }
    }

    /**
     * 重置指定工具的熔断器状态
     *
     * @param toolName 工具名称
     */
    public void reset(String toolName) {
        FailureStats stats = failureStats.get(toolName);
        if (stats != null) {
            stats.reset();
            log.info("🔄 熔断器重置: toolName={}", toolName);
        }
    }

    /**
     * 重置所有熔断器状态
     */
    public void resetAll() {
        failureStats.clear();
        log.info("🔄 熔断器全部重置");
    }

    /**
     * 获取工具的当前状态
     *
     * @param toolName 工具名称
     * @return 状态字符串
     */
    public String getState(String toolName) {
        FailureStats stats = failureStats.get(toolName);
        if (stats == null) {
            return "CLOSED";
        }
        return stats.getState().name();
    }

    // ==================== 内部类 ====================

    /**
     * 失败统计信息
     */
    @Data
    private static class FailureStats {

        /**
         * 失败次数
         */
        private int failureCount = 0;

        /**
         * 触发熔断的失败次数（阻断性错误）
         */
        private int circuitBreakFailureCount = 0;

        /**
         * 最后一次失败时间
         */
        private long lastFailureTime = 0;

        /**
         * 熔断器状态
         */
        private CircuitState state = CircuitState.CLOSED;

        /**
         * 半开状态下的调用次数
         */
        private int halfOpenCalls = 0;

        /**
         * 判断熔断器是否打开
         */
        boolean isCircuitOpen() {
            return state == CircuitState.OPEN;
        }

        /**
         * 判断是否处于半开状态
         */
        boolean isHalfOpen() {
            return state == CircuitState.HALF_OPEN;
        }

        /**
         * 增加失败计数
         */
        void incrementFailure() {
            failureCount++;
        }

        /**
         * 增加阻断性失败计数
         */
        void incrementCircuitBreakFailure() {
            circuitBreakFailureCount++;
        }

        /**
         * 重置所有状态
         */
        void reset() {
            failureCount = 0;
            circuitBreakFailureCount = 0;
            state = CircuitState.CLOSED;
            halfOpenCalls = 0;
        }

        /**
         * 转换到熔断状态
         */
        void transitionToOpen() {
            state = CircuitState.OPEN;
            halfOpenCalls = 0;
        }

        /**
         * 转换到半开状态
         */
        void transitionToHalfOpen() {
            state = CircuitState.HALF_OPEN;
            halfOpenCalls = 0;
            failureCount = 0; // 半开状态重置失败计数
        }

        /**
         * 半开状态增加调用次数
         */
        void incrementHalfOpenCalls() {
            halfOpenCalls++;
        }
    }

    /**
     * 熔断器状态枚举
     */
    public enum CircuitState {
        /**
         * 关闭状态（正常）
         */
        CLOSED,

        /**
         * 打开状态（熔断）
         */
        OPEN,

        /**
         * 半开状态（试探）
         */
        HALF_OPEN
    }

    // ==================== Getter 方法（用于监控）====================

    public int getFailureThreshold() {
        return failureThreshold;
    }

    public long getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public int getHalfOpenMaxCalls() {
        return halfOpenMaxCalls;
    }

    public Map<String, FailureStats> getFailureStats() {
        return new ConcurrentHashMap<>(failureStats);
    }
}
