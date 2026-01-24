package com.dbdoctor.common.util;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.util.JdbcConstants;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * SQL 指纹工具类
 * 用于计算 SQL 指纹（MD5）和提取 SQL 模板
 *
 * 核心功能：
 * 1. 计算 SQL 指纹（MD5 哈希值）
 * 2. 提取 SQL 模板（参数化后的 SQL）
 *
 * @author DB-Doctor
 * @version 2.0.0
 */
@Slf4j
public class SqlFingerprintUtil {

    /**
     * 计算 SQL 指纹
     *
     * 原理：
     * 1. 使用 Druid 格式化 SQL，将参数替换为问号
     *    例如："SELECT * FROM t WHERE id=1" → "SELECT * FROM t WHERE id=?"
     * 2. 对参数化后的 SQL 计算 MD5 哈希值
     *
     * @param rawSql 原始 SQL
     * @return MD5 哈希值（32位小写十六进制字符串）
     */
    public static String calculateFingerprint(String rawSql) {
        if (rawSql == null || rawSql.isBlank()) {
            return "";
        }

        try {
            // 1. Druid 格式化：把参数变成问号
            String sqlTemplate = SQLUtils.format(
                rawSql,
                JdbcConstants.MYSQL,
                SQLUtils.DEFAULT_FORMAT_OPTION
            );

            // 2. 计算 MD5 作为指纹
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
            // Druid 格式化：把参数变成问号
            return SQLUtils.format(
                rawSql,
                JdbcConstants.MYSQL,
                SQLUtils.DEFAULT_FORMAT_OPTION
            );

        } catch (Exception e) {
            log.error("提取 SQL 模板失败: {}", rawSql, e);
            // 降级：返回原始 SQL
            return rawSql;
        }
    }

    /**
     * 清理 SQL（去除多余空格、注释等）
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
