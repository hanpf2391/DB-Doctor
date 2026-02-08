package com.dbdoctor.controller;

import com.dbdoctor.service.NotifyService;
import com.dbdoctor.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 通知设置控制器
 *
 * 提供基于 system_config 表的通知配置 API：
 * - 获取所有通知配置
 * - 批量更新配置
 * - 发送测试邮件
 *
 * @author DB-Doctor
 * @version 3.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/notification-settings")
@RequiredArgsConstructor
public class NotificationSettingsController {

    private final SystemConfigService configService;
    private final NotifyService notifyService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 获取所有通知配置
     *
     * @return 配置 Map（键值对）
     */
    @GetMapping
    public Map<String, Object> getAllConfigs() {
        log.info("[通知设置] 获取所有通知配置");

        Map<String, Object> result = new HashMap<>();

        // ========== 通知渠道开关 ==========
        result.put("emailEnabled", configService.getBoolean("notify.email.enabled", false));
        result.put("dingtalkEnabled", configService.getBoolean("notify.dingtalk.enabled", false));
        result.put("feishuEnabled", configService.getBoolean("notify.feishu.enabled", false));
        result.put("wecomEnabled", configService.getBoolean("notify.wecom.enabled", false));

        // ========== SMTP 配置 ==========
        result.put("smtpHost", configService.getDecryptedValue("mail.smtp.host"));
        result.put("smtpPort", configService.getInteger("mail.smtp.port", 587));
        result.put("smtpUsername", configService.getDecryptedValue("mail.smtp.username"));
        // SMTP 密码不返回给前端（敏感信息）
        result.put("smtpFrom", configService.getDecryptedValue("mail.smtp.from"));
        result.put("smtpDisplayName", configService.getDecryptedValue("mail.smtp.display-name"));

        // ========== 批量报告收件人 ==========
        result.put("batchTo", getListFromConfig("mail.batch.to"));
        result.put("batchCc", getListFromConfig("mail.batch.cc"));

        // ========== 钉钉配置 ==========
        result.put("dingtalkWebhook", configService.getDecryptedValue("dingtalk.webhook"));
        // 钉钉密钥不返回给前端

        // ========== 飞书配置 ==========
        result.put("feishuWebhook", configService.getDecryptedValue("feishu.webhook"));

        // ========== 企业微信配置 ==========
        result.put("wecomWebhook", configService.getDecryptedValue("wecom.webhook"));

        // ========== 定时批量通知配置 ==========
        result.put("batchCron", configService.getDecryptedValue("notification.batch-cron"));
        result.put("enabledChannels", getListFromConfig("notification.enabled-channels"));

        return result;
    }

    /**
     * 批量更新配置
     *
     * @param request 配置请求
     * @return 更新结果
     */
    @PostMapping
    public Map<String, Object> updateConfigs(@RequestBody Map<String, String> request) {
        log.info("[通知设置] 批量更新配置，配置项数: {}", request.size());

        try {
            // 1. 验证配置
            validateConfigs(request);

            // 2. 保存到数据库
            Map<String, Object> result = configService.batchUpdateConfigs(request, "system");

            // 3. 触发热重载事件
            eventPublisher.publishEvent(new ConfigChangedEvent(request));

            log.info("[通知设置] 配置更新成功并已触发热重载");

            // 4. 返回统一格式的成功响应
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "配置保存成功并已立即生效");
            response.put("data", result);
            return response;

        } catch (Exception e) {
            log.error("[通知设置] 配置更新失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "配置保存失败: " + e.getMessage());
            return error;
        }
    }

