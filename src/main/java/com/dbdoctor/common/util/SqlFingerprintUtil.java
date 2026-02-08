package com.dbdoctor.common.util;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.util.JdbcConstants;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SQL 指纹工具类
 * 用于计算 SQL 指纹（MD5）和提取 SQL 模板
 *
 * 核心功能：
 * 1. 计算 SQL 指纹（MD5 哈希值）
 * 2. 提取 SQL 模板（参数化后的 SQL）
 *
 * 标准化处理（V2.1.0）：
 * - 大小写统一：SELECT / select → SELECT
 * - 移除反引号：`table` → table
 * - 移除注释：-- comment → 删除
 * - 压缩空格：多个空格 → 一个空格
 * - 移除换行：\n → 空格
 *
 * @author DB-Doctor
 * @version 2.1.0
 */
@Slf4j
public class SqlFingerprintUtil {

    /**
     * 计算 SQL 指纹
     *
     * 标准化步骤：
     * 1. 移除 SQL 注释（单行注释和多行注释）
     * 2. 压缩空白字符（多个空格→一个，换行→空格）
     * 3. 转大写 + 移除反引号
     * 4. Druid 参数化（把值替换为 ?）
     * 5. 计算 MD5
     *
     * @param rawSql 原始 SQL
     * @return MD5 哈希值（32位小写十六进制字符串）
     */
    public static String calculateFingerprint(String rawSql) {
        if (rawSql == null || rawSql.isBlank()) {
            return "";
        }

        try {
            // 1. 移除 SQL 注释
            String normalized = removeSqlComments(rawSql);

            // 2. 压缩空白字符
            normalized = normalizeWhitespace(normalized);

            // 3. 标准化：转大写 + 移除反引号
            normalized = normalized
                    .toUpperCase()
                    .replaceAll("`", "")
                    .trim();

            // 4. 解析 SQL 语句并格式化
            List<SQLStatement> statements = SQLUtils.parseStatements(normalized, JdbcConstants.MYSQL);

            String sqlTemplate;
            if (statements.isEmpty()) {
                // 降级：直接参数化
                sqlTemplate = parameterizeSql(normalized);
            } else {
                // 格式化后再参数化
                String formatted = SQLUtils.toSQLString(statements.get(0), JdbcConstants.MYSQL);
                sqlTemplate = parameterizeSql(formatted);
            }

            // 5. 二次标准化（可能会引入多余空格）
            sqlTemplate = normalizeWhitespace(sqlTemplate);

            // 6. 计算 MD5 作为指纹
            return calculateMD5(sqlTemplate);

        } catch (Exception e) {
            log.error("计算 SQL 指纹失败: {}", rawSql, e);
            // 降级：直接对原始 SQL 计算 MD5
            return calculateMD5(rawSql);
        }
    }

    /**
     * 提取 SQL 模板（参数化后的 SQL）
     *
     * @param rawSql 原始 SQL
     * @return 参数化后的 SQL 模板
     */
    public static String extractTemplate(String rawSql) {
        if (rawSql == null || rawSql.isBlank()) {
            return "";
        }

        try {
            // 先标准化
            String normalized = normalizeWhitespace(rawSql.trim());

            // 解析 SQL 语句
            List<SQLStatement> statements = SQLUtils.parseStatements(normalized, JdbcConstants.MYSQL);

            if (statements.isEmpty()) {
                return cleanSql(rawSql);
            }

            // 格式化 SQL（统一格式）
            String formatted = SQLUtils.toSQLString(statements.get(0), JdbcConstants.MYSQL);

            // 参数化：使用正则表达式替换常量值
            String parameterized = parameterizeSql(formatted);

            return parameterized.replaceAll("\\s+", " ").trim();

        } catch (Exception e) {
            log.error("提取 SQL 模板失败: {}", rawSql, e);
            // 降级：使用简单参数化
            return parameterizeSql(cleanSql(rawSql));
        }
    }

