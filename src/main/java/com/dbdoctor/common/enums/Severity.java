package com.dbdoctor.common.enums;

import lombok.Getter;

/**
 * 严重程度枚举
 * 用于定义慢查询的严重程度
 *
 * @author DB-Doctor
 * @version 1.0.0
 */
@Getter
public enum Severity {

    /**
     * 低
     */
    LOW(3.0, "低", 1),

    /**
     * 中
     */
    MEDIUM(5.0, "中", 2),

    /**
     * 高
     */
    HIGH(10.0, "高", 3),

    /**
     * 严重
     */
    CRITICAL(999.0, "严重", 4);

    /**
     * 查询耗时阈值（秒）
     */
    private final Double queryTimeThreshold;

    /**
     * 描述
     */
    private final String description;

    /**
     * 级别（数字越大越严重）
     */
    private final Integer level;

    Severity(Double queryTimeThreshold, String description, Integer level) {
        this.queryTimeThreshold = queryTimeThreshold;
        this.description = description;
        this.level = level;
    }

    /**
     * 根据查询耗时判断严重程度
     *
     * @param queryTime 查询耗时（秒）
     * @return 严重程度
     */
    public static Severity fromQueryTime(Double queryTime) {
        if (queryTime == null) {
            return LOW;
        }

        if (queryTime < LOW.queryTimeThreshold) {
            return LOW;
        } else if (queryTime < MEDIUM.queryTimeThreshold) {
            return MEDIUM;
        } else if (queryTime < HIGH.queryTimeThreshold) {
            return HIGH;
        } else {
            return CRITICAL;
        }
    }

    /**
     * 判断是否需要通知
     *
     * @param threshold 配置的通知阈值
     * @return true-需要通知，false-不需要通知
     */
    public boolean shouldNotify(Double threshold) {
        if (threshold == null) {
            return this.level >= MEDIUM.level;
        }
        return this.queryTimeThreshold >= threshold;
    }
}
