package com.dbdoctor.service;

import com.dbdoctor.config.DbDoctorProperties;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 通知服务
 * 负责将慢查询诊断报告发送给用户（邮件、Webhook）
 *
 * @author DB-Doctor
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotifyService {

    private final JavaMailSender mailSender;
    private final DbDoctorProperties properties;

    /**
     * 通知频率控制（防止同一问题频繁通知）
     * Key: SQL 指纹
     * Value: 上次通知时间
     */
    private final ConcurrentHashMap<String, LocalDateTime> lastNotifyTime = new ConcurrentHashMap<>();

    /**
     * 发送邮件通知
     *
     * @param report 诊断报告（Markdown 格式）
     */
    public void sendNotification(String report) {
        // 检查是否启用邮件通知
        if (!properties.getNotify().getEmail().getEnabled()) {
            log.debug("邮件通知已禁用，跳过发送");
            return;
        }

        try {
            // 构建邮件内容
            String subject = buildSubject();
            String content = buildEmailContent(report);

            // 发送邮件
            sendEmail(subject, content);

            log.info("✅ 邮件通知发送成功");

        } catch (Exception e) {
            log.error("❌ 邮件通知发送失败", e);
        }
    }

    /**
     * 发送邮件通知（带去重控制）
     *
     * @param fingerprint SQL 指纹
     * @param report      诊断报告
     */
    public void sendNotificationWithRateLimit(String fingerprint, String report) {
        // 检查通知频率
        if (!shouldNotify(fingerprint)) {
            log.debug("通知频率限制：fingerprint={} 在冷却时间内，跳过通知", fingerprint);
            return;
        }

        // 发送通知
        sendNotification(report);

        // 更新最后通知时间
        lastNotifyTime.put(fingerprint, LocalDateTime.now());
    }

    /**
     * 判断是否应该通知（频率控制）
     *
     * @param fingerprint SQL 指纹
     * @return true-应该通知，false-跳过通知
     */
    private boolean shouldNotify(String fingerprint) {
        LocalDateTime lastTime = lastNotifyTime.get(fingerprint);
        if (lastTime == null) {
            return true; // 首次通知
        }

        // 计算距离上次通知的时间间隔
        long interval = properties.getNotify().getNotifyInterval();
        long elapsedSeconds = java.time.Duration.between(lastTime, LocalDateTime.now()).getSeconds();

        return elapsedSeconds >= interval;
    }

    /**
     * 发送邮件
     *
     * @param subject 邮件主题
     * @param content 邮件内容（HTML 格式）
     */
    private void sendEmail(String subject, String content) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // 发件人
            helper.setFrom(properties.getNotify().getEmail().getFrom());

            // 收件人
            String[] toAddresses = properties.getNotify().getEmail().getTo().toArray(new String[0]);
            helper.setTo(toAddresses);

            // 抄送（如果有）
            if (properties.getNotify().getEmail().getCc() != null &&
                !properties.getNotify().getEmail().getCc().isEmpty()) {
                String[] ccAddresses = properties.getNotify().getEmail().getCc().toArray(new String[0]);
                helper.setCc(ccAddresses);
            }

            // 邮件主题
            helper.setSubject(subject);

            // 邮件内容（HTML）
            helper.setText(content, true);

            // 发送
            mailSender.send(message);

            log.info("邮件发送成功: to={}, subject={}", String.join(",", toAddresses), subject);

        } catch (Exception e) {
            log.error("邮件发送失败", e);
            throw new RuntimeException("邮件发送失败", e);
        }
    }

    /**
     * 构建邮件主题
     *
     * @return 邮件主题
     */
    private String buildSubject() {
        return String.format("[DB-Doctor] 慢查询预警 - %s",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    }

    /**
     * 构建邮件内容（HTML 格式）
     *
     * @param markdownReport Markdown 格式的报告
     * @return HTML 格式的邮件内容
     */
    private String buildEmailContent(String markdownReport) {
        // 简单的 Markdown 转 HTML
        String htmlContent = convertMarkdownToHtml(markdownReport);

        return String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        h1 { color: #e74c3c; border-bottom: 2px solid #e74c3c; padding-bottom: 10px; }
                        h2 { color: #e67e22; margin-top: 30px; }
                        h3 { color: #3498db; }
                        ul { margin: 10px 0; padding-left: 20px; }
                        li { margin: 5px 0; }
                        code { background-color: #f4f4f4; padding: 2px 6px; border-radius: 3px; }
                        pre { background-color: #2c3e50; color: #ecf0f1; padding: 15px; border-radius: 5px; overflow-x: auto; }
                        pre code { background-color: transparent; padding: 0; }
                        .highlight { background-color: #fff3cd; padding: 10px; border-left: 4px solid #ffc107; margin: 15px 0; }
                        .footer { margin-top: 30px; padding-top: 20px; border-top: 1px solid #ddd; color: #777; font-size: 12px; }
                    </style>
                </head>
                <body>
                    <div class="highlight">
                        <strong>📢 DB-Doctor 慢查询预警</strong><br>
                        系统检测到慢查询，请及时处理。
                    </div>
                    %s
                    <div class="footer">
                        <p>此邮件由 DB-Doctor 自动发送，请勿回复。</p>
                        <p>生成时间: %s</p>
                    </div>
                </body>
                </html>
                """,
                htmlContent,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        );
    }

    /**
     * 简单的 Markdown 转 HTML
     *
     * @param markdown Markdown 文本
     * @return HTML 文本
     */
    private String convertMarkdownToHtml(String markdown) {
        String html = markdown;

        // 标题转换
        html = html.replaceAll("### (.*)", "<h3>$1</h3>");
        html = html.replaceAll("## (.*)", "<h2>$1</h2>");
        html = html.replaceAll("# (.*)", "<h1>$1</h1>");

        // 粗体
        html = html.replaceAll("\\*\\*(.*?)\\*\\*", "<strong>$1</strong>");

        // 代码块
        html = html.replaceAll("```sql\\n([\\s\\S]*?)```", "<pre><code class=\"sql\">$1</code></pre>");
        html = html.replaceAll("```([\\s\\S]*?)```", "<pre><code>$1</code></pre>");

        // 行内代码
        html = html.replaceAll("`([^`]+)`", "<code>$1</code>");

        // 列表（简单处理）
        html = html.replaceAll("^- (.*)", "<li>$1</li>");
        html = html.replaceAll("(<li>.*</li>\\n)+", "<ul>$0</ul>");

        // 换行
        html = html.replaceAll("\\n", "<br>");

        return html;
    }
}
