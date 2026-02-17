package com.dbdoctor.repository;

import com.dbdoctor.entity.NotificationQueue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知队列数据访问接口
 *
 * @author DB-Doctor
 * @version 3.0.0
 */
@Repository
public interface NotificationQueueRepository extends JpaRepository<NotificationQueue, Long> {

    /**
     * 查询待发送的记录
     *
     * @return 待发送的记录列表
     */
    List<NotificationQueue> findByStatus(NotificationQueue.NotificationStatus status);

    /**
     * 查询时间窗口内的待发送记录
     *
     * @param status 状态
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 记录列表
     */
    @Query("SELECT nq FROM NotificationQueue nq " +
           "WHERE nq.status = :status " +
           "AND nq.analyzedTime BETWEEN :startTime AND :endTime " +
           "ORDER BY nq.analyzedTime ASC")
    List<NotificationQueue> findByStatusAndAnalyzedTimeBetween(
        @Param("status") NotificationQueue.NotificationStatus status,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    /**
     * 查询所有待发送的记录
     *
     * @return 待发送的记录列表
     */
    @Query("SELECT nq FROM NotificationQueue nq " +
           "WHERE nq.status = 'PENDING' " +
           "ORDER BY nq.analyzedTime ASC")
    List<NotificationQueue> findAllPending();

    /**
     * 查询指定指纹的待发送记录
     *
     * @param fingerprint SQL指纹
     * @return 记录列表
     */
    @Query("SELECT nq FROM NotificationQueue nq " +
           "WHERE nq.status = 'PENDING' " +
           "AND nq.sqlFingerprint = :fingerprint " +
           "ORDER BY nq.analyzedTime ASC")
    List<NotificationQueue> findPendingByFingerprint(@Param("fingerprint") String fingerprint);

    /**
     * 删除已发送的记录（定期清理）
     *
     * @param beforeTime 早于此时间的记录
     */
    @Query("DELETE FROM NotificationQueue nq " +
           "WHERE nq.status = 'SENT' " +
           "AND nq.sentTime < :beforeTime")
    void deleteSentRecordsBefore(@Param("beforeTime") LocalDateTime beforeTime);
}
