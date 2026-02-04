package com.dbdoctor.model;

import com.dbdoctor.entity.SlowQueryTemplate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 批次通知报告
 * 用于定时批量通知
 *
 * 核心职责：
 * - 封装一个批次的时间窗口和统计信息
 * - 按严重程度分组展示问题
 * - 提供邮件内容渲染所需的数据
 *
 * @author DB-Doctor
 * @version 3.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationBatchReport {

    /**
     * 时间窗口开始
     */
    private LocalDateTime windowStart;

    /**
     * 时间窗口结束
     */
    private LocalDateTime windowEnd;

    /**
     * 指纹总数（按 SQL 指纹去重后的数量）
     */
    private int totalCount;

    /**
     * 样本总数（所有指纹的出现次数之和）
     */
    private long totalSamples;

    /**
     * 严重问题数量
     */
    private int criticalCount;

    /**
     * 中等问题数量
     */
    private int mediumCount;

    /**
     * 轻微问题数量
     */
    private int lowCount;

    /**
     * 严重问题列表（已按优先级排序）
     */
    private List<SlowQueryTemplate> criticalIssues;

    /**
     * 中等问题列表（已按优先级排序）
     */
    private List<SlowQueryTemplate> mediumIssues;

    /**
     * 轻微问题列表（已按优先级排序）
     */
    private List<SlowQueryTemplate> lowIssues;

    /**
     * 最需要关注的 Top 3 表
     */
    private List<String> topProblematicTables;

    /**
     * 格式化时间窗口（用于邮件显示）
     * 例如：2024-01-22 08:00 ~ 09:00
     */
    public String getFormattedWindow() {
        return String.format("%s ~ %s",
            formatDateTime(windowStart),
            formatDateTime(windowEnd)
        );
    }

    /**
     * 格式化日期时间
     */
    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "未知";
        }
        return String.format("%s %s",
            dateTime.toLocalDate(),
            dateTime.toLocalTime().withSecond(0).withNano(0)
        );
    }

    /**
     * 获取仪表盘 URL
     * TODO: 从配置文件读取，支持自定义域名
     */
    public String getDashboardUrl() {
        return "http://localhost:5173/home";
    }
}
