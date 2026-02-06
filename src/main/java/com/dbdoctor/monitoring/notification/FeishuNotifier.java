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
import java.util.List;
import java.util.Map;

/**
 * 飞书通知器
 *
 * <p>使用飞书机器人 Webhook 发送告警消息</p>
 *
 * @author DB-Doctor
 * @version 3.2.0
 * @since 3.2.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FeishuNotifier implements Notifier {

    private final RestTemplate restTemplate;

    @Value("${db-doctor.notification.webhook.feishu.enabled:false}")
    private boolean enabled;

    @Value("${db-doctor.notification.webhook.feishu.webhook:}")
    private String webhookUrl;

    @Override
    public boolean supports(NotificationChannel channel) {
        return channel == NotificationChannel.FEISHU;
    }

    @Override
    public ChannelResult notify(AlertHistory alert) {
        if (!enabled) {
            log.debug("[飞书通知] 飞书通知未启用");
            return ChannelResult.success(NotificationChannel.FEISHU, "飞书通知未启用");
        }

        if (webhookUrl == null || webhookUrl.trim().isEmpty()) {
            log.warn("[飞书通知] 飞书 Webhook URL 未配置");
            return ChannelResult.failed(NotificationChannel.FEISHU, "Webhook URL 未配置");
        }

        try {
            // 构建飞书消息格式
            Map<String, Object> message = buildFeishuMessage(alert);

            // 发送 HTTP POST 请求
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(message, headers);

            Map<String, Object> response = restTemplate.postForObject(webhookUrl, entity, Map.class);

            // 飞书返回格式: {"code":0,"msg":"ok"}
            if (response != null) {
                Integer code = (Integer) response.get("code");
                if (code != null && code == 0) {
                    log.info("[飞书通知] 告警通知发送成功: alertId={}", alert.getId());
                    return ChannelResult.success(NotificationChannel.FEISHU);
                } else {
                    String msg = (String) response.get("msg");
                    log.error("[飞书通知] 告警通知发送失败: alertId={}, code={}, msg={}",
                            alert.getId(), code, msg);
                    return ChannelResult.failed(NotificationChannel.FEISHU, msg);
                }
            }

            return ChannelResult.failed(NotificationChannel.FEISHU, "无响应");

        } catch (Exception e) {
            log.error("[飞书通知] 告警通知发送失败: alertId={}", alert.getId(), e);
            return ChannelResult.failed(NotificationChannel.FEISHU, e.getMessage());
        }
    }

    /**
     * 构建飞书消息格式（富文本类型）
     */
    private Map<String, Object> buildFeishuMessage(AlertHistory alert) {
        Map<String, Object> message = new HashMap<>();

        message.put("msg_type", "interactive");

        Map<String, Object> card = new HashMap<>();

        // 配置卡片头部
        Map<String, Object> header = new HashMap<>();
        header.put("title", Map.of(
            "content", String.format("【%s】%s", alert.getSeverity(), alert.getRuleName()),
            "tag", "plain_text"
        ));
        header.put("template", getSeverityColor(alert.getSeverity()));
        card.put("header", header);

        // 配置卡片内容
        Map<String, Object> element = new HashMap<>();
        element.put("tag", "div");
        element.put("text", Map.of(
            "tag", "lark_md",
            "content", buildCardContent(alert)
        ));
        card.put("elements", List.of(element));

        message.put("card", card);

        return message;
    }

    /**
     * 构建卡片内容
     */
    private String buildCardContent(AlertHistory alert) {
        return String.format("""
                **DB-Doctor 系统告警**

                **级别**: %s
                **规则**: %s
                **时间**: %s

                **告警内容**
                %s

                ---
                *请登录 DB-Doctor 系统查看详情*
                """,
                alert.getSeverity(),
                alert.getRuleName(),
                alert.getTriggeredAt(),
                alert.getMessage().replace("\n", "\n\n")
        );
    }

    /**
     * 获取严重程度对应的颜色
     */
    private String getSeverityColor(String severity) {
        return switch (severity) {
            case "CRITICAL" -> "red";
            case "WARNING" -> "orange";
            case "INFO" -> "blue";
            default -> "grey";
        };
    }
}
