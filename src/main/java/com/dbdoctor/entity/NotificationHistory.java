package com.dbdoctor.entity;

import com.dbdoctor.common.enums.SeverityLevel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 通知历史实体
 *
 * 核心职责：
 * - 永久存储每次发送的完整通知报告
 * - 支持历史查询和时间线展示
 * - 直接保存完整 HTML 报告到数据库 TEXT 字段
 *
 * 设计原则：
 * - 与 notification_queue 解耦
 * - 完整保存，不丢失任何分析结果
 * - 支持按批次、指纹、时间范围查询
 *
 * @author DB-Doctor
 * @version 3.0.0
 */
@Entity
@Table(name = "notification_history", indexes = {
    @Index(name = "idx_batch_id", columnList = "batch_id"),
    @Index(name = "idx_sql_fingerprint", columnList = "sql_fingerprint"),
    @Index(name = "idx_sent_time", columnList = "sent_time"),
    @Index(name = "idx_analyzed_time", columnList = "analyzed_time")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationHistory {

    /**
     * 主键
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 批次ID（UUID，用于标识一次通知）
     */
    @Column(name = "batch_id", nullable = false, length = 64)
    private String batchId;

    /**
     * SQL指纹
     */
    @Column(name = "sql_fingerprint", nullable = false, length = 255)
    private String sqlFingerprint;

    /**
     * 数据库名称
     */
    @Column(name = "db_name", nullable = false, length = 100)
    private String dbName;

    /**
     * 表名
     */
    @Column(name = "table_name", length = 255)
    private String tableName;

    /**
     * 分析时间
     */
    @Column(name = "analyzed_time", nullable = false)
    private LocalDateTime analyzedTime;

    /**
     * 发送时间
     */
    @Column(name = "sent_time")
    private LocalDateTime sentTime;

    /**
     * 严重程度
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private SeverityLevel severity;

    /**
     * 执行次数
     */
    @Column(name = "occurrence_count", nullable = false)
    private Long occurrenceCount;

    /**
     * 平均耗时（秒）
     */
    @Column(name = "avg_query_time")
    private Double avgQueryTime;

    /**
     * 最大耗时（秒）
     */
    @Column(name = "max_query_time")
    private Double maxQueryTime;

    /**
     * SQL模板
     */
    @Lob
    @Column(name = "sql_template", columnDefinition = "TEXT")
    private String sqlTemplate;

    /**
     * AI分析报告（完整）
     */
    @Lob
    @Column(name = "ai_report", columnDefinition = "TEXT")
    private String aiReport;

    /**
     * 通知状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_status", nullable = false, length = 20)
    private NotificationStatus notificationStatus;

    /**
     * 失败原因
     */
    @Lob
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * 创建时间
     */
    @Column(name = "created_time", nullable = false, updatable = false)
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    @Column(name = "updated_time", nullable = false)
    private LocalDateTime updatedTime;

    /**
     * 通知状态枚举
     */
    public enum NotificationStatus {
        /**
         * 已发送
         */
        SENT,

        /**
         * 发送失败
         */
        FAILED
    }

    /**
     * 生命周期回调：创建前设置时间
     */
    @PrePersist
    protected void onCreate() {
        createdTime = LocalDateTime.now();
        updatedTime = LocalDateTime.now();
    }

    /**
     * 生命周期回调：更新前设置时间
     */
    @PreUpdate
    protected void onUpdate() {
        updatedTime = LocalDateTime.now();
    }
}
