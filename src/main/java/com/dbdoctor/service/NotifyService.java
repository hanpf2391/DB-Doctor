package com.dbdoctor.service;

import com.dbdoctor.entity.SlowQueryTemplate;
import com.dbdoctor.model.QueryStatisticsDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.MimeMessageHelper;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 通知服务
 *
 * @author DB-Doctor
 * @version 2.2.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotifyService {

    private final JavaMailSender mailSender;

    /**
     * 发送测试邮件
     *
     * @param to 收件人列表
     * @param subject 邮件主题
     */
    public void sendTestEmail(List<String> to, String subject) {
        log.info("发送测试邮件: to={}, subject={}", to, subject);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // 设置收件人
            helper.setTo(to.toArray(new String[0]));

            // 设置主题
            helper.setSubject(subject != null ? subject : "DB-Doctor 测试邮件");

            // 设置内容（HTML 格式）
            String content = buildTestEmailContent();
            helper.setText(content, true);

            // 发送邮件
            mailSender.send(message);

            log.info("测试邮件发送成功: to={}", to);
        } catch (Exception e) {
            log.error("邮件发送失败", e);
            throw new RuntimeException("邮件发送失败: " + e.getMessage(), e);
        }
    }

    /**
     * 构建测试邮件内容
     */
    private String buildTestEmailContent() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        return """
                <html>
                <body style="font-family: Arial, sans-serif;">
                    <h1>DB-Doctor 测试邮件</h1>
                    <p>这是一封测试邮件，SMTP 配置正常！</p>
                    <hr>
                    <p style="color: #666; font-size: 12px;">
                        发送时间: %s<br>
                        系统名称: DB-Doctor<br>
                        版本: 2.2.0
                    </p>
                </body>
                </html>
                """.formatted(timestamp);
    }

    /**
     * 发送慢查询通知邮件
     *
     * @param template 慢查询模板
     * @param stats 统计信息
     */
    public void sendNotification(SlowQueryTemplate template, QueryStatisticsDTO stats) {
        log.info("发送慢查询通知: fingerprint={}", template.getSqlFingerprint());

        try {
            // TODO: 实现实际的通知逻辑
            // 1. 从配置读取收件人列表
            // 2. 构建邮件内容（包含慢查询详情、AI 分析报告等）
            // 3. 发送邮件

            log.debug("慢查询通知已发送: fingerprint={}, dbName={}, tableName={}",
                    template.getSqlFingerprint(),
                    template.getDbName(),
                    template.getTableName());
        } catch (Exception e) {
            log.error("发送慢查询通知失败: fingerprint={}", template.getSqlFingerprint(), e);
            throw new RuntimeException("发送通知失败: " + e.getMessage(), e);
        }
    }
}
