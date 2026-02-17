package com.dbdoctor.service;

import com.dbdoctor.config.DbDoctorProperties;
import com.dbdoctor.common.enums.NotificationStatus;
import com.dbdoctor.common.enums.SeverityLevel;
import com.dbdoctor.entity.NotificationHistory;
import com.dbdoctor.entity.NotificationScheduleLog;
import com.dbdoctor.entity.NotificationQueue;
import com.dbdoctor.entity.SlowQueryTemplate;
import com.dbdoctor.model.NotificationBatchReport;
import com.dbdoctor.model.QueryStatisticsDTO;
import com.dbdoctor.repository.NotificationHistoryRepository;
import com.dbdoctor.repository.NotificationQueueRepository;
import com.dbdoctor.repository.SlowQueryTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 通知调度器
 * 负责定时批量发送慢查询通知
 *
 * 核心职责：
 * - 定时扫描 WAITING 状态的记录
 * - 构建批次报告（时间窗口 + 统计信息）
 * - 批量发送聚合邮件通知
 * - 更新通知状态为 SENT
 *
 * @author DB-Doctor
 * @version 3.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationScheduler {

    private final SlowQueryTemplateRepository templateRepo;
    private final NotificationQueueRepository notificationQueueRepo;
    private final NotificationHistoryRepository notificationHistoryRepo;
    private final NotifyService notifyService;
    private final DbDoctorProperties properties;
    private final NotificationScheduleLogService notificationLogService;

    /**
     * 定时批量发送通知（V3.0.0 - 从通知队列读取，支持时间线聚合）
     *
     * Cron 表达式：默认每小时执行一次（可通过配置文件修改）
     * 功能：
     * 1. 计算时间窗口（默认过去 60 分钟）
     * 2. 从通知队列查询 PENDING 状态的记录（事件驱动，解决状态覆盖问题）
     * 3. 按批次 + 指纹聚合（支持时间线展示）
     * 4. 构建批次报告
     * 5. 批量发送聚合邮件
     * 6. 删除已发送的队列记录
     */
    @Scheduled(cron = "${db-doctor.notify.batch-cron:0 0 * * * ?}")
    @Transactional
    public void batchSendNotifications() {
        long startTime = System.currentTimeMillis();
        log.info("📬 开始执行定时批量通知任务");

        // 创建日志记录
        NotificationScheduleLog scheduleLog = NotificationScheduleLog.builder()
            .triggerTime(LocalDateTime.now())
            .windowStart(LocalDateTime.now().minusHours(1))
            .windowEnd(LocalDateTime.now())
            .waitingCount(0)
            .build();

        try {
            // 1. 计算时间窗口
            LocalDateTime windowEnd = LocalDateTime.now();
            LocalDateTime windowStart = windowEnd.minusHours(1); // 默认过去 1 小时

            log.info("⏰ 时间窗口：{} ~ {}", windowStart, windowEnd);

            // 2. 【修改】从通知队列查询待发送的记录（事件驱动）
            List<NotificationQueue> pendingQueues = notificationQueueRepo
                .findByStatusAndAnalyzedTimeBetween(
                    NotificationQueue.NotificationStatus.PENDING,
                    windowStart,
                    windowEnd
                );

            if (pendingQueues.isEmpty()) {
                log.info("✅ 本时间窗口内没有待发送的通知记录");
                return;
            }

            log.info("📋 找到 {} 条待发送的通知记录", pendingQueues.size());

            // 更新日志：等待数量
            scheduleLog.setWaitingCount(pendingQueues.size());
            scheduleLog.setWindowStart(windowStart);
            scheduleLog.setWindowEnd(windowEnd);

            // 3. 【新增】生成批次 ID（UUID，用于标识一次通知）
            String batchId = UUID.randomUUID().toString();
            log.debug("🆔 生成批次 ID: {}", batchId);

            // 4. 【新增】按 SQL 指纹聚合（支持时间线展示）
            Map<String, List<NotificationQueue>> groupedByFingerprint = pendingQueues.stream()
                .collect(Collectors.groupingBy(NotificationQueue::getSqlFingerprint));

            log.info("📊 聚合结果 - 去重后共 {} 个指纹", groupedByFingerprint.size());

            // 5. 【修改】构建批次报告（基于通知队列）
            NotificationBatchReport report = buildBatchReportFromQueue(
                pendingQueues,
                groupedByFingerprint,
                windowStart,
                windowEnd
            );

            log.info("📊 批次统计 - 总计:{} | 🔥严重:{} | ⚠️中等:{} | 💡轻微:{}",
                report.getTotalCount(),
                report.getCriticalCount(),
                report.getMediumCount(),
                report.getLowCount()
            );

            // 6. 发送批量通知（支持立即重试，最多3次）
            boolean sendSuccess = false;
            final int MAX_RETRY_TIMES = 3; // 写死，不需要配置

            for (int retryCount = 0; retryCount < MAX_RETRY_TIMES; retryCount++) {
                sendSuccess = notifyService.sendBatchNotification(report);

                if (sendSuccess) {
                    log.info("✅ 批量通知发送成功（第 {} 次尝试）", retryCount + 1);
                    break; // 发送成功，退出重试循环
                }

                // 发送失败
                log.warn("⚠️ 第 {} 次发送失败", retryCount + 1);

                // 如果还有重试机会，等待5秒后重试
                if (retryCount < MAX_RETRY_TIMES - 1) {
                    log.info("🔄 5秒后将进行第 {} 次重试...", retryCount + 2);
                    try {
                        Thread.sleep(5000); // 等待5秒后重试
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.error("❌ 重试等待被中断");
                        break;
                    }
                }
            }

            // 7. 根据发送结果处理队列记录
            if (sendSuccess) {
                // 成功：保存到历史表
                saveNotificationHistory(pendingQueues, batchId, LocalDateTime.now());
                log.info("✅ 已保存 {} 条记录到通知历史表", pendingQueues.size());

                // 删除队列记录（队列模式）
                notificationQueueRepo.deleteAll(pendingQueues);
                log.info("✅ 批量通知发送成功，已删除 {} 条队列记录", pendingQueues.size());

                // 同时更新 Template 表的状态（兼容现有逻辑）
                updateTemplateStatusFromQueue(pendingQueues);

                // 更新日志：成功
                scheduleLog.setStatus("SUCCESS");
                scheduleLog.setSentCount(pendingQueues.size());
            } else {
                // 失败：3次重试均失败，保存失败历史后删除队列记录
                saveFailedNotificationHistory(pendingQueues, batchId);
                log.error("❌ 已保存失败记录到通知历史表");

                notificationQueueRepo.deleteAll(pendingQueues);
                log.error("❌ 批量通知发送失败，已重试 {} 次，删除 {} 条队列记录",
                    MAX_RETRY_TIMES, pendingQueues.size());

                // 更新日志：失败
                scheduleLog.setStatus("FAILED");
                scheduleLog.setSentCount(0);
                scheduleLog.setFailedChannels("[\"EMAIL\"]");
            }

            // 8. 保存日志
            scheduleLog.setDurationMs(System.currentTimeMillis() - startTime);
            notificationLogService.save(scheduleLog);

            log.info("⏱️ 批量通知任务完成，总耗时={}ms", scheduleLog.getDurationMs());

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("❌ 批量通知任务执行失败，耗时={}ms", duration, e);

            // 记录失败日志
            scheduleLog.setStatus("FAILED");
            scheduleLog.setDurationMs(duration);
            scheduleLog.setSentCount(0);
            scheduleLog.setFailedChannels("[\"EMAIL\"]");
            notificationLogService.save(scheduleLog);
        }
    }

    /**
     * 从通知队列构建批次报告（支持时间线聚合）
     *
     * @param pendingQueues 待发送的队列记录列表
     * @param groupedByFingerprint 按指纹分组的数据
     * @param windowStart 时间窗口开始
     * @param windowEnd 时间窗口结束
     * @return 批次报告
     */
    private NotificationBatchReport buildBatchReportFromQueue(
            List<NotificationQueue> pendingQueues,
            Map<String, List<NotificationQueue>> groupedByFingerprint,
            LocalDateTime windowStart,
            LocalDateTime windowEnd) {

        // 1. 按严重程度分组（将 severity 为 null 的记录归为中等问题）
        Map<SeverityLevel, List<NotificationQueue>> groupedBySeverity = pendingQueues.stream()
            .collect(Collectors.groupingBy(q ->
                q.getSeverity() != null ? q.getSeverity() : SeverityLevel.WARNING
            ));

        // 2. 按优先级排序（影响力 = 平均耗时 × 出现次数）
        Comparator<NotificationQueue> priorityComparator = (a, b) -> {
            double scoreA = (a.getQueryTime() != null ? a.getQueryTime() : 0.0);
            double scoreB = (b.getQueryTime() != null ? b.getQueryTime() : 0.0);
            return Double.compare(scoreB, scoreA); // 降序
        };

        // 3. 转换为 SlowQueryTemplate 并按严重程度分组
        List<SlowQueryTemplate> criticalTemplates = groupedBySeverity
            .getOrDefault(SeverityLevel.CRITICAL, Collections.emptyList())
            .stream()
            .sorted(priorityComparator)
            .map(this::convertQueueToTemplate)
            .collect(Collectors.toList());

        List<SlowQueryTemplate> mediumTemplates = groupedBySeverity
            .getOrDefault(SeverityLevel.WARNING, Collections.emptyList())
            .stream()
            .sorted(priorityComparator)
            .map(this::convertQueueToTemplate)
            .collect(Collectors.toList());

        List<SlowQueryTemplate> lowTemplates = groupedBySeverity
            .getOrDefault(SeverityLevel.NORMAL, Collections.emptyList())
            .stream()
            .sorted(priorityComparator)
            .map(this::convertQueueToTemplate)
            .collect(Collectors.toList());

        // 4. 统计各严重程度的数量
        int criticalCount = criticalTemplates.size();
        int mediumCount = mediumTemplates.size();
        int lowCount = lowTemplates.size();

        // 5. 计算总样本数（队列中的记录数）
        long totalSamples = pendingQueues.size();

        // 6. 提取最需要关注的 Top 3 表
        List<String> topTables = extractTopProblematicTablesFromQueue(pendingQueues);

        return NotificationBatchReport.builder()
            .windowStart(windowStart)
            .windowEnd(windowEnd)
            .totalCount(groupedByFingerprint.size()) // 去重后的指纹数量
            .totalSamples(totalSamples)
            .criticalCount(criticalCount)
            .mediumCount(mediumCount)
            .lowCount(lowCount)
            .criticalIssues(criticalTemplates)
            .mediumIssues(mediumTemplates)
            .lowIssues(lowTemplates)
            .topProblematicTables(topTables)
            .build();
    }

    /**
     * 将 NotificationQueue 转换为 SlowQueryTemplate（临时对象，用于邮件渲染）
     *
     * @param queue 通知队列对象
     * @return SlowQueryTemplate 临时对象
     */
    private SlowQueryTemplate convertQueueToTemplate(NotificationQueue queue) {
        SlowQueryTemplate template = new SlowQueryTemplate();
        template.setSqlFingerprint(queue.getSqlFingerprint());
        template.setSqlTemplate(queue.getSqlTemplate());
        template.setDbName(queue.getDbName());
        template.setTableName(queue.getTableName());
        template.setSeverityLevel(queue.getSeverity());
        template.setAiAnalysisReport(queue.getAiReport());
        template.setAvgQueryTime(queue.getQueryTime());
        template.setAvgLockTime(queue.getLockTime());
        template.setMaxRowsExamined(queue.getRowsExamined());
        template.setFirstSeenTime(queue.getAnalyzedTime());
        template.setLastSeenTime(queue.getAnalyzedTime());
        template.setOccurrenceCount(1L); // 队列中每条记录代表一次分析
        return template;
    }

    /**
     * 从通知队列提取最需要关注的 Top 3 表
     * 按问题数量排序
     */
    private List<String> extractTopProblematicTablesFromQueue(List<NotificationQueue> queues) {
        return queues.stream()
            .filter(q -> q.getTableName() != null && !q.getTableName().isEmpty())
            .collect(Collectors.groupingBy(
                NotificationQueue::getTableName,
                Collectors.counting()
            ))
            .entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(3)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

    /**
     * 根据通知队列更新 Template 表的状态（兼容现有逻辑）
     *
     * @param pendingQueues 待发送的队列记录列表
     */
    private void updateTemplateStatusFromQueue(List<NotificationQueue> pendingQueues) {
        // 获取所有涉及到的指纹
        Set<String> fingerprints = pendingQueues.stream()
            .map(NotificationQueue::getSqlFingerprint)
            .collect(Collectors.toSet());

        // 更新 Template 表的状态
        fingerprints.forEach(fingerprint -> {
            templateRepo.findBySqlFingerprint(fingerprint).ifPresent(template -> {
                template.setNotificationStatus(NotificationStatus.SENT);
                template.setLastNotifiedTime(LocalDateTime.now());
                templateRepo.save(template);
            });
        });
    }

    /**
     * 构建批次报告
     *
     * @param templates 等待通知的指纹列表
     * @param windowStart 时间窗口开始
     * @param windowEnd 时间窗口结束
     * @return 批次报告
     */
    private NotificationBatchReport buildBatchReport(
            List<SlowQueryTemplate> templates,
            LocalDateTime windowStart,
            LocalDateTime windowEnd) {

        // 1. 按严重程度分组（将 severityLevel 为 null 的记录归为中等问题）
        Map<SeverityLevel, List<SlowQueryTemplate>> grouped = templates.stream()
            .collect(Collectors.groupingBy(t ->
                t.getSeverityLevel() != null ? t.getSeverityLevel() : SeverityLevel.WARNING
            ));

        List<SlowQueryTemplate> critical = new ArrayList<>(grouped.getOrDefault(SeverityLevel.CRITICAL, Collections.emptyList()));
        List<SlowQueryTemplate> medium = new ArrayList<>(grouped.getOrDefault(SeverityLevel.WARNING, Collections.emptyList()));
        List<SlowQueryTemplate> low = new ArrayList<>(grouped.getOrDefault(SeverityLevel.NORMAL, Collections.emptyList()));

        // 2. 按优先级排序（影响力 = 平均耗时 × 出现次数）
        critical.sort(priorityComparator());
        medium.sort(priorityComparator());
        low.sort(priorityComparator());

        // 3. 计算总样本数
        long totalSamples = templates.stream()
            .mapToLong(t -> t.getOccurrenceCount() != null ? t.getOccurrenceCount() : 0L)
            .sum();

        // 4. 提取最需要关注的 Top 3 表
        List<String> topTables = extractTopProblematicTables(templates);

        return NotificationBatchReport.builder()
            .windowStart(windowStart)
            .windowEnd(windowEnd)
            .totalCount(templates.size())
            .totalSamples(totalSamples)
            .criticalCount(critical.size())
            .mediumCount(medium.size())
            .lowCount(low.size())
            .criticalIssues(critical)
            .mediumIssues(medium)
            .lowIssues(low)
            .topProblematicTables(topTables)
            .build();
    }

    /**
     * 优先级比较器：影响力越大，优先级越高
     * 影响力 = 平均耗时 × 出现次数
     */
    private Comparator<SlowQueryTemplate> priorityComparator() {
        return (a, b) -> {
            double scoreA = (a.getAvgQueryTime() != null ? a.getAvgQueryTime() : 0.0)
                * (a.getOccurrenceCount() != null ? a.getOccurrenceCount() : 1L);
            double scoreB = (b.getAvgQueryTime() != null ? b.getAvgQueryTime() : 0.0)
                * (b.getOccurrenceCount() != null ? b.getOccurrenceCount() : 1L);
            return Double.compare(scoreB, scoreA); // 降序
        };
    }

    /**
     * 提取最需要关注的 Top 3 表
     * 按问题数量排序
     */
    private List<String> extractTopProblematicTables(List<SlowQueryTemplate> templates) {
        return templates.stream()
            .filter(t -> t.getTableName() != null && !t.getTableName().isEmpty())
            .collect(Collectors.groupingBy(
                SlowQueryTemplate::getTableName,
                Collectors.counting()
            ))
            .entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(3)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

    /**
     * 手动触发批量通知（用于测试）
     *
     * @return 批次报告
     */
    @Transactional
    public NotificationBatchReport manualBatchSend() {
        log.info("📬 手动触发批量通知任务");
        batchSendNotifications();
        return null; // TODO: 返回批次报告用于测试
    }

    /**
     * 从 Template 构建 QueryStatisticsDTO 对象
     * 保留用于单条通知的兼容性
     *
     * @param template 模板记录
     * @return 统计信息 DTO
     * @deprecated 使用批次报告替代
     */
    @Deprecated
    private QueryStatisticsDTO buildStatisticsFromTemplate(SlowQueryTemplate template) {
        return QueryStatisticsDTO.builder()
                .fingerprint(template.getSqlFingerprint())
                .dbName(template.getDbName())
                .tableName(template.getTableName())
                .firstSeenTime(template.getFirstSeenTime())
                .lastSeenTime(template.getLastSeenTime())
                .occurrenceCount(template.getOccurrenceCount() != null ? template.getOccurrenceCount() : 0L)
                .avgQueryTime(template.getAvgQueryTime() != null ? template.getAvgQueryTime() : 0.0)
                .maxQueryTime(template.getMaxQueryTime() != null ? template.getMaxQueryTime() : 0.0)
                .avgLockTime(template.getAvgLockTime() != null ? template.getAvgLockTime() : 0.0)
                .maxLockTime(template.getMaxLockTime() != null ? template.getMaxLockTime() : 0.0)
                .avgRowsSent(template.getAvgRowsSent())
                .maxRowsSent(template.getMaxRowsSent() != null ? template.getMaxRowsSent() : 0L)
                .avgRowsExamined(template.getAvgRowsExamined())
                .maxRowsExamined(template.getMaxRowsExamined() != null ? template.getMaxRowsExamined() : 0L)
                .build();
    }

    /**
     * 保存通知历史记录（发送成功）
     *
     * @param pendingQueues 待发送的队列记录列表
     * @param batchId 批次ID
     * @param sentTime 发送时间
     */
    private void saveNotificationHistory(List<NotificationQueue> pendingQueues, String batchId, LocalDateTime sentTime) {
        try {
            List<NotificationHistory> historyList = pendingQueues.stream()
                .map(queue -> NotificationHistory.builder()
                        .batchId(batchId)
                        .sqlFingerprint(queue.getSqlFingerprint())
                        .dbName(queue.getDbName())
                        .tableName(queue.getTableName())
                        .analyzedTime(queue.getAnalyzedTime())
                        .sentTime(sentTime)
                        .severity(queue.getSeverity())
                        .occurrenceCount(1L) // 队列中每条记录代表一次分析
                        .avgQueryTime(queue.getQueryTime())
                        .maxQueryTime(queue.getQueryTime())
                        .sqlTemplate(queue.getSqlTemplate())
                        .aiReport(queue.getAiReport())
                        .notificationStatus(NotificationHistory.NotificationStatus.SENT)
                        .errorMessage(null)
                        .build())
                .collect(Collectors.toList());

            notificationHistoryRepo.saveAll(historyList);
            log.debug("📋 已保存 {} 条通知历史记录", historyList.size());
        } catch (Exception e) {
            log.error("保存通知历史失败: batchId={}", batchId, e);
            // 不抛出异常，避免影响主流程
        }
    }

    /**
     * 保存失败的通知历史记录
     *
     * @param pendingQueues 待发送的队列记录列表
     * @param batchId 批次ID
     */
    private void saveFailedNotificationHistory(List<NotificationQueue> pendingQueues, String batchId) {
        try {
            List<NotificationHistory> historyList = pendingQueues.stream()
                .map(queue -> NotificationHistory.builder()
                        .batchId(batchId)
                        .sqlFingerprint(queue.getSqlFingerprint())
                        .dbName(queue.getDbName())
                        .tableName(queue.getTableName())
                        .analyzedTime(queue.getAnalyzedTime())
                        .sentTime(null) // 失败时没有发送时间
                        .severity(queue.getSeverity())
                        .occurrenceCount(1L)
                        .avgQueryTime(queue.getQueryTime())
                        .maxQueryTime(queue.getQueryTime())
                        .sqlTemplate(queue.getSqlTemplate())
                        .aiReport(queue.getAiReport())
                        .notificationStatus(NotificationHistory.NotificationStatus.FAILED)
                        .errorMessage("已重试3次，仍然发送失败")
                        .build())
                .collect(Collectors.toList());

            notificationHistoryRepo.saveAll(historyList);
            log.debug("📋 已保存 {} 条失败通知历史记录", historyList.size());
        } catch (Exception e) {
            log.error("保存失败通知历史失败: batchId={}", batchId, e);
            // 不抛出异常，避免影响主流程
        }
    }
}
