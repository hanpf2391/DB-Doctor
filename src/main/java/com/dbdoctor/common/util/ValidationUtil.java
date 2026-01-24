package com.dbdoctor.common.util;

import com.dbdoctor.common.constants.RegexConstants;

/**
 * 校验工具类
 *
 * @author DB-Doctor
 * @version 1.0.0
 */
public class ValidationUtil {

    /**
     * 校验 SQL 语句是否安全（防止注入）
     *
     * @param sql SQL 语句
     * @return true-安全，false-不安全
     */
    public static boolean isSqlSafe(String sql) {
        if (sql == null || sql.isEmpty()) {
            return false;
        }

        String upperSql = sql.toUpperCase().trim();

        // 检查是否包含危险关键词
        String[] dangerousKeywords = {
                "DROP", "TRUNCATE", "DELETE", "INSERT", "UPDATE",
                "ALTER", "CREATE", "GRANT", "REVOKE"
        };

        for (String keyword : dangerousKeywords) {
            if (upperSql.contains(keyword)) {
                return false;
            }
        }

        // 只允许 SELECT、SHOW、EXPLAIN、DESCRIBE
        return upperSql.startsWith("SELECT") ||
                upperSql.startsWith("SHOW") ||
                upperSql.startsWith("EXPLAIN") ||
                upperSql.startsWith("DESCRIBE");
    }

    /**
     * 校验 Email 格式
     *
     * @param email Email 地址
     * @return true-格式正确，false-格式错误
     */
    public static boolean isEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        return email.matches(RegexConstants.EMAIL_REGEX);
    }

    /**
     * 校验 URL 格式
     *
     * @param url URL 地址
     * @return true-格式正确，false-格式错误
     */
    public static boolean isUrl(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        return url.matches(RegexConstants.URL_REGEX);
    }

    /**
     * 校验文件路径是否合法
     *
     * @param path 文件路径
     * @return true-合法，false-非法
     */
    public static boolean isValidPath(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }

        // 检查路径遍历攻击
        if (path.contains("../") || path.contains("..\\")) {
            return false;
        }

        return true;
    }

    /**
     * 校验端口号是否合法
     *
     * @param port 端口号
     * @return true-合法，false-非法
     */
    public static boolean isValidPort(Integer port) {
        return port != null && port >= 1 && port <= 65535;
    }

    /**
     * 校验配置值是否为空
     *
     * @param value 配置值
     * @param name  配置名称
     * @throws IllegalArgumentException 如果配置值为空
     */
    public static void requireNonEmpty(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " 不能为空");
        }
    }

    /**
     * 校验配置值是否在指定范围内
     *
     * @param value 配置值
     * @param min   最小值
     * @param max   最大值
     * @param name  配置名称
     * @throws IllegalArgumentException 如果配置值不在范围内
     */
    public static void requireInRange(Integer value, int min, int max, String name) {
        if (value == null || value < min || value > max) {
            throw new IllegalArgumentException(name + " 必须在 " + min + " 到 " + max + " 之间");
        }
    }
}
