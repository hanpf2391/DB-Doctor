package com.dbdoctor.monitoring.notification;

import com.dbdoctor.entity.AlertHistory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import jakarta.mail.internet.MimeMessage;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * 邮件通知器
 *
 * <p>使用 SMTP 协议发送告警邮件</p>
 *
 * @author DB-Doctor
 * @version 3.2.0
 * @since 3.2.0
 */
@Slf4j
@Component
public class EmailNotifier implements Notifier {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Autowired
    private com.dbdoctor.service.SystemConfigService configService;

    @Value("${db-doctor.notification.mail.enabled:true}")
    private boolean enabled;

    @Value("${db-doctor.notification.mail.from:noreply@dbdoctor.com}")
    private String fromEmail;

    @Value("${db-doctor.notification.mail.subject-prefix:[DB-Doctor告警]}")
    private String subjectPrefix;

    @Value("${db-doctor.notification.mail.to.CRITICAL:}")
    private String criticalRecipients;

    @Value("${db-doctor.notification.mail.to.WARNING:}")
    private String warningRecipients;

    @Value("${db-doctor.notification.mail.to.INFO:}")
    private String infoRecipients;

    public EmailNotifier(com.dbdoctor.service.SystemConfigService configService) {
        this.configService = configService;
    }

    @Override
    public boolean supports(NotificationChannel channel) {
        return channel == NotificationChannel.EMAIL;
    }

    @Override
    public ChannelResult notify(AlertHistory alert) {
        if (!enabled) {
            log.debug("[邮件通知] 邮件通知未启用");
            return ChannelResult.success(NotificationChannel.EMAIL, "邮件通知未启用");
        }

        try {
            // 获取收件人列表
            List<String> recipients = getRecipients(alert.getSeverity());
            if (recipients.isEmpty()) {
                log.warn("[邮件通知] 没有配置收件人: severity={}", alert.getSeverity());
                return ChannelResult.failed(NotificationChannel.EMAIL, "没有配置收件人");
            }

            // 动态创建 JavaMailSender
            JavaMailSender dynamicMailSender = getOrCreateMailSender();
            if (dynamicMailSender == null) {
                log.warn("[邮件通知] 无法创建 JavaMailSender（配置不完整），跳过发送");
                return ChannelResult.failed(NotificationChannel.EMAIL, "邮件配置不完整");
            }

            // 创建邮件消息
            MimeMessage message = dynamicMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // 发件人
            helper.setFrom(fromEmail);

            // 收件人
            helper.setTo(recipients.toArray(new String[0]));

            // 主题
            String subject = String.format("%s %s - %s",
                subjectPrefix,
                alert.getSeverity(),
                alert.getRuleName());
            helper.setSubject(subject);

            // 内容（HTML 格式）
            String content = buildEmailContent(alert);
            helper.setText(content, true);

            // 发送
            dynamicMailSender.send(message);

            log.info("[邮件通知] 告警邮件发送成功: alertId={}, recipients={}", alert.getId(), recipients);

            return ChannelResult.success(NotificationChannel.EMAIL);

        } catch (Exception e) {
            log.error("[邮件通知] 告警邮件发送失败: alertId={}", alert.getId(), e);
            return ChannelResult.failed(NotificationChannel.EMAIL, e.getMessage());
        }
    }

    /**
     * 获取收件人列表
     */
    private List<String> getRecipients(String severity) {
        String recipientsStr = switch (severity) {
            case "CRITICAL" -> criticalRecipients;
            case "WARNING" -> warningRecipients;
            case "INFO" -> infoRecipients;
            default -> "";
        };

        if (recipientsStr == null || recipientsStr.trim().isEmpty()) {
            return List.of();
        }

        // 支持逗号或分号分隔
        return Arrays.stream(recipientsStr.split("[,;]"))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList();
    }

