package com.dbdoctor.repository;

import com.dbdoctor.common.enums.NotificationStatus;
import com.dbdoctor.common.enums.SeverityLevel;
import com.dbdoctor.entity.SlowQueryTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 慢查询模板Repository
 * 操作 slow_query_template 表
 *
 * @author DB-Doctor
 * @version 2.1.0
 */
@Repository
public interface SlowQueryTemplateRepository extends JpaRepository<SlowQueryTemplate, Long> {

    /**
     * 根据SQL指纹查询
     *
     * @param sqlFingerprint SQL指纹
     * @return 查询结果
     */
    Optional<SlowQueryTemplate> findBySqlFingerprint(String sqlFingerprint);

    /**
     * 统计指定时间之后出现的慢查询数量
     * 用于自适应轮询的负载统计
     *
     * @param lastSeenTime 最后见到时间（晚于此时间）
     * @return 记录数量
     */
    long countByLastSeenTimeAfter(LocalDateTime lastSeenTime);

    /**
     * 查询指定状态的记录
     *
     * @param status 状态
     * @return 指定状态的记录列表
     */
    List<SlowQueryTemplate> findByStatus(SlowQueryTemplate.AnalysisStatus status);

    /**
     * 查询需要重试的PENDING任务
     *
     * @param firstSeenAfter  首次发现时间晚于此时间
     * @param lastSeenBefore  最近发现时间早于此时间
     * @return 需要重试的任务列表
     */
    @Query("""
        SELECT t FROM SlowQueryTemplate t
        WHERE t.status = 'PENDING'
          AND t.firstSeenTime > :firstSeenAfter
          AND t.lastSeenTime < :lastSeenBefore
        ORDER BY t.lastSeenTime ASC
        """)
    List<SlowQueryTemplate> findPendingTasksForRetry(
        @Param("firstSeenAfter") LocalDateTime firstSeenAfter,
        @Param("lastSeenBefore") LocalDateTime lastSeenBefore
    );

    /**
     * 将所有PENDING状态改为ABANDONED
     *
     * @return 影响的行数
     */
    @Modifying
    @Query("""
        UPDATE SlowQueryTemplate t
        SET t.status = 'ABANDONED',
            t.aiAnalysisReport = CONCAT(
                COALESCE(t.aiAnalysisReport, ''),
                '\n\n**系统说明**: 诊断在程序关闭时中断'
            )
        WHERE t.status = 'PENDING'
        """)
    int markPendingAsAbandoned();

    /**
     * 更新模板的最后见到时间
     *
     * @param fingerprint SQL指纹
     * @param lastSeenTime 最后见到时间
     * @return 影响的行数
     */
    @Modifying
    @Query("""
        UPDATE SlowQueryTemplate t
        SET t.lastSeenTime = :lastSeenTime
        WHERE t.sqlFingerprint = :fingerprint
        """)
    int updateLastSeenTime(
        @Param("fingerprint") String fingerprint,
        @Param("lastSeenTime") LocalDateTime lastSeenTime
    );

    // === 通知相关查询方法 ===

    /**
     * 查询所有等待通知的记录
     *
     * @param notificationStatus 通知状态
     * @return 等待通知的记录列表
     */
    List<SlowQueryTemplate> findAllByNotificationStatus(NotificationStatus notificationStatus);

    /**
     * 统计指定通知状态的记录数
     *
     * @param notificationStatus 通知状态
     * @return 记录数
     */
    long countByNotificationStatus(NotificationStatus notificationStatus);

    // === 报表查询相关方法 ===

    /**
     * 分页查询慢查询模板（支持筛选）
     *
     * @param dbName 数据库名（可选）
     * @param severityLevel 严重程度（可选）
     * @param pageable 分页参数
     * @return 分页结果
     */
    @Query("""
        SELECT t FROM SlowQueryTemplate t
        WHERE (:dbName IS NULL OR t.dbName = :dbName)
          AND (:severityLevel IS NULL OR t.severityLevel = :severityLevel)
        ORDER BY t.lastSeenTime DESC
        """)
    Page<SlowQueryTemplate> findByFilters(
            @Param("dbName") String dbName,
            @Param("severityLevel") SeverityLevel severityLevel,
            Pageable pageable
    );

    /**
     * 查询指定时间之前的记录（用于数据清理）
     *
     * @param cutoffDate 截止时间
     * @return 需要清理的记录列表
     */
    List<SlowQueryTemplate> findByLastSeenTimeBefore(LocalDateTime cutoffDate);
}
