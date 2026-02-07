package com.dbdoctor.service;

import com.dbdoctor.config.DbDoctorProperties;
import com.dbdoctor.dto.NotificationScheduleConfigDTO;
import com.dbdoctor.entity.SystemConfig;
import com.dbdoctor.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 定时批量通知配置服务
 *
 * <p>提供定时批量通知的配置查询、更新和手动触发功能</p>
 *
 * @author DB-Doctor
 * @version 3.2.0
 * @since 3.2.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationScheduleService {

    private final SystemConfigRepository configRepository;
    private final DbDoctorProperties properties;
    private final NotificationScheduler notificationScheduler;
    private final DynamicScheduleService dynamicScheduleService;

    // 配置键常量
    private static final String KEY_BATCH_CRON = "notification.batch-cron";
    private static final String KEY_ENABLED_CHANNELS = "notification.enabled-channels";

    // 默认值
    private static final String DEFAULT_BATCH_CRON = "0 0 * * * ?"; // 每小时
    private static final String DEFAULT_ENABLED_CHANNELS = "EMAIL"; // 只启用邮件

    /**
     * 查询配置
     *
     * @return 配置信息
     */
    public Map<String, Object> getConfig() {
        log.info("[定时通知配置服务] 查询配置");

        String batchCron = getConfigValue(KEY_BATCH_CRON, DEFAULT_BATCH_CRON);
        String channelsStr = getConfigValue(KEY_ENABLED_CHANNELS, DEFAULT_ENABLED_CHANNELS);
        List<String> enabledChannels = Arrays.asList(channelsStr.split(","));

        return Map.of(
                "batchCron", batchCron,
                "enabledChannels", enabledChannels,
                "cronDescription", getCronDescription(batchCron),
                "nextExecutionTime", getNextExecutionTime(batchCron),
                "lastExecutionTime", null // TODO: 从日志表查询
        );
    }

    /**
     * 更新配置
     *
     * @param config 配置 DTO
     * @return 更新后的配置
     */
    @Transactional
    public Map<String, Object> updateConfig(NotificationScheduleConfigDTO config) {
        log.info("[定时通知配置服务] 更新配置: {}", config);

        // 验证
        config.validateChannels();

        // 保存到数据库
        saveConfig(KEY_BATCH_CRON, config.getBatchCron());
        saveConfig(KEY_ENABLED_CHANNELS, String.join(",", config.getEnabledChannels()));

        log.info("[定时通知配置服务] 配置已保存到数据库");
        log.info("[定时通知配置服务] ✓ Cron 表达式: {}", config.getBatchCron());
        log.info("[定时通知配置服务] ✓ 启用渠道: {}", config.getEnabledChannels());

        // 🔥 热更新：重新调度定时任务（立即生效）
        dynamicScheduleService.scheduleOrUpdateTask(config.getBatchCron());

        // 同步更新配置对象
        properties.getNotify().setBatchCron(config.getBatchCron());

        log.info("[定时通知配置服务] ✅ 定时任务已重新调度，无需重启应用");

        return Map.of(
                "code", "SUCCESS",
                "message", "配置已保存并立即生效",
                "data", Map.of(
                        "batchCron", config.getBatchCron(),
                        "cronDescription", getCronDescription(config.getBatchCron()),
                        "nextExecutionTime", getNextExecutionTime(config.getBatchCron())
                )
        );
    }

    /**
     * 手动触发定时任务
     *
     * @param reason 触发原因
     * @return 执行结果
     */
    @Transactional
    public Map<String, Object> triggerNow(String reason) {
        log.info("[定时通知配置服务] 手动触发: reason={}", reason);

        try {
            // 调用定时任务
            notificationScheduler.batchSendNotifications();

            return Map.of(
                    "code", "SUCCESS",
                    "message", "定时任务已触发",
                    "data", Map.of(
                            "executionId", UUID.randomUUID().toString(),
                            "triggerTime", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                            "reason", reason
                    )
            );
        } catch (Exception e) {
            log.error("[定时通知配置服务] 触发失败", e);
            return Map.of(
                    "code", "ERROR",
                    "message", "触发失败: " + e.getMessage()
            );
        }
    }

    /**
     * 获取下次执行时间
     *
     * @param batchCron Cron 表达式
     * @return 下次执行时间
     */
    private String getNextExecutionTime(String batchCron) {
        try {
            LocalDateTime next = CronParserUtil.getNextExecutionTime(batchCron);
            return next.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception e) {
            log.warn("[定时通知配置服务] 计算下次执行时间失败: cron={}", batchCron, e);
            return null;
        }
    }

    /**
     * 获取 Cron 表达式描述
     *
     * @param batchCron Cron 表达式
     * @return 描述文字
     */
    private String getCronDescription(String batchCron) {
        Map<String, String> descriptions = Map.ofEntries(
                Map.entry("0 0 * * * ?", "每小时"),
                Map.entry("0 0 */2 * * ?", "每 2 小时"),
                Map.entry("0 0 */6 * * ?", "每 6 小时"),
                Map.entry("0 0 9 * * ?", "每天上午 9 点"),
                Map.entry("0 0 18 * * ?", "每天下午 18 点"),
                Map.entry("0 0 9,18 * * ?", "每天 9 点和 18 点"),
                Map.entry("0 0 9 * * MON", "每周一上午 9 点")
        );

        return descriptions.getOrDefault(batchCron, "自定义: " + batchCron);
    }

    /**
     * 获取配置值
     *
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    private String getConfigValue(String key, String defaultValue) {
        return configRepository.findByConfigKey(key)
                .map(SystemConfig::getConfigValue)
                .orElse(defaultValue);
    }

    /**
     * 保存配置
     *
     * @param key 配置键
     * @param value 配置值
     */
    private void saveConfig(String key, String value) {
        SystemConfig config = configRepository.findByConfigKey(key)
                .orElse(new SystemConfig());

        config.setConfigKey(key);
        config.setConfigValue(value);
        config.setConfigGroup("notification");
        config.setConfigDescription(getConfigDescription(key));
        config.setUpdatedBy("admin");
        config.setUpdatedTime(LocalDateTime.now());

        configRepository.save(config);
    }

    /**
     * 获取配置说明
     *
     * @param key 配置键
     * @return 配置说明
     */
    private String getConfigDescription(String key) {
        return switch (key) {
            case KEY_BATCH_CRON -> "定时批量通知的 Cron 表达式，控制任务执行频率";
            case KEY_ENABLED_CHANNELS -> "参与定时批量通知的通知渠道（逗号分隔）";
            default -> "";
        };
    }

    /**
     * Cron 表达式工具类
     */
    private static class CronParserUtil {
        /**
         * 获取下次执行时间（简化实现）
         * TODO: 完整实现需要使用 Quartz 或 CronTrigger 库
         */
        public static LocalDateTime getNextExecutionTime(String cronExpression) {
            LocalDateTime now = LocalDateTime.now();

            // 解析 Cron 表达式（简化版，只支持常见格式）
            if (cronExpression.matches("0 0 \\*/\\d+ \\* \\* \\* \\?")) {
                // 每 N 小时: "0 0 */2 * * ?"
                String hourPart = cronExpression.split("\\s+")[2];
                if (hourPart.startsWith("*/")) {
                    int hours = Integer.parseInt(hourPart.substring(2));
                    return now.plusHours(hours).withMinute(0).withSecond(0);
                }
            } else if (cronExpression.matches("0 0 \\d+ \\* \\* \\*")) {
                // 每天 H 点: "0 0 9 * * ?"
                int hour = Integer.parseInt(cronExpression.split("\\s+")[2]);
                return now.withHour(hour).withMinute(0).withSecond(0);
            } else if (cronExpression.matches("0 0 ([\\d]+,)+ \\* \\* \\?")) {
                // 每天 H1,H2 点: "0 0 9,18 * * ?"
                String hourPart = cronExpression.split("\\s+")[2];
                String[] hours = hourPart.split(",");
                int currentHour = now.getHour();
                for (String hour : hours) {
                    int h = Integer.parseInt(hour);
                    if (h > currentHour) {
                        return now.withHour(h).withMinute(0).withSecond(0);
                    }
                }
                // 如果今天所有时间都过了，使用第一个明天的
                return now.withHour(Integer.parseInt(hours[0])).plusDays(1).withMinute(0).withSecond(0);
            }

            // 默认：每小时
            return now.plusHours(1).withMinute(0).withSecond(0);
        }
    }
}
