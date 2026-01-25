package com.dbdoctor.repository;

import com.dbdoctor.model.SlowQueryHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 慢查询分析历史 Repository
 * 操作 H2 数据库
 *
 * @author DB-Doctor
 * @version 2.0.0
 */
@Repository
public interface SlowQueryHistoryRepository extends JpaRepository<SlowQueryHistory, Long> {

    /**
     * 根据 SQL 指纹查询（用于去重判断）
     *
     * @param sqlFingerprint SQL 指纹
     * @return 查询结果
     */
    Optional<SlowQueryHistory> findBySqlFingerprint(String sqlFingerprint);

    /**
     * 查询最常见的慢查询（按出现次数排序）
     *
     * @param limit 限制数量
     * @return 最常见的慢查询列表
     */
    List<SlowQueryHistory> findTopByOrderByOccurrenceCountDesc();

    /**
     * 查询最近新增的慢查询（按首次发现时间排序）
     *
     * @return 最近的慢查询列表
     */
    List<SlowQueryHistory> findTop10ByOrderByFirstSeenTimeDesc();

    /**
     * 查询指定状态的记录
     *
     * @param status 状态
     * @return 指定状态的记录列表
     */
    List<SlowQueryHistory> findByStatus(SlowQueryHistory.AnalysisStatus status);

    /**
     * 查询指定数据库的慢查询
     *
     * @param dbName 数据库名
     * @return 慢查询列表
     */
    List<SlowQueryHistory> findByDbName(String dbName);

    /**
     * 统计指定状态的记录数量
     *
     * @param status 状态
     * @return 记录数量
     */
    long countByStatus(SlowQueryHistory.AnalysisStatus status);

    // ==================== V2.0 新增方法 ====================

    /**
     * 将所有 PENDING 状态的记录改为 ABANDONED
     * 用于应用启动时清理上次运行中断的记录
     *
     * @return 影响的行数
     */
    @Modifying
    @Query("""
        UPDATE SlowQueryHistory h
        SET h.status = 'ABANDONED',
            h.aiAnalysisReport = CONCAT(
                COALESCE(h.aiAnalysisReport, ''),
                '\n\n**系统说明**: 诊断在程序关闭时中断'
            )
        WHERE h.status = 'PENDING'
    """)
    int markPendingAsAbandoned();

    /**
     * 查询需要重试的 PENDING 任务
     * 用于 PENDING 补扫机制
     *
     * @param createdAfter     创建时间晚于此时间（本次运行的任务）
     * @param lastSeenBefore   最后见到时间早于此时间（超过 15 分钟未更新）
     * @param maxRetryCount    最大重试次数
     * @return 需要重试的任务列表
     */
    @Query("""
        SELECT h FROM SlowQueryHistory h
        WHERE h.status = 'PENDING'
          AND h.firstSeenTime > :createdAfter
          AND h.lastSeenTime < :lastSeenBefore
          AND h.retryCount < :maxRetryCount
        ORDER BY h.lastSeenTime ASC
    """)
    List<SlowQueryHistory> findPendingTasksForRetry(
        @Param("createdAfter") LocalDateTime createdAfter,
        @Param("lastSeenBefore") LocalDateTime lastSeenBefore,
        @Param("maxRetryCount") int maxRetryCount
    );

    /**
     * 原子自增出现次数
     * 使用数据库层面自增，避免并发导致的统计错误
     *
     * @param fingerprint SQL 指纹
     * @param now         当前时间（更新 lastSeenTime）
     * @return 影响的行数
     */
    @Modifying
    @Query("""
        UPDATE SlowQueryHistory h
        SET h.occurrenceCount = h.occurrenceCount + 1,
            h.lastSeenTime = :now
        WHERE h.sqlFingerprint = :fingerprint
    """)
    int incrementOccurrence(
        @Param("fingerprint") String fingerprint,
        @Param("now") LocalDateTime now
    );

    /**
     * 原子更新统计信息
     * 用于更新耗时、锁时间、行数等统计字段
     *
     * @param fingerprint    SQL 指纹
     * @param now            当前时间
     * @param queryTime      查询耗时
     * @param lockTime       锁等待时间
     * @param rowsSent       返回行数
     * @param rowsExamined   扫描行数
     * @return 影响的行数
     */
    @Modifying
    @Query("""
        UPDATE SlowQueryHistory h
        SET h.occurrenceCount = h.occurrenceCount + 1,
            h.lastSeenTime = :now,
            h.avgQueryTime = (h.avgQueryTime * h.occurrenceCount + :queryTime) / (h.occurrenceCount + 1),
            h.maxQueryTime = GREATEST(COALESCE(h.maxQueryTime, 0), :queryTime),
            h.avgLockTime = (h.avgLockTime * h.occurrenceCount + :lockTime) / (h.occurrenceCount + 1),
            h.maxLockTime = GREATEST(COALESCE(h.maxLockTime, 0), :lockTime),
            h.avgRowsSent = (h.avgRowsSent * h.occurrenceCount + :rowsSent) / (h.occurrenceCount + 1),
            h.maxRowsSent = GREATEST(COALESCE(h.maxRowsSent, 0), :rowsSent),
            h.maxRowsExamined = GREATEST(COALESCE(h.maxRowsExamined, 0), :rowsExamined)
        WHERE h.sqlFingerprint = :fingerprint
    """)
    int updateStatistics(
        @Param("fingerprint") String fingerprint,
        @Param("now") LocalDateTime now,
        @Param("queryTime") Double queryTime,
        @Param("lockTime") Double lockTime,
        @Param("rowsSent") Long rowsSent,
        @Param("rowsExamined") Long rowsExamined
    );
}
