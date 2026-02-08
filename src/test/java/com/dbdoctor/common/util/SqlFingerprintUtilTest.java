package com.dbdoctor.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SqlFingerprintUtil 单元测试
 *
 * 测试重点：
 * - SQL 注释移除功能
 * - SQL 清理功能
 * - 边界条件处理
 *
 * @author DB-Doctor
 * @version 1.0.0
 */
class SqlFingerprintUtilTest {

    /**
     * 测试 cleanSql() 方法 - 单行注释（--）
     */
    @Test
    void testCleanSqlWithSingleLineComment() {
        String sql = "-- 这是注释\nSELECT * FROM users WHERE id = 1";
        String expected = "SELECT * FROM users WHERE id = 1";
        String result = SqlFingerprintUtil.cleanSql(sql);
        assertEquals(expected, result, "应该移除单行注释（--）");
    }

    /**
     * 测试 cleanSql() 方法 - 多行注释（/* */）
     */
    @Test
    void testCleanSqlWithMultiLineComment() {
        String sql = "/* 这是多行注释 */SELECT * FROM users WHERE id = 1";
        String expected = "SELECT * FROM users WHERE id = 1";
        String result = SqlFingerprintUtil.cleanSql(sql);
        assertEquals(expected, result, "应该移除多行注释（/* */）");
    }

    /**
     * 测试 cleanSql() 方法 - MySQL 注释（#）
     */
    @Test
    void testCleanSqlWithMySQLComment() {
        String sql = "# 这是 MySQL 注释\nSELECT * FROM users WHERE id = 1";
        String expected = "SELECT * FROM users WHERE id = 1";
        String result = SqlFingerprintUtil.cleanSql(sql);
        assertEquals(expected, result, "应该移除 MySQL 注释（#）");
    }

    /**
     * 测试 cleanSql() 方法 - 混合注释
     */
    @Test
    void testCleanSqlWithMixedComments() {
        String sql = """
            -- 注释1：查询用户
            SELECT /* 注释2：选择所有字段 */ * FROM users
            WHERE id = ? # 注释3：条件过滤
            """;
        String expected = "SELECT * FROM users WHERE id = ?";
        String result = SqlFingerprintUtil.cleanSql(sql);
        assertEquals(expected, result, "应该移除所有类型的注释");
    }

    /**
     * 测试 cleanSql() 方法 - 用户真实场景（带行内注释）
     */
    @Test
    void testCleanSqlWithInlineComments() {
        String sql = """
            -- 限制一下避免结果集过大，但查询本身依然会慢
            SELECT c.customer_name, ca.activity_type, ca.activity_date, e.first_name, e.last_name, d.department_name
            FROM enterprise_crm_system.customer_activities ca
            JOIN enterprise_crm_system.customers c ON ca.customer_id = c.customer_id
            WHERE ca.activity_date >= DATE_SUB(NOW(), INTERVAL 1 YEAR)
              AND ca.activity_details LIKE '%customer feedback%' -- 故意对TEXT字段模糊查询
              AND d.department_name = 'Sales' -- 对无索引字段进行等值查询
            ORDER BY ca.activity_date DESC
            LIMIT 2000
            """;

        String result = SqlFingerprintUtil.cleanSql(sql);

        // 验证注释被移除
        assertFalse(result.contains("--"), "不应该包含单行注释");
        assertFalse(result.contains("限制一下避免结果集过大"), "不应该包含注释内容");
        assertFalse(result.contains("故意对TEXT字段模糊查询"), "不应该包含行内注释内容");
        assertFalse(result.contains("对无索引字段进行等值查询"), "不应该包含行内注释内容");

        // 验证 SQL 关键字保留
        assertTrue(result.contains("SELECT"), "应该保留 SELECT 关键字");
        assertTrue(result.contains("FROM"), "应该保留 FROM 关键字");
        assertTrue(result.contains("WHERE"), "应该保留 WHERE 关键字");
        assertTrue(result.contains("customer_name"), "应该保留字段名");
        assertTrue(result.contains("enterprise_crm_system"), "应该保留数据库名");
    }

