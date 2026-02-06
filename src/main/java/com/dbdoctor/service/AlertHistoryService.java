package com.dbdoctor.service;

import com.dbdoctor.entity.AlertHistory;
import com.dbdoctor.repository.AlertHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 告警历史管理服务
 *
 * <p>提供告警历史的保存、查询、统计和管理功能</p>
 *
 * @author DB-Doctor
 * @version 3.2.0
 * @since 3.2.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertHistoryService {

    private final AlertHistoryRepository alertHistoryRepository;

    /**
     * 异步保存告警历史（不阻塞业务流程）
     *
     * @param alertHistory 告警历史实体
     */
    @Async("monitoringExecutor")
    @Transactional
    public void saveAsync(AlertHistory alertHistory) {
        try {
            alertHistoryRepository.save(alertHistory);
            log.info("[告警历史] 告警已保存: id={}, ruleName={}, severity={}",
                    alertHistory.getId(), alertHistory.getRuleName(), alertHistory.getSeverity());
        } catch (Exception e) {
            log.error("[告警历史] 保存告警失败: ruleName={}", alertHistory.getRuleName(), e);
        }
    }

    /**
     * 同步保存告警历史
     *
     * @param alertHistory 告警历史实体
     * @return 保存后的实体
     */
    @Transactional
    public AlertHistory save(AlertHistory alertHistory) {
        return alertHistoryRepository.save(alertHistory);
    }

    /**
     * 根据ID查询告警历史
     *
     * @param id 告警ID
     * @return 告警历史对象（可能为空）
     */
    @Transactional(readOnly = true)
    public AlertHistory findById(Long id) {
        return alertHistoryRepository.findById(id).orElse(null);
    }

    /**
     * 查询所有告警历史（分页）
     *
     * @param pageable 分页参数
     * @return 告警列表
     */
    @Transactional(readOnly = true)
    public Page<AlertHistory> findAll(Pageable pageable) {
        return alertHistoryRepository.findAll(pageable);
    }

    /**
     * 根据状态查询告警列表（分页）
     *
     * @param status 状态
     * @param pageable 分页参数
     * @return 告警列表
     */
    @Transactional(readOnly = true)
    public Page<AlertHistory> findByStatus(String status, Pageable pageable) {
        return alertHistoryRepository.findByStatus(status, pageable);
    }

    /**
     * 根据严重程度查询告警列表（分页）
     *
     * @param severity 严重程度
     * @param pageable 分页参数
     * @return 告警列表
     */
    @Transactional(readOnly = true)
    public Page<AlertHistory> findBySeverity(String severity, Pageable pageable) {
        return alertHistoryRepository.findBySeverity(severity, pageable);
    }

    /**
     * 根据规则ID查询告警列表
     *
     * @param ruleId 规则ID
     * @return 告警列表
     */
    @Transactional(readOnly = true)
    public List<AlertHistory> findByRuleId(Long ruleId) {
        return alertHistoryRepository.findByRuleId(ruleId);
    }

    /**
     * 查询指定时间范围内的告警列表（分页）
     *
     * @param status 状态
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param pageable 分页参数
     * @return 告警列表
     */
    @Transactional(readOnly = true)
    public Page<AlertHistory> findByTimeRange(String status, LocalDateTime startTime,
                                               LocalDateTime endTime, Pageable pageable) {
        return alertHistoryRepository.findByStatusAndTriggeredAtBetween(status, startTime, endTime, pageable);
    }

    /**
     * 标记告警已解决
     *
     * @param alertId 告警ID
     * @param resolvedBy 解决人
     * @return 是否成功
     */
    @Transactional
    public boolean markAsResolved(Long alertId, String resolvedBy) {
        try {
            AlertHistory alert = alertHistoryRepository.findById(alertId).orElse(null);
            if (alert == null) {
                log.warn("[告警历史] 告警不存在: id={}", alertId);
                return false;
            }

            if (alert.isResolved()) {
                log.warn("[告警历史] 告警已经解决: id={}", alertId);
                return false;
            }

            alert.markAsResolved(resolvedBy);
            alertHistoryRepository.save(alert);

            log.info("[告警历史] 告警已标记为解决: id={}, ruleName={}, resolvedBy={}",
                    alertId, alert.getRuleName(), resolvedBy);
            return true;
        } catch (Exception e) {
            log.error("[告警历史] 标记告警解决失败: id={}", alertId, e);
            return false;
        }
    }

    /**
     * 获取告警统计信息
     *
     * @return 统计信息 Map
     */
    @Transactional(readOnly = true)
    public Map<String, Long> getStatistics() {
        // 统计各状态的告警数量
        List<Object[]> statusStats = alertHistoryRepository.countByStatusGroupBy();
        Map<String, Long> statistics = statusStats.stream()
            .collect(Collectors.toMap(
                row -> (String) row[0],
                row -> (Long) row[1]
            ));

        // 统计各严重程度的告警数量
        List<Object[]> severityStats = alertHistoryRepository.countBySeverityGroupBy();
        Map<String, Long> severityMap = severityStats.stream()
            .collect(Collectors.toMap(
                row -> (String) row[0],
                row -> (Long) row[1]
            ));

        // 合并统计结果
        statistics.put("severity_CRITICAL", severityMap.getOrDefault("CRITICAL", 0L));
        statistics.put("severity_WARNING", severityMap.getOrDefault("WARNING", 0L));
        statistics.put("severity_INFO", severityMap.getOrDefault("INFO", 0L));

        return statistics;
    }

    /**
     * 获取告警趋势数据
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 告警趋势列表
     */
    @Transactional(readOnly = true)
    public List<AlertHistory> getAlertTrend(LocalDateTime startTime, LocalDateTime endTime) {
        return alertHistoryRepository.findByTriggeredAtBetweenOrderByTriggeredAtAsc(startTime, endTime);
    }

    /**
     * 清理指定时间之前的告警历史
     *
     * @param beforeTime 时间阈值
     * @return 删除的记录数
     */
    @Transactional
    public int cleanOldAlerts(LocalDateTime beforeTime) {
        try {
            List<AlertHistory> oldAlerts = alertHistoryRepository.findAll().stream()
                .filter(alert -> alert.getCreatedAt().isBefore(beforeTime))
                .toList();

            if (!oldAlerts.isEmpty()) {
                alertHistoryRepository.deleteAll(oldAlerts);
                log.info("[告警历史] 清理旧告警记录: 时间阈值={}, 删除数量={}", beforeTime, oldAlerts.size());
            }

            return oldAlerts.size();
        } catch (Exception e) {
            log.error("[告警历史] 清理旧告警记录失败: 时间阈值={}", beforeTime, e);
            return 0;
        }
    }

    /**
     * 查询规则最后触发的告警
     *
     * @param ruleId 规则ID
     * @return 最后触发的告警
     */
    @Transactional(readOnly = true)
    public AlertHistory findLastAlertByRuleId(Long ruleId) {
        return alertHistoryRepository.findFirstByRuleIdOrderByTriggeredAtDesc(ruleId);
    }
}
