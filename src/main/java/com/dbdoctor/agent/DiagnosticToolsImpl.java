package com.dbdoctor.agent;

import com.alibaba.fastjson2.JSON;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

/**
 * 数据库诊断工具实现类(非 Spring Bean)
 *
 * 核心特性：
 * - 纯 POJO 类,不被 Spring 代理
 * - 专门用于 LangChain4j 工具调用
 * - 避免了 Spring CGLIB 代理导致的工具注册失败问题
 *
 * 使用方式：
 * 在 AiConfig 中手动创建实例: new DiagnosticToolsImpl(jdbcTemplate)
 *
 * @author DB-Doctor
 * @version 2.2.0
 */
@Slf4j
@RequiredArgsConstructor
public class DiagnosticToolsImpl implements DiagnosticTools {

    private final JdbcTemplate targetJdbcTemplate;

    /**
     * 获取表结构信息
     *
     * @param database  数据库名
     * @param tableName 表名
     * @return 表结构信息(JSON 格式字符串)
     */
    @Tool("""
    获取指定表的结构信息,包括列名、数据类型、是否可空、键类型等。

    参数说明:
    - database: 数据库名称
    - tableName: 表名

    返回: JSON 格式的表结构信息
    """)
    @Override
    public String getTableSchema(String database, String tableName) {
        log.info("🔧 [工具调用] 查询表结构: database={}, table={}", database, tableName);

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
        log.info("✅ [工具返回] 查询到 {} 列", result.size());
        return JSON.toJSONString(result);
    }

    /**
     * 获取执行计划
     *
     * @param database 数据库名
     * @param sql      SQL 语句
     * @return EXPLAIN 结果(JSON 格式字符串)
     */
    @Tool
    @Override
    public String getExecutionPlan(String database, String sql) {
        log.info("🔧 [工具调用] 获取执行计划: database={}, sql={}", database, sql);

        // 数据库名称安全验证(防止 SQL 注入)
        if (!database.matches("^[a-zA-Z0-9_]+$")) {
            throw new IllegalArgumentException("❌ 无效的数据库名称: " + database);
        }

        // 先切换到目标数据库(因为 JdbcTemplate 连接的是 information_schema)
        targetJdbcTemplate.execute("USE `" + database + "`");

        // 在目标数据库执行 EXPLAIN
        String explainSql = "EXPLAIN " + sql;
        List<Map<String, Object>> result = targetJdbcTemplate.queryForList(explainSql);
        log.info("✅ [工具返回] 执行计划包含 {} 步", result.size());
        return JSON.toJSONString(result);
    }

    /**
     * 获取表统计信息
     *
     * @param database  数据库名
     * @param tableName 表名
     * @return 表统计信息(JSON 格式字符串)
     */
    @Tool
    @Override
    public String getTableStatistics(String database, String tableName) {
        log.info("🔧 [工具调用] 查询表统计信息: database={}, table={}", database, tableName);

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
        log.info("✅ [工具返回] 表统计信息查询完成");
        return JSON.toJSONString(result.isEmpty() ? Map.of() : result.get(0));
    }

    /**
     * 获取索引选择性
     *
     * @param database  数据库名
     * @param tableName 表名
     * @return 索引选择性信息(JSON 格式字符串)
     */
    @Tool
    @Override
    public String getIndexSelectivity(String database, String tableName) {
        log.info("🔧 [工具调用] 查询索引选择性: database={}, table={}", database, tableName);

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
        log.info("✅ [工具返回] 索引选择性查询完成,共 {} 个索引字段", result.size());
        return JSON.toJSONString(result);
    }

    /**
     * 获取锁等待信息
     *
     * @return 锁等待信息(JSON 格式字符串)
     */
    @Tool
    @Override
    public String getLockInfo() {
        log.info("🔧 [工具调用] 查询锁等待信息");

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
        log.info("✅ [工具返回] 锁等待信息查询完成,共 {} 个锁等待", result.size());
        return JSON.toJSONString(result);
    }

    /**
     * 对比 SQL 性能
     *
     * @param oldSql 旧 SQL
     * @param newSql 新 SQL
     * @return 性能对比结果(JSON 格式字符串)
     */
    @Tool
    @Override
    public String compareSqlPerformance(String oldSql, String newSql) {
        log.info("🔧 [工具调用] 对比 SQL 性能");

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
                "improvement", String.format("%.2f%%", (1 - (double) newTime / oldTime) * 100)
        );

        log.info("✅ [工具返回] 性能对比完成: 旧SQL {}ms, 新SQL {}ms, 提升 {}", oldTime, newTime, result.get("improvement"));
        return JSON.toJSONString(result);
    }

    /**
     * 获取表的索引信息
     *
     * @param database  数据库名
     * @param tableName 表名
     * @return 索引信息(JSON 格式字符串)
     */
    @Tool
    @Override
    public String getTableIndexes(String database, String tableName) {
        log.info("🔧 [工具调用] 查询表索引: database={}, table={}", database, tableName);

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
        log.info("✅ [工具返回] 索引信息查询完成,共 {} 个索引", result.size());
        return JSON.toJSONString(result);
    }
}
