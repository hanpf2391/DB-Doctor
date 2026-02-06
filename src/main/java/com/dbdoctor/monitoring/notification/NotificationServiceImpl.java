package com.dbdoctor.monitoring.notification;

import com.dbdoctor.entity.AlertHistory;
import com.dbdoctor.entity.NotificationConfig;
import com.dbdoctor.repository.NotificationConfigRepository;
import com.dbdoctor.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 通知服务实现
 *
 * <p>协调多个通知渠道发送告警通知</p>
 *
 * @author DB-Doctor
 * @version 3.2.0
 * @since 3.2.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationConfigRepository notificationConfigRepository;
    private final NotificationLogRepository notificationLogRepository;
    private final List<Notifier> notifiers;

    @Override
    @Async("monitoringExecutor")
    public NotificationResult sendNotification(AlertHistory alert) {
        log.info("[通知服务] 开始发送告警通知: alertId={}, ruleName={}", alert.getId(), alert.getRuleName());

        NotificationResult result = NotificationResult.builder()
            .alertId(alert.getId())
            .sentAt(LocalDateTime.now())
            .channelResults(new ArrayList<>())
            .build();

        try {
            // 查询所有启用的通知配置
            List<NotificationConfig> configs = notificationConfigRepository.findByEnabled(true);

            if (configs.isEmpty()) {
                log.warn("[通知服务] 没有启用的通知配置: alertId={}", alert.getId());
                result.setSuccess(false);
                return result;
            }

            // 筛选适用于当前告警级别的渠道
            List<NotificationConfig> applicableConfigs = configs.stream()
                .filter(config -> config.isApplicableForSeverity(alert.getSeverity()))
                .toList();

            if (applicableConfigs.isEmpty()) {
                log.warn("[通知服务] 没有适用于告警级别 {} 的通知配置", alert.getSeverity());
                result.setSuccess(false);
                return result;
            }

            // 并行发送到所有适用的渠道
            List<CompletableFuture<ChannelResult>> futures = applicableConfigs.stream()
                .map(config -> sendNotificationAsync(config, alert))
                .toList();

            // 等待所有发送完成
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            // 收集结果
            List<ChannelResult> channelResults = futures.stream()
                .map(CompletableFuture::join)
                .toList();

            result.setChannelResults(channelResults);

            // 判断是否全部成功
            boolean allSuccess = channelResults.stream().allMatch(ChannelResult::isSuccess);
            result.setSuccess(allSuccess);

            // 记录日志
            log.info("[通知服务] 通知发送完成: alertId={}, success={}, 总数={}, 成功={}, 失败={}",
                    alert.getId(), allSuccess, channelResults.size(),
                    result.getSuccessCount(), result.getFailedCount());

        } catch (Exception e) {
            log.error("[通知服务] 发送通知失败: alertId={}", alert.getId(), e);
            result.setSuccess(false);
        }

        return result;
    }

    @Override
    public boolean sendTestNotification(NotificationChannel channel) {
        try {
            // 创建测试告警
            AlertHistory testAlert = AlertHistory.builder()
                .id(0L)
                .ruleName("测试告警")
                .severity("INFO")
                .message("这是一条测试通知，如果您收到此消息，说明通知配置正确。")
                .triggeredAt(LocalDateTime.now())
                .build();

            // 查找对应渠道的通知器
            Notifier notifier = notifiers.stream()
                .filter(n -> n.supports(channel))
                .findFirst()
                .orElse(null);

            if (notifier == null) {
                log.error("[通知服务] 找不到通知器: channel={}", channel);
                return false;
            }

            // 发送测试通知
            ChannelResult result = notifier.notify(testAlert);

            log.info("[通知服务] 测试通知发送: channel={}, success={}", channel, result.isSuccess());

            return result.isSuccess();
        } catch (Exception e) {
            log.error("[通知服务] 发送测试通知失败: channel={}", channel, e);
            return false;
        }
    }

    /**
     * 异步发送通知到指定渠道
     */
    private CompletableFuture<ChannelResult> sendNotificationAsync(NotificationConfig config, AlertHistory alert) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 解析渠道类型
                NotificationChannel channel = NotificationChannel.valueOf(config.getChannel());

                // 查找对应的通知器
                Notifier notifier = notifiers.stream()
                    .filter(n -> n.supports(channel))
                    .findFirst()
                    .orElse(null);

                if (notifier == null) {
                    log.error("[通知服务] 找不到通知器: channel={}", channel);
                    return ChannelResult.failed(channel, "找不到通知器");
                }

                // 发送通知
                ChannelResult result = notifier.notify(alert);

                // 记录日志
                saveNotificationLog(alert.getId(), channel, result);

                return result;
            } catch (Exception e) {
                log.error("[通知服务] 通知发送失败: channel={}, alertId={}", config.getChannel(), alert.getId(), e);

                NotificationChannel channel = NotificationChannel.valueOf(config.getChannel());
                ChannelResult failedResult = ChannelResult.failed(channel, e.getMessage());

                // 记录失败日志
                saveNotificationLog(alert.getId(), channel, failedResult);

                return failedResult;
            }
        });
    }

    /**
     * 保存通知日志
     */
    private void saveNotificationLog(Long alertId, NotificationChannel channel, ChannelResult result) {
        try {
            com.dbdoctor.entity.NotificationLog logEntry = com.dbdoctor.entity.NotificationLog.builder()
                .alertId(alertId)
                .channel(channel.name())
                .status(result.isSuccess() ? "SUCCESS" : "FAILED")
                .errorMessage(result.getErrorMessage())
                .retryCount(0)
                .build();

            if (result.isSuccess()) {
                logEntry.markAsSuccess();
            } else {
                logEntry.markAsFailed(result.getErrorMessage());
            }

            notificationLogRepository.save(logEntry);
        } catch (Exception e) {
            log.error("[通知服务] 保存通知日志失败", e);
        }
    }
}