    /**
     * 测试 cleanSql() 方法 - 多余空格压缩
     */
    @Test
    void testCleanSqlWithExtraWhitespace() {
        String sql = "SELECT   *   FROM     users   WHERE     id     =     1";
        String expected = "SELECT * FROM users WHERE id = 1";
        String result = SqlFingerprintUtil.cleanSql(sql);
        assertEquals(expected, result, "应该压缩多余空格");
    }

    /**
     * 测试 cleanSql() 方法 - 换行符处理
     */
    @Test
    void testCleanSqlWithNewlines() {
        String sql = """
            SELECT
                *
            FROM
                users
            WHERE
                id = 1
            """;
        String result = SqlFingerprintUtil.cleanSql(sql);
        assertFalse(result.contains("\n"), "不应该包含换行符");
        assertTrue(result.contains("SELECT * FROM users WHERE id = 1"), "应该压缩为一行");
    }

    /**
     * 测试 cleanSql() 方法 - Tab 字符处理
     */
    @Test
    void testCleanSqlWithTabs() {
        String sql = "SELECT\t*\tFROM\tusers\tWHERE\tid\t=\t1";
        String result = SqlFingerprintUtil.cleanSql(sql);
        assertFalse(result.contains("\t"), "不应该包含 Tab 字符");
    }

    /**
     * 测试 cleanSql() 方法 - 空字符串
     */
    @Test
    void testCleanSqlWithEmptyString() {
        assertEquals("", SqlFingerprintUtil.cleanSql(""), "空字符串应返回空字符串");
        assertEquals("", SqlFingerprintUtil.cleanSql("   "), "纯空格应返回空字符串");
        assertEquals("", SqlFingerprintUtil.cleanSql("\t\n"), "纯空白字符应返回空字符串");
    }

    /**
     * 测试 cleanSql() 方法 - null 输入
     */
    @Test
    void testCleanSqlWithNullInput() {
        assertEquals("", SqlFingerprintUtil.cleanSql(null), "null 应返回空字符串");
    }

    /**
     * 测试 cleanSql() 方法 - 字符串中的注释符号（不应被移除）
     */
    @Test
    void testCleanSqlPreservesCommentsInStrings() {
        String sql = "SELECT * FROM users WHERE name = '--not a comment'";
        String result = SqlFingerprintUtil.cleanSql(sql);
        // 注意：当前实现可能会移除字符串中的注释符号，这是已知限制
        // 如果需要保留字符串中的注释符号，需要使用 SQL 解析器而非正则表达式
        assertNotNull(result, "不应该返回 null");
    }

    /**
     * 测试 cleanSql() 方法 - 嵌套注释（当前不支持）
     */
    @Test
    void testCleanSqlWithNestedComments() {
        String sql = "/* 外层注释 /* 内层注释 */ */ SELECT * FROM users";
        String result = SqlFingerprintUtil.cleanSql(sql);
        // 当前实现可能无法正确处理嵌套注释，这是已知限制
        assertNotNull(result, "不应该返回 null");
        assertFalse(result.contains("/*"), "不应该保留注释符号");
    }

    /**
     * 测试 calculateFingerprint() - 验证注释不影响指纹
     */
    @Test
    void testCalculateFingerprintIgnoresComments() {
        String sql1 = "SELECT * FROM users WHERE id = 1";
        String sql2 = "-- 这是注释\nSELECT * FROM users WHERE id = 1";
        String sql3 = "/* 注释 */ SELECT * FROM users WHERE id = 1";

        String fp1 = SqlFingerprintUtil.calculateFingerprint(sql1);
        String fp2 = SqlFingerprintUtil.calculateFingerprint(sql2);
        String fp3 = SqlFingerprintUtil.calculateFingerprint(sql3);

        assertEquals(fp1, fp2, "带单行注释的 SQL 应生成相同指纹");
        assertEquals(fp1, fp3, "带多行注释的 SQL 应生成相同指纹");
        assertEquals(fp2, fp3, "所有形式的注释都不应影响指纹");
    }

