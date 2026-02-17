package com.dbdoctor.repository;

import com.dbdoctor.entity.NotificationHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知历史 Repository
 *
 * 核心职责：
 * - 支持按批次、指纹、时间范围查询
 * - 支持分页查询
 * - 支持统计信息查询
 *
 * @author DB-Doctor
 * @version 3.0.0
 */
@Repository
public interface NotificationHistoryRepository extends JpaRepository<NotificationHistory, Long> {

    /**
     * 按批次ID查询历史记录
     *
     * @param batchId 批次ID
     * @return 历史记录列表
     */
    List<NotificationHistory> findByBatchId(String batchId);

    /**
     * 按SQL指纹查询历史记录（按分析时间倒序）
     *
     * @param sqlFingerprint SQL指纹
     * @param pageable 分页参数
     * @return 历史记录分页结果
     */
    Page<NotificationHistory> findBySqlFingerprintOrderByAnalyzedTimeDesc(
            String sqlFingerprint,
            Pageable pageable
    );

    /**
     * 按时间范围查询历史记录（按发送时间倒序）
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param pageable 分页参数
     * @return 历史记录分页结果
     */
    Page<NotificationHistory> findBySentTimeBetweenOrderBySentTimeDesc(
            LocalDateTime startTime,
            LocalDateTime endTime,
            Pageable pageable
    );

    /**
     * 按通知状态查询历史记录
     *
     * @param status 通知状态
     * @param pageable 分页参数
     * @return 历史记录分页结果
     */
    Page<NotificationHistory> findByNotificationStatusOrderBySentTimeDesc(
            NotificationHistory.NotificationStatus status,
            Pageable pageable
    );

    /**
     * 统计指定时间范围内的通知数量（按状态分组）
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 统计结果：List<Object[]> { status, count }
     */
    @Query("SELECT h.notificationStatus, COUNT(h) FROM NotificationHistory h " +
           "WHERE h.sentTime BETWEEN :startTime AND :endTime " +
           "GROUP BY h.notificationStatus")
    List<Object[]> countByStatusInTimeRange(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * 统计指定时间范围内的通知数量（按严重程度分组）
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 统计结果：List<Object[]> { severity, count }
     */
    @Query("SELECT h.severity, COUNT(h) FROM NotificationHistory h " +
           "WHERE h.sentTime BETWEEN :startTime AND :endTime " +
           "GROUP BY h.severity")
    List<Object[]> countBySeverityInTimeRange(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * 查询指定批次ID的所有指纹（去重）
     *
     * @param batchId 批次ID
     * @return SQL指纹列表
     */
    @Query("SELECT DISTINCT h.sqlFingerprint FROM NotificationHistory h " +
           "WHERE h.batchId = :batchId")
    List<String> findDistinctFingerprintsByBatchId(@Param("batchId") String batchId);

    /**
     * 统计总通知数量
     *
     * @return 总数量
     */
    @Query("SELECT COUNT(h) FROM NotificationHistory h")
    Long totalCount();

    /**
     * 统计成功发送的数量
     *
     * @return 成功数量
     */
    @Query("SELECT COUNT(h) FROM NotificationHistory h WHERE h.notificationStatus = 'SENT'")
    Long countSuccess();

    /**
     * 统计失败发送的数量
     *
     * @return 失败数量
     */
    @Query("SELECT COUNT(h) FROM NotificationHistory h WHERE h.notificationStatus = 'FAILED'")
    Long countFailed();
}
