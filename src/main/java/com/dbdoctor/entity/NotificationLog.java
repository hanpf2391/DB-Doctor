package com.dbdoctor.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通知发送日志实体类
 *
 * <p>记录每次通知发送的详细日志，包括接收人、发送状态、重试次数等</p>
 *
 * @author DB-Doctor
 * @version 3.2.0
 * @since 3.2.0
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "notification_log", indexes = {
    @Index(name = "idx_notify_alert_id", columnList = "alertId"),
    @Index(name = "idx_notify_status_time", columnList = "status,createdAt"),
    @Index(name = "idx_notify_channel_time", columnList = "channel,createdAt")
})
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 告警ID
     */
    @Column(name = "alert_id", nullable = false)
    private Long alertId;

    /**
     * 通知渠道：EMAIL、WEBHOOK、DINGTALK、FEISHU、WECOM
     */
    @Column(name = "channel", nullable = false, length = 20)
    private String channel;

    /**
     * 接收人
     */
    @Column(name = "recipient", length = 255)
    private String recipient;

    /**
     * 发送状态：SUCCESS（成功）、FAILED（失败）、PENDING（待发送）
     */
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    /**
     * 错误信息
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * 重试次数
     */
    @Column(name = "retry_count", nullable = false)
    private Integer retryCount;

    /**
     * 发送时间
     */
    @Column(name = "sent_time")
    private java.time.LocalDateTime sentTime;

    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private java.time.LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = java.time.LocalDateTime.now();
        if (status == null) {
            status = "PENDING";
        }
        if (retryCount == null) {
            retryCount = 0;
        }
    }

    /**
     * 标记发送成功
     */
    public void markAsSuccess() {
        this.status = "SUCCESS";
        this.sentTime = java.time.LocalDateTime.now();
    }

    /**
     * 标记发送失败
     */
    public void markAsFailed(String errorMessage) {
        this.status = "FAILED";
        this.errorMessage = errorMessage;
        this.sentTime = java.time.LocalDateTime.now();
    }

    /**
     * 增加重试次数
     */
    public void incrementRetryCount() {
        this.retryCount++;
    }

    /**
     * 判断是否发送成功
     */
    public boolean isSuccess() {
        return "SUCCESS".equals(this.status);
    }

    /**
     * 判断是否发送失败
     */
    public boolean isFailed() {
        return "FAILED".equals(this.status);
    }

    /**
     * 判断是否待发送
     */
    public boolean isPending() {
        return "PENDING".equals(this.status);
    }

    /**
     * 判断是否可以重试
     */
    public boolean canRetry(int maxRetryTimes) {
        return this.retryCount < maxRetryTimes;
    }
}