    /**
     * 构建邮件内容（HTML 格式）
     */
    private String buildEmailContent(AlertHistory alert) {
        String severityColor = getSeverityColor(alert.getSeverity());

        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, 'Microsoft YaHei', sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .alert-box {
                        border: 1px solid #ddd;
                        border-left-width: 5px;
                        border-left-color: %s;
                        padding: 15px;
                        margin: 10px 0;
                        background-color: #f9f9f9;
                        border-radius: 4px;
                    }
                    .alert-title { font-size: 18px; font-weight: bold; margin-bottom: 10px; color: #333; }
                    .alert-time { color: #666; font-size: 14px; }
                    .alert-message { margin-top: 15px; white-space: pre-wrap; font-family: monospace; }
                    .footer { margin-top: 30px; padding-top: 20px; border-top: 1px solid #eee; color: #999; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <h2>DB-Doctor 系统告警通知</h2>

                    <div class="alert-box">
                        <div class="alert-title">[%s] %s</div>
                        <div class="alert-time">触发时间: %s</div>
                        <div class="alert-message">%s</div>
                    </div>

                    <p><strong>请登录 DB-Doctor 系统查看详情并及时处理。</strong></p>

                    <div class="footer">
                        <p>这是一封自动发送的邮件，请勿直接回复。</p>
                        <p>DB-Doctor - MySQL 慢查询智能诊疗系统</p>
                    </div>
                </div>
            </body>
            </html>
            """,
            severityColor,
            alert.getSeverity(),
            alert.getRuleName(),
            alert.getTriggeredAt(),
            alert.getMessage()
        );
    }

    /**
     * 获取严重程度对应的颜色
     */
    private String getSeverityColor(String severity) {
        return switch (severity) {
            case "CRITICAL" -> "#d9534f";  // 红色
            case "WARNING" -> "#f0ad4e";   // 橙色
            case "INFO" -> "#5bc0de";      // 蓝色
            default -> "#5bc0de";
        };
    }

    /**
     * 动态创建 JavaMailSender（从数据库配置）
     *
     * @return JavaMailSender 实例，如果配置不完整则返回 null
     */
    private JavaMailSender getOrCreateMailSender() {
        try {
            // 1. 从数据库读取 SMTP 配置
            String host = configService.getString("mail.smtp.host");
            String portStr = configService.getString("mail.smtp.port");
            String username = configService.getDecryptedValue("mail.smtp.username");
            String password = configService.getDecryptedValue("mail.smtp.password");

            // 2. 验证配置是否完整
            if (host == null || host.trim().isEmpty()) {
                log.debug("[邮件通知] SMTP host 未配置");
                return null;
            }

            if (portStr == null || portStr.trim().isEmpty()) {
                log.debug("[邮件通知] SMTP port 未配置");
                return null;
            }

            if (username == null || username.trim().isEmpty()) {
                log.debug("[邮件通知] SMTP username 未配置");
                return null;
            }

            if (password == null || password.trim().isEmpty()) {
                log.debug("[邮件通知] SMTP password 未配置");
                return null;
            }

            // 3. 创建 JavaMailSender
            JavaMailSenderImpl mailSenderImpl = new JavaMailSenderImpl();
            mailSenderImpl.setHost(host);
            mailSenderImpl.setPort(Integer.parseInt(portStr));
            mailSenderImpl.setUsername(username);
            mailSenderImpl.setPassword(password);

            // 4. 配置 SMTP 属性
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
            props.put("mail.smtp.connectiontimeout", "5000");
            props.put("mail.smtp.timeout", "3000");
            props.put("mail.smtp.writetimeout", "5000");
            mailSenderImpl.setJavaMailProperties(props);

            log.debug("[邮件通知] 成功创建 JavaMailSender: host={}, port={}, username={}",
                host, portStr, username);

            return mailSenderImpl;

        } catch (Exception e) {
            log.error("[邮件通知] 创建 JavaMailSender 失败", e);
            return null;
        }
    }
}
