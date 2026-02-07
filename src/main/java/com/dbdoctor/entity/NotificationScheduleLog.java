package com.dbdoctor.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 定时批量通知执行日志实体类
 *
 * <p>记录每次定时批量通知任务的执行情况</p>
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
@Table(name = "notification_schedule_log", indexes = {
    @Index(name = "idx_trigger_time", columnList = "trigger_time"),
    @Index(name = "idx_status", columnList = "status")
})
public class NotificationScheduleLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 执行ID（UUID）
     */
    @Column(name = "execution_id", nullable = false, unique = true, length = 50)
    private String executionId;

    /**
     * 触发时间
     */
    @Column(name = "trigger_time", nullable = false)
    private java.time.LocalDateTime triggerTime;

    /**
     * 状态
     * SUCCESS - 执行成功
     * FAILED - 执行失败
     */
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    /**
     * 时间窗口开始
     */
    @Column(name = "window_start")
    private java.time.LocalDateTime windowStart;

    /**
     * 时间窗口结束
     */
    @Column(name = "window_end")
    private java.time.LocalDateTime windowEnd;

    /**
     * 等待通知的记录数
     */
    @Column(name = "waiting_count")
    private Integer waitingCount;

    /**
     * 成功发送的数量
     */
    @Column(name = "sent_count")
    private Integer sentCount;

    /**
     * 失败的渠道（JSON 格式）
     * 示例：["DINGTALK", "WECOM"]
     */
    @Column(name = "failed_channels", columnDefinition = "TEXT")
    private String failedChannels;

    /**
     * 执行耗时（毫秒）
     */
    @Column(name = "duration_ms")
    private Long durationMs;

    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private java.time.LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = java.time.LocalDateTime.now();
        if (executionId == null) {
            executionId = java.util.UUID.randomUUID().toString();
        }
    }
}
