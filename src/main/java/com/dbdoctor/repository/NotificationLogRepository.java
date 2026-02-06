package com.dbdoctor.repository;

import com.dbdoctor.entity.NotificationLog;
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
 * 通知日志 Repository
 *
 * @author DB-Doctor
 * @version 3.2.0
 * @since 3.2.0
 */
@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {

    /**
     * 根据告警ID查询通知日志
     *
     * @param alertId 告警ID
     * @return 通知日志列表
     */
    List<NotificationLog> findByAlertId(Long alertId);

    /**
     * 根据发送状态查询日志（分页）
     *
     * @param status 状态
     * @param pageable 分页参数
     * @return 通知日志列表
     */
    Page<NotificationLog> findByStatus(String status, Pageable pageable);

    /**
     * 根据渠道查询日志（分页）
     *
     * @param channel 渠道
     * @param pageable 分页参数
     * @return 通知日志列表
     */
    Page<NotificationLog> findByChannel(String channel, Pageable pageable);

    /**
     * 查询指定时间范围内发送失败的日志
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 失败的日志列表
     */
    List<NotificationLog> findByStatusAndCreatedAtBetween(
        String status,
        LocalDateTime startTime,
        LocalDateTime endTime
    );

    /**
     * 统计各渠道的通知发送成功率
     *
     * @return 统计结果
     */
    @Query("SELECT nl.channel, " +
           "COUNT(CASE WHEN nl.status = 'SUCCESS' THEN 1 END) as successCount, " +
           "COUNT(CASE WHEN nl.status = 'FAILED' THEN 1 END) as failedCount, " +
           "COUNT(nl) as totalCount " +
           "FROM NotificationLog nl " +
           "GROUP BY nl.channel")
    List<Object[]> statisticsByChannel();

    /**
     * 删除指定时间之前的日志
     *
     * @param beforeTime 时间阈值
     * @return 删除的记录数
     */
    @Modifying
    @Query("DELETE FROM NotificationLog nl WHERE nl.createdAt < :beforeTime")
    int deleteOldLogs(@Param("beforeTime") LocalDateTime beforeTime);
}
