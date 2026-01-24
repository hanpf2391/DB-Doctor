package com.dbdoctor.common.util;

import lombok.experimental.UtilityClass;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * 日期时间工具类
 *
 * @author DB-Doctor
 * @version 1.0.0
 */
@UtilityClass
public class DateUtil {

    /**
     * 默认日期时间格式
     */
    private static final String DEFAULT_DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    /**
     * ISO 日期时间格式
     */
    private static final String ISO_DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    /**
     * 格式化日期时间
     *
     * @param date 日期
     * @return 格式化后的字符串
     */
    public String format(Date date) {
        return format(date, DEFAULT_DATETIME_FORMAT);
    }

    /**
     * 格式化日期时间
     *
     * @param date    日期
     * @param pattern 格式
     * @return 格式化后的字符串
     */
    public String format(Date date, String pattern) {
        if (date == null) {
            return "";
        }
        LocalDateTime localDateTime = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        return localDateTime.format(formatter);
    }

    /**
     * 解析日期时间字符串
     *
     * @param dateStr 日期字符串
     * @return 日期
     */
    public Date parse(String dateStr) {
        return parse(dateStr, DEFAULT_DATETIME_FORMAT);
    }

    /**
     * 解析日期时间字符串
     *
     * @param dateStr 日期字符串
     * @param pattern 格式
     * @return 日期
     */
    public Date parse(String dateStr, String pattern) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        LocalDateTime localDateTime = LocalDateTime.parse(dateStr, formatter);
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    /**
     * 获取当前时间戳
     *
     * @return 时间戳（毫秒）
     */
    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    /**
     * 获取当前日期时间
     *
     * @return 当前日期时间
     */
    public Date now() {
        return new Date();
    }

    /**
     * 计算时间差（秒）
     *
     * @param start 开始时间
     * @param end   结束时间
     * @return 时间差（秒）
     */
    public long diffSeconds(Date start, Date end) {
        if (start == null || end == null) {
            return 0;
        }
        return (end.getTime() - start.getTime()) / 1000;
    }

    /**
     * 判断日期是否在指定天数内
     *
     * @param date 日期
     * @param days 天数
     * @return true-在指定天数内，false-不在
     */
    public boolean isWithinDays(Date date, int days) {
        if (date == null) {
            return false;
        }
        long diffMillis = System.currentTimeMillis() - date.getTime();
        long daysMillis = days * 24L * 60 * 60 * 1000;
        return diffMillis <= daysMillis;
    }
}
