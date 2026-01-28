package com.dbdoctor.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Mock AI Agent（用于性能测试）
 * 模拟真实的 AI 分析过程，耗时可配置（默认 30 秒）
 *
 * 功能：
 * 1. 模拟 AI 分析耗时（通过配置文件设置）
 * 2. 生成模拟的分析报告（包含随机数据）
 * 3. 可配置是否真正调用 AI
 *
 * 使用方式：
 * - 在 application-local.yml 中设置 db-doctor.ai.mock-enabled=true
 * - 设置 db-doctor.ai.mock-delay-seconds=30（模拟30秒耗时）
 *
 * @author DB-Doctor
 * @version 2.0.0
 */
@Slf4j
@Component
@Primary  // 优先使用 Mock Agent
@ConditionalOnProperty(name = "db-doctor.ai.mock-enabled", havingValue = "true", matchIfMissing = false)
public class MockDBAgent implements DBAgent {

    @Value("${db-doctor.ai.mock-delay-seconds:30}")
    private int mockDelaySeconds;

    /**
     * 模拟慢查询分析
     *
     * @param database      数据库名
     * @param logTime       日志时间
     * @param queryTime     查询耗时（秒）
     * @param lockTime      锁等待时间（秒）
     * @param rowsExamined  扫描行数
     * @param rowsSent      返回行数
     * @param sql           SQL 语句
     * @return 模拟的诊断报告（Markdown 格式）
     */
    @Override
    public String analyzeSlowLog(
            String database,
            String logTime,
            Double queryTime,
            Double lockTime,
            Long rowsExamined,
            Long rowsSent,
            String sql) {

        long startTime = System.currentTimeMillis();
        log.info("🤖 Mock AI 开始分析: db={}, queryTime={}s, 预计耗时: {}s",
                database, queryTime, mockDelaySeconds);

        try {
            // === 阶段 1：模拟 AI 思考时间 ===
            simulateAiThinking();

            // === 阶段 2：生成模拟报告 ===
            String report = generateMockReport(
                    database, logTime, queryTime, lockTime,
                    rowsExamined, rowsSent, sql);

            long duration = System.currentTimeMillis() - startTime;
            log.info("✅ Mock AI 分析完成: db={}, 耗时: {}ms", database, duration);

            return report;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("❌ Mock AI 分析被中断: db={}", database, e);
            throw new RuntimeException("Mock AI 分析被中断", e);
        }
    }

    /**
     * 模拟 AI 思考过程（分段 sleep，模拟真实 AI 的流式响应）
     */
    private void simulateAiThinking() throws InterruptedException {
        int totalDelay = mockDelaySeconds * 1000; // 转换为毫秒

        // 分成 3 个阶段，模拟不同步骤的耗时
        int[] phases = {
                (int) (totalDelay * 0.3),  // 阶段1：读取慢查询日志（30%）
                (int) (totalDelay * 0.5),  // 阶段2：调用工具分析（50%）
                (int) (totalDelay * 0.2)   // 阶段3：生成报告（20%）
        };

        for (int i = 0; i < phases.length; i++) {
            log.debug("Mock AI 阶段 {}/3: 耗时 {}ms", i + 1, phases[i]);
            Thread.sleep(phases[i]);
        }
    }

    /**
     * 生成模拟的分析报告
     */
    private String generateMockReport(
            String database,
            String logTime,
            Double queryTime,
            Double lockTime,
            Long rowsExamined,
            Long rowsSent,
            String sql) {

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String currentTime = LocalDateTime.now().format(formatter);

        // 随机生成一些性能数据
        int oldCost = ThreadLocalRandom.current().nextInt(10000, 50000);
        int newCost = oldCost / ThreadLocalRandom.current().nextInt(2, 5);
        int improvement = (int) ((1 - (double) newCost / oldCost) * 100);

        StringBuilder report = new StringBuilder();
        report.append("## 问题诊断\n\n");
        report.append(String.format("- **根本原因**: 缺少索引导致全表扫描\n"));
        report.append(String.format("- **严重程度**: %s\n\n", determineSeverity(queryTime)));

        report.append("## 执行计划分析\n\n");
        report.append("```\n");
        report.append("+----+-------------+-------+------+---------------+------+---------+------+------+-------+\n");
        report.append("| id | select_type | table | type | possible_keys | key  | key_len | ref  | rows | Extra |\n");
        report.append("+----+-------------+-------+------+---------------+------+---------+------+------+-------+\n");
        report.append("|  1 | SIMPLE      | ").append(extractTableName(sql)).append(" | ALL  | NULL          | NULL | NULL    | NULL | ").append(rowsExamined).append(" | Using where |\n");
        report.append("+----+-------------+-------+------+---------------+------+---------+------+------+-------+\n");
        report.append("```\n\n");

        report.append("## 优化建议\n\n");
        report.append("1. **建议添加索引**：\n");
        report.append("   ```sql\n");
        report.append("   CREATE INDEX idx_").append(generateIndexName(sql)).append(" ON ").append(extractTableName(sql)).append("(").append(generateColumnName(sql)).append(");\n");
        report.append("   ```\n\n");

        report.append("2. **建议 SQL 优化**：\n");
        report.append("   ```sql\n");
        report.append("   -- 原始 SQL\n");
        report.append(sql.trim()).append("\n\n");
        report.append("   -- 优化后 SQL\n");
        report.append("   SELECT * FROM ").append(extractTableName(sql)).append(" WHERE ").append(generateColumnName(sql)).append(" = ? LIMIT 1000;\n");
        report.append("   ```\n\n");

        report.append("3. **其他建议**：\n");
        report.append("   - 执行 `ANALYZE TABLE ").append(extractTableName(sql)).append("` 刷新统计信息\n");
        report.append("   - 调整 `innodb_buffer_pool_size` 参数\n\n");

        report.append("## 预期效果\n\n");
        report.append(String.format("- 查询成本从 %d 降低到 %d\n", oldCost, newCost));
        report.append(String.format("- 性能提升约 %d%%\n\n", improvement));

        report.append("---\n");
        report.append(String.format("生成时间：%s\n", currentTime));
        report.append("DB-Doctor v2.0.0 (Mock AI Mode)\n");

        return report.toString();
    }

    /**
     * 根据查询耗时判断严重程度
     */
    private String determineSeverity(Double queryTime) {
        if (queryTime >= 10) {
            return "高 🔴";
        } else if (queryTime >= 3) {
            return "中 🟡";
        } else {
            return "低 🟢";
        }
    }

    /**
     * 从 SQL 中提取表名
     */
    private String extractTableName(String sql) {
        try {
            String lowerSql = sql.toLowerCase();
            int fromIndex = lowerSql.indexOf(" from ");
            if (fromIndex != -1) {
                int start = fromIndex + 6;
                String sub = sql.substring(start).trim();
                String[] parts = sub.split("\\s+");
                if (parts.length > 0) {
                    return parts[0].replaceAll("[`;,]", "");
                }
            }
            return "table_name";
        } catch (Exception e) {
            return "table_name";
        }
    }

    /**
     * 生成索引名
     */
    private String generateIndexName(String sql) {
        return "idx_" + System.currentTimeMillis() % 10000;
    }

    /**
     * 生成列名
     */
    private String generateColumnName(String sql) {
        String[] columns = {"id", "user_id", "created_at", "status", "type"};
        return columns[ThreadLocalRandom.current().nextInt(columns.length)];
    }
}