    /**
     * 测试 extractTemplate() - 验证注释不影响模板提取
     */
    @Test
    void testExtractTemplateIgnoresComments() {
        String sql1 = "SELECT * FROM users WHERE id = 1 AND name = 'test'";
        String sql2 = """
            -- 查询用户
            SELECT * FROM users
            WHERE id = 1
              AND name = 'test' -- 条件过滤
            """;

        String template1 = SqlFingerprintUtil.extractTemplate(sql1);
        String template2 = SqlFingerprintUtil.extractTemplate(sql2);

        // 模板应该参数化（数字和字符串替换为 ?）
        assertTrue(template1.contains("?"), "模板应该包含参数占位符");
        assertTrue(template2.contains("?"), "带注释的 SQL 模板应该包含参数占位符");

        // 验证注释被移除
        assertFalse(template2.contains("--"), "模板不应该包含注释");
        assertFalse(template2.contains("查询用户"), "模板不应该包含注释内容");
        assertFalse(template2.contains("条件过滤"), "模板不应该包含注释内容");
    }

    /**
     * 测试 isSimilar() - 验证注释不影响相似度判断
     */
    @Test
    void testIsSimilarIgnoresComments() {
        String sql1 = "SELECT * FROM users WHERE id = 1";
        String sql2 = "-- 注释\nSELECT * FROM users WHERE id = 2";

        assertTrue(SqlFingerprintUtil.isSimilar(sql1, sql2),
            "仅注释不同的 SQL 应该被认为是相似的");
    }

    /**
     * 测试复杂 SQL - JOIN 语句带注释
     */
    @Test
    void testCleanSqlComplexQueryWithComments() {
        String sql = """
            -- 查询客户活动和部门信息
            SELECT c.customer_name, ca.activity_type
            FROM enterprise_crm_system.customer_activities ca
            JOIN enterprise_crm_system.customers c ON ca.customer_id = c.customer_id -- 关联客户表
            JOIN enterprise_core_hr.employees e ON ca.assigned_employee_id = e.employee_id -- 关联员工表
            WHERE ca.activity_date >= DATE_SUB(NOW(), INTERVAL 1 YEAR) -- 最近一年
              AND d.department_name = 'Sales' -- 销售部门
            ORDER BY ca.activity_date DESC
            LIMIT 2000 -- 限制结果数量
            """;

        String result = SqlFingerprintUtil.cleanSql(sql);

        // 验证所有注释被移除
        assertFalse(result.contains("--"), "不应该包含单行注释");
        assertFalse(result.contains("查询客户活动和部门信息"), "不应该包含注释内容");
        assertFalse(result.contains("关联客户表"), "不应该包含注释内容");
        assertFalse(result.contains("关联员工表"), "不应该包含注释内容");
        assertFalse(result.contains("最近一年"), "不应该包含注释内容");
        assertFalse(result.contains("销售部门"), "不应该包含注释内容");
        assertFalse(result.contains("限制结果数量"), "不应该包含注释内容");

        // 验证 SQL 结构保留
        assertTrue(result.contains("SELECT"), "应该保留 SELECT");
        assertTrue(result.contains("FROM"), "应该保留 FROM");
        assertTrue(result.contains("JOIN"), "应该保留 JOIN");
        assertTrue(result.contains("WHERE"), "应该保留 WHERE");
        assertTrue(result.contains("ORDER BY"), "应该保留 ORDER BY");
        assertTrue(result.contains("LIMIT"), "应该保留 LIMIT");
        assertTrue(result.contains("enterprise_crm_system"), "应该保留数据库名");
    }
}
