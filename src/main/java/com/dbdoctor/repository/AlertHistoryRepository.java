package com.dbdoctor.repository;

import com.dbdoctor.entity.AlertHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 告警历史 Repository
 *
 * @author DB-Doctor
 * @version 3.2.0
 * @since 3.2.0
 */
@Repository
public interface AlertHistoryRepository extends JpaRepository<AlertHistory, Long> {

    /**
     * 根据状态查询告警列表（分页）
     *
     * @param status 状态
     * @param pageable 分页参数
     * @return 告警列表
     */
    Page<AlertHistory> findByStatus(String status, Pageable pageable);

    /**
     * 根据严重程度查询告警列表（分页）
     *
     * @param severity 严重程度
     * @param pageable 分页参数
     * @return 告警列表
     */
    Page<AlertHistory> findBySeverity(String severity, Pageable pageable);

    /**
     * 根据规则ID查询告警列表
     *
     * @param ruleId 规则ID
     * @return 告警列表
     */
    List<AlertHistory> findByRuleId(Long ruleId);

    /**
     * 根据状态和触发时间范围查询（分页）
     *
     * @param status 状态
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param pageable 分页参数
     * @return 告警列表
     */
    Page<AlertHistory> findByStatusAndTriggeredAtBetween(
        String status,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Pageable pageable
    );

    /**
     * 统计各状态的告警数量
     *
     * @return 统计结果列表
     */
    @Query("SELECT a.status, COUNT(a) FROM AlertHistory a GROUP BY a.status")
    List<Object[]> countByStatusGroupBy();

    /**
     * 统计各严重程度的告警数量
     *
     * @return 统计结果列表
     */
    @Query("SELECT a.severity, COUNT(a) FROM AlertHistory a GROUP BY a.severity")
    List<Object[]> countBySeverityGroupBy();

    /**
     * 查询指定时间范围内的告警趋势
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 告警列表
     */
    List<AlertHistory> findByTriggeredAtBetweenOrderByTriggeredAtAsc(
        LocalDateTime startTime,
        LocalDateTime endTime
    );

    /**
     * 查询最后触发的告警（按规则）
     *
     * @param ruleId 规则ID
     * @return 最后触发的告警
     */
    AlertHistory findFirstByRuleIdOrderByTriggeredAtDesc(Long ruleId);
}
