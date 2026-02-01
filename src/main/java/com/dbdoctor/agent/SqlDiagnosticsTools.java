package com.dbdoctor.agent;

import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * SQL 诊断工具箱实现（已废弃）
 *
 * ⚠️ 该类已被 DiagnosticToolsImpl 替代
 * 新实现返回 ToolResult 类型，提供企业级错误处理
 *
 * @deprecated 请使用 DiagnosticToolsImpl 代替
 * @author DB-Doctor
 * @version 2.2.0 (废弃于 3.0.0)
 */
@Slf4j
// @Component  // ⚠️ 已禁用，使用 DiagnosticToolsImpl 代替
@RequiredArgsConstructor
@Deprecated
public class SqlDiagnosticsTools {  // 不再实现 DiagnosticTools 接口

    private final JdbcTemplate targetJdbcTemplate;

    /**
     * 推理专家（用于深度分析）
     * 由 Spring 注入
     */
    private ReasoningAgent reasoningAgent;

    /**
     * 编码专家（用于生成优化代码）
     * 由 Spring 注入
     */
    private CodingAgent codingAgent;

    /**
     * Spring 注入方法（避免循环依赖）
     */
    public void setReasoningAgent(ReasoningAgent reasoningAgent) {
        this.reasoningAgent = reasoningAgent;
    }

    public void setCodingAgent(CodingAgent codingAgent) {
        this.codingAgent = codingAgent;
    }

    public ReasoningAgent getReasoningAgent() {
        return reasoningAgent;
    }

    public CodingAgent getCodingAgent() {
        return codingAgent;
    }

    /**
     * 获取表结构信息
     *
     * @param database  数据库名
     * @param tableName 表名
     * @return 表结构信息（JSON 格式字符串）
     */
    public String getTableSchema(String database, String tableName) {
        log.info("查询表结构: database={}, table={}", database, tableName);

        try {
            String sql = """
                    SELECT
                        COLUMN_NAME as column_name,
                        COLUMN_TYPE as column_type,
                        IS_NULLABLE as is_nullable,
                        COLUMN_KEY as column_key,
                        COLUMN_DEFAULT as column_default,
                        EXTRA as extra
                    FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?
                    ORDER BY ORDINAL_POSITION
                    """;

            List<Map<String, Object>> result = targetJdbcTemplate.queryForList(sql, database, tableName);

            if (result.isEmpty()) {
                log.warn("表结构查询结果为空: database={}, table={}", database, tableName);
                return "⚠️ 工具执行结果: 表 '" + database + "." + tableName + "' 不存在或无法访问。请检查表名是否正确，或基于 SQL 文本进行静态分析。";
            }

            return JSON.toJSONString(result);
        } catch (Exception e) {
            log.error("查询表结构失败: database={}, table={}", database, tableName, e);
            return "⚠️ 工具执行失败: 无法获取表 '" + database + "." + tableName + "' 的结构信息。错误: " + e.getMessage() + "。建议基于 SQL 文本进行静态分析。";
        }
    }

    /**
     * 获取执行计划
     *
     * @param database 数据库名
     * @param sql      SQL 语句
     * @return EXPLAIN 结果（JSON 格式字符串）
     */
    public String getExecutionPlan(String database, String sql) {
        log.info("获取执行计划: database={}, sql={}", database, sql);

        try {
            // 数据库名称安全验证（防止 SQL 注入）
            if (!database.matches("^[a-zA-Z0-9_]+$")) {
                return "⚠️ 工具执行失败: 无效的数据库名称 '" + database + "'（包含非法字符）。请使用字母、数字和下划线。";
            }

            // 先切换到目标数据库（因为 JdbcTemplate 连接的是 information_schema）
            targetJdbcTemplate.execute("USE `" + database + "`");

            // 在目标数据库执行 EXPLAIN
            String explainSql = "EXPLAIN " + sql;
            List<Map<String, Object>> result = targetJdbcTemplate.queryForList(explainSql);

            if (result.isEmpty()) {
                log.warn("执行计划查询结果为空: database={}", database);
                return "⚠️ 工具执行结果: 执行计划查询为空，可能是 SQL 语法错误或目标数据库 '" + database + "' 不存在。建议检查 SQL 语法。";
            }

            return JSON.toJSONString(result);
        } catch (Exception e) {
            log.error("获取执行计划失败: database={}, sql={}", database, sql, e);
            return "⚠️ 工具执行失败: 无法获取数据库 '" + database + "' 的执行计划。错误: " + e.getMessage() + "。建议基于 SQL 文本进行静态分析。";
        }
    }

