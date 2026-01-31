package com.dbdoctor.agent;

import com.dbdoctor.model.ToolResult;

/**
 * 数据库诊断工具接口
 * 定义所有 AI Agent 可调用的诊断方法
 *
 * v3.0.0 更新：
 * - 所有方法返回类型从 String 改为 ToolResult
 * - 统一错误处理和封装
 * - AI 可以根据 ToolResult 的 success 字段判断是否成功
 * - AI 可以根据 errorCode 和 userMessage 生成错误报告
 *
 * @author DB-Doctor
 * @version 3.0.0
 */
public interface DiagnosticTools {

    /**
     * 获取表结构信息
     *
     * @param database  数据库名
     * @param tableName 表名
     * @return ToolResult - 成功时 data 字段包含表结构信息（JSON 格式）
     */
    ToolResult getTableSchema(String database, String tableName);

    /**
     * 获取执行计划
     *
     * @param database 数据库名
     * @param sql      SQL 语句
     * @return ToolResult - 成功时 data 字段包含 EXPLAIN 结果（JSON 格式）
     */
    ToolResult getExecutionPlan(String database, String sql);

    /**
     * 获取表统计信息
     *
     * @param database  数据库名
     * @param tableName 表名
     * @return ToolResult - 成功时 data 字段包含表统计信息（JSON 格式）
     */
    ToolResult getTableStatistics(String database, String tableName);

    /**
     * 获取索引选择性
     *
     * @param database  数据库名
     * @param tableName 表名
     * @return ToolResult - 成功时 data 字段包含索引选择性信息（JSON 格式）
     */
    ToolResult getIndexSelectivity(String database, String tableName);

    /**
     * 获取锁等待信息
     *
     * @return ToolResult - 成功时 data 字段包含锁等待信息（JSON 格式）
     */
    ToolResult getLockInfo();

    /**
     * 对比 SQL 性能
     *
     * @param oldSql 旧 SQL
     * @param newSql 新 SQL
     * @return ToolResult - 成功时 data 字段包含性能对比结果（JSON 格式）
     */
    ToolResult compareSqlPerformance(String oldSql, String newSql);

    /**
     * 获取表的索引信息
     *
     * @param database  数据库名
     * @param tableName 表名
     * @return ToolResult - 成功时 data 字段包含索引信息（JSON 格式）
     */
    ToolResult getTableIndexes(String database, String tableName);
}
