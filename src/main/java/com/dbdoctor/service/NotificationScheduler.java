package com.dbdoctor.service;

import com.dbdoctor.config.DbDoctorProperties;
import com.dbdoctor.entity.SlowQueryTemplate;
import com.dbdoctor.model.QueryStatisticsDTO;
import com.dbdoctor.repository.SlowQueryTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知调度器
 * 负责定时批量发送慢查询通知
 *
 * 核心职责：
 * - 定时扫描 WAITING 状态的记录
 * - 批量发送邮件通知
 * - 更新通知状态为 SENT
 * - 处理发送失败的情况
 *
 * @author DB-Doctor
 * @version 1.0.0
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
     * 1. 查询所有 notificationStatus = WAITING 的记录
     * 2. 批量发送邮件通知
     * 3. 更新状态为 SENT
     * 4. 处理发送失败的情况
     */
    @Scheduled(cron = "${db-doctor.notify.batch-cron:0 0 * * * ?}")
    @Transactional
    public void batchSendNotifications() {
        long startTime = System.currentTimeMillis();
        log.info("📬 开始执行定时批量通知任务");

        try {
            // 1. 查询所有等待通知的记录
            List<SlowQueryTemplate> waitingTemplates = templateRepo.findAllByNotificationStatus(
                com.dbdoctor.common.enums.NotificationStatus.WAITING
            );

            if (waitingTemplates.isEmpty()) {
                log.info("✅ 没有等待通知的记录");
                return;
            }

            log.info("📋 找到 {} 条等待通知的记录", waitingTemplates.size());

            int successCount = 0;
            int failureCount = 0;

            // 2. 逐条发送通知
            for (SlowQueryTemplate template : waitingTemplates) {
                try {
                    // 构建统计信息 DTO
                    QueryStatisticsDTO stats = buildStatisticsFromTemplate(template);

                    // 发送通知
                    notifyService.sendNotification(template, stats);

                    // 更新状态为 SENT
                    template.setNotificationStatus(com.dbdoctor.common.enums.NotificationStatus.SENT);
                    template.setLastNotifiedTime(LocalDateTime.now());
                    templateRepo.save(template);

                    successCount++;
                    log.debug("✅ 通知发送成功: fingerprint={}", template.getSqlFingerprint());

                } catch (Exception e) {
                    failureCount++;
                    log.error("❌ 通知发送失败: fingerprint={}, error={}",
                        template.getSqlFingerprint(), e.getMessage(), e);

                    // 保持 WAITING 状态，下次重试
                    // 可以选择记录失败次数，超过阈值后标记为失败
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("✅ 定时批量通知任务完成: 成功={}, 失败={}, 总耗时={}ms",
                successCount, failureCount, duration);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("❌ 定时批量通知任务执行失败: 耗时={}ms", duration, e);
        }
    }

    /**
     * 从 Template 构建 QueryStatisticsDTO 对象
     *
     * @param template 模板记录
     * @return 统计信息 DTO
     */
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
     * 手动触发批量通知（用于测试）
     *
     * @return 处理的记录数
     */
    @Transactional
    public long manualBatchSend() {
        log.info("📬 手动触发批量通知任务");
        batchSendNotifications();

        // 返回当前等待通知的记录数
        return templateRepo.countByNotificationStatus(
            com.dbdoctor.common.enums.NotificationStatus.WAITING
        );
    }
}