    /**
     * 获取表统计信息
     *
     * @param database  数据库名
     * @param tableName 表名
     * @return 表统计信息（JSON 格式字符串）
     */
    public String getTableStatistics(String database, String tableName) {
        log.info("查询表统计信息: database={}, table={}", database, tableName);

        try {
            String sql = """
                    SELECT
                        TABLE_ROWS as table_rows,
                        AVG_ROW_LENGTH as avg_row_length,
                        DATA_LENGTH as data_length,
                        INDEX_LENGTH as index_length,
                        UPDATE_TIME as update_time,
                        AUTO_INCREMENT as auto_increment
                    FROM information_schema.TABLES
                    WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?
                    """;

            List<Map<String, Object>> result = targetJdbcTemplate.queryForList(sql, database, tableName);

            if (result.isEmpty()) {
                log.warn("表统计信息查询结果为空: database={}, table={}", database, tableName);
                return "{}";
            }

            return JSON.toJSONString(result.get(0));
        } catch (Exception e) {
            log.error("查询表统计信息失败: database={}, table={}", database, tableName, e);
            return "{}";
        }
    }

    /**
     * 获取索引选择性
     *
     * @param database  数据库名
     * @param tableName 表名
     * @return 索引选择性信息（JSON 格式字符串）
     */
    public String getIndexSelectivity(String database, String tableName) {
        log.info("查询索引选择性: database={}, table={}", database, tableName);

        try {
            String sql = """
                    SELECT
                        INDEX_NAME as index_name,
                        COLUMN_NAME as column_name,
                        CARDINALITY as cardinality,
                        SUBPART as subpart,
                        NULLABLE as nullable,
                        INDEX_TYPE as index_type
                    FROM information_schema.STATISTICS
                    WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?
                    ORDER BY INDEX_NAME, SEQ_IN_INDEX
                    """;

            List<Map<String, Object>> result = targetJdbcTemplate.queryForList(sql, database, tableName);

            if (result.isEmpty()) {
                log.warn("索引选择性查询结果为空: database={}, table={}", database, tableName);
                return "⚠️ 工具执行结果: 表 '" + database + "." + tableName + "' 没有索引或表不存在。";
            }

            return JSON.toJSONString(result);
        } catch (Exception e) {
            log.error("查询索引选择性失败: database={}, table={}", database, tableName, e);
            return "⚠️ 工具执行失败: 无法获取表 '" + database + "." + tableName + "' 的索引信息。错误: " + e.getMessage();
        }
    }

    /**
     * 获取锁等待信息
     *
     * @return 锁等待信息（JSON 格式字符串）
     */
    public String getLockInfo() {
        log.info("查询锁等待信息");

        try {
            String sql = """
                    SELECT
                        r.TRX_ID as waiting_trx_id,
                        r.TRX_MYSQL_THREAD_ID as waiting_thread,
                        r.TRX_QUERY as waiting_query,
                        b.TRX_ID as blocking_trx_id,
                        b.TRX_MYSQL_THREAD_ID as blocking_thread,
                        b.TRX_QUERY as blocking_query
                    FROM information_schema.INNODB_LOCK_WAITS w
                    JOIN information_schema.INNODB_TRX b ON b.TRX_ID = w.BLOCKING_TRX_ID
                    JOIN information_schema.INNODB_TRX r ON r.TRX_ID = w.REQUESTING_TRX_ID
                    """;

            List<Map<String, Object>> result = targetJdbcTemplate.queryForList(sql);

            if (result.isEmpty()) {
                log.debug("当前无锁等待");
                return "✅ 工具执行结果: 当前无锁等待";
            }

            return JSON.toJSONString(result);
        } catch (Exception e) {
            log.error("查询锁等待信息失败", e);
            return "⚠️ 工具执行失败: 无法查询锁等待信息。错误: " + e.getMessage();
        }
    }

    /**
     * 对比 SQL 性能
     *
     * @param oldSql 旧 SQL
     * @param newSql 新 SQL
     * @return 性能对比结果（JSON 格式字符串）
     */
    public String compareSqlPerformance(String oldSql, String newSql) {
        log.info("对比 SQL 性能: oldSql={}, newSql={}", oldSql, newSql);

        try {
            // 执行旧 SQL 并记录性能
            long oldStart = System.currentTimeMillis();
            List<Map<String, Object>> oldResult = targetJdbcTemplate.queryForList(oldSql);
            long oldTime = System.currentTimeMillis() - oldStart;

            // 执行新 SQL 并记录性能
            long newStart = System.currentTimeMillis();
            List<Map<String, Object>> newResult = targetJdbcTemplate.queryForList(newSql);
            long newTime = System.currentTimeMillis() - newStart;

            // 返回对比结果
            Map<String, Object> result = Map.of(
                    "oldSqlTime", oldTime + "ms",
                    "newSqlTime", newTime + "ms",
                    "oldRows", oldResult.size(),
                    "newRows", newResult.size(),
                    "improvement", oldTime > 0 ? String.format("%.2f%%", (1 - (double) newTime / oldTime) * 100) : "N/A"
            );

            return JSON.toJSONString(result);
        } catch (Exception e) {
            log.error("对比 SQL 性能失败", e);
            return "⚠️ 工具执行失败: 无法执行 SQL 性能对比。错误: " + e.getMessage() + "。请检查 SQL 语法或数据库权限。";
        }
    }

