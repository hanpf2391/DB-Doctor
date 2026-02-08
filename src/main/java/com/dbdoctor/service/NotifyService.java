package com.dbdoctor.service;

import com.dbdoctor.common.enums.SeverityLevel;
import com.dbdoctor.entity.SlowQueryTemplate;
import com.dbdoctor.model.NotificationBatchReport;
import com.dbdoctor.model.QueryStatisticsDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.MimeMessageHelper;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 通知服务
 *
 * 核心职责：
 * - 发送测试邮件
 * - 发送批量通知（聚合报告）
 * - 发送单条慢查询通知（预留接口）
 *
 * 配置读取（从 SystemConfig 数据库配置表）：
 * - notify.email.enabled - 邮件通知开关
 * - mail.smtp.from - 发件人邮箱（纯邮箱地址）
 * - mail.smtp.display-name - 发件人显示名称
 * - mail.batch.to - 批量报告收件人（逗号分隔）
 * - mail.batch.cc - 批量报告抄送（逗号分隔）
 *
 * @author DB-Doctor
 * @version 3.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotifyService {

    private final JavaMailSender mailSender;
    private final SystemConfigService configService;

    /**
     * 发送批量通知（聚合报告）
     *
     * @param report 批次报告
     * @return true=发送成功, false=发送失败
     */
    public boolean sendBatchNotification(NotificationBatchReport report) {
        try {
            // 1. 检查邮件通知是否启用
            boolean enabled = configService.getBoolean("notify.email.enabled", false);
            if (!enabled) {
                log.info("[批量通知] 邮件通知未启用，跳过发送");
                return true;
            }

            log.info("📧 开始发送批量通知邮件: 指纹数={}", report.getTotalCount());

            // 2. 读取收件人配置
            List<String> toEmails = getListFromConfig("mail.batch.to");
            if (toEmails.isEmpty()) {
                log.warn("[批量通知] 未配置收件人，跳过发送");
                return false;
            }

            List<String> ccEmails = getListFromConfig("mail.batch.cc");

            // 3. 构造发件人（包装为 "DB-Doctor <noreply@dbdoctor.com>" 格式）
            String from = getFromEmail();

            // 4. 构建邮件内容
            String emailSubject = buildEmailSubject(report);
            String emailContent = buildEmailContent(report);

            // 5. 发送邮件
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(from);
            helper.setTo(toEmails.toArray(new String[0]));

            if (!ccEmails.isEmpty()) {
                helper.setCc(ccEmails.toArray(new String[0]));
            }

            helper.setSubject(emailSubject);
            helper.setText(emailContent, true); // HTML 格式

            mailSender.send(message);

            log.info("✅ 批量通知邮件发送成功: to={}, cc={}", toEmails, ccEmails);
            return true;

        } catch (Exception e) {
            log.error("❌ 批量通知邮件发送失败", e);
            return false;
        }
    }

    /**
     * 获取发件人邮箱（自动包装格式）
     *
     * @return 格式: "DB-Doctor <noreply@dbdoctor.com>"
     */
    private String getFromEmail() {
        String fromEmail = configService.getString("mail.smtp.from");
        String displayName = configService.getString("mail.smtp.display-name");
        if (displayName == null || displayName.trim().isEmpty()) {
            displayName = "DB-Doctor";
        }

        // 兼容旧配置（如果配置值已包含显示名称）
        if (fromEmail != null && fromEmail.contains("<")) {
            log.debug("[邮件服务] 发件人配置已包含显示名称，直接使用: {}", fromEmail);
            return fromEmail;
        }

        // 自动包装为 "显示名称 <邮箱地址>" 格式
        if (fromEmail == null || fromEmail.trim().isEmpty()) {
            log.warn("[邮件服务] 未配置发件人邮箱，使用默认值");
            fromEmail = "noreply@dbdoctor.com";
        }

        return String.format("%s <%s>", displayName, fromEmail);
    }

    /**
     * 从配置获取列表（逗号分隔）
     *
     * @param configKey 配置键
     * @return 列表（如果配置为空或不存在，返回空列表）
     */
    private List<String> getListFromConfig(String configKey) {
        String value = configService.getString(configKey);
        if (value == null || value.trim().isEmpty()) {
            return Collections.emptyList();
        }

        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * 构建邮件主题
     */
    private String buildEmailSubject(NotificationBatchReport report) {
        return String.format(
            "🩺 DB-Doctor 慢查询诊断报告 %s | 🔥严重:%d ⚠️中等:%d 💡轻微:%d",
            report.getFormattedWindow(),
            report.getCriticalCount(),
            report.getMediumCount(),
            report.getLowCount()
        );
    }

    /**
     * 构建邮件内容（HTML 格式）
     */
    private String buildEmailContent(NotificationBatchReport report) {
        StringBuilder sb = new StringBuilder();

        // HTML 头部
        sb.append("<!DOCTYPE html>")
          .append("<html><head>")
          .append("<meta charset='UTF-8'>")
          .append("<style>")
          .append("body { font-family: Arial, 'Microsoft YaHei', sans-serif; line-height: 1.6; color: #333; }")
          .append(".container { max-width: 800px; margin: 0 auto; padding: 20px; }")
          .append(".header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; border-radius: 10px 10px 0 0; text-align: center; }")
          .append(".summary { background: #f8f9fa; padding: 20px; border-radius: 10px; margin: 20px 0; }")
          .append(".summary-table { width: 100%; border-collapse: collapse; }")
          .append(".summary-table td { padding: 8px; border-bottom: 1px solid #ddd; }")
          .append(".severity-critical { color: #dc3545; font-weight: bold; font-size: 18px; }")
          .append(".severity-medium { color: #ffc107; font-weight: bold; }")
          .append(".severity-low { color: #28a745; }")
          .append(".section { margin: 30px 0; }")
          .append(".section-title { font-size: 20px; font-weight: bold; margin-bottom: 15px; padding-bottom: 10px; border-bottom: 3px solid #ddd; }")
          .append(".issue-item { background: white; border-left: 4px solid #ddd; padding: 15px; margin: 15px 0; border-radius: 5px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }")
          .append(".issue-critical { border-left-color: #dc3545; }")
          .append(".issue-medium { border-left-color: #ffc107; }")
          .append(".issue-low { border-left-color: #28a745; }")
          .append(".issue-header { font-size: 16px; font-weight: bold; margin-bottom: 10px; }")
          .append(".issue-stats { font-size: 14px; color: #666; margin: 8px 0; }")
          .append(".ai-report { background: #f0f7ff; padding: 12px; margin-top: 10px; border-radius: 5px; font-size: 13px; line-height: 1.8; white-space: pre-wrap; word-wrap: break-word; }")
          .append(".footer { text-align: center; margin-top: 40px; padding: 20px; background: #f8f9fa; border-radius: 10px; font-size: 12px; color: #999; }")
          .append(".btn { display: inline-block; padding: 12px 30px; background: #667eea; color: white; text-decoration: none; border-radius: 5px; font-weight: bold; margin: 10px 0; }")
          .append(".btn:hover { background: #5568d3; }")
          .append(".top-tables { background: #fff3cd; padding: 15px; border-radius: 5px; margin: 20px 0; }")
          .append("</style></head><body>");

        // 邮件容器
        sb.append("<div class='container'>");

        // ========== 邮件头部 ==========
        sb.append("<div class='header'>")
          .append("<h1>🩺 DB-Doctor 慢查询诊断报告</h1>")
          .append("<p style='margin: 10px 0; opacity: 0.9;'>").append(report.getFormattedWindow()).append("</p>")
          .append("</div>");

        // ========== 执行摘要 ==========
        sb.append("<div class='summary'>")
          .append("<h2 style='margin-top: 0;'>📊 本期概览</h2>")
          .append("<table class='summary-table'>")
          .append("<tr><td><strong>统计周期：</strong></td><td>").append(report.getFormattedWindow()).append("</td></tr>")
          .append("<tr><td><strong>新增慢查询指纹：</strong></td><td style='font-size: 18px; font-weight: bold;'>").append(String.valueOf(report.getTotalCount())).append(" 条</td></tr>")
          .append("<tr><td><strong>样本总数：</strong></td><td>").append(String.valueOf(report.getTotalSamples())).append(" 条</td></tr>")
          .append("<tr><td><strong>🔥 严重问题：</strong></td><td class='severity-critical'>").append(String.valueOf(report.getCriticalCount())).append(" 条</td></tr>")
          .append("<tr><td><strong>⚠️ 中等问题：</strong></td><td class='severity-medium'>").append(String.valueOf(report.getMediumCount())).append(" 条</td></tr>")
          .append("<tr><td><strong>💡 轻微问题：</strong></td><td class='severity-low'>").append(String.valueOf(report.getLowCount())).append(" 条</td></tr>")
          .append("</table>")
          .append("</div>");

        // ========== 严重问题 ==========
        if (report.getCriticalCount() > 0) {
            sb.append("<div class='section'>")
              .append("<div class='section-title' style='color: #dc3545; border-bottom-color: #dc3545;'>")
              .append("🔥 严重问题（需立即处理）")
              .append("</div>");
            appendIssues(sb, report.getCriticalIssues(), SeverityLevel.CRITICAL);
            sb.append("</div>");
        }

        // ========== 中等问题 ==========
        if (report.getMediumCount() > 0) {
            sb.append("<div class='section'>")
              .append("<div class='section-title' style='color: #ffc107; border-bottom-color: #ffc107;'>")
              .append("⚠️ 中等问题")
              .append("</div>");
            appendIssues(sb, report.getMediumIssues(), SeverityLevel.WARNING);
            sb.append("</div>");
        }

        // ========== 轻微问题 ==========
        if (report.getLowCount() > 0) {
            sb.append("<div class='section'>")
              .append("<div class='section-title' style='color: #28a745; border-bottom-color: #28a745;'>")
              .append("💡 轻微问题")
              .append("</div>");
            appendIssues(sb, report.getLowIssues(), SeverityLevel.NORMAL);
            sb.append("</div>");
        }

        // ========== 最需要关注的表 ==========
        if (!report.getTopProblematicTables().isEmpty()) {
            sb.append("<div class='top-tables'>")
              .append("<h3 style='margin-top: 0;'>🎯 最需要关注的表</h3>");
            for (int i = 0; i < report.getTopProblematicTables().size(); i++) {
                sb.append("<p style='margin: 5px 0;'>").append(i + 1).append(". <strong>")
                  .append(report.getTopProblematicTables().get(i)).append("</strong></p>");
            }
            sb.append("</div>");
        }

        // ========== 跳转链接 ==========
        sb.append("<div style='text-align: center; margin: 30px 0;'>")
          .append("<a href='").append(report.getDashboardUrl()).append("' class='btn'>")
          .append("🔗 查看完整报告")
          .append("</a>")
          .append("<p style='margin-top: 10px; font-size: 12px; color: #666;'>")
          .append(report.getDashboardUrl())
          .append("</p>")
          .append("</div>");

        // ========== 页脚 ==========
        sb.append("<div class='footer'>")
          .append("<p>本邮件由 DB-Doctor 自动发送，请勿回复</p>")
          .append("<p>如需调整通知频率，请访问系统配置页面</p>")
          .append("<p style='margin-top: 10px;'>发送时间：").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("</p>")
          .append("</div>");

        sb.append("</div></body></html>");

        return sb.toString();
    }

    /**
     * 追加问题列表（HTML 格式）
     * 统一折叠：所有问题都不展开 AI 报告，点击可展开
     */
    private void appendIssues(StringBuilder sb, List<SlowQueryTemplate> issues, SeverityLevel severity) {
        for (int i = 0; i < issues.size(); i++) {
            SlowQueryTemplate template = issues.get(i);

            String severityClass = "issue-" + severity.name().toLowerCase();

            sb.append("<div class='issue-item ").append(severityClass).append("'>")
              .append("<div class='issue-header'>")
              .append(i + 1).append(". ").append(escapeHtml(getSqlSummary(template)))
              .append("</div>");

            // 基本信息
            sb.append("<div class='issue-stats'>")
              .append("<strong>表名：</strong>").append(escapeHtml(template.getTableName() != null ? template.getTableName() : "unknown")).append("<br>")
              .append("<strong>数据库：</strong>").append(escapeHtml(template.getDbName())).append("<br>")
              .append("<strong>影响力：</strong>").append(template.getOccurrenceCount()).append(" 次/24h<br>")
              .append("<strong>平均耗时：</strong>").append(String.format("%.3f", template.getAvgQueryTime())).append(" 秒")
              .append("</div>");

            // AI 报告（折叠显示）
            if (template.getAiAnalysisReport() != null && !template.getAiAnalysisReport().isEmpty()) {
                sb.append("<details style='margin-top: 10px;'>")
                  .append("<summary style='cursor: pointer; color: #667eea; font-weight: bold;'>")
                  .append("💡 查看详细 AI 分析报告")
                  .append("</summary>")
                  .append("<div class='ai-report'>")
                  .append(escapeHtml(template.getAiAnalysisReport()))
                  .append("</div>")
                  .append("</details>");
            }

            sb.append("</div>");
        }
    }

    /**
     * HTML 转义
     */
    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }

    /**
     * 获取 SQL 摘要（前 60 个字符）
     */
    private String getSqlSummary(SlowQueryTemplate template) {
        String sql = template.getSqlTemplate();
        if (sql != null && sql.length() > 60) {
            return sql.substring(0, 60) + "...";
        }
        return sql != null ? sql : "";
    }

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

            // 设置发件人（使用配置）
            String from = getFromEmail();
            helper.setFrom(from);

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
                        版本: 3.0.0
                    </p>
                </body>
                </html>
                """.formatted(timestamp);
    }

    /**
     * 发送慢查询通知邮件（单条，预留接口）
     *
     * @param template 慢查询模板
     * @param stats 统计信息
     * @deprecated 使用 sendBatchNotification 批量通知替代
     */
    @Deprecated
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
