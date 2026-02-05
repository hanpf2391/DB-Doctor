package com.dbdoctor.agent;

import com.alibaba.fastjson2.JSON;
import com.dbdoctor.common.enums.ErrorCode;
import com.dbdoctor.model.ToolResult;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * 数据库诊断工具实现类(非 Spring Bean)
 *
 * 核心特性：
 * - 纯 POJO 类,不被 Spring 代理
 * - 专门用于 LangChain4j 工具调用
 * - 避免了 Spring CGLIB 代理导致的工具注册失败问题
 * - v3.0.0：所有方法返回 ToolResult，统一错误处理
 *
 * 使用方式：
 * 在 AiConfig 中手动创建实例: new DiagnosticToolsImpl(jdbcTemplate)
 *
 * @author DB-Doctor
 * @version 3.0.0
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
     * @return ToolResult - 成功时 data 字段包含表结构信息（JSON 格式）
     */
    @Tool("""
    获取指定表的结构信息,包括列名、数据类型、是否可空、键类型等。

    参数说明:
    - database: 数据库名称
    - tableName: 表名

    返回: ToolResult - JSON 格式，包含 success 字段
    - success=true: data 字段包含表结构信息
    - success=false: errorCode 和 userMessage 字段包含错误信息
    """)
    @Override
    public ToolResult getTableSchema(String database, String tableName) {
        long startTime = System.currentTimeMillis();

        try {
            log.info("🔧 [工具调用] 查询表结构: database={}, table={}", database, tableName);

            // 参数校验
            if (database == null || database.trim().isEmpty()) {
                return ToolResult.failure(ErrorCode.TABLE_NOT_FOUND, "数据库名称不能为空", tableName);
            }
            if (tableName == null || tableName.trim().isEmpty()) {
                return ToolResult.failure(ErrorCode.TABLE_NOT_FOUND, "表名称不能为空", database);
            }

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

            // 检查结果是否为空
            if (result.isEmpty()) {
                log.warn("⚠️ [工具返回] 表不存在或查询结果为空: {}.{}", database, tableName);
                return ToolResult.failure(
                    ErrorCode.TABLE_NOT_FOUND,
                    String.format("表 '%s.%s' 不存在或查询结果为空", database, tableName),
                    database, tableName
                );
            }

            long executionTime = System.currentTimeMillis() - startTime;
            log.info("✅ [工具返回] 查询到 {} 列, 耗时 {}ms", result.size(), executionTime);

            return ToolResult.success(JSON.toJSONString(result), executionTime);

        } catch (DataAccessException e) {
            // 解析数据库异常
            ErrorCode errorCode = parseDatabaseException(e);
            String errorMessage = String.format("查询表结构失败: %s.%s - %s", database, tableName, e.getMessage());

            log.error("❌ [工具返回] {}", errorMessage, e);
            return ToolResult.failure(errorCode, errorMessage, database, tableName);
        } catch (Exception e) {
            log.error("❌ [工具返回] 查询表结构时发生未知错误", e);
            return ToolResult.failure(ErrorCode.SYNTAX_ERROR, e.getMessage());
        }
    }

    /**
     * 获取执行计划
     *
     * @param database 数据库名（可为空，会自动从 SQL 提取）
     * @param sql      SQL 语句
     * @return ToolResult - 成功时 data 字段包含 EXPLAIN 结果（JSON 格式）
     */
    @Tool
    @Override
    public ToolResult getExecutionPlan(String database, String sql) {
        long startTime = System.currentTimeMillis();

        try {
            log.info("🔧 [工具调用] 获取执行计划: database={}, sql={}", database, sql);

            // 参数校验
            if (sql == null || sql.trim().isEmpty()) {
                return ToolResult.failure(ErrorCode.SYNTAX_ERROR, "SQL 语句不能为空");
            }

            // 如果 database 为空，尝试从 SQL 中提取
            if (database == null || database.trim().isEmpty()) {
                database = extractDatabaseFromSql(sql);
                if (database == null || database.trim().isEmpty()) {
                    return ToolResult.failure(ErrorCode.DB_NOT_FOUND,
                        "无法从 SQL 中提取数据库名，且未提供 database 参数。SQL: " + sql.substring(0, Math.min(100, sql.length())));
                }
                log.info("🔍 [自动提取] 从 SQL 中提取数据库名: {}", database);
            }

            // 数据库名称安全验证(防止 SQL 注入)
            if (!database.matches("^[a-zA-Z0-9_]+$")) {
                return ToolResult.failure(ErrorCode.SYNTAX_ERROR, "无效的数据库名称: " + database);
            }

            // 先切换到目标数据库
            try {
                targetJdbcTemplate.execute("USE `" + database + "`");
            } catch (DataAccessException e) {
                // 数据库不存在
                if (e.getMessage() != null && e.getMessage().contains("Unknown database")) {
                    log.warn("⚠️ [工具返回] 数据库不存在: {}", database);
                    return ToolResult.failure(ErrorCode.DB_NOT_FOUND, e.getMessage(), database);
                }
                throw e;
            }

            // 在目标数据库执行 EXPLAIN
            String explainSql = "EXPLAIN " + sql;
            List<Map<String, Object>> result = targetJdbcTemplate.queryForList(explainSql);

            long executionTime = System.currentTimeMillis() - startTime;
            log.info("✅ [工具返回] 执行计划包含 {} 步, 耗时 {}ms", result.size(), executionTime);

            return ToolResult.success(JSON.toJSONString(result), executionTime);

        } catch (DataAccessException e) {
            ErrorCode errorCode = parseDatabaseException(e);
            String errorMessage = String.format("获取执行计划失败: %s - %s", database, e.getMessage());

            log.error("❌ [工具返回] {}", errorMessage, e);
            return ToolResult.failure(errorCode, errorMessage, database);
        } catch (Exception e) {
            log.error("❌ [工具返回] 获取执行计划时发生未知错误", e);
            return ToolResult.failure(ErrorCode.SYNTAX_ERROR, e.getMessage());
        }
    }

    /**
     * 获取表统计信息
     *
     * @param database  数据库名
     * @param tableName 表名
     * @return ToolResult - 成功时 data 字段包含表统计信息（JSON 格式）
     */
    @Tool
    @Override
    public ToolResult getTableStatistics(String database, String tableName) {
        long startTime = System.currentTimeMillis();

        try {
            log.info("🔧 [工具调用] 查询表统计信息: database={}, table={}", database, tableName);

            // 参数校验
            if (database == null || database.trim().isEmpty()) {
                return ToolResult.failure(ErrorCode.TABLE_NOT_FOUND, "数据库名称不能为空", tableName);
            }
            if (tableName == null || tableName.trim().isEmpty()) {
                return ToolResult.failure(ErrorCode.TABLE_NOT_FOUND, "表名称不能为空", database);
            }

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

            // 检查结果是否为空
            if (result.isEmpty()) {
                log.warn("⚠️ [工具返回] 表不存在: {}.{}", database, tableName);
                return ToolResult.failure(
                    ErrorCode.TABLE_NOT_FOUND,
                    String.format("表 '%s.%s' 不存在", database, tableName),
                    database, tableName
                );
            }

            long executionTime = System.currentTimeMillis() - startTime;
            log.info("✅ [工具返回] 表统计信息查询完成, 耗时 {}ms", executionTime);

            return ToolResult.success(JSON.toJSONString(result.get(0)), executionTime);

        } catch (DataAccessException e) {
            ErrorCode errorCode = parseDatabaseException(e);
            String errorMessage = String.format("查询表统计信息失败: %s.%s - %s", database, tableName, e.getMessage());

            log.error("❌ [工具返回] {}", errorMessage, e);
            return ToolResult.failure(errorCode, errorMessage, database, tableName);
        } catch (Exception e) {
            log.error("❌ [工具返回] 查询表统计信息时发生未知错误", e);
            return ToolResult.failure(ErrorCode.SYNTAX_ERROR, e.getMessage());
        }
    }

    /**
     * 获取索引选择性
     *
     * @param database  数据库名
     * @param tableName 表名
     * @return ToolResult - 成功时 data 字段包含索引选择性信息（JSON 格式）
     */
    @Tool
    @Override
    public ToolResult getIndexSelectivity(String database, String tableName) {
        long startTime = System.currentTimeMillis();

        try {
            log.info("🔧 [工具调用] 查询索引选择性: database={}, table={}", database, tableName);

            // 参数校验
            if (database == null || database.trim().isEmpty()) {
                return ToolResult.failure(ErrorCode.TABLE_NOT_FOUND, "数据库名称不能为空", tableName);
            }
            if (tableName == null || tableName.trim().isEmpty()) {
                return ToolResult.failure(ErrorCode.TABLE_NOT_FOUND, "表名称不能为空", database);
            }

            String sql = """
                    SELECT
                        INDEX_NAME as index_name,
                        COLUMN_NAME as column_name,
                        CARDINALITY as cardinality,
                        SUB_PART as sub_part,
                        NULLABLE as nullable,
                        INDEX_TYPE as index_type
                    FROM information_schema.STATISTICS
                    WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?
                    ORDER BY INDEX_NAME, SEQ_IN_INDEX
                    """;

            List<Map<String, Object>> result = targetJdbcTemplate.queryForList(sql, database, tableName);

            long executionTime = System.currentTimeMillis() - startTime;
            log.info("✅ [工具返回] 索引选择性查询完成,共 {} 个索引字段, 耗时 {}ms", result.size(), executionTime);

            return ToolResult.success(JSON.toJSONString(result), executionTime);

        } catch (DataAccessException e) {
            ErrorCode errorCode = parseDatabaseException(e);
            String errorMessage = String.format("查询索引选择性失败: %s.%s - %s", database, tableName, e.getMessage());

            log.error("❌ [工具返回] {}", errorMessage, e);
            return ToolResult.failure(errorCode, errorMessage, database, tableName);
        } catch (Exception e) {
            log.error("❌ [工具返回] 查询索引选择性时发生未知错误", e);
            return ToolResult.failure(ErrorCode.SYNTAX_ERROR, e.getMessage());
        }
    }

    /**
     * 获取锁等待信息
     *
     * @return ToolResult - 成功时 data 字段包含锁等待信息（JSON 格式）
     */
    @Tool
    @Override
    public ToolResult getLockInfo() {
        long startTime = System.currentTimeMillis();

        try {
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

            long executionTime = System.currentTimeMillis() - startTime;
            log.info("✅ [工具返回] 锁等待信息查询完成,共 {} 个锁等待, 耗时 {}ms", result.size(), executionTime);

            return ToolResult.success(JSON.toJSONString(result), executionTime);

        } catch (DataAccessException e) {
            ErrorCode errorCode = parseDatabaseException(e);
            String errorMessage = "查询锁等待信息失败: " + e.getMessage();

            log.error("❌ [工具返回] {}", errorMessage, e);
            return ToolResult.failure(errorCode, errorMessage);
        } catch (Exception e) {
            log.error("❌ [工具返回] 查询锁等待信息时发生未知错误", e);
            return ToolResult.failure(ErrorCode.SYNTAX_ERROR, e.getMessage());
        }
    }

    /**
     * 对比 SQL 性能
     *
     * @param oldSql 旧 SQL
     * @param newSql 新 SQL
     * @return ToolResult - 成功时 data 字段包含性能对比结果（JSON 格式）
     */
    @Tool
    @Override
    public ToolResult compareSqlPerformance(String oldSql, String newSql) {
        long startTime = System.currentTimeMillis();

        try {
            log.info("🔧 [工具调用] 对比 SQL 性能");

            // 参数校验
            if (oldSql == null || oldSql.trim().isEmpty()) {
                return ToolResult.failure(ErrorCode.SYNTAX_ERROR, "旧 SQL 语句不能为空");
            }
            if (newSql == null || newSql.trim().isEmpty()) {
                return ToolResult.failure(ErrorCode.SYNTAX_ERROR, "新 SQL 语句不能为空");
            }

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

            long executionTime = System.currentTimeMillis() - startTime;
            log.info("✅ [工具返回] 性能对比完成: 旧SQL {}ms, 新SQL {}ms, 提升 {}, 耗时 {}ms",
                    oldTime, newTime, result.get("improvement"), executionTime);

            return ToolResult.success(JSON.toJSONString(result), executionTime);

        } catch (DataAccessException e) {
            ErrorCode errorCode = parseDatabaseException(e);
            String errorMessage = "SQL 性能对比失败: " + e.getMessage();

            log.error("❌ [工具返回] {}", errorMessage, e);
            return ToolResult.failure(errorCode, errorMessage);
        } catch (Exception e) {
            log.error("❌ [工具返回] SQL 性能对比时发生未知错误", e);
            return ToolResult.failure(ErrorCode.SYNTAX_ERROR, e.getMessage());
        }
    }

    /**
     * 获取表的索引信息
     *
     * @param database  数据库名
     * @param tableName 表名
     * @return ToolResult - 成功时 data 字段包含索引信息（JSON 格式）
     */
    @Tool
    @Override
    public ToolResult getTableIndexes(String database, String tableName) {
        long startTime = System.currentTimeMillis();

        try {
            log.info("🔧 [工具调用] 查询表索引: database={}, table={}", database, tableName);

            // 参数校验
            if (database == null || database.trim().isEmpty()) {
                return ToolResult.failure(ErrorCode.TABLE_NOT_FOUND, "数据库名称不能为空", tableName);
            }
            if (tableName == null || tableName.trim().isEmpty()) {
                return ToolResult.failure(ErrorCode.TABLE_NOT_FOUND, "表名称不能为空", database);
            }

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

            // 检查结果是否为空（表可能不存在）
            if (result.isEmpty()) {
                log.warn("⚠️ [工具返回] 表不存在或无索引: {}.{}", database, tableName);
                // 返回空结果，而不是错误（空结果也是有效信息）
                return ToolResult.success("[]", startTime);
            }

            long executionTime = System.currentTimeMillis() - startTime;
            log.info("✅ [工具返回] 索引信息查询完成,共 {} 个索引, 耗时 {}ms", result.size(), executionTime);

            return ToolResult.success(JSON.toJSONString(result), executionTime);

        } catch (DataAccessException e) {
            ErrorCode errorCode = parseDatabaseException(e);
            String errorMessage = String.format("查询表索引失败: %s.%s - %s", database, tableName, e.getMessage());

            log.error("❌ [工具返回] {}", errorMessage, e);
            return ToolResult.failure(errorCode, errorMessage, database, tableName);
        } catch (Exception e) {
            log.error("❌ [工具返回] 查询表索引时发生未知错误", e);
            return ToolResult.failure(ErrorCode.SYNTAX_ERROR, e.getMessage());
        }
    }

    /**
     * 解析数据库异常为错误码
     *
     * @param e 数据库异常
     * @return 错误码
     */
    private ErrorCode parseDatabaseException(DataAccessException e) {
        Throwable rootCause = e.getRootCause();
        if (rootCause instanceof SQLException sqlEx) {
            return ErrorCode.fromDatabaseError(sqlEx.getSQLState(), sqlEx.getMessage());
        }

        // 如果无法获取 SQLException，尝试从异常消息推断
        String message = e.getMessage();
        if (message != null) {
            return ErrorCode.fromDatabaseError(null, message);
        }

        // 默认返回语法错误
        return ErrorCode.SYNTAX_ERROR;
    }

    /**
     * 从 SQL 语句中提取数据库名
     * 支持格式：database.table 或 `database`.`table`
     *
     * @param sql SQL 语句
     * @return 数据库名，如果无法提取则返回 null
     */
    private String extractDatabaseFromSql(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return null;
        }

        try {
            // 转大写并移除多余空格，便于解析
            String normalizedSql = sql.toUpperCase().replaceAll("\\s+", " ");

            // 查找 FROM 子句的位置
            int fromIndex = normalizedSql.indexOf(" FROM ");
            if (fromIndex == -1) {
                // 如果没有 FROM，尝试查找 UPDATE
                fromIndex = normalizedSql.indexOf(" UPDATE ");
                if (fromIndex == -1) {
                    // 如果也没有 UPDATE，尝试查找 INSERT INTO
                    fromIndex = normalizedSql.indexOf(" INTO ");
                    if (fromIndex == -1) {
                        return null;
                    }
                    fromIndex += 6; // " INTO ".length()
                } else {
                    fromIndex += 7; // " UPDATE ".length()
                }
            } else {
                fromIndex += 6; // " FROM ".length()
            }

            // 提取 FROM/UPDATE/INTO 之后的部分（到下一个关键字之前）
            String afterFrom = normalizedSql.substring(fromIndex).trim();

            // 查找第一个表引用（可能带有数据库名前缀）
            // 匹配模式：`database`.`table` 或 database.table 或 table
            String firstTableRef;
            int spaceIndex = afterFrom.indexOf(' ');
            int commaIndex = afterFrom.indexOf(',');
            int joinIndex = afterFrom.indexOf(" JOIN ");

            // 取最近的分隔符
            int endIndex = afterFrom.length();
            if (spaceIndex > 0 && spaceIndex < endIndex) endIndex = spaceIndex;
            if (commaIndex > 0 && commaIndex < endIndex) endIndex = commaIndex;
            if (joinIndex > 0 && joinIndex < endIndex) endIndex = joinIndex;

            firstTableRef = afterFrom.substring(0, endIndex).trim();

            // 移除可能的别名（AS 或空格后的别名）
            if (firstTableRef.contains(" AS ")) {
                firstTableRef = firstTableRef.substring(0, firstTableRef.indexOf(" AS ")).trim();
            }

            // 提取数据库名（支持带反引号和不带反引号）
            String dbName = null;

            // 匹配 `database`.`table` 或 database.table
            if (firstTableRef.contains(".")) {
                String[] parts = firstTableRef.split("\\.");
                if (parts.length >= 2) {
                    dbName = parts[0].replaceAll("`", "").trim();
                }
            }

            log.debug("🔍 [SQL解析] 从 SQL 中提取数据库名: {} (来源: {})", dbName, firstTableRef);

            return dbName;

        } catch (Exception e) {
            log.warn("⚠️ [SQL解析] 提取数据库名失败: {}", e.getMessage());
            return null;
        }
    }
}
