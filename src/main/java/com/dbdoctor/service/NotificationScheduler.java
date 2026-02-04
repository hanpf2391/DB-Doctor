package com.dbdoctor.service;

import com.dbdoctor.config.DbDoctorProperties;
import com.dbdoctor.common.enums.NotificationStatus;
import com.dbdoctor.common.enums.SeverityLevel;
import com.dbdoctor.entity.SlowQueryTemplate;
import com.dbdoctor.model.NotificationBatchReport;
import com.dbdoctor.model.QueryStatisticsDTO;
import com.dbdoctor.repository.SlowQueryTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
    private final NotifyService notifyService;
    private final DbDoctorProperties properties;

    /**
     * 定时批量发送通知
     *
     * Cron 表达式：默认每小时执行一次（可通过配置文件修改）
     * 功能：
     * 1. 计算时间窗口（默认过去 60 分钟）
     * 2. 查询时间窗口内 WAITING 状态的记录
     * 3. 按严重程度分组统计
     * 4. 构建批次报告
     * 5. 批量发送聚合邮件
     * 6. 更新状态为 SENT
     */
    @Scheduled(cron = "${db-doctor.notify.batch-cron:0 0 * * * ?}")
    @Transactional
    public void batchSendNotifications() {
        long startTime = System.currentTimeMillis();
        log.info("📬 开始执行定时批量通知任务");

        try {
            // 1. 计算时间窗口
            LocalDateTime windowEnd = LocalDateTime.now();
            LocalDateTime windowStart = windowEnd.minusHours(1); // 默认过去 1 小时

            log.info("⏰ 时间窗口：{} ~ {}", windowStart, windowEnd);

            // 2. 查询时间窗口内等待通知的记录（按 lastSeenTime 过滤）
            List<SlowQueryTemplate> waitingTemplates = templateRepo
                .findByNotificationStatusAndLastSeenTimeBetween(
                    NotificationStatus.WAITING,
                    windowStart,
                    windowEnd
                );

            if (waitingTemplates.isEmpty()) {
                log.info("✅ 本时间窗口内没有等待通知的记录");
                return;
            }

            log.info("📋 找到 {} 条等待通知的指纹", waitingTemplates.size());

            // 3. 构建批次报告
            NotificationBatchReport report = buildBatchReport(waitingTemplates, windowStart, windowEnd);

            log.info("📊 批次统计 - 总计:{} | 🔥严重:{} | ⚠️中等:{} | 💡轻微:{}",
                report.getTotalCount(),
                report.getCriticalCount(),
                report.getMediumCount(),
                report.getLowCount()
            );

            // 4. 发送批量通知
            boolean sendSuccess = notifyService.sendBatchNotification(report);

            // 5. 更新所有记录的状态
            if (sendSuccess) {
                waitingTemplates.forEach(template -> {
                    template.setNotificationStatus(NotificationStatus.SENT);
                    template.setLastNotifiedTime(LocalDateTime.now());
                });
                templateRepo.saveAll(waitingTemplates);

                log.info("✅ 批量通知发送成功，共 {} 条指纹", waitingTemplates.size());
            } else {
                log.error("❌ 批量通知发送失败，保持 WAITING 状态");
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("⏱️ 批量通知任务完成，总耗时={}ms", duration);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("❌ 批量通知任务执行失败，耗时={}ms", duration, e);
        }
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

        // 1. 按严重程度分组
        Map<SeverityLevel, List<SlowQueryTemplate>> grouped = templates.stream()
            .collect(Collectors.groupingBy(t -> t.getSeverityLevel()));

        List<SlowQueryTemplate> critical = grouped.getOrDefault(SeverityLevel.CRITICAL, List.of());
        List<SlowQueryTemplate> medium = grouped.getOrDefault(SeverityLevel.WARNING, List.of());
        List<SlowQueryTemplate> low = grouped.getOrDefault(SeverityLevel.NORMAL, List.of());

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
}
