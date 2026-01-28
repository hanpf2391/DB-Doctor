package com.dbdoctor.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 慢查询样本实体
 * 存储每次捕获的具体SQL和性能数据（1:N关系，一个指纹对应多条记录）
 *
 * 核心职责：
 * - 存储每次捕获的原始SQL（脱敏后）
 * - 存储每次的性能数据（查询耗时、锁时间、行数等）
 * - 用于实时计算统计信息
 * - 保留完整的历史样本
 *
 * 索引策略：
 * - idx_fingerprint_time: (sqlFingerprint, capturedAt) - 用于查询某SQL的所有样本
 * - idx_captured_at: (capturedAt) - 用于时间范围查询
 *
 * @author DB-Doctor
 * @version 2.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "slow_query_sample",
       indexes = {
           @Index(name = "idx_fingerprint_time", columnList = "sqlFingerprint,capturedAt"),
           @Index(name = "idx_captured_at", columnList = "capturedAt")
       })
public class SlowQuerySample {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // === 关联字段 ===

    /**
     * SQL指纹（关联slow_query_template表）
     */
    @Column(length = 64, nullable = false)
    private String sqlFingerprint;

    // === SQL信息 ===

    /**
     * 原始SQL（脱敏后）
     * 完全保留原始SQL的结构（包含空格、换行、注释等）
     */
    @Lob
    @Column(columnDefinition = "TEXT", nullable = false)
    private String originalSql;

    /**
     * 执行用户@主机
     */
    @Column(length = 255)
    private String userHost;

    // === 性能数据 ===

    /**
     * 查询耗时（秒）
     * H2 数据库使用 DOUBLE 类型，自动保留高精度
     */
    @Column(columnDefinition = "DOUBLE")
    private Double queryTime;

    /**
     * 锁等待时间（秒）
     * H2 数据库使用 DOUBLE 类型，自动保留高精度
     */
    @Column(columnDefinition = "DOUBLE")
    private Double lockTime;

    /**
     * 返回行数
     */
    private Long rowsSent;

    /**
     * 扫描行数
     */
    private Long rowsExamined;

    // === 时间信息 ===

    /**
     * 慢查询发生时间（从mysql.slow_log读取的start_time）
     */
    @Column(nullable = false)
    private LocalDateTime capturedAt;

    /**
     * 记录创建时间（自动填充）
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
