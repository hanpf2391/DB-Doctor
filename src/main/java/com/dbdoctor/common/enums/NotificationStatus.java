package com.dbdoctor.common.enums;

/**
 * 通知状态枚举
 *
 * 核心作用：控制慢查询报告的通知流程
 *
 * 状态流转：
 * 1. PENDING → WAITING：分析完成，等待批量通知
 * 2. WAITING → SENT：定时任务批量发送通知
 * 3. WAITING → WAITING：发送失败，保持等待状态，下次重试
 *
 * @author DB-Doctor
 * @version 1.0.0
 */
public enum NotificationStatus {

    /**
     * 待分析
     * 初始状态，尚未开始 AI 分析
     */
    PENDING("待分析"),

    /**
     * 分析中
     * AI 正在进行分析
     */
    ANALYZING("分析中"),

    /**
     * 等待通知
     * AI 分析完成，等待定时任务批量发送通知
     */
    WAITING("等待通知"),

    /**
     * 已发送
     * 通知已成功发送
     */
    SENT("已发送");

    private final String description;

    NotificationStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
