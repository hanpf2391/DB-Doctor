package com.dbdoctor.repository;

import com.dbdoctor.model.QueryStatisticsDTO;
import com.dbdoctor.entity.SlowQuerySample;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 慢查询样本Repository
 * 操作 slow_query_sample 表
 *
 * @author DB-Doctor
 * @version 2.1.0
 */
@Repository
public interface SlowQuerySampleRepository extends JpaRepository<SlowQuerySample, Long> {

    /**
     * 查询指定SQL指纹的所有样本（按时间倒序）
     *
     * @param sqlFingerprint SQL指纹
     * @return 样本列表
     */
    List<SlowQuerySample> findAllBySqlFingerprintOrderByCapturedAtDesc(String sqlFingerprint);

    /**
     * 分页查询指定SQL指纹的样本（按捕获时间倒序）
     *
     * @param sqlFingerprint SQL指纹
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<SlowQuerySample> findBySqlFingerprintOrderByCapturedAt(String sqlFingerprint, Pageable pageable);

    /**
     * 查询指定SQL指纹的最近N条样本
     *
     * @param sqlFingerprint SQL指纹
     * @param limit 限制数量
     * @return 样本列表
     */
    @Query("""
        SELECT s FROM SlowQuerySample s
        WHERE s.sqlFingerprint = :fingerprint
        ORDER BY s.capturedAt DESC
        LIMIT :limit
        """)
    List<SlowQuerySample> findRecentSamplesByFingerprint(
        @Param("fingerprint") String sqlFingerprint,
        @Param("limit") int limit
    );

    /**
     * 统计指定SQL的出现次数
     *
     * @param sqlFingerprint SQL指纹
     * @return 出现次数
     */
    long countBySqlFingerprint(String sqlFingerprint);

    /**
     * 查询指定SQL的第一次和最后一次捕获时间
     *
     * @param sqlFingerprint SQL指纹
     * @return [firstSeenTime, lastSeenTime]
     */
    @Query("""
        SELECT
            MIN(s.capturedAt) as firstSeenTime,
            MAX(s.capturedAt) as lastSeenTime
        FROM SlowQuerySample s
        WHERE s.sqlFingerprint = :fingerprint
        """)
    Object[] findFirstAndLastSeenTime(@Param("fingerprint") String sqlFingerprint);

    /**
     * 计算统计信息（聚合查询）
     * 这是核心查询：实时计算统计信息
     *
     * @param sqlFingerprint SQL指纹
     * @return 统计信息DTO
     */
    @Query("""
        SELECT new com.dbdoctor.model.QueryStatisticsDTO(
            COUNT(s) as occurrenceCount,
            AVG(s.queryTime) as avgQueryTime,
            MAX(s.queryTime) as maxQueryTime,
            AVG(s.lockTime) as avgLockTime,
            MAX(s.lockTime) as maxLockTime,
            AVG(s.rowsSent) as avgRowsSent,
            MAX(s.rowsSent) as maxRowsSent,
            AVG(s.rowsExamined) as avgRowsExamined,
            MAX(s.rowsExamined) as maxRowsExamined,
            MIN(s.capturedAt) as firstSeenTime,
            MAX(s.capturedAt) as lastSeenTime
        )
        FROM SlowQuerySample s
        WHERE s.sqlFingerprint = :fingerprint
        """)
    QueryStatisticsDTO calculateStatistics(@Param("fingerprint") String sqlFingerprint);

    /**
     * 统计指定时间之后出现的慢查询数量
     * 用于自适应轮询的负载统计
     *
     * @param capturedAt 捕获时间晚于此时间
     * @return 记录数量
     */
    long countByCapturedAtAfter(LocalDateTime capturedAt);

    /**
     * 根据 SQL 指纹删除所有样本
     *
     * @param sqlFingerprint SQL 指纹
     */
    @Modifying
    @Query("""
        DELETE FROM SlowQuerySample s
        WHERE s.sqlFingerprint = :fingerprint
        """)
    void deleteBySqlFingerprint(@Param("fingerprint") String sqlFingerprint);

    /**
     * 统计指定日期范围内每小时的慢查询数量
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 每小时的统计数量 [hour, count]
     */
    @Query("""
        SELECT
            HOUR(s.capturedAt) as hour,
            COUNT(s) as count
        FROM SlowQuerySample s
        WHERE s.capturedAt >= :startTime
          AND s.capturedAt < :endTime
        GROUP BY HOUR(s.capturedAt)
        ORDER BY hour
        """)
    List<Object[]> countByHourRange(
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );
}