    /**
     * 发送测试邮件
     *
     * @param request 测试邮件请求
     * @return 发送结果
     */
    @PostMapping("/test/email")
    public Map<String, Object> sendTestEmail(@RequestBody Map<String, Object> request) {
        log.info("[通知设置] 发送测试邮件");

        try {
            // 获取收件人列表
            @SuppressWarnings("unchecked")
            List<String> to = (List<String>) request.getOrDefault("to", Collections.emptyList());

            if (to.isEmpty()) {
                return Map.of("success", false, "message", "请指定收件人");
            }

            // 获取主题（可选）
            String subject = (String) request.get("subject");

            // 使用 NotifyService 发送测试邮件
            notifyService.sendTestEmail(to, subject);

            log.info("[通知设置] 测试邮件已发送: to={}", to);
            return Map.of(
                "success", true,
                "message", "测试邮件已发送",
                "to", to
            );

        } catch (Exception e) {
            log.error("[通知设置] 发送测试邮件失败", e);
            return Map.of(
                "success", false,
                "message", "发送测试邮件失败: " + e.getMessage()
            );
        }
    }

    /**
     * 从配置获取列表（逗号分隔）
     * 使用解密后的值，避免返回加密的邮箱地址
     *
     * @param configKey 配置键
     * @return 列表（如果配置为空或不存在，返回空列表）
     */
    private List<String> getListFromConfig(String configKey) {
        String value = configService.getDecryptedValue(configKey);
        if (value == null || value.trim().isEmpty()) {
            return Collections.emptyList();
        }

        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    /**
     * 验证配置
     *
     * @param configs 配置 Map
     * @throws IllegalArgumentException 验证失败时抛出
     */
    private void validateConfigs(Map<String, String> configs) {
        // 验证发件人邮箱格式
        String fromEmail = configs.get("mail.smtp.from");
        if (fromEmail != null && !fromEmail.trim().isEmpty()) {
            validateEmailFormat(fromEmail);
        }

        // 验证收件人邮箱格式
        String batchTo = configs.get("mail.batch.to");
        if (batchTo != null && !batchTo.trim().isEmpty()) {
            String[] emails = batchTo.split(",");
            for (String email : emails) {
                validateEmailFormat(email.trim());
            }
        }

        // 验证 SMTP 端口
        String smtpPort = configs.get("mail.smtp.port");
        if (smtpPort != null && !smtpPort.trim().isEmpty()) {
            try {
                int port = Integer.parseInt(smtpPort);
                if (port < 1 || port > 65535) {
                    throw new IllegalArgumentException("SMTP 端口必须在 1-65535 之间");
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("SMTP 端口格式不正确");
            }
        }

        // 验证 Cron 表达式（可选）
        String cron = configs.get("notification.batch-cron");
        if (cron != null && !cron.trim().isEmpty()) {
            validateCronExpression(cron);
        }
    }

    /**
     * 验证邮箱格式
     *
     * @param email 邮箱地址
     * @throws IllegalArgumentException 验证失败时抛出
     */
    private void validateEmailFormat(String email) {
        if (email == null || email.trim().isEmpty()) {
            return;
        }

        // 检查是否包含显示名称（用户应该只填写纯邮箱）
        if (email.contains("<") || email.contains(">")) {
            throw new IllegalArgumentException("发件人邮箱请填写纯邮箱地址，系统会自动添加显示名称");
        }

        // 简单的邮箱格式验证
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new IllegalArgumentException("邮箱格式不正确: " + email);
        }
    }

    /**
     * 验证 Cron 表达式
     *
     * @param cron Cron 表达式
     * @throws IllegalArgumentException 验证失败时抛出
     */
    private void validateCronExpression(String cron) {
        // 简单验证：Cron 表达式应该包含 5 或 6 个部分
        String[] parts = cron.trim().split("\\s+");
        if (parts.length < 5 || parts.length > 6) {
            throw new IllegalArgumentException("Cron 表达式格式不正确，应该包含 5 或 6 个部分");
        }
    }

    /**
     * 配置变更事件
     */
    public static class ConfigChangedEvent {
        private final Map<String, String> configs;

        public ConfigChangedEvent(Map<String, String> configs) {
            this.configs = configs;
        }

        public Map<String, String> getConfigs() {
            return configs;
        }
    }
}
