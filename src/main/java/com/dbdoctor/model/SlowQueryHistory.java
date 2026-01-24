package com.dbdoctor.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 慢查询分析历史实体
 * 存储在 H2 数据库中，用于去重和历史记录
 *
 * @author DB-Doctor
 * @version 2.0.0
 */
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
    private AnalysisStatus status;   // PENDING / SUCCESS / ERROR

    // === 统计信息（去重的关键） ===
    @Builder.Default
    private Long occurrenceCount = 1L;       // 出现次数
    private LocalDateTime firstSeenTime;     // 首次发现时间
    private LocalDateTime lastSeenTime;      // 最近发现时间

    // === 元数据 ===
    private Double avgQueryTime;      // 平均查询耗时
    private Long maxRowsExamined;     // 最大扫描行数

    /**
     * 分析状态枚举
     */
    public enum AnalysisStatus {
        PENDING,   // 待分析
        SUCCESS,   // 已生成
        ERROR      // 失败
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
     * 更新统计信息
     *
     * @param queryTime 查询耗时
     * @param rowsExamined 扫描行数
     */
    public void updateStatistics(Double queryTime, Long rowsExamined) {
        // 更新计数
        this.occurrenceCount++;

        // 更新最近时间
        this.lastSeenTime = LocalDateTime.now();

        // 更新平均耗时
        if (this.avgQueryTime == null) {
            this.avgQueryTime = queryTime;
        } else {
            // 简单平均：新值 = 旧值 + (新值 - 旧值) / count
            this.avgQueryTime = this.avgQueryTime + (queryTime - this.avgQueryTime) / this.occurrenceCount;
        }

        // 更新最大扫描行数
        if (rowsExamined != null) {
            if (this.maxRowsExamined == null || rowsExamined > this.maxRowsExamined) {
                this.maxRowsExamined = rowsExamined;
            }
        }
    }
}
