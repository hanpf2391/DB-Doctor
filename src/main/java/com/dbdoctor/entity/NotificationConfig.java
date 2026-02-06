package com.dbdoctor.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 通知配置实体类
 *
 * <p>管理各通知渠道的配置，包括邮件、Webhook、钉钉、飞书、企业微信等</p>
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
@Table(name = "notification_config", uniqueConstraints = {
    @UniqueConstraint(name = "uk_channel", columnNames = "channel")
})
public class NotificationConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 通知渠道：EMAIL、WEBHOOK、DINGTALK、FEISHU、WECOM
     */
    @Column(name = "channel", nullable = false, unique = true, length = 20)
    private String channel;

    /**
     * 是否启用
     */
    @Column(name = "enabled", nullable = false)
    private Boolean enabled;

    /**
     * 渠道配置（JSON格式）
     * 例如邮件配置：{"host":"smtp.example.com","port":587,"username":"xxx","password":"xxx"}
     * 例如钉钉配置：{"webhook":"https://oapi.dingtalk.com/robot/send?access_token=xxx","secret":"SECxxx"}
     */
    @Column(name = "config", nullable = false, columnDefinition = "JSON")
    @JdbcTypeCode(SqlTypes.JSON)
    private String config;

    /**
     * 适用的告警级别：CRITICAL,WARNING,INFO
     */
    @Column(name = "severity_levels", length = 50)
    private String severityLevels;

    /**
     * 创建人
     */
    @Column(name = "created_by", length = 50)
    private String createdBy;

    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private java.time.LocalDateTime createdAt;

    /**
     * 更新人
     */
    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    /**
     * 更新时间
     */
    @Column(name = "updated_at", nullable = false)
    private java.time.LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (enabled == null) {
            enabled = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = java.time.LocalDateTime.now();
    }

    /**
     * 判断该渠道是否适用于指定严重程度的告警
     */
    public boolean isApplicableForSeverity(String severity) {
        if (severityLevels == null || severityLevels.isEmpty()) {
            return true; // 如果未配置级别限制，则适用于所有级别
        }
        return severityLevels.contains(severity);
    }

    /**
     * 判断配置是否有效
     */
    public boolean isValid() {
        return enabled != null && enabled && config != null && !config.isEmpty();
    }
}
