package com.dbdoctor.service;

import com.dbdoctor.common.util.SqlFingerprintUtil;
import com.dbdoctor.model.SlowQueryHistory;
import com.dbdoctor.model.SlowQueryLog;
import com.dbdoctor.repository.SlowQueryHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 分析服务
 * 负责调用 AI Agent 进行慢查询分析
 *
 * 核心功能：
 * 1. 从 mysql.slow_log 表接收慢查询数据
 * 2. 计算 SQL 指纹，去重判断
 * 3. 调用 AI Agent 进行智能分析
 * 4. 生成诊断报告并保存到 H2
 * 5. 发送通知
 *
 * 去重机制：
 * - 使用 SQL 指纹（MD5）判断是否为同一类型的 SQL
 * - 新 SQL：触发 AI 分析
 * - 老 SQL：更新计数，跳过分析（除非满足重新分析条件）
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

    // TODO: 注入 DBAgent（AI 分析服务）

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
     *
     * @param history 历史记录
     * @param slowLog 慢查询日志
     * @param cleanedSql 清洗后的 SQL
     */
    private void handleExistingQuery(SlowQueryHistory history, SlowQueryLog slowLog, String cleanedSql) {
        // 更新统计信息
        history.updateStatistics(slowLog.getQueryTime(), slowLog.getRowsExamined());

        // 更新最新样本
        history.setExampleSql(cleanedSql);

        // 判断是否需要重新分析
        if (history.shouldReAnalyze()) {
            log.info("🔄 满足重新分析条件: fingerprint={}, count={}",
                    history.getSqlFingerprint(), history.getOccurrenceCount());

            // 触发 AI 分析（异步）
            runAiAnalysis(history);
        } else {
            log.info("📋 重复 SQL，跳过 AI 分析: fingerprint={}, count={}, db={}",
                    history.getSqlFingerprint(), history.getOccurrenceCount(), slowLog.getDbName());
        }

        // 保存到 H2
        historyRepo.save(history);
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

        // 创建新记录
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
                .avgQueryTime(slowLog.getQueryTime())
                .maxRowsExamined(slowLog.getRowsExamined())
                .build();

        // 保存到 H2（本地数据库）
        history = historyRepo.save(history);

        log.info("✨ 新发现慢查询: fingerprint={}, db={}, table={}",
                fingerprint, dbName, tableName);

        // 触发 AI 分析（异步）
        runAiAnalysis(history);
    }

    /**
     * 异步执行 AI 分析
     *
     * @param history 历史记录
     */
    @Async("analysisExecutor")
    @Transactional
    public void runAiAnalysis(SlowQueryHistory history) {
        String fingerprint = history.getSqlFingerprint();

        try {
            log.info("🔬 开始 AI 分析: fingerprint={}", fingerprint);

            // 1. 构建分析报告
            StringBuilder report = new StringBuilder();
            report.append(String.format("# 慢查询分析报告\n\n"));
            report.append(String.format("- **指纹**: `%s`\n", fingerprint));
            report.append(String.format("- **数据库**: `%s`\n", history.getDbName()));
            report.append(String.format("- **出现次数**: %d\n", history.getOccurrenceCount()));
            report.append(String.format("- **平均耗时**: %.3f 秒\n", history.getAvgQueryTime()));

            // 2. 基础规则分析
            if (history.getAvgQueryTime() != null && history.getAvgQueryTime() > 5.0) {
                report.append("\n⚠️ **严重慢查询**：平均耗时超过 5 秒\n");
            }

            if (history.getMaxRowsExamined() != null && history.getMaxRowsExamined() > 10000) {
                report.append("⚠️ **可能存在全表扫描**：最大扫描行数超过 10000\n");
            }

            // 3. SQL 语句示例
            report.append("\n## SQL 模板\n\n```sql\n");
            report.append(history.getSqlTemplate());
            report.append("\n```\n");

            // 4. SQL 样本
            report.append("\n## SQL 样本（最近一次）\n\n```sql\n");
            report.append(history.getExampleSql());
            report.append("\n```\n");

            // 5. 调用 AI Agent 深度分析（TODO）
            String aiAnalysis = "";
            // TODO: 调用 DBAgent 进行深度分析
            // aiAnalysis = dbAgent.analyze(history);

            if (aiAnalysis != null && !aiAnalysis.isEmpty()) {
                report.append("\n## AI 深度分析\n\n");
                report.append(aiAnalysis);
            }

            // 6. 保存报告到 H2
            history.setAiAnalysisReport(report.toString());
            history.setStatus(SlowQueryHistory.AnalysisStatus.SUCCESS);
            historyRepo.save(history);

            // 7. 发送通知（如果是严重慢查询）
            if (history.getAvgQueryTime() != null && history.getAvgQueryTime() > 3.0) {
                notifyService.sendNotification(report.toString());
            }

            log.info("✅ AI 分析完成: fingerprint={}", fingerprint);

        } catch (Exception e) {
            log.error("❌ AI 分析失败: fingerprint={}", fingerprint, e);

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
}