    /**
     * 获取表的索引信息
     *
     * @param database  数据库名
     * @param tableName 表名
     * @return 索引信息（JSON 格式字符串）
     */
    public String getTableIndexes(String database, String tableName) {
        log.info("查询表索引: database={}, table={}", database, tableName);

        try {
            String sql = """
                    SELECT
                        INDEX_NAME as index_name,
                        GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) as index_columns,
                        NON_UNIQUE as non_unique,
                        INDEX_TYPE as index_type
                    FROM information_schema.STATISTICS
                    WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?
                    GROUP BY INDEX_NAME, NON_UNIQUE, INDEX_TYPE
                    """;

            List<Map<String, Object>> result = targetJdbcTemplate.queryForList(sql, database, tableName);

            if (result.isEmpty()) {
                log.warn("表索引查询结果为空: database={}, table={}", database, tableName);
                return "⚠️ 工具执行结果: 表 '" + database + "." + tableName + "' 没有索引或表不存在。";
            }

            return JSON.toJSONString(result);
        } catch (Exception e) {
            log.error("查询表索引失败: database={}, table={}", database, tableName, e);
            return "⚠️ 工具执行失败: 无法获取表 '" + database + "." + tableName + "' 的索引信息。错误: " + e.getMessage();
        }
    }

    // === 多 Agent 协作方法 ===

    /**
     * 咨询推理专家（进行深度推理分析）
     *
     * 用途：当主治医生发现复杂问题时，调用推理专家进行深度分析
     *
     * @param diagnosisReport 主治医生的初步诊断报告
     * @param statistics      统计信息（JSON 格式）
     * @param executionPlan   执行计划（JSON 格式）
     * @return 推理专家的深度分析报告
     */
    public String consultExpert(String diagnosisReport, String statistics, String executionPlan) {
        log.info("调用推理专家进行深度分析");
        log.debug("诊断报告: {}", diagnosisReport);

        try {
            // 🔧 手动格式化提示词（解决 LangChain4j 占位符替换问题）
            String formattedPrompt = String.format(
                "请基于主治医生的诊断报告，进行深度推理分析：\n\n" +
                "【主治医生诊断报告】\n%s\n\n" +
                "【统计信息】\n%s\n\n" +
                "【执行计划】\n%s\n\n" +
                "请按照你的分析框架，从症状分析→根因推理→优化路径推导，给出完整的推理报告。",
                diagnosisReport, statistics, executionPlan
            );

            String deepAnalysis = reasoningAgent.performDeepReasoning(formattedPrompt);
            log.info("推理专家分析完成");
            return deepAnalysis;
        } catch (Exception e) {
            log.error("推理专家分析失败", e);
            return "推理专家分析失败: " + e.getMessage();
        }
    }

    /**
     * 执行 SQL 优化手术（生成优化代码）
     *
     * 用途：当需要优化 SQL 或添加索引时，调用编码专家生成优化代码
     *
     * @param originalSql  原始 SQL
     * @param problemDesc  问题描述（来自推理专家的分析）
     * @param executionPlan 执行计划（JSON 格式）
     * @return 编码专家生成的优化方案
     */
    public String performSurgery(String originalSql, String problemDesc, String executionPlan) {
        log.info("调用编码专家生成优化方案");
        log.debug("原始 SQL: {}", originalSql);

        try {
            // 🔧 手动格式化提示词（解决 LangChain4j 占位符替换问题）
            String formattedPrompt = String.format(
                "请基于问题描述，生成 SQL 优化方案：\n\n" +
                "【原始 SQL】\n%s\n\n" +
                "【问题分析】\n%s\n\n" +
                "【执行计划】\n%s\n\n" +
                "请按照你的优化原则，生成完整的优化方案，包括：\n" +
                "1. 优化后的 SQL（保持语义等价）\n" +
                "2. 推荐的索引设计\n" +
                "3. 实施建议和回滚方案",
                originalSql, problemDesc, executionPlan
            );

            String optimizationCode = codingAgent.generateOptimizationCode(formattedPrompt);
            log.info("编码专家优化方案生成完成");
            return optimizationCode;
        } catch (Exception e) {
            log.error("编码专家生成优化方案失败", e);
            return "编码专家生成优化方案失败: " + e.getMessage();
        }
    }

    /**
     * 生成索引创建语句
     *
     * @param tableName  表名
     * @param columns    索引列（逗号分隔）
     * @param indexType  索引类型（INDEX, UNIQUE INDEX, FULLTEXT INDEX）
     * @param comment    索引注释
     * @return 索引创建 SQL
     */
    public String generateIndexSql(String tableName, String columns, String indexType, String comment) {
        log.info("生成索引创建语句: table={}, columns={}, type={}", tableName, columns, indexType);

        try {
            return codingAgent.generateIndexSql(tableName, columns, indexType, comment);
        } catch (Exception e) {
            log.error("生成索引语句失败", e);
            return "-- 生成索引语句失败: " + e.getMessage();
        }
    }
}
