package com.dbdoctor.repository;

import com.dbdoctor.entity.NotificationScheduleLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * 通知调度日志 Repository
 *
 * @author DB-Doctor
 * @version 1.0.0
 */
@Repository
public interface NotificationScheduleLogRepository extends JpaRepository<NotificationScheduleLog, Long> {

    /**
     * 根据状态查询
     */
    Page<NotificationScheduleLog> findByStatus(String status, Pageable pageable);

    /**
     * 根据时间范围查询
     */
    Page<NotificationScheduleLog> findByTriggerTimeBetween(
        LocalDateTime startTime,
        LocalDateTime endTime,
        Pageable pageable
    );

    /**
     * 根据状态和时间范围查询
     */
    Page<NotificationScheduleLog> findByStatusAndTriggerTimeBetween(
        String status,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Pageable pageable
    );

    /**
     * 查询所有日志（按触发时间倒序）
     */
    @Query("SELECT l FROM NotificationScheduleLog l ORDER BY l.triggerTime DESC")
    Page<NotificationScheduleLog> findAllOrderByTriggerTimeDesc(Pageable pageable);
}
