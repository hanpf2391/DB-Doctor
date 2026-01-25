package com.dbdoctor.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

/**
 * 慢查询分析历史实体
 * 存储在 H2 数据库中，用于去重和历史记录
 *
 * @author DB-Doctor
 * @version 2.0.0
 */
@Slf4j
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "slow_query_history",
       indexes = @Index(name = "idx_fingerprint", columnList = "sqlFingerprint"))
public class SlowQueryHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // === 核心：SQL 指纹（唯一标识） ===
    @Column(length = 64, unique = true, nullable = false)
    private String sqlFingerprint;

    // 参数化后的 SQL 模板
    @Lob
    @Column(columnDefinition = "TEXT")
    private String sqlTemplate;

    // 最近一次捕获的具体 SQL（样本）
    @Lob
    @Column(columnDefinition = "TEXT")
    private String exampleSql;

    // === 基本信息 ===
    @Column(length = 64)
    private String dbName;           // 所属数据库

    @Column(length = 64)
    private String tableName;         // 涉及的表

    // === AI 分析结果 ===
    @Lob
    @Column(columnDefinition = "TEXT")
    private String aiAnalysisReport;  // AI 生成的报告（Markdown）

    // === 状态管理 ===
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AnalysisStatus status;   // PENDING / SUCCESS / ERROR / ABANDONED / FAILED

    // === 统计信息（去重的关键） ===
    @Builder.Default
    private Long occurrenceCount = 1L;       // 出现次数
    private LocalDateTime firstSeenTime;     // 首次发现时间
    private LocalDateTime lastSeenTime;      // 最近发现时间

    // === 重试控制 ===
    @Builder.Default
    private Integer retryCount = 0;          // 重试次数（用于 PENDING 补扫）

    // === 元数据（统计信息） ===
    private Double avgQueryTime;      // 平均查询耗时
    private Double avgLockTime;       // 平均锁等待时间
    private Long avgRowsSent;         // 平均返回行数
    private Long maxRowsExamined;     // 最大扫描行数
    private Long maxRowsSent;         // 最大返回行数
    private Double maxLockTime;       // 最大锁等待时间
    private Double maxQueryTime;      // 最大查询耗时

    // === 通知控制（智能通知策略） ===
    private LocalDateTime lastNotifiedTime;      // 上次发送通知的时间
    private Double lastNotifiedAvgTime;          // 上次通知时的平均耗时
    private Long todayOccurrenceCount;           // 今天（24小时内）的出现次数

    /**
     * 分析状态枚举
     */
    public enum AnalysisStatus {
        PENDING,   // 待分析（本次运行中的任务）
        SUCCESS,   // 已生成报告
        ERROR,     // 分析失败（会自动重试）
        ABANDONED, // 已放弃（上次运行中断，不会自动重试）
        FAILED     // 彻底失败（超过最大重试次数）
    }

    /**
     * 判断是否需要重新分析
     *
     * @return true-需要重新分析，false-不需要
     */
    public boolean shouldReAnalyze() {
        // 1. 上次分析失败
        if (this.status == AnalysisStatus.ERROR) {
            return true;
        }

        // 2. 距离首次发现超过 7 天
        if (this.lastSeenTime != null && this.firstSeenTime != null) {
            if (this.lastSeenTime.isAfter(this.firstSeenTime.plusDays(7))) {
                return true;
            }
        }

        return false;
    }

    /**
     * 判断是否需要通知（智能通知策略）
     *
     * @param coolDownHours           冷却期时间（小时）
     * @param degradationMultiplier   性能恶化倍率
     * @param highFrequencyThreshold  高频异常阈值（一天内的次数）
     * @return true-需要通知，false-跳过通知
     */
    public boolean shouldNotify(int coolDownHours, double degradationMultiplier, int highFrequencyThreshold) {
        // 场景 A：首次通知（从未通知过）
        if (this.lastNotifiedTime == null) {
            return true;
        }

        // 计算距离上次通知的时间间隔
        long hoursSinceLastNotify = java.time.Duration.between(this.lastNotifiedTime, LocalDateTime.now()).toHours();

        // 场景 B：性能显著恶化（二次唤醒）
        // 即使在冷却期内，如果耗时恶化超过倍率，立即通知
        if (this.avgQueryTime != null && this.lastNotifiedAvgTime != null) {
            double degradationRatio = this.avgQueryTime / this.lastNotifiedAvgTime;
            if (degradationRatio >= degradationMultiplier) {
                log.info("🚨 性能恶化警告: 耗时从 {} 增加到 {} (恶化 {}%)",
                        this.lastNotifiedAvgTime, this.avgQueryTime,
                        String.format("%.1f", (degradationRatio - 1) * 100));
                return true;
            }
        }

        // 场景 C：频率暴增（突发量警告）
        // 24小时内出现次数超过阈值
        if (this.todayOccurrenceCount != null && this.todayOccurrenceCount >= highFrequencyThreshold) {
            log.info("⚡ 高频异常警告: 24小时内已出现 {} 次", this.todayOccurrenceCount);
            return true;
        }

        // 场景 A：冷却期过滤（防骚扰）
        // 如果不满足上述特殊条件，且在冷却期内，跳过通知
        if (hoursSinceLastNotify < coolDownHours) {
            log.debug("冷却期过滤: 距离上次通知 {} 小时 < {} 小时",
                    hoursSinceLastNotify, coolDownHours);
            return false;
        }

        // 超过冷却期，可以通知
        return true;
    }

    /**
     * 更新统计信息
     *
     * @param queryTime     查询耗时
     * @param lockTime      锁等待时间
     * @param rowsSent      返回行数
     * @param rowsExamined  扫描行数
     */
    public void updateStatistics(Double queryTime, Double lockTime, Long rowsSent, Long rowsExamined) {
        // 更新计数
        this.occurrenceCount++;

        // 更新最近时间
        this.lastSeenTime = LocalDateTime.now();

        // 更新今天（24小时内）的出现次数
        updateTodayOccurrenceCount();

        // 更新平均查询耗时
        if (queryTime != null) {
            if (this.avgQueryTime == null) {
                this.avgQueryTime = queryTime;
            } else {
                // 简单平均：新值 = 旧值 + (新值 - 旧值) / count
                this.avgQueryTime = this.avgQueryTime + (queryTime - this.avgQueryTime) / this.occurrenceCount;
            }
            // 更新最大查询耗时
            if (this.maxQueryTime == null || queryTime > this.maxQueryTime) {
                this.maxQueryTime = queryTime;
            }
        }

        // 更新平均锁等待时间
        if (lockTime != null) {
            if (this.avgLockTime == null) {
                this.avgLockTime = lockTime;
            } else {
                this.avgLockTime = this.avgLockTime + (lockTime - this.avgLockTime) / this.occurrenceCount;
            }
            // 更新最大锁等待时间
            if (this.maxLockTime == null || lockTime > this.maxLockTime) {
                this.maxLockTime = lockTime;
            }
        }

        // 更新平均返回行数
        if (rowsSent != null) {
            if (this.avgRowsSent == null) {
                this.avgRowsSent = rowsSent;
            } else {
                this.avgRowsSent = this.avgRowsSent + (rowsSent - this.avgRowsSent) / this.occurrenceCount;
            }
            // 更新最大返回行数
            if (this.maxRowsSent == null || rowsSent > this.maxRowsSent) {
                this.maxRowsSent = rowsSent;
            }
        }

        // 更新最大扫描行数
        if (rowsExamined != null) {
            if (this.maxRowsExamined == null || rowsExamined > this.maxRowsExamined) {
                this.maxRowsExamined = rowsExamined;
            }
        }
    }

    /**
     * 更新今天（24小时内）的出现次数
     */
    private void updateTodayOccurrenceCount() {
        if (this.todayOccurrenceCount == null) {
            this.todayOccurrenceCount = 1L;
        } else {
            this.todayOccurrenceCount++;
        }
    }

    /**
     * 更新通知信息（记录本次通知的时间和耗时）
     *
     * @param notifiedAvgTime 本次通知时的平均耗时
     */
    public void updateNotificationInfo(Double notifiedAvgTime) {
        this.lastNotifiedTime = LocalDateTime.now();
        this.lastNotifiedAvgTime = notifiedAvgTime;

        // 重置今天的出现次数（通知后重置计数器）
        this.todayOccurrenceCount = 0L;

        log.debug("更新通知信息: fingerprint={}, lastNotifiedTime={}, lastNotifiedAvgTime={}",
                this.sqlFingerprint, this.lastNotifiedTime, this.lastNotifiedAvgTime);
    }
}