    /**
     * 参数化 SQL（使用正则表达式替换常量值）
     *
     * @param sql SQL 语句
     * @return 参数化后的 SQL
     */
    private static String parameterizeSql(String sql) {
        if (sql == null || sql.isBlank()) {
            return sql;
        }

        String result = sql;

        // 1. 替换单引号字符串
        result = result.replaceAll("'[^']*'", "?");

        // 2. 替换双引号字符串
        result = result.replaceAll("\"[^\"]*\"", "?");

        // 3. 替换数字（包括小数和负数）
        // 匹配：123, 123.45, -123, -123.45
        result = result.replaceAll("\\b-?\\d+(\\.\\d+)?\\b", "?");

        // 4. 替换 NULL 值（不区分大小写）
        result = Pattern.compile("\\bNULL\\b", Pattern.CASE_INSENSITIVE)
                .matcher(result)
                .replaceAll("?");

        // 5. 替换 TRUE/FALSE（不区分大小写）
        result = Pattern.compile("\\bTRUE\\b", Pattern.CASE_INSENSITIVE)
                .matcher(result)
                .replaceAll("?");
        result = Pattern.compile("\\bFALSE\\b", Pattern.CASE_INSENSITIVE)
                .matcher(result)
                .replaceAll("?");

        return result;
    }

    /**
     * 移除 SQL 注释
     *
     * @param sql 原始 SQL
     * @return 移除注释后的 SQL
     */
    private static String removeSqlComments(String sql) {
        if (sql == null || sql.isBlank()) {
            return sql;
        }

        String result = sql;

        try {
            // 移除单行注释（-- comment）
            result = result.replaceAll("--[^\\n]*", "");

            // 移除多行注释（/* comment */）
            result = result.replaceAll("/\\*.*?\\*/", "");

            // 移除 MySQL 注释（# comment）
            result = result.replaceAll("#[^\\n]*", "");

        } catch (Exception e) {
            log.debug("移除 SQL 注释失败: {}", sql, e);
        }

        return result;
    }

    /**
     * 标准化空白字符
     *
     * @param sql 原始 SQL
     * @return 标准化后的 SQL
     */
    private static String normalizeWhitespace(String sql) {
        if (sql == null || sql.isBlank()) {
            return sql;
        }

        return sql
                // 换行符 → 空格
                .replaceAll("\\r\\n|\\n|\\r", " ")
                // Tab → 空格
                .replaceAll("\\t", " ")
                // 多个空格 → 一个空格
                .replaceAll(" +", " ")
                // 括号前后的空格
                .replaceAll(" \\( ", "(")
                .replaceAll(" \\)", ")")
                // 逗号后的空格标准化
                .replaceAll(", ", ",")
                .trim();
    }

    /**
     * 清理 SQL（去除注释和多余空格）
     *
     * @param rawSql 原始 SQL
     * @return 清理后的 SQL
     */
    public static String cleanSql(String rawSql) {
        if (rawSql == null || rawSql.isBlank()) {
            return "";
        }

        // 去除前后空格
        String cleaned = rawSql.trim();

        // 移除 SQL 注释（单行注释 --、多行注释 /* */、MySQL 注释 #）
        cleaned = removeSqlComments(cleaned);

        // 去除多余空格（多个连续空格替换为一个）
        cleaned = cleaned.replaceAll("\\s+", " ");

        return cleaned;
    }

    /**
     * 判断两个 SQL 是否相似（基于指纹）
     *
     * @param sql1 SQL 1
     * @param sql2 SQL 2
     * @return true-相似，false-不相似
     */
    public static boolean isSimilar(String sql1, String sql2) {
        String fingerprint1 = calculateFingerprint(sql1);
        String fingerprint2 = calculateFingerprint(sql2);
        return fingerprint1.equals(fingerprint2);
    }

    /**
     * 计算 MD5 哈希值（使用 Java 标准库）
     *
     * @param input 输入字符串
     * @return MD5 哈希值（32位小写十六进制字符串）
     */
    private static String calculateMD5(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(input.getBytes(StandardCharsets.UTF_8));

            // 转换为十六进制字符串
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            log.error("MD5 算法不可用", e);
            return "";
        }
    }
}
