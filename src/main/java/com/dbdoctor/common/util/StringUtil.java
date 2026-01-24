package com.dbdoctor.common.util;

import lombok.experimental.UtilityClass;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 字符串工具类
 *
 * @author DB-Doctor
 * @version 1.0.0
 */
@UtilityClass
public class StringUtil {

    /**
     * 判断字符串是否为空
     *
     * @param str 字符串
     * @return true-空，false-非空
     */
    public boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * 判断字符串是否不为空
     *
     * @param str 字符串
     * @return true-非空，false-空
     */
    public boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    /**
     * 截断字符串
     *
     * @param str        字符串
     * @param maxLength  最大长度
     * @return 截断后的字符串
     */
    public String truncate(String str, int maxLength) {
        if (isEmpty(str) || str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength) + "...";
    }

    /**
     * 隐藏敏感信息
     *
     * @param str      字符串
     * @param keepStart 保留开头字符数
     * @param keepEnd   保留结尾字符数
     * @return 隐藏后的字符串
     */
    public String maskSensitive(String str, int keepStart, int keepEnd) {
        if (isEmpty(str) || str.length() <= keepStart + keepEnd) {
            return str;
        }

        int maskLength = str.length() - keepStart - keepEnd;
        String masked = "*".repeat(Math.min(maskLength, 6));

        return str.substring(0, keepStart) + masked + str.substring(str.length() - keepEnd);
    }

    /**
     * 检查字符串是否匹配正则表达式
     *
     * @param str   字符串
     * @param regex 正则表达式
     * @return true-匹配，false-不匹配
     */
    public boolean matches(String str, String regex) {
        if (isEmpty(str) || isEmpty(regex)) {
            return false;
        }
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(str);
        return matcher.matches();
    }

    /**
     * 提取字符串中的数字
     *
     * @param str 字符串
     * @return 数字字符串
     */
    public String extractNumbers(String str) {
        if (isEmpty(str)) {
            return "";
        }
        return str.replaceAll("[^0-9]", "");
    }

    /**
     * 清理 SQL 语句（移除注释和多余空格）
     *
     * @param sql SQL 语句
     * @return 清理后的 SQL
     */
    public String cleanSql(String sql) {
        if (isEmpty(sql)) {
            return "";
        }

        // 移除单行注释
        String cleaned = sql.replaceAll("--.*", "");

        // 移除多行注释
        cleaned = cleaned.replaceAll("/\\*.*?\\*/", "");

        // 移除多余空格和换行
        cleaned = cleaned.replaceAll("\\s+", " ").trim();

        return cleaned;
    }
}
