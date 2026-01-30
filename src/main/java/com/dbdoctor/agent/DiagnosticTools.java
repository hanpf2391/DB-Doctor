package com.dbdoctor.agent;

/**
 * 数据库诊断工具接口
 * 定义所有 AI Agent 可调用的诊断方法
 *
 * @author DB-Doctor
 * @version 2.2.0
 */
public interface DiagnosticTools {

    /**
     * 获取表结构信息
     *
     * @param database  数据库名
     * @param tableName 表名
     * @return 表结构信息（JSON 格式字符串）
     */
    String getTableSchema(String database, String tableName);

    /**
     * 获取执行计划
     *
     * @param database 数据库名
     * @param sql      SQL 语句
     * @return EXPLAIN 结果（JSON 格式字符串）
     */
    String getExecutionPlan(String database, String sql);

    /**
     * 获取表统计信息
     *
     * @param database  数据库名
     * @param tableName 表名
     * @return 表统计信息（JSON 格式字符串）
     */
    String getTableStatistics(String database, String tableName);

    /**
     * 获取索引选择性
     *
     * @param database  数据库名
     * @param tableName 表名
     * @return 索引选择性信息（JSON 格式字符串）
     */
    String getIndexSelectivity(String database, String tableName);

    /**
     * 获取锁等待信息
     *
     * @return 锁等待信息（JSON 格式字符串）
     */
    String getLockInfo();

    /**
     * 对比 SQL 性能
     *
     * @param oldSql 旧 SQL
     * @param newSql 新 SQL
     * @return 性能对比结果（JSON 格式字符串）
     */
    String compareSqlPerformance(String oldSql, String newSql);

    /**
     * 获取表的索引信息
     *
     * @param database  数据库名
     * @param tableName 表名
     * @return 索引信息（JSON 格式字符串）
     */
    String getTableIndexes(String database, String tableName);
}
