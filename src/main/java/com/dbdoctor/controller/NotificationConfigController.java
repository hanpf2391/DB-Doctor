package com.dbdoctor.controller;

import com.dbdoctor.entity.NotificationConfig;
import com.dbdoctor.monitoring.notification.NotificationChannel;
import com.dbdoctor.monitoring.notification.NotificationService;
import com.dbdoctor.repository.NotificationConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 通知配置 API 控制器
 *
 * <p>提供通知配置管理的 REST API</p>
 *
 * @author DB-Doctor
 * @version 3.2.0
 * @since 3.2.0
 */
@Slf4j
@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
public class NotificationConfigController {

    private final NotificationConfigRepository notificationConfigRepository;
    private final NotificationService notificationService;

    /**
     * 获取所有通知配置
     *
     * GET /api/notification/config
     *
     * @return 通知配置列表
     */
    @GetMapping("/config")
    public Map<String, Object> getNotificationConfig() {
        log.info("[通知配置API] 查询通知配置");

        try {
            List<NotificationConfig> configs = notificationConfigRepository.findAll();

            return Map.of(
                "code", "SUCCESS",
                "message", "查询成功",
                "data", Map.of(
                    "channels", configs
                )
            );
        } catch (Exception e) {
            log.error("[通知配置API] 查询通知配置失败", e);
            return Map.of(
                "code", "ERROR",
                "message", "查询失败: " + e.getMessage()
            );
        }
    }

    /**
     * 更新通知配置
     *
     * POST /api/notification/config
     *
     * @param configs 配置列表
     * @return 操作结果
     */
    @PostMapping("/config")
    public Map<String, Object> updateNotificationConfig(@RequestBody Map<String, Object> requestBody) {
        log.info("[通知配置API] 更新通知配置");

        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> channels = (List<Map<String, Object>>) requestBody.get("channels");

            if (channels == null || channels.isEmpty()) {
                return Map.of(
                    "code", "INVALID_PARAM",
                    "message", "配置列表不能为空"
                );
            }

            // 更新或创建配置
            for (Map<String, Object> channelData : channels) {
                String channel = (String) channelData.get("channel");
                Boolean enabled = (Boolean) channelData.getOrDefault("enabled", false);
                String config = (String) channelData.get("config");
                String severityLevels = (String) channelData.get("severityLevels");

                // 查找现有配置或创建新配置
                NotificationConfig notificationConfig = notificationConfigRepository.findByChannel(channel)
                    .orElse(new NotificationConfig());

                notificationConfig.setChannel(channel);
                notificationConfig.setEnabled(enabled);
                notificationConfig.setConfig(config);
                notificationConfig.setSeverityLevels(severityLevels);

                notificationConfigRepository.save(notificationConfig);

                log.info("[通知配置API] 配置已更新: channel={}, enabled={}", channel, enabled);
            }

            return Map.of(
                "code", "SUCCESS",
                "message", "更新成功"
            );
        } catch (Exception e) {
            log.error("[通知配置API] 更新通知配置失败", e);
            return Map.of(
                "code", "ERROR",
                "message", "更新失败: " + e.getMessage()
            );
        }
    }

    /**
     * 发送测试通知
     *
     * POST /api/notification/test
     *
     * @param requestBody 请求体（包含 channel）
     * @return 发送结果
     */
    @PostMapping("/test")
    public Map<String, Object> sendTestNotification(@RequestBody Map<String, String> requestBody) {
        String channelStr = requestBody.get("channel");

        if (channelStr == null || channelStr.trim().isEmpty()) {
            return Map.of(
                "code", "INVALID_PARAM",
                "message", "渠道不能为空"
            );
        }

        try {
            NotificationChannel channel = NotificationChannel.valueOf(channelStr);
            boolean success = notificationService.sendTestNotification(channel);

            if (success) {
                return Map.of(
                    "code", "SUCCESS",
                    "message", "测试通知发送成功"
                );
            } else {
                return Map.of(
                    "code", "FAILED",
                    "message", "测试通知发送失败，请检查配置"
                );
            }
        } catch (IllegalArgumentException e) {
            return Map.of(
                "code", "INVALID_PARAM",
                "message", "无效的通知渠道: " + channelStr
            );
        } catch (Exception e) {
            log.error("[通知配置API] 发送测试通知失败: channel={}", channelStr, e);
            return Map.of(
                "code", "ERROR",
                "message", "发送失败: " + e.getMessage()
            );
        }
    }

    /**
     * 删除通知配置
     *
     * DELETE /api/notification/config/{channel}
     *
     * @param channel 渠道名称
     * @return 操作结果
     */
    @DeleteMapping("/config/{channel}")
    public Map<String, Object> deleteNotificationConfig(@PathVariable String channel) {
        log.info("[通知配置API] 删除通知配置: channel={}", channel);

        try {
            var config = notificationConfigRepository.findByChannel(channel);

            if (config.isEmpty()) {
                return Map.of(
                    "code", "NOT_FOUND",
                    "message", "配置不存在"
                );
            }

            notificationConfigRepository.delete(config.get());

            return Map.of(
                "code", "SUCCESS",
                "message", "删除成功"
            );
        } catch (Exception e) {
            log.error("[通知配置API] 删除通知配置失败: channel={}", channel, e);
            return Map.of(
                "code", "ERROR",
                "message", "删除失败: " + e.getMessage()
            );
        }
    }
}
