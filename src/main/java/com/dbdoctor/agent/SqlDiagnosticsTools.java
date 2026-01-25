package com.dbdoctor.agent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * SQL 诊断工具箱
 * 提供 AI Agent 可调用的数据库诊断工具方法
 *
 * 核心功能：
 * - 查询表结构信息
 * - 获取执行计划
 * - 查询表统计信息
 * - 检查索引选择性
 * - 对比 SQL 性能
 *
 * @author DB-Doctor
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SqlDiagnosticsTools {

    private final JdbcTemplate targetJdbcTemplate;

    /**
     * 获取表结构信息
     *
     * @param database  数据库名
     * @param tableName 表名
     * @return 表结构信息（字段名、类型、是否为空、键信息等）
     */
    public List<Map<String, Object>> getTableSchema(String database, String tableName) {
        log.info("查询表结构: database={}, table={}", database, tableName);

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

        return targetJdbcTemplate.queryForList(sql, database, tableName);
    }

    /**
     * 获取执行计划
     *
     * @param database 数据库名
     * @param sql      SQL 语句
     * @return EXPLAIN 结果
     */
    public List<Map<String, Object>> getExecutionPlan(String database, String sql) {
        log.info("获取执行计划: database={}, sql={}", database, sql);

        // 在目标数据库执行 EXPLAIN
        String explainSql = "EXPLAIN " + sql;
        return targetJdbcTemplate.queryForList(explainSql);
    }

    /**
     * 获取表统计信息
     *
     * @param database  数据库名
     * @param tableName 表名
     * @return 表统计信息（行数、更新时间等）
     */
    public Map<String, Object> getTableStatistics(String database, String tableName) {
        log.info("查询表统计信息: database={}, table={}", database, tableName);

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
        return result.isEmpty() ? Map.of() : result.get(0);
    }

    /**
     * 获取索引选择性
     *
     * @param database  数据库名
     * @param tableName 表名
     * @return 索引选择性信息
     */
    public List<Map<String, Object>> getIndexSelectivity(String database, String tableName) {
        log.info("查询索引选择性: database={}, table={}", database, tableName);

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

        return targetJdbcTemplate.queryForList(sql, database, tableName);
    }

    /**
     * 获取锁等待信息
     *
     * @return 锁等待信息
     */
    public List<Map<String, Object>> getLockInfo() {
        log.info("查询锁等待信息");

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

        return targetJdbcTemplate.queryForList(sql);
    }

    /**
     * 对比 SQL 性能
     *
     * @param oldSql 旧 SQL
     * @param newSql 新 SQL
     * @return 性能对比结果
     */
    public Map<String, Object> compareSqlPerformance(String oldSql, String newSql) {
        log.info("对比 SQL 性能: oldSql={}, newSql={}", oldSql, newSql);

        // 执行旧 SQL 并记录性能
        long oldStart = System.currentTimeMillis();
        List<Map<String, Object>> oldResult = targetJdbcTemplate.queryForList(oldSql);
        long oldTime = System.currentTimeMillis() - oldStart;

        // 执行新 SQL 并记录性能
        long newStart = System.currentTimeMillis();
        List<Map<String, Object>> newResult = targetJdbcTemplate.queryForList(newSql);
        long newTime = System.currentTimeMillis() - newStart;

        // 返回对比结果
        return Map.of(
                "oldSqlTime", oldTime + "ms",
                "newSqlTime", newTime + "ms",
                "oldRows", oldResult.size(),
                "newRows", newResult.size(),
                "improvement", String.format("%.2f%%", (1 - (double) newTime / oldTime) * 100)
        );
    }

    /**
     * 获取表的索引信息
     *
     * @param database  数据库名
     * @param tableName 表名
     * @return 索引信息
     */
    public List<Map<String, Object>> getTableIndexes(String database, String tableName) {
        log.info("查询表索引: database={}, table={}", database, tableName);

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

        return targetJdbcTemplate.queryForList(sql, database, tableName);
    }
}
