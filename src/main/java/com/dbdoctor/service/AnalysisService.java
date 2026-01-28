package com.dbdoctor.service;

import com.dbdoctor.common.util.SqlFingerprintUtil;
import com.dbdoctor.common.util.SqlMaskingUtil;
import com.dbdoctor.config.DbDoctorProperties;
import com.dbdoctor.model.QueryStatisticsDTO;
import com.dbdoctor.model.SlowQueryLog;
import com.dbdoctor.entity.SlowQuerySample;
import com.dbdoctor.entity.SlowQueryTemplate;
import com.dbdoctor.repository.SlowQuerySampleRepository;
import com.dbdoctor.repository.SlowQueryTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * 分析服务（V2.1.0 - 使用 Template + Sample 架构）
 * 负责处理慢查询日志并发送通知
 *
 * 核心功能：
 * 1. 从 mysql.slow_log 表接收慢查询数据
 * 2. 计算 SQL 指纹，去重判断
 * 3. 写入 slow_query_template（模板表）和 slow_query_sample（样本表）
 * 4. 生成报告并发送邮件通知
 *
 * 去重机制：
 * - 使用 SQL 指纹（MD5）判断是否为同一类型的 SQL
 * - 新 SQL：创建 Template + Sample 记录
 * - 老 SQL：只新增 Sample 记录，更新 Template 的 lastSeenTime
 *
 * @author DB-Doctor
 * @version 2.1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final NotifyService notifyService;
    private final SlowQueryTemplateRepository templateRepo;
    private final SlowQuerySampleRepository sampleRepo;
    private final DbDoctorProperties properties;

    /**
     * 处理慢查询日志（入口方法）
     *
     * @param slowLog 慢查询日志对象
     */
    @Transactional
    public void processSlowQuery(SlowQueryLog slowLog) {
        String rawSql = slowLog.getSqlText();
        String dbName = slowLog.getDbName();

        // 1. 数据清洗
        String cleanedSql = SqlFingerprintUtil.cleanSql(rawSql);
        if (cleanedSql.isBlank()) {
            log.warn("SQL 为空，跳过处理");
            return;
        }

        // 2. 计算 SQL 指纹
        String fingerprint = SqlFingerprintUtil.calculateFingerprint(cleanedSql);

        // 3. 查询 H2 数据库：是否已存在？
        Optional<SlowQueryTemplate> templateOpt = templateRepo.findBySqlFingerprint(fingerprint);

        if (templateOpt.isPresent()) {
            // === 情况 A：老面孔（已分析过） ===
            SlowQueryTemplate template = templateOpt.get();
            handleExistingQuery(template, slowLog, cleanedSql);
        } else {
            // === 情况 B：新面孔（首次发现） ===
            handleNewQuery(fingerprint, cleanedSql, dbName, slowLog);
        }
    }

    /**
     * 处理已存在的慢查询（老面孔）
     *
     * 核心逻辑：
     * - 新增一条 Sample 记录（保留完整历史）
     * - 更新 Template 的 lastSeenTime
     * - 触发通知判断
     *
     * @param template 模板记录
     * @param slowLog 慢查询日志
     * @param cleanedSql 清洗后的 SQL
     */
    private void handleExistingQuery(SlowQueryTemplate template, SlowQueryLog slowLog, String cleanedSql) {
        String fingerprint = template.getSqlFingerprint();

        // 1. SQL 脱敏处理（保护敏感数据）
        String maskedSql = SqlMaskingUtil.maskSensitiveData(cleanedSql);

        // 2. 新增 Sample 记录
        SlowQuerySample sample = SlowQuerySample.builder()
                .sqlFingerprint(fingerprint)
                .originalSql(maskedSql)  // 存储脱敏后的 SQL
                .userHost(slowLog.getUserHost())
                .queryTime(slowLog.getQueryTime())
                .lockTime(slowLog.getLockTime())
                .rowsSent(slowLog.getRowsSent())
                .rowsExamined(slowLog.getRowsExamined())
                .capturedAt(slowLog.getStartTime())
                .build();
        sampleRepo.save(sample);

        // 3. 更新 Template 的 lastSeenTime
        templateRepo.updateLastSeenTime(fingerprint, LocalDateTime.now());

        log.debug("📋 更新重复 SQL: fingerprint={}, db={}", fingerprint, slowLog.getDbName());

        // 4. 触发报告生成和通知（异步，使用智能通知策略判断）
        generateReportAndNotify(template);
    }

    /**
     * 处理新发现的慢查询（新面孔）
     *
     * 核心逻辑：
     * - 创建一条 Template 记录
     * - 创建第一条 Sample 记录
     * - 触发通知
     *
     * @param fingerprint SQL 指纹
     * @param cleanedSql 清洗后的 SQL
     * @param dbName 数据库名
     * @param slowLog 慢查询日志
     */
    private void handleNewQuery(String fingerprint, String cleanedSql, String dbName, SlowQueryLog slowLog) {
        // 提取表名（可选）
        String tableName = extractTableName(cleanedSql);

        // 1. 提取 SQL 模板（Druid 参数化，把真实值替换成 ?）
        String sqlTemplate = SqlFingerprintUtil.extractTemplate(cleanedSql);

        // 2. SQL 脱敏处理（用于 Sample 表存储）
        String maskedSql = SqlMaskingUtil.maskSensitiveData(cleanedSql);

        // 3. 创建 Template 记录
        SlowQueryTemplate template = SlowQueryTemplate.builder()
                .sqlFingerprint(fingerprint)
                .sqlTemplate(sqlTemplate)  // ← 存储参数化后的模板（全是 ?）
                .dbName(dbName)
                .tableName(tableName)
                .firstSeenTime(LocalDateTime.now())
                .lastSeenTime(LocalDateTime.now())
                .status(SlowQueryTemplate.AnalysisStatus.PENDING)
                .build();

        template = templateRepo.save(template);

        // 4. 创建第一条 Sample 记录
        SlowQuerySample sample = SlowQuerySample.builder()
                .sqlFingerprint(fingerprint)
                .originalSql(maskedSql)  // ← 存储脱敏后的原始 SQL
                .userHost(slowLog.getUserHost())
                .queryTime(slowLog.getQueryTime())
                .lockTime(slowLog.getLockTime())
                .rowsSent(slowLog.getRowsSent())
                .rowsExamined(slowLog.getRowsExamined())
                .capturedAt(slowLog.getStartTime())
                .build();
        sampleRepo.save(sample);

        log.info("✨ 新发现慢查询: fingerprint={}, db={}, table={}",
                fingerprint, dbName, tableName);

        // 5. 触发报告生成和通知（异步）
        generateReportAndNotify(template);
    }

    /**
     * 异步生成报告并发送通知
     *
     * @param template 模板记录
     */
    @Async("analysisExecutor")
    @Transactional
    public void generateReportAndNotify(SlowQueryTemplate template) {
        String fingerprint = template.getSqlFingerprint();
        long startTime = System.currentTimeMillis();

        try {
            log.info("📋 生成报告: fingerprint={}, db={}, table={}",
                    fingerprint, template.getDbName(), template.getTableName());

            // 1. 从 Sample 表实时计算统计信息
            QueryStatisticsDTO stats = sampleRepo.calculateStatistics(fingerprint);

            // 2. 构建基础数据报告
            StringBuilder report = new StringBuilder();
            report.append("# 慢查询分析报告\n\n");

            // === 基本信息 ===
            report.append("## 基本信息\n\n");
            report.append(String.format("- **指纹**: `%s`\n", fingerprint));
            report.append(String.format("- **数据库**: `%s`\n", template.getDbName()));
            report.append(String.format("- **表**: `%s`\n", template.getTableName()));
            report.append(String.format("- **首次发现**: %s\n", formatTime(stats.getFirstSeenTime())));
            report.append(String.format("- **最近发现**: %s\n", formatTime(stats.getLastSeenTime())));
            report.append(String.format("- **出现次数**: %d\n\n", stats.getOccurrenceCount()));

            // === 慢查询基础数据 ===
            report.append("## 慢查询基础数据\n\n");

            // 查询耗时
            report.append("### 查询耗时\n");
            report.append(String.format("- 平均耗时: **%.3f 秒**\n", stats.getAvgQueryTime()));
            report.append(String.format("- 最大耗时: **%.3f 秒**\n\n", stats.getMaxQueryTime()));

            // 锁等待时间
            report.append("### 锁等待时间\n");
            report.append(String.format("- 平均锁等待: **%.3f 秒**\n", stats.getAvgLockTime()));
            report.append(String.format("- 最大锁等待: **%.3f 秒**\n\n", stats.getMaxLockTime()));

            // 扫描行数
            report.append("### 扫描行数\n");
            report.append(String.format("- 平均返回行数: %d\n", stats.getAvgRowsSent() != null ? stats.getAvgRowsSent().longValue() : 0));
            report.append(String.format("- 最大返回行数: %d\n", stats.getMaxRowsSent()));
            report.append(String.format("- 最大扫描行数: %d\n\n", stats.getMaxRowsExamined()));

            // SQL 模板
            report.append("## SQL 模板\n\n");
            report.append("```sql\n");
            report.append(template.getSqlTemplate()).append("\n");
            report.append("```\n\n");

            // 3. 保存报告到 Template
            template.setAiAnalysisReport(report.toString());
            template.setStatus(SlowQueryTemplate.AnalysisStatus.SUCCESS);
            templateRepo.save(template);

            // 4. 判断是否需要通知
            if (shouldNotify(template, stats)) {
                notifyService.sendNotification(template, stats);
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("✅ 报告生成完成: fingerprint={}, 耗时={}ms", fingerprint, duration);

        } catch (Exception e) {
            log.error("❌ 生成报告失败: fingerprint={}", fingerprint, e);

            // 标记状态为 ERROR
            template.setStatus(SlowQueryTemplate.AnalysisStatus.ERROR);
            template.setAiAnalysisReport("报告生成失败: " + e.getMessage());
            templateRepo.save(template);
        }
    }

    /**
     * 判断是否需要通知
     *
     * @param template 模板记录
     * @param stats 统计信息
     * @return true-需要通知，false-跳过通知
     */
    private boolean shouldNotify(SlowQueryTemplate template, QueryStatisticsDTO stats) {
        // 1. 检查严重程度阈值
        if (stats.getAvgQueryTime() < properties.getNotify().getSeverityThreshold()) {
            log.debug("跳过通知：平均耗时低于阈值 ({} < {})",
                    stats.getAvgQueryTime(), properties.getNotify().getSeverityThreshold());
            return false;
        }

        // 2. 智能通知策略判断
        int coolDownHours = properties.getNotify().getCoolDownHours();
        double degradationMultiplier = properties.getNotify().getDegradationMultiplier();

        return template.shouldNotify(coolDownHours, degradationMultiplier, stats.getAvgQueryTime());
    }

    /**
     * 提取表名（简单实现）
     *
     * @param sql SQL 语句
     * @return 表名
     */
    private String extractTableName(String sql) {
        try {
            String upperSql = sql.toUpperCase().replaceAll("\\s+", " ");

            // FROM table_name
            int fromIndex = upperSql.indexOf(" FROM ");
            if (fromIndex > 0) {
                int start = fromIndex + 6;
                int end = upperSql.indexOf(' ', start);
                if (end == -1) end = upperSql.indexOf('(', start);
                if (end == -1) end = upperSql.length();
                return sql.substring(start, end).trim().replaceAll("[`;\"]", "");
            }

            // UPDATE table_name
            int updateIndex = upperSql.indexOf("UPDATE ");
            if (updateIndex == 0) {
                int start = 7;
                int end = upperSql.indexOf(' ', start);
                if (end == -1) end = upperSql.length();
                return sql.substring(start, end).trim().replaceAll("[`;\"]", "");
            }

            // INSERT INTO table_name
            int insertIndex = upperSql.indexOf("INSERT INTO ");
            if (insertIndex == 0) {
                int start = 12;
                int end = upperSql.indexOf(' ', start);
                if (end == -1) end = upperSql.indexOf('(', start);
                if (end == -1) end = upperSql.length();
                return sql.substring(start, end).trim().replaceAll("[`;\"]", "");
            }

        } catch (Exception e) {
            log.debug("提取表名失败: {}", e.getMessage());
        }

        return "unknown";
    }

    /**
     * 格式化时间
     *
     * @param time 时间
     * @return 格式化后的字符串
     */
    private String formatTime(LocalDateTime time) {
        if (time == null) {
            return "未知";
        }
        return time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
