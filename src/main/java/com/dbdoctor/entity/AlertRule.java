package com.dbdoctor.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 告警规则实体类
 *
 * <p>定义系统告警的触发规则，支持阈值告警、异常告警、趋势告警三种类型</p>
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
@Table(name = "alert_rule", indexes = {
    @Index(name = "idx_enabled", columnList = "enabled"),
    @Index(name = "idx_type", columnList = "type")
})
public class AlertRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 规则名称（唯一标识）
     */
    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    /**
     * 显示名称
     */
    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    /**
     * 规则类型：THRESHOLD（阈值）、ANOMALY（异常）、TREND（趋势）
     */
    @Column(name = "type", nullable = false, length = 20)
    private String type;

    /**
     * 指标名称
     */
    @Column(name = "metric_name", nullable = false, length = 50)
    private String metricName;

    /**
     * 比较操作符：>、<、=、>=、<=
     */
    @Column(name = "condition_operator", nullable = false, length = 10)
    private String conditionOperator;

    /**
     * 阈值（类型为THRESHOLD时使用）
     */
    @Column(name = "threshold_value")
    private Double thresholdValue;

    /**
     * 严重程度：CRITICAL、WARNING、INFO
     */
    @Column(name = "severity", nullable = false, length = 20)
    private String severity;

    /**
     * 是否启用
     */
    @Column(name = "enabled", nullable = false)
    private Boolean enabled;

    /**
     * 冷却期（分钟）
     */
    @Column(name = "cool_down_minutes", nullable = false)
    private Integer coolDownMinutes;

    /**
     * 规则描述
     */
    @Column(name = "description", length = 500)
    private String description;

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
            enabled = true;
        }
        if (coolDownMinutes == null) {
            coolDownMinutes = 30;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = java.time.LocalDateTime.now();
    }
}
