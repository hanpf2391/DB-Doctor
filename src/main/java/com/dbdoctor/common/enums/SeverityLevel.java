package com.dbdoctor.common.enums;

/**
 * 慢查询严重程度枚举
 *
 * @author DB-Doctor
 * @version 2.2.0
 */
public enum SeverityLevel {
    /**
     * 严重（红色）- 平均耗时 >= 5 秒
     */
    CRITICAL("🔴 严重", "严重"),

    /**
     * 警告（橙色）- 平均耗时 >= 3 秒
     */
    WARNING("🟠 警告", "警告"),

    /**
     * 正常（绿色）- 平均耗时 < 3 秒
     */
    NORMAL("🟢 正常", "正常");

    private final String displayName;
    private final String shortName;

    SeverityLevel(String displayName, String shortName) {
        this.displayName = displayName;
        this.shortName = shortName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getShortName() {
        return shortName;
    }

    /**
     * 根据平均查询耗时计算严重程度
     *
     * @param avgQueryTime 平均查询耗时（秒）
     * @param severityThreshold 严重程度阈值（秒）
     * @return 严重程度
     */
    public static SeverityLevel fromQueryTime(Double avgQueryTime, Double severityThreshold) {
        if (avgQueryTime == null) {
            return NORMAL;
        }

        // 严重：平均耗时 >= 5 秒
        if (avgQueryTime >= 5.0) {
            return CRITICAL;
        }

        // 警告：平均耗时 >= 配置的阈值（默认 3 秒）
        if (avgQueryTime >= severityThreshold) {
            return WARNING;
        }

        return NORMAL;
    }
}
