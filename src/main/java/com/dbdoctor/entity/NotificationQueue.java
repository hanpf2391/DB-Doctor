package com.dbdoctor.entity;

import com.dbdoctor.common.enums.SeverityLevel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 通知队列实体
 *
 * 核心职责：
 * - 临时存储每次分析完成的事件
 * - 支持时间线聚合展示
 * - 发送后删除（队列模式）
 *
 * 设计原则：
 * - 与 slow_query_template 解耦
 * - 事件驱动，不覆盖历史
 * - 消费后删除，避免数据积累
 *
 * @author DB-Doctor
 * @version 3.0.0
 */
@Entity
@Table(name = "notification_queue")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationQueue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * SQL 指纹（关联 slow_query_template）
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
     * SQL 模板（用于展示）
     */
    @Lob
    @Column(name = "sql_template", nullable = false)
    private String sqlTemplate;

    /**
     * 严重程度（本次分析）
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private SeverityLevel severity;

    /**
     * AI 分析报告（本次分析）
     */
    @Lob
    @Column(name = "ai_report", nullable = false)
    private String aiReport;

    /**
     * 执行统计信息
     */
    @Column(name = "query_time", nullable = false)
    private Double queryTime;

    @Column(name = "lock_time")
    private Double lockTime;

    @Column(name = "rows_examined")
    private Long rowsExamined;

    /**
     * 分析完成时间
     */
    @Column(name = "analyzed_time", nullable = false)
    private LocalDateTime analyzedTime;

    /**
     * 通知状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private NotificationStatus status;

    /**
     * 发送时间
     */
    @Column(name = "sent_time")
    private LocalDateTime sentTime;

    /**
     * 通知状态枚举
     */
    public enum NotificationStatus {
        /**
         * 待发送
         */
        PENDING,

        /**
         * 已发送
         */
        SENT
    }
}
