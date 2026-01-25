package com.dbdoctor.service;

import com.dbdoctor.common.util.SqlFingerprintUtil;
import com.dbdoctor.config.DbDoctorProperties;
import com.dbdoctor.model.SlowQueryHistory;
import com.dbdoctor.model.SlowQueryLog;
import com.dbdoctor.repository.SlowQueryHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * 分析服务
 * 负责处理慢查询日志并发送通知
 *
 * 核心功能：
 * 1. 从 mysql.slow_log 表接收慢查询数据
 * 2. 计算 SQL 指纹，去重判断
 * 3. 统计慢查询数据（平均值、最大值）
 * 4. 生成报告并发送邮件通知
 *
 * 去重机制：
 * - 使用 SQL 指纹（MD5）判断是否为同一类型的 SQL
 * - 新 SQL：触发通知
 * - 老 SQL：更新计数，根据配置决定是否重新通知
 *
 * @author DB-Doctor
 * @version 2.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final NotifyService notifyService;
    private final SlowQueryHistoryRepository historyRepo;
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
        Optional<SlowQueryHistory> historyOpt = historyRepo.findBySqlFingerprint(fingerprint);

        if (historyOpt.isPresent()) {
            // === 情况 A：老面孔（已分析过） ===
            SlowQueryHistory history = historyOpt.get();
            handleExistingQuery(history, slowLog, cleanedSql);
        } else {
            // === 情况 B：新面孔（首次发现） ===
            handleNewQuery(fingerprint, cleanedSql, dbName, slowLog);
        }
    }

    /**
     * 处理已存在的慢查询（老面孔）
     * 使用原子自增，避免并发统计错误
     *
     * @param history 历史记录
     * @param slowLog 慢查询日志
     * @param cleanedSql 清洗后的 SQL
     */
    private void handleExistingQuery(SlowQueryHistory history, SlowQueryLog slowLog, String cleanedSql) {
        String fingerprint = history.getSqlFingerprint();

        // 【优化】使用原子自增更新统计信息
        // 避免 Java 层面读取-计算-写回导致的并发误差
        historyRepo.updateStatistics(
                fingerprint,
                LocalDateTime.now(),  // now
                slowLog.getQueryTime(),
                slowLog.getLockTime(),
                slowLog.getRowsSent(),
                slowLog.getRowsExamined()
        );

        log.info("📋 更新重复 SQL 统计: fingerprint={}, db={}", fingerprint, slowLog.getDbName());

        // 触发报告生成和通知（异步，使用智能通知策略判断）
        generateReportAndNotify(history);
    }

    /**
     * 处理新发现的慢查询（新面孔）
     *
     * @param fingerprint SQL 指纹
     * @param cleanedSql 清洗后的 SQL
     * @param dbName 数据库名
     * @param slowLog 慢查询日志
     */
    private void handleNewQuery(String fingerprint, String cleanedSql, String dbName, SlowQueryLog slowLog) {
        // 提取表名（可选）
        String tableName = extractTableName(cleanedSql);

        // 创建新记录（包含完整的统计信息）
        SlowQueryHistory history = SlowQueryHistory.builder()
                .sqlFingerprint(fingerprint)
                .sqlTemplate(SqlFingerprintUtil.extractTemplate(cleanedSql))
                .exampleSql(cleanedSql)
                .dbName(dbName)
                .tableName(tableName)
                .firstSeenTime(LocalDateTime.now())
                .lastSeenTime(LocalDateTime.now())
                .status(SlowQueryHistory.AnalysisStatus.PENDING)
                .occurrenceCount(1L)
                // 查询耗时
                .avgQueryTime(slowLog.getQueryTime())
                .maxQueryTime(slowLog.getQueryTime())
                // 锁等待时间
                .avgLockTime(slowLog.getLockTime())
                .maxLockTime(slowLog.getLockTime())
                // 返回行数
                .avgRowsSent(slowLog.getRowsSent())
                .maxRowsSent(slowLog.getRowsSent())
                // 扫描行数
                .maxRowsExamined(slowLog.getRowsExamined())
                .build();

        // 保存到 H2（本地数据库）
        history = historyRepo.save(history);

        log.info("✨ 新发现慢查询: fingerprint={}, db={}, table={}",
                fingerprint, dbName, tableName);

        // 触发报告生成和通知（异步）
        generateReportAndNotify(history);
    }

    /**
     * 异步生成报告并发送通知
     *
     * @param history 历史记录
     */
    @Async("analysisExecutor")
    @Transactional
    public void generateReportAndNotify(SlowQueryHistory history) {
        String fingerprint = history.getSqlFingerprint();
        long startTime = System.currentTimeMillis();

        try {
            log.info("📋 生成报告: fingerprint={}, db={}, table={}",
                    fingerprint, history.getDbName(), history.getTableName());

            // 1. 构建基础数据报告
            StringBuilder report = new StringBuilder();
            report.append("# 慢查询分析报告\n\n");

            // === 基本信息 ===
            report.append("## 基本信息\n\n");
            report.append(String.format("- **指纹**: `%s`\n", fingerprint));
            report.append(String.format("- **数据库**: `%s`\n", history.getDbName()));
            report.append(String.format("- **表**: `%s`\n", history.getTableName()));
            report.append(String.format("- **首次发现**: %s\n", formatTime(history.getFirstSeenTime())));
            report.append(String.format("- **最近发现**: %s\n", formatTime(history.getLastSeenTime())));
            report.append(String.format("- **出现次数**: %d\n\n", history.getOccurrenceCount()));

            // === 慢查询基础数据 ===
            report.append("## 慢查询基础数据\n\n");

            // 查询耗时
            report.append("### 查询耗时\n");
            if (history.getAvgQueryTime() != null) {
                report.append(String.format("- **平均耗时**: %.3f 秒\n", history.getAvgQueryTime()));
            }
            if (history.getMaxQueryTime() != null) {
                report.append(String.format("- **最大耗时**: %.3f 秒\n", history.getMaxQueryTime()));
            }

            // 锁等待时间
            report.append("\n### 锁等待时间\n");
            if (history.getAvgLockTime() != null) {
                report.append(String.format("- **平均锁等待**: %.3f 秒\n", history.getAvgLockTime()));
            } else {
                report.append("- **平均锁等待**: 0 秒\n");
            }
            if (history.getMaxLockTime() != null) {
                report.append(String.format("- **最大锁等待**: %.3f 秒\n", history.getMaxLockTime()));
            }

            // 返回行数
            report.append("\n### 返回行数\n");
            if (history.getAvgRowsSent() != null) {
                report.append(String.format("- **平均返回行数**: %d 行\n", history.getAvgRowsSent()));
            }
            if (history.getMaxRowsSent() != null) {
                report.append(String.format("- **最大返回行数**: %d 行\n", history.getMaxRowsSent()));
            }

            // 扫描行数
            report.append("\n### 扫描行数\n");
            if (history.getMaxRowsExamined() != null) {
                report.append(String.format("- **扫描行数**: %d 行\n", history.getMaxRowsExamined()));
            }

            // 2. SQL 语句
            report.append("\n## SQL 模板\n\n```sql\n");
            report.append(history.getSqlTemplate());
            report.append("\n```\n");

            report.append("\n## SQL 样本（最近一次）\n\n```sql\n");
            report.append(history.getExampleSql());
            report.append("\n```\n");

            // 3. 保存报告到 H2
            history.setAiAnalysisReport(report.toString());
            history.setStatus(SlowQueryHistory.AnalysisStatus.SUCCESS);
            historyRepo.save(history);

            // 4. 发送通知（使用智能通知策略）
            log.debug("检查智能通知条件: avgQueryTime={}, coolDownHours={}, degradationMultiplier={}, highFrequencyThreshold={}",
                    history.getAvgQueryTime(),
                    properties.getNotify().getCoolDownHours(),
                    properties.getNotify().getDegradationMultiplier(),
                    properties.getNotify().getHighFrequencyThreshold());

            // 使用智能通知策略判断是否需要通知
            if (history.shouldNotify(
                    properties.getNotify().getCoolDownHours(),
                    properties.getNotify().getDegradationMultiplier(),
                    properties.getNotify().getHighFrequencyThreshold())) {

                log.info("📧 触发邮件通知: fingerprint={}, avgQueryTime={}, occurrenceCount={}",
                        fingerprint, history.getAvgQueryTime(), history.getOccurrenceCount());
                notifyService.sendNotificationWithRateLimit(fingerprint, report.toString());

                // 更新通知信息（记录本次通知的时间和耗时）
                history.updateNotificationInfo(history.getAvgQueryTime());
                historyRepo.save(history);
            } else {
                log.info("⏭️ 跳过邮件通知: fingerprint={}, avgQueryTime={}, lastNotifiedTime={}",
                        fingerprint, history.getAvgQueryTime(), history.getLastNotifiedTime());
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("✅ 报告生成完成: fingerprint={}, 耗时={}ms", fingerprint, duration);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("❌ 报告生成失败: fingerprint={}, 耗时={}ms", fingerprint, duration, e);

            // 更新状态为失败
            history.setStatus(SlowQueryHistory.AnalysisStatus.ERROR);
            historyRepo.save(history);
        }
    }

    /**
     * 从 SQL 中提取表名（简单实现）
     *
     * @param sql SQL 语句
     * @return 表名（如果有）
     */
    private String extractTableName(String sql) {
        try {
            // 简单提取：匹配 FROM 或 JOIN 后面的表名
            // 例如：SELECT * FROM users → users
            // 例如：SELECT * FROM shop.users → shop.users

            String lowerSql = sql.toLowerCase();

            // 查找 FROM
            int fromIndex = lowerSql.indexOf(" from ");
            if (fromIndex != -1) {
                int start = fromIndex + 6; // " from ".length()
                String sub = sql.substring(start).trim();

                // 提取第一个单词（表名）
                String[] parts = sub.split("\\s+");
                if (parts.length > 0) {
                    return parts[0].replaceAll("[`;,]", ""); // 去除反引号和分号
                }
            }

            return "unknown";

        } catch (Exception e) {
            log.warn("提取表名失败: {}", sql, e);
            return "unknown";
        }
    }

    /**
     * 格式化时间为友好格式
     *
     * @param time 时间
     * @return 格式化后的时间字符串（yyyy-MM-dd HH:mm:ss）
     */
    private String formatTime(LocalDateTime time) {
        if (time == null) {
            return "未知";
        }
        return time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
