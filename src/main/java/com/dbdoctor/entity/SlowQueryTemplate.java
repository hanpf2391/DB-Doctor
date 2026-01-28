package com.dbdoctor.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 慢查询模板实体
 * 存储SQL模板和聚合分析结果（1:1关系，一个指纹对应一条记录）
 *
 * 核心职责：
 * - 存储SQL模板和指纹（去重标识）
 * - 存储AI分析报告
 * - 存储分析状态和元数据
 * - 不存储统计信息（统计信息从SlowQuerySample实时计算）
 *
 * @author DB-Doctor
 * @version 2.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "slow_query_template",
       indexes = {
           @Index(name = "idx_fingerprint", columnList = "sqlFingerprint", unique = true),
           @Index(name = "idx_status_time", columnList = "status, lastSeenTime")
       })
public class SlowQueryTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // === 核心：SQL 指纹和模板 ===

    /**
     * SQL指纹（唯一标识）
     * 用于去重和关联样本表
     */
    @Column(length = 64, unique = true, nullable = false)
    private String sqlFingerprint;

    /**
     * 参数化后的SQL模板
     * 例如：SELECT * FROM users WHERE id = ?
     */
    @Lob
    @Column(columnDefinition = "TEXT", nullable = false)
    private String sqlTemplate;

    // === 基本信息 ===

    /**
     * 所属数据库
     */
    @Column(length = 64)
    private String dbName;

    /**
     * 涉及的表名
     */
    @Column(length = 64)
    private String tableName;

    // === AI 分析结果 ===

    /**
     * AI生成的分析报告（Markdown格式）
     */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String aiAnalysisReport;

    // === 状态管理 ===

    /**
     * 分析状态
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AnalysisStatus status;

    // === 时间信息 ===

    /**
     * 首次发现时间
     */
    private LocalDateTime firstSeenTime;

    /**
     * 最近发现时间
     */
    private LocalDateTime lastSeenTime;

    /**
     * 上次发送通知的时间
     */
    private LocalDateTime lastNotifiedTime;

    /**
     * 上次通知时的平均耗时
     */
    private Double lastNotifiedAvgTime;

    /**
     * 上次执行EXPLAIN的时间
     */
    private LocalDateTime lastExplainTime;

    /**
     * 上次的EXPLAIN结果（JSON格式）
     * 用于对比执行计划是否变化
     */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String lastExplainJson;

    /**
     * 分析状态枚举
     */
    public enum AnalysisStatus {
        PENDING,   // 待分析
        SUCCESS,   // 分析成功
        ERROR,     // 分析失败
        ABANDONED, // 已放弃
        FAILED     // 彻底失败
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
     * @param currentAvgTime          当前平均耗时
     * @return true-需要通知，false-跳过通知
     */
    public boolean shouldNotify(int coolDownHours, double degradationMultiplier, Double currentAvgTime) {
        // 场景 A：首次通知（从未通知过）
        if (this.lastNotifiedTime == null) {
            return true;
        }

        // 计算距离上次通知的时间间隔
        long hoursSinceLastNotify = java.time.Duration.between(this.lastNotifiedTime, LocalDateTime.now()).toHours();

        // 场景 B：性能显著恶化（二次唤醒）
        if (currentAvgTime != null && this.lastNotifiedAvgTime != null) {
            double degradationRatio = currentAvgTime / this.lastNotifiedAvgTime;
            if (degradationRatio >= degradationMultiplier) {
                return true;
            }
        }

        // 场景 C：冷却期过滤（防骚扰）
        if (hoursSinceLastNotify < coolDownHours) {
            return false;
        }

        // 超过冷却期，可以通知
        return true;
    }

    /**
     * 更新通知信息（记录本次通知的时间和耗时）
     *
     * @param notifiedAvgTime 本次通知时的平均耗时
     */
    public void updateNotificationInfo(Double notifiedAvgTime) {
        this.lastNotifiedTime = LocalDateTime.now();
        this.lastNotifiedAvgTime = notifiedAvgTime;
    }
}
