package com.dbdoctor.service;

import com.dbdoctor.common.enums.SeverityLevel;
import com.dbdoctor.entity.SlowQueryTemplate;
import com.dbdoctor.model.NotificationBatchReport;
import com.dbdoctor.model.QueryStatisticsDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.MimeMessageHelper;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
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
public class NotifyService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    private final SystemConfigService configService;

    public NotifyService(SystemConfigService configService) {
        this.configService = configService;
    }

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

            // 2. 动态获取或创建 JavaMailSender（从数据库配置）
            JavaMailSender dynamicMailSender = getOrCreateMailSender();
            if (dynamicMailSender == null) {
                log.warn("[批量通知] 无法创建 JavaMailSender（配置不完整），跳过发送");
                return false;
            }

            // 3. 读取收件人配置
            List<String> toEmails = getListFromConfig("mail.batch.to");
            if (toEmails.isEmpty()) {
                log.warn("[批量通知] 未配置收件人，跳过发送");
                return false;
            }

            List<String> ccEmails = getListFromConfig("mail.batch.cc");

            // 4. 构造发件人（包装为 "DB-Doctor <noreply@dbdoctor.com>" 格式）
            String from = getFromEmail();

            // 5. 构建邮件内容
            String emailSubject = buildEmailSubject(report);
            String emailContent = buildEmailContent(report);

            // 6. 发送邮件
            MimeMessage message = dynamicMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(from);
            helper.setTo(toEmails.toArray(new String[0]));

            if (!ccEmails.isEmpty()) {
                helper.setCc(ccEmails.toArray(new String[0]));
            }

            helper.setSubject(emailSubject);
            helper.setText(emailContent, true); // HTML 格式

            dynamicMailSender.send(message);

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
        String fromEmail = configService.getDecryptedValue("mail.smtp.from");
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
                log.debug("[动态邮件] SMTP host 未配置");
                return null;
            }

            if (portStr == null || portStr.trim().isEmpty()) {
                log.debug("[动态邮件] SMTP port 未配置");
                return null;
            }

            if (username == null || username.trim().isEmpty()) {
                log.debug("[动态邮件] SMTP username 未配置");
                return null;
            }

            if (password == null || password.trim().isEmpty()) {
                log.debug("[动态邮件] SMTP password 未配置");
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

            log.debug("[动态邮件] 成功创建 JavaMailSender: host={}, port={}, username={}",
                host, portStr, username);

            return mailSenderImpl;

        } catch (Exception e) {
            log.error("[动态邮件] 创建 JavaMailSender 失败", e);
            return null;
        }
    }

    /**
     * 从配置获取列表（逗号分隔）
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
                .collect(Collectors.toList());
    }

    /**
     * 构建邮件主题
     */
    private String buildEmailSubject(NotificationBatchReport report) {
        return String.format(
            "DB-Doctor 慢查询分析报告 - %s",
            report.getFormattedWindow()
        );
    }

    /**
     * 构建邮件内容（HTML 格式）
     * 设计原则：现代化卡片式布局，可折叠查询详情，支持展开/收起查看AI报告
     */
    private String buildEmailContent(NotificationBatchReport report) {
        StringBuilder sb = new StringBuilder();

        // 计算统计数据
        double avgQueryTime = calculateAvgQueryTime(report);
        double maxQueryTime = calculateMaxQueryTime(report);

        // HTML 头部
        sb.append("<!DOCTYPE html>")
          .append("<html><head>")
          .append("<meta charset='UTF-8'>")
          .append("<style>")
          .append("body { font-family: 'Inter', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif; margin: 0; padding: 0; background-color: #f7f8fa; -webkit-font-smoothing: antialiased; -moz-osx-font-smoothing: grayscale; }")
          .append(".email-wrapper { padding: 20px; }")
          .append(".email-container { max-width: 750px; margin: 0 auto; background-color: #ffffff; border: 1px solid #e2e8f0; border-radius: 8px; overflow: hidden; }")
          .append(".top-border { height: 4px; background-color: #4A90E2; }")
          .append(".header { padding: 32px 40px; border-bottom: 1px solid #e2e8f0; }")
          .append(".header-main { display: flex; justify-content: space-between; align-items: flex-start; }")
          .append(".header-title h1 { margin: 0 0 4px 0; font-size: 24px; font-weight: 600; color: #1a202c; }")
          .append(".header-title p { margin: 0; font-size: 14px; color: #718096; }")
          .append(".header-stats { display: flex; gap: 24px; text-align: right; }")
          .append(".stat-item .value { font-size: 20px; font-weight: 600; color: #1a202c; }")
          .append(".stat-item .label { font-size: 12px; color: #718096; margin-top: 2px; }")
          .append(".query-list { padding: 24px 40px 32px; }")
          .append(".list-title { font-size: 18px; font-weight: 600; color: #1a202c; margin: 0 0 20px 0; }")
          .append(".collapsible-card { border: 1px solid #e2e8f0; border-radius: 8px; margin-bottom: 16px; transition: box-shadow 0.2s ease-in-out; }")
          .append(".collapsible-card:hover { box-shadow: 0 4px 12px rgba(0,0,0,0.05); }")
          .append(".content-inner h4 { font-size: 15px; font-weight: 600; color: #2d3748; margin: 20px 0 10px 0; }")
          .append(".content-inner p, .content-inner li { font-size: 14px; line-height: 1.6; color: #4a5568; }")
          .append(".code-block { background-color: #f7fafc; border: 1px solid #e2e8f0; padding: 12px; border-radius: 6px; font-family: 'SF Mono', 'Menlo', 'Monaco', 'Courier New', monospace; font-size: 13px; white-space: pre-wrap; word-break: break-all; margin-top: 8px; color: #1a202c; }")
          .append(".footer { text-align: center; padding: 24px; font-size: 12px; color: #a0aec0; }")
          .append(".btn { display: inline-block; padding: 12px 24px; background: #4A90E2; color: white; text-decoration: none; border-radius: 6px; font-weight: 500; transition: background 0.2s ease; margin: 16px 0; }")
          .append(".btn:hover { background: #357ABD; }")
          .append("</style></head><body>");

        // 邮件容器
        sb.append("<div class='email-wrapper'>")
          .append("<div class='email-container'>");

        // ========== 顶部蓝色边框 ==========
        sb.append("<div class='top-border'></div>");

        // ========== 邮件头部（包含标题和统计数据） ==========
        sb.append("<div class='header'>")
          .append("<div class='header-main'>")
          .append("<div class='header-title'>")
          .append("<h1>慢查询分析报告</h1>")
          .append("<p>DB-Doctor • ").append(report.getFormattedWindow()).append("</p>")
          .append("</div>")
          .append("<div class='header-stats'>")
          .append("<div class='stat-item'>")
          .append("<div class='value'>").append(report.getTotalCount()).append("</div>")
          .append("<div class='label'>查询指纹</div>")
          .append("</div>")
          .append("<div class='stat-item'>")
          .append("<div class='value'>").append(String.format("%.3fs", avgQueryTime)).append("</div>")
          .append("<div class='label'>平均耗时</div>")
          .append("</div>")
          .append("<div class='stat-item'>")
          .append("<div class='value'>").append(String.format("%.3fs", maxQueryTime)).append("</div>")
          .append("<div class='label'>最慢查询</div>")
          .append("</div>")
          .append("</div>")
          .append("</div>")
          .append("</div>");

        // ========== 所有慢查询详情 ==========
        sb.append("<div class='query-list'>")
          .append("<h2 class='list-title'>慢查询列表</h2>");

        // 合并所有严重程度的查询
        List<SlowQueryTemplate> allQueries = new ArrayList<>();
        if (report.getCriticalIssues() != null) allQueries.addAll(report.getCriticalIssues());
        if (report.getMediumIssues() != null) allQueries.addAll(report.getMediumIssues());
        if (report.getLowIssues() != null) allQueries.addAll(report.getLowIssues());

        // 按优先级排序
        allQueries.sort((a, b) -> {
            double scoreA = (a.getAvgQueryTime() != null ? a.getAvgQueryTime() : 0.0)
                * (a.getOccurrenceCount() != null ? a.getOccurrenceCount() : 1L);
            double scoreB = (b.getAvgQueryTime() != null ? b.getAvgQueryTime() : 0.0)
                * (b.getOccurrenceCount() != null ? b.getOccurrenceCount() : 1L);
            return Double.compare(scoreB, scoreA);
        });

        // 显示所有查询
        for (int i = 0; i < allQueries.size(); i++) {
            SlowQueryTemplate template = allQueries.get(i);
            appendModernQueryCard(sb, template, i + 1);
        }

        sb.append("</div>");

        // ========== 底部按钮 ==========
        sb.append("<div style='text-align: center; padding: 0 40px 32px;'>")
          .append("<a href='").append(report.getDashboardUrl()).append("' class='btn'>")
          .append("查看详情")
          .append("</a>")
          .append("</div>");

        // ========== 页脚 ==========
        sb.append("<div class='footer'>")
          .append("本邮件由 DB-Doctor 自动生成 • 发送时间：").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
          .append("</div>");

        sb.append("</div></div></body></html>");

        return sb.toString();
    }

    /**
     * 追加现代化查询卡片（可折叠）
     */
    private void appendModernQueryCard(StringBuilder sb, SlowQueryTemplate template, int rank) {
        String sqlSummary = escapeHtml(getSqlSummary(template));
        String dbName = escapeHtml(template.getDbName() != null ? template.getDbName() : "unknown");
        String tableName = escapeHtml(template.getTableName() != null ? template.getTableName() : "unknown");
        long occurrenceCount = template.getOccurrenceCount() != null ? template.getOccurrenceCount() : 0L;
        double avgQueryTime = template.getAvgQueryTime() != null ? template.getAvgQueryTime() : 0.0;
        double maxQueryTime = template.getMaxQueryTime() != null ? template.getMaxQueryTime() : 0.0;
        String sqlTemplate = escapeHtml(template.getSqlTemplate() != null ? template.getSqlTemplate() : "");

        // 使用 <details> 标签（邮件客户端支持更好）
        sb.append("<div class='collapsible-card'>")
          .append("<details>")
          .append("<summary style='cursor: pointer; padding: 20px; display: flex; justify-content: space-between; align-items: center;'>")
          .append("<div>")
          .append("<div style='font-family: \"SF Mono\", \"Menlo\", \"Monaco\", \"Courier New\", monospace; font-size: 14px; font-weight: 500; color: #2d3748; margin-bottom: 12px;'>")
          .append(rank).append(". ").append(sqlSummary)
          .append("</div>")
          .append("<div>")
          .append("<span style='display: inline-block; font-size: 12px; padding: 4px 10px; background-color: #edf2f7; color: #4a5568; border-radius: 12px; margin-right: 8px;'>")
          .append("执行 ").append(occurrenceCount).append(" 次")
          .append("</span>")
          .append("<span style='display: inline-block; font-size: 12px; padding: 4px 10px; background-color: #edf2f7; color: #4a5568; border-radius: 12px; margin-right: 8px;'>")
          .append("均耗 ").append(String.format("%.3fs", avgQueryTime))
          .append("</span>")
          .append("<span style='display: inline-block; font-size: 12px; padding: 4px 10px; background-color: #edf2f7; color: #4a5568; border-radius: 12px;'>")
          .append("DB: ").append(dbName)
          .append("</span>")
          .append("</div>")
          .append("</div>")
          .append("<span style='font-size: 14px; font-weight: 500; color: #4A90E2;'>")
          .append("详情 ▼")
          .append("</span>")
          .append("</summary>")
          .append("<div style='padding: 0 20px 20px; border-top: 1px solid #e2e8f0; margin: 0 20px;'>");

        // SQL 语句代码块
        sb.append("<h4 style='font-size: 15px; font-weight: 600; color: #2d3748; margin: 20px 0 10px 0;'>SQL 语句</h4>")
          .append("<div style='background-color: #f7fafc; border: 1px solid #e2e8f0; padding: 12px; border-radius: 6px; font-family: \"SF Mono\", \"Menlo\", \"Monaco\", \"Courier New\", monospace; font-size: 13px; white-space: pre-wrap; word-break: break-all; margin-top: 8px; color: #1a202c;'>")
          .append(sqlTemplate)
          .append("</div>");

        // 执行统计信息
        sb.append("<h4 style='font-size: 15px; font-weight: 600; color: #2d3748; margin: 20px 0 10px 0;'>执行统计</h4>")
          .append("<p style='font-size: 14px; line-height: 1.6; color: #4a5568; margin: 8px 0;'>")
          .append("<strong>表名：</strong>").append(tableName).append("<br>")
          .append("<strong>数据库：</strong>").append(dbName).append("<br>")
          .append("<strong>执行次数：</strong>").append(occurrenceCount).append(" 次<br>")
          .append("<strong>平均耗时：</strong>").append(String.format("%.3f 秒", avgQueryTime)).append("<br>")
          .append("<strong>最大耗时：</strong>").append(String.format("%.3f 秒", maxQueryTime));

        if (template.getAvgLockTime() != null && template.getAvgLockTime() > 0) {
            sb.append("<br><strong>平均锁时间：</strong>").append(String.format("%.3f 秒", template.getAvgLockTime()));
        }

        sb.append("</p>");

        // AI 分析报告
        if (template.getAiAnalysisReport() != null && !template.getAiAnalysisReport().isEmpty()) {
            String formattedReport = formatAiReportWithCodeBlocks(template.getAiAnalysisReport());
            sb.append("<h4 style='font-size: 15px; font-weight: 600; color: #2d3748; margin: 20px 0 10px 0;'>智能诊断报告</h4>")
              .append(formattedReport);
        } else {
            sb.append("<h4 style='font-size: 15px; font-weight: 600; color: #2d3748; margin: 20px 0 10px 0;'>智能诊断报告</h4>")
              .append("<p style='font-size: 14px; line-height: 1.6; color: #4a5568; margin: 8px 0;'>")
              .append("📝 AI 分析报告正在生成中，请稍后查看系统获取详细分析...")
              .append("</p>");
        }

        sb.append("</div>")
          .append("</details>")
          .append("</div>");
    }

    /**
     * 格式化 AI 报告，将代码块转换为 HTML 代码块样式
     * 识别以下格式的代码块：
     * - ```sql ... ```
     * - ``` ... ```
     * - 直接以 CREATE, ALTER, SELECT 等开头的行
     */
    private String formatAiReportWithCodeBlocks(String aiReport) {
        if (aiReport == null || aiReport.isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        String[] lines = aiReport.split("\n");
        boolean inCodeBlock = false;
        StringBuilder codeBlockContent = new StringBuilder();

        for (String line : lines) {
            String trimmedLine = line.trim();

            // 检测代码块开始 ```
            if (trimmedLine.startsWith("```")) {
                if (inCodeBlock) {
                    // 代码块结束
                    result.append("<div class='code-block'>").append(escapeHtml(codeBlockContent.toString())).append("</div>\n");
                    codeBlockContent = new StringBuilder();
                    inCodeBlock = false;
                } else {
                    // 代码块开始
                    inCodeBlock = true;
                }
                continue;
            }

            // 如果在代码块中，累积内容
            if (inCodeBlock) {
                codeBlockContent.append(line).append("\n");
                continue;
            }

            // 检测是否是 SQL 语句行（直接以 SQL 关键字开头）
            if (isSqlStatement(trimmedLine)) {
                // 将之前的内容作为段落
                if (result.length() > 0 && !result.toString().endsWith("<p>") && !result.toString().endsWith("</p>")) {
                    // 确保之前的内容已闭合
                }
                result.append("<div class='code-block'>").append(escapeHtml(line)).append("</div>\n");
            } else {
                // 普通文本段落
                if (!trimmedLine.isEmpty()) {
                    result.append("<p>").append(escapeHtml(line)).append("</p>\n");
                }
            }
        }

        // 处理未闭合的代码块
        if (inCodeBlock && codeBlockContent.length() > 0) {
            result.append("<div class='code-block'>").append(escapeHtml(codeBlockContent.toString())).append("</div>\n");
        }

        return result.toString();
    }

    /**
     * 判断一行是否是 SQL 语句
     * 通过检查是否以常见的 SQL 关键字开头
     */
    private boolean isSqlStatement(String line) {
        if (line == null || line.isEmpty()) {
            return false;
        }

        String upperLine = line.toUpperCase();
        String[] sqlKeywords = {
            "SELECT", "INSERT", "UPDATE", "DELETE", "CREATE", "ALTER", "DROP",
            "TRUNCATE", "GRANT", "REVOKE", "SHOW", "DESCRIBE", "DESC", "EXPLAIN",
            "WITH", "MERGE", "CALL", "REPLACE", "LOAD", "HANDLER"
        };

        for (String keyword : sqlKeywords) {
            if (upperLine.startsWith(keyword + " ") || upperLine.startsWith(keyword + "(")) {
                return true;
            }
        }

        return false;
    }

    /**
     * 计算平均查询时间
     */
    private double calculateAvgQueryTime(NotificationBatchReport report) {
        List<SlowQueryTemplate> allQueries = new ArrayList<>();
        if (report.getCriticalIssues() != null) allQueries.addAll(report.getCriticalIssues());
        if (report.getMediumIssues() != null) allQueries.addAll(report.getMediumIssues());
        if (report.getLowIssues() != null) allQueries.addAll(report.getLowIssues());

        if (allQueries.isEmpty()) {
            return 0.0;
        }

        double total = allQueries.stream()
            .mapToDouble(t -> t.getAvgQueryTime() != null ? t.getAvgQueryTime() : 0.0)
            .sum();

        return total / allQueries.size();
    }

    /**
     * 计算最大查询时间
     */
    private double calculateMaxQueryTime(NotificationBatchReport report) {
        List<SlowQueryTemplate> allQueries = new ArrayList<>();
        if (report.getCriticalIssues() != null) allQueries.addAll(report.getCriticalIssues());
        if (report.getMediumIssues() != null) allQueries.addAll(report.getMediumIssues());
        if (report.getLowIssues() != null) allQueries.addAll(report.getLowIssues());

        return allQueries.stream()
            .mapToDouble(t -> t.getMaxQueryTime() != null ? t.getMaxQueryTime() : 0.0)
            .max()
            .orElse(0.0);
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

        // 动态获取或创建 JavaMailSender
        JavaMailSender dynamicMailSender = getOrCreateMailSender();
        if (dynamicMailSender == null) {
            log.error("无法创建 JavaMailSender（配置不完整），无法发送测试邮件");
            return;
        }

        try {
            MimeMessage message = dynamicMailSender.createMimeMessage();
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
            dynamicMailSender.send(message);

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
