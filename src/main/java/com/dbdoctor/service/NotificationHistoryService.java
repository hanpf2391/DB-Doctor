package com.dbdoctor.service;

import com.dbdoctor.entity.NotificationHistory;
import com.dbdoctor.repository.NotificationHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 通知历史服务
 *
 * 核心职责：
 * - 查询通知历史列表（分页）
 * - 查询通知详情（按批次ID）
 * - 查询时间线（按SQL指纹）
 * - 统计信息
 *
 * @author DB-Doctor
 * @version 3.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationHistoryService {

    private final NotificationHistoryRepository notificationHistoryRepo;

    /**
     * 查询历史列表（分页）
     *
     * @param startTime 开始时间（可选）
     * @param endTime 结束时间（可选）
     * @param status 通知状态（可选）
     * @param pageable 分页参数
     * @return 历史记录分页结果
     */
    public Page<NotificationHistory> queryHistoryList(
            LocalDateTime startTime,
            LocalDateTime endTime,
            NotificationHistory.NotificationStatus status,
            Pageable pageable) {

        if (startTime != null && endTime != null && status != null) {
            // 按时间范围和状态查询
            return notificationHistoryRepo.findBySentTimeBetweenOrderBySentTimeDesc(
                startTime, endTime, pageable);
        } else if (status != null) {
            // 按状态查询
            return notificationHistoryRepo.findByNotificationStatusOrderBySentTimeDesc(
                status, pageable);
        } else if (startTime != null && endTime != null) {
            // 按时间范围查询
            return notificationHistoryRepo.findBySentTimeBetweenOrderBySentTimeDesc(
                startTime, endTime, pageable);
        } else {
            // 查询全部
            return notificationHistoryRepo.findAll(pageable);
        }
    }

    /**
     * 查询通知详情（按批次ID）
     *
     * @param batchId 批次ID
     * @return 历史记录列表
     */
    public List<NotificationHistory> queryByBatchId(String batchId) {
        return notificationHistoryRepo.findByBatchId(batchId);
    }

    /**
     * 查询时间线（按SQL指纹）
     *
     * @param sqlFingerprint SQL指纹
     * @param pageable 分页参数
     * @return 历史记录分页结果（按分析时间倒序）
     */
    public Page<NotificationHistory> queryTimeline(String sqlFingerprint, Pageable pageable) {
        return notificationHistoryRepo.findBySqlFingerprintOrderByAnalyzedTimeDesc(
            sqlFingerprint, pageable);
    }

    /**
     * 统计信息
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 统计结果
     */
    public Map<String, Object> getStatistics(LocalDateTime startTime, LocalDateTime endTime) {
        // 总数统计
        Long totalCount = notificationHistoryRepo.totalCount();
        Long successCount = notificationHistoryRepo.countSuccess();
        Long failedCount = notificationHistoryRepo.countFailed();

        // 按状态统计（指定时间范围）
        List<Object[]> statusStats = notificationHistoryRepo.countByStatusInTimeRange(
            startTime, endTime);
        Map<String, Long> statusCountMap = statusStats.stream()
            .collect(Collectors.toMap(
                row -> ((NotificationHistory.NotificationStatus) row[0]).name(),
                row -> (Long) row[1]
            ));

        // 按严重程度统计（指定时间范围）
        List<Object[]> severityStats = notificationHistoryRepo.countBySeverityInTimeRange(
            startTime, endTime);
        Map<String, Long> severityCountMap = severityStats.stream()
            .collect(Collectors.toMap(
                row -> ((com.dbdoctor.common.enums.SeverityLevel) row[0]).name(),
                row -> (Long) row[1]
            ));

        return Map.of(
            "totalCount", totalCount,
            "successCount", successCount,
            "failedCount", failedCount,
            "statusStats", statusCountMap,
            "severityStats", severityCountMap
        );
    }

    /**
     * 查询批次中的所有指纹（去重）
     *
     * @param batchId 批次ID
     * @return SQL指纹列表
     */
    public List<String> getFingerprintsByBatchId(String batchId) {
        return notificationHistoryRepo.findDistinctFingerprintsByBatchId(batchId);
    }

    /**
     * 查询批次列表（聚合展示）
     *
     * 核心逻辑：
     * - 按 batchId 分组
     * - 计算每个批次的统计信息
     * - 支持分页查询
     *
     * @param startTime 开始时间（可选）
     * @param endTime 结束时间（可选）
     * @param status 通知状态（可选）
     * @param pageable 分页参数
     * @return 批次列表（包含聚合统计）
     */
    public Page<Map<String, Object>> queryBatchList(
            LocalDateTime startTime,
            LocalDateTime endTime,
            NotificationHistory.NotificationStatus status,
            Pageable pageable) {

        // 1. 查询符合条件的所有历史记录
        Page<NotificationHistory> historyPage;
        if (startTime != null && endTime != null) {
            historyPage = notificationHistoryRepo.findBySentTimeBetweenOrderBySentTimeDesc(
                startTime, endTime, pageable);
        } else if (status != null) {
            historyPage = notificationHistoryRepo.findByNotificationStatusOrderBySentTimeDesc(
                status, pageable);
        } else {
            historyPage = notificationHistoryRepo.findAll(pageable);
        }

        // 2. 按 batchId 分组并聚合
        Map<String, List<NotificationHistory>> groupedByBatch = historyPage.getContent().stream()
            .collect(Collectors.groupingBy(NotificationHistory::getBatchId));

        // 3. 构建批次聚合结果
        List<Map<String, Object>> batches = groupedByBatch.values().stream()
            .map(this::buildBatchSummary)
            .sorted((a, b) -> {
                LocalDateTime timeA = (LocalDateTime) a.get("sentTime");
                LocalDateTime timeB = (LocalDateTime) b.get("sentTime");
                return timeB.compareTo(timeA); // 降序
            })
            .toList();

        // 4. 构造分页结果
        return new org.springframework.data.domain.PageImpl<>(batches, pageable, batches.size());
    }

    /**
     * 构建批次聚合信息
     *
     * @param histories 同一批次的所有历史记录
     * @return 批次聚合信息
     */
    private Map<String, Object> buildBatchSummary(List<NotificationHistory> histories) {
        if (histories.isEmpty()) {
            return Map.of();
        }

        NotificationHistory first = histories.get(0);

        // 统计各严重程度的数量
        Map<com.dbdoctor.common.enums.SeverityLevel, Long> severityCount = histories.stream()
            .collect(Collectors.groupingBy(
                NotificationHistory::getSeverity,
                Collectors.counting()
            ));

        // 计算平均耗时和最大耗时
        DoubleSummaryStatistics queryTimeStats = histories.stream()
            .mapToDouble(h -> h.getAvgQueryTime() != null ? h.getAvgQueryTime() : 0.0)
            .summaryStatistics();

        // 计算时间窗口
        LocalDateTime windowStart = histories.stream()
            .map(NotificationHistory::getAnalyzedTime)
            .min(LocalDateTime::compareTo)
            .orElse(first.getAnalyzedTime());

        LocalDateTime windowEnd = histories.stream()
            .map(NotificationHistory::getAnalyzedTime)
            .max(LocalDateTime::compareTo)
            .orElse(first.getAnalyzedTime());

        // 使用 HashMap（因为字段数量超过了 Map.of() 的限制）
        Map<String, Object> result = new HashMap<>();
        result.put("batchId", first.getBatchId());
        result.put("sentTime", first.getSentTime());
        result.put("status", first.getNotificationStatus());
        result.put("totalCount", histories.size());
        result.put("windowStart", windowStart);
        result.put("windowEnd", windowEnd);
        result.put("criticalCount", severityCount.getOrDefault(com.dbdoctor.common.enums.SeverityLevel.CRITICAL, 0L));
        result.put("warningCount", severityCount.getOrDefault(com.dbdoctor.common.enums.SeverityLevel.WARNING, 0L));
        result.put("normalCount", severityCount.getOrDefault(com.dbdoctor.common.enums.SeverityLevel.NORMAL, 0L));
        result.put("avgQueryTime", queryTimeStats.getAverage());
        result.put("maxQueryTime", queryTimeStats.getMax());

        return result;
    }
}
