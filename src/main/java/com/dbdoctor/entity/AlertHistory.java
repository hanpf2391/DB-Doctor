package com.dbdoctor.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 告警历史实体类
 *
 * <p>记录系统触发过的所有告警历史，包括触发时间、告警内容、解决状态等</p>
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
@Table(name = "alert_history", indexes = {
    @Index(name = "idx_alert_status_time", columnList = "status,triggeredAt"),
    @Index(name = "idx_alert_severity_time", columnList = "severity,triggeredAt"),
    @Index(name = "idx_alert_rule_id", columnList = "ruleId")
})
public class AlertHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 告警规则ID
     */
    @Column(name = "rule_id", nullable = false)
    private Long ruleId;

    /**
     * 告警规则名称
     */
    @Column(name = "rule_name", nullable = false, length = 100)
    private String ruleName;

    /**
     * 严重程度：CRITICAL、WARNING、INFO
     */
    @Column(name = "severity", nullable = false, length = 20)
    private String severity;

    /**
     * 状态：FIRING（触发中）、RESOLVED（已解决）
     */
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    /**
     * 告警消息
     */
    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    /**
     * 触发告警时的指标数据（JSON格式）
     */
    @Column(name = "metrics", columnDefinition = "JSON")
    @JdbcTypeCode(SqlTypes.JSON)
    private String metrics;

    /**
     * 触发时间
     */
    @Column(name = "triggered_at", nullable = false)
    private java.time.LocalDateTime triggeredAt;

    /**
     * 解决时间
     */
    @Column(name = "resolved_at")
    private java.time.LocalDateTime resolvedAt;

    /**
     * 解决人
     */
    @Column(name = "resolved_by", length = 50)
    private String resolvedBy;

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
        if (triggeredAt == null) {
            triggeredAt = now;
        }
        if (status == null) {
            status = "FIRING";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = java.time.LocalDateTime.now();
    }

    /**
     * 标记告警已解决
     */
    public void markAsResolved(String resolvedBy) {
        this.status = "RESOLVED";
        this.resolvedAt = java.time.LocalDateTime.now();
        this.resolvedBy = resolvedBy;
    }

    /**
     * 判断告警是否已解决
     */
    public boolean isResolved() {
        return "RESOLVED".equals(this.status);
    }

    /**
     * 判断告警是否正在触发
     */
    public boolean isFiring() {
        return "FIRING".equals(this.status);
    }
}
