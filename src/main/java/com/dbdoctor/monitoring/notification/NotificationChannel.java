package com.dbdoctor.monitoring.notification;

/**
 * 通知渠道枚举
 *
 * @author DB-Doctor
 * @version 3.2.0
 * @since 3.2.0
 */
public enum NotificationChannel {
    /**
     * 邮件通知
     */
    EMAIL,

    /**
     * Webhook 通知（通用）
     */
    WEBHOOK,

    /**
     * 钉钉通知
     */
    DINGTALK,

    /**
     * 飞书通知
     */
    FEISHU,

    /**
     * 企业微信通知
     */
    WECOM
}
