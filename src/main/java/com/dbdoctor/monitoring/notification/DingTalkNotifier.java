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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * 钉钉通知器
 *
 * <p>使用钉钉机器人 Webhook 发送告警消息</p>
 *
 * @author DB-Doctor
 * @version 3.2.0
 * @since 3.2.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DingTalkNotifier implements Notifier {

    private final RestTemplate restTemplate;

    @Value("${db-doctor.notification.webhook.dingtalk.enabled:false}")
    private boolean enabled;

    @Value("${db-doctor.notification.webhook.dingtalk.webhook:}")
    private String webhookUrl;

    @Value("${db-doctor.notification.webhook.dingtalk.secret:}")
    private String webhookSecret;

    @Override
    public boolean supports(NotificationChannel channel) {
        return channel == NotificationChannel.DINGTALK;
    }

    @Override
    public ChannelResult notify(AlertHistory alert) {
        if (!enabled) {
            log.debug("[钉钉通知] 钉钉通知未启用");
            return ChannelResult.success(NotificationChannel.DINGTALK, "钉钉通知未启用");
        }

        if (webhookUrl == null || webhookUrl.trim().isEmpty()) {
            log.warn("[钉钉通知] 钉钉 Webhook URL 未配置");
            return ChannelResult.failed(NotificationChannel.DINGTALK, "Webhook URL 未配置");
        }

        try {
            // 构建钉钉消息格式
            Map<String, Object> message = buildDingTalkMessage(alert);

            // 如果配置了密钥，添加签名
            String url = webhookUrl;
            if (webhookSecret != null && !webhookSecret.trim().isEmpty()) {
                long timestamp = System.currentTimeMillis();
                String sign = generateSign(timestamp, webhookSecret);
                url = webhookUrl + "&timestamp=" + timestamp + "&sign=" + sign;
            }

            // 发送 HTTP POST 请求
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(message, headers);

            Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);

            // 钉钉返回格式: {"errcode":0,"errmsg":"ok"}
            if (response != null) {
                Integer errcode = (Integer) response.get("errcode");
                if (errcode != null && errcode == 0) {
                    log.info("[钉钉通知] 告警通知发送成功: alertId={}", alert.getId());
                    return ChannelResult.success(NotificationChannel.DINGTALK);
                } else {
                    String errmsg = (String) response.get("errmsg");
                    log.error("[钉钉通知] 告警通知发送失败: alertId={}, errcode={}, errmsg={}",
                            alert.getId(), errcode, errmsg);
                    return ChannelResult.failed(NotificationChannel.DINGTALK, errmsg);
                }
            }

            return ChannelResult.failed(NotificationChannel.DINGTALK, "无响应");

        } catch (Exception e) {
            log.error("[钉钉通知] 告警通知发送失败: alertId={}", alert.getId(), e);
            return ChannelResult.failed(NotificationChannel.DINGTALK, e.getMessage());
        }
    }

    /**
     * 构建钉钉消息格式（Markdown 类型）
     */
    private Map<String, Object> buildDingTalkMessage(AlertHistory alert) {
        Map<String, Object> message = new HashMap<>();

        message.put("msgtype", "markdown");

        Map<String, String> markdown = new HashMap<>();
        markdown.put("title", String.format("【%s】%s", alert.getSeverity(), alert.getRuleName()));

        markdown.put("text", String.format("""
            ### DB-Doctor 系统告警

            > **级别**: %s
            > **规则**: %s
            > **时间**: %s

            #### 告警内容

            %s

            ---
            *请登录 DB-Doctor 系统查看详情*
            """,
            alert.getSeverity(),
            alert.getRuleName(),
            alert.getTriggeredAt(),
            alert.getMessage().replace("\n", "\n\n")  // Markdown 换行
        ));

        message.put("markdown", markdown);

        return message;
    }

    /**
     * 生成钉钉签名
     */
    private String generateSign(long timestamp, String secret) {
        try {
            String stringToSign = timestamp + "\n" + secret;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] signData = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
            return java.net.URLEncoder.encode(Base64.getEncoder().encodeToString(signData), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("[钉钉通知] 生成签名失败", e);
            return "";
        }
    }
}
