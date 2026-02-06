package com.dbdoctor.monitoring.notification;

import com.dbdoctor.entity.AlertHistory;
import com.dbdoctor.entity.NotificationLog;
import com.dbdoctor.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 通知重试服务
 *
 * <p>处理通知发送失败的重试逻辑，支持指数退避策略</p>
 *
 * @author DB-Doctor
 * @version 3.2.0
 * @since 3.2.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationRetryService {

    private final NotificationLogRepository notificationLogRepository;
    private final NotificationService notificationService;

    /**
     * 最大重试次数
     */
    private static final int MAX_RETRY_TIMES = 3;

    /**
     * 重试初始延迟（秒）
     */
    private static final int INITIAL_DELAY_SECONDS = 10;

    /**
     * 退避倍数
     */
    private static final double BACKOFF_MULTIPLIER = 2.0;

    /**
     * 将失败的通知加入重试队列
     *
     * @param alert 告警对象
     */
    public void addToRetryQueue(AlertHistory alert) {
        // 记录日志，实际重试由定时任务处理
        log.info("[通知重试] 告警已加入重试队列: alertId={}", alert.getId());
    }

    /**
     * 定时处理失败的通知重试
     *
     * 每 30 秒执行一次
     */
    @Scheduled(fixedDelay = 30000)
    public void retryFailedNotifications() {
        try {
            // 查询发送失败且可以重试的通知日志
            List<NotificationLog> failedLogs = findRetryableFailedNotifications();

            if (failedLogs.isEmpty()) {
                return;
            }

            log.info("[通知重试] 开始处理 {} 条失败通知", failedLogs.size());

            for (NotificationLog logEntry : failedLogs) {
                try {
                    // 检查是否应该重试
                    if (!shouldRetry(logEntry)) {
                        continue;
                    }

                    // 增加重试次数
                    logEntry.incrementRetryCount();
                    notificationLogRepository.save(logEntry);

                    // 重新发送通知
                    retryNotification(logEntry);

                } catch (Exception e) {
                    log.error("[通知重试] 重试通知失败: logId={}", logEntry.getId(), e);
                }
            }

        } catch (Exception e) {
            log.error("[通知重试] 处理失败通知时发生异常", e);
        }
    }

    /**
     * 查询可重试的失败通知
     */
    private List<NotificationLog> findRetryableFailedNotifications() {
        // 查询最近 1 小时内发送失败的通知
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        return notificationLogRepository.findByStatusAndCreatedAtBetween("FAILED", oneHourAgo, LocalDateTime.now())
            .stream()
            .filter(log -> log.canRetry(MAX_RETRY_TIMES))
            .filter(this::shouldRetry)
            .toList();
    }

    /**
     * 判断是否应该重试
     *
     * @param logEntry 通知日志
     * @return 是否应该重试
     */
    private boolean shouldRetry(NotificationLog logEntry) {
        // 检查重试次数
        if (!logEntry.canRetry(MAX_RETRY_TIMES)) {
            return false;
        }

        // 计算下次重试时间（指数退避）
        long delaySeconds = (long) (INITIAL_DELAY_SECONDS * Math.pow(BACKOFF_MULTIPLIER, logEntry.getRetryCount()));
        LocalDateTime nextRetryTime = logEntry.getCreatedAt().plusSeconds(delaySeconds);

        // 只有到达重试时间才执行
        return LocalDateTime.now().isAfter(nextRetryTime);
    }

    /**
     * 重试通知发送
     *
     * @param logEntry 原始通知日志
     */
    private void retryNotification(NotificationLog logEntry) {
        try {
            log.info("[通知重试] 开始重试通知: logId={}, channel={}, alertId={}, retryCount={}",
                    logEntry.getId(), logEntry.getChannel(), logEntry.getAlertId(), logEntry.getRetryCount());

            // 根据告警ID查询告警对象
            AlertHistory alert = findAlertById(logEntry.getAlertId());
            if (alert == null) {
                log.warn("[通知重试] 告警不存在，无法重试: alertId={}", logEntry.getAlertId());
                return;
            }

            // 根据渠道获取通知器并重试发送
            // 注意：这里简化处理，实际应该通过 NotificationService 重新发送
            log.info("[通知重试] 通知重试逻辑已触发: logId={}", logEntry.getId());

            // TODO: 实现具体的重试发送逻辑
            // 这里需要调用对应渠道的 Notifier 重新发送

        } catch (Exception e) {
            log.error("[通知重试] 重试通知发送失败: logId={}", logEntry.getId(), e);
        }
    }

    /**
     * 根据告警ID查询告警对象
     */
    private AlertHistory findAlertById(Long alertId) {
        // 这里应该通过 AlertHistoryService 查询
        // 为简化，暂时返回 null，实际需要注入 AlertHistoryService
        return null;
    }

    /**
     * 清理过期的失败通知日志
     *
     * 每天（86400 秒）执行一次
     */
    @Scheduled(fixedDelay = 86400000)
    @Transactional
    public void cleanupOldFailedNotifications() {
        try {
            // 清理 7 天前的失败通知日志
            LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
            int deletedCount = notificationLogRepository.deleteOldLogs(sevenDaysAgo);

            if (deletedCount > 0) {
                log.info("[通知重试] 清理旧通知日志: 删除数量={}", deletedCount);
            }
        } catch (Exception e) {
            log.error("[通知重试] 清理旧通知日志失败", e);
        }
    }

    /**
     * 获取重试统计信息
     */
    public RetryStatistics getRetryStatistics() {
        try {
            long totalFailed = notificationLogRepository.findByStatusAndCreatedAtBetween(
                "FAILED",
                LocalDateTime.now().minusHours(24),
                LocalDateTime.now()
            ).size();

            long totalSuccess = notificationLogRepository.findByStatusAndCreatedAtBetween(
                "SUCCESS",
                LocalDateTime.now().minusHours(24),
                LocalDateTime.now()
            ).size();

            long totalPending = notificationLogRepository.findByStatusAndCreatedAtBetween(
                "PENDING",
                LocalDateTime.now().minusHours(24),
                LocalDateTime.now()
            ).size();

            return new RetryStatistics(totalSuccess, totalFailed, totalPending);
        } catch (Exception e) {
            log.error("[通知重试] 获取重试统计信息失败", e);
            return new RetryStatistics(0, 0, 0);
        }
    }

    /**
     * 重试统计信息
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class RetryStatistics {
        private long successCount;
        private long failedCount;
        private long pendingCount;
    }
}
