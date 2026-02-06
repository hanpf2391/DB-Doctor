package com.dbdoctor.monitoring.notification;

import com.dbdoctor.entity.AlertHistory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * 企业微信通知器
 *
 * <p>使用企业微信机器人 Webhook 发送告警消息</p>
 *
 * @author DB-Doctor
 * @version 3.2.0
 * @since 3.2.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeComNotifier implements Notifier {

    private final RestTemplate restTemplate;

    @Value("${db-doctor.notification.webhook.wecom.enabled:false}")
    private boolean enabled;

    @Value("${db-doctor.notification.webhook.wecom.webhook:}")
    private String webhookUrl;

    @Override
    public boolean supports(NotificationChannel channel) {
        return channel == NotificationChannel.WECOM;
    }

    @Override
    public ChannelResult notify(AlertHistory alert) {
        if (!enabled) {
            log.debug("[企业微信通知] 企业微信通知未启用");
            return ChannelResult.success(NotificationChannel.WECOM, "企业微信通知未启用");
        }

        if (webhookUrl == null || webhookUrl.trim().isEmpty()) {
            log.warn("[企业微信通知] 企业微信 Webhook URL 未配置");
            return ChannelResult.failed(NotificationChannel.WECOM, "Webhook URL 未配置");
        }

        try {
            // 构建企业微信消息格式
            Map<String, Object> message = buildWeComMessage(alert);

            // 发送 HTTP POST 请求
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(message, headers);

            Map<String, Object> response = restTemplate.postForObject(webhookUrl, entity, Map.class);

            // 企业微信返回格式: {"errcode":0,"errmsg":"ok"}
            if (response != null) {
                Integer errcode = (Integer) response.get("errcode");
                if (errcode != null && errcode == 0) {
                    log.info("[企业微信通知] 告警通知发送成功: alertId={}", alert.getId());
                    return ChannelResult.success(NotificationChannel.WECOM);
                } else {
                    String errmsg = (String) response.get("errmsg");
                    log.error("[企业微信通知] 告警通知发送失败: alertId={}, errcode={}, errmsg={}",
                            alert.getId(), errcode, errmsg);
                    return ChannelResult.failed(NotificationChannel.WECOM, errmsg);
                }
            }

            return ChannelResult.failed(NotificationChannel.WECOM, "无响应");

        } catch (Exception e) {
            log.error("[企业微信通知] 告警通知发送失败: alertId={}", alert.getId(), e);
            return ChannelResult.failed(NotificationChannel.WECOM, e.getMessage());
        }
    }

    /**
     * 构建企业微信消息格式（Markdown 类型）
     */
    private Map<String, Object> buildWeComMessage(AlertHistory alert) {
        Map<String, Object> message = new HashMap<>();

        message.put("msgtype", "markdown");

        Map<String, String> markdown = new HashMap<>();
        markdown.put("content", String.format("""
                > DB-Doctor 系统告警

                > **级别**: %s
                > **规则**: %s
                > **时间**: %s

                **告警内容**
                > %s

                ---
                请登录 DB-Doctor 系统查看详情
                """,
                alert.getSeverity(),
                alert.getRuleName(),
                alert.getTriggeredAt(),
                alert.getMessage().replace("\n", "\n> ")
        ));

        message.put("markdown", markdown);

        return message;
    }
}
