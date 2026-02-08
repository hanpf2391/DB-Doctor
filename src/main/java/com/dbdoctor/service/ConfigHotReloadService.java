package com.dbdoctor.service;

import com.dbdoctor.controller.NotificationSettingsController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 配置热重载服务
 *
 * <p>监听配置变更事件，自动刷新相关服务的配置，无需重启应用</p>
 *
 * @author DB-Doctor
 * @version 3.0.0
 */
@Slf4j
@Service
public class ConfigHotReloadService {

    private final SystemConfigService configService;
    private final NotifyService notifyService;

    // 可选注入（避免循环依赖）
    @Autowired(required = false)
    private DynamicScheduleService dynamicScheduleService;

    /**
     * 构造函数
     */
    @Autowired
    public ConfigHotReloadService(SystemConfigService configService, NotifyService notifyService) {
        this.configService = configService;
        this.notifyService = notifyService;
    }

    /**
     * 监听配置变更事件
     *
     * @param event 配置变更事件
     */
    @EventListener
    @Async
    public void onConfigChanged(NotificationSettingsController.ConfigChangedEvent event) {
        log.info("[配置热重载] 检测到配置变更事件");

        for (String configKey : event.getConfigs().keySet()) {
            log.debug("[配置热重载] 配置项变更: {}", configKey);

            // 邮件相关配置变更
            if (configKey.startsWith("mail.") || configKey.startsWith("notify.")) {
                handleMailConfigChanged(configKey);
            }

            // 定时批量通知配置变更
            if (configKey.startsWith("notification.batch-cron") ||
                configKey.startsWith("notification.enabled-channels")) {
                handleScheduleConfigChanged(configKey);
            }
        }

        log.info("[配置热重载] 配置热重载完成");
    }

    /**
     * 处理邮件配置变更
     *
     * @param configKey 配置键
     */
    private void handleMailConfigChanged(String configKey) {
        log.info("[配置热重载] 邮件配置变更: {}", configKey);

        // NotifyService 使用 SystemConfigService，配置会自动从数据库读取
        // 这里只需要记录日志，不需要额外操作
        if (configKey.equals("notify.email.enabled")) {
            boolean enabled = configService.getBoolean("notify.email.enabled", false);
            log.info("[配置热重载] 邮件通知开关已更新: {}", enabled ? "启用" : "禁用");
        }

        if (configKey.equals("mail.smtp.from") || configKey.equals("mail.smtp.display-name")) {
            String from = configService.getString("mail.smtp.from");
            String displayName = configService.getString("mail.smtp.display-name");
            if (displayName == null || displayName.trim().isEmpty()) {
                displayName = "DB-Doctor";
            }
            log.info("[配置热重载] 发件人配置已更新: {} <{}>", displayName, from);
        }

        if (configKey.equals("mail.batch.to") || configKey.equals("mail.batch.cc")) {
            log.info("[配置热重载] 收件人配置已更新");
        }
    }

    /**
     * 处理定时调度配置变更
     *
     * @param configKey 配置键
     */
    private void handleScheduleConfigChanged(String configKey) {
        log.info("[配置热重载] 定时调度配置变更: {}", configKey);

        if (dynamicScheduleService == null) {
            log.warn("[配置热重载] DynamicScheduleService 未注入，无法自动重新调度定时任务");
            return;
        }

        try {
            // 读取新的 Cron 表达式
            String batchCron = configService.getString("notification.batch-cron");
            if (batchCron == null || batchCron.trim().isEmpty()) {
                batchCron = "0 0 * * * ?";
            }

            // 重新调度定时任务
            dynamicScheduleService.scheduleOrUpdateTask(batchCron);

            log.info("[配置热重载] 定时任务已重新调度: {}", batchCron);

        } catch (Exception e) {
            log.error("[配置热重载] 重新调度定时任务失败", e);
        }
    }
}
