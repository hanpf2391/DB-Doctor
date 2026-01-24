package com.dbdoctor.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 慢查询日志实体类
 * 对应 mysql.slow_log 表结构
 *
 * @author DB-Doctor
 * @version 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlowQueryLog {

    /**
     * 查询开始时间
     */
    private LocalDateTime startTime;

    /**
     * 用户和主机信息
     */
    private String userHost;

    /**
     * 数据库名称
     * 注意：这是 mysql.slow_log 表的 db 字段，直接提供，无需解析
     */
    private String dbName;

    /**
     * SQL 语句内容
     */
    private String sqlText;

    /**
     * 查询耗时（秒）
     */
    private Double queryTime;

    /**
     * 锁等待时间（秒）
     */
    private Double lockTime;

    /**
     * 返回行数
     */
    private Long rowsSent;

    /**
     * 扫描行数
     */
    private Long rowsExamined;

    /**
     * 判断是否为严重慢查询
     *
     * @param threshold 阈值（秒）
     * @return true-严重，false-正常
     */
    public boolean isSevere(double threshold) {
        return queryTime != null && queryTime > threshold;
    }

    /**
     * 判断是否为全表扫描
     * 简单判断：扫描行数 >> 返回行数
     *
     * @return true-可能是全表扫描，false-正常
     */
    public boolean isFullTableScan() {
        if (rowsExamined == null || rowsSent == null) {
            return false;
        }
        // 扫描行数超过返回行数的100倍，或者扫描行数超过10000
        return rowsExamined > rowsSent * 100 || rowsExamined > 10000;
    }

    /**
     * 判断是否锁等待严重
     *
     * @return true-锁等待严重，false-正常
     */
    public boolean isLockHeavy() {
        return lockTime != null && lockTime > 1.0;
    }
}
