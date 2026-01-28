package com.dbdoctor.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 慢查询监控配置属性
 *
 * @author DB-Doctor
 * @version 2.1.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "db-doctor.slow-log-monitor")
public class SlowLogMonitorProperties {

    /**
     * 每次查询的最大记录数
     * 验证范围：1-1000
     */
    private Integer maxRecordsPerPoll = 100;

    /**
     * 自动清理配置
     */
    private AutoCleanup autoCleanup = new AutoCleanup();

    @Data
    public static class AutoCleanup {
        /**
         * 是否启用自动清理
         * 默认值：false（默认关闭，需手动启用）
         */
        private Boolean enabled = false;

        /**
         * 清理任务的 cron 表达式
         * 默认值：每天凌晨 3 点
         *
         * 示例：
         * - "0 0 3 * * ?"     每天凌晨 3 点
         * - "0 0 * / 6 * * ?"  每 6 小时执行一次
         * - "0 0 4 * * MON"   每周一凌晨 4 点
         */
        private String cronExpression = "0 0 3 * * ?";

        /**
         * 是否允许回退到 TRUNCATE 模式
         * 默认值：false（默认禁止，防止数据丢失）
         *
         * 说明：
         * - 当 slow_log 表是 CSV 引擎（MySQL 默认）时，无法使用安全清理
         * - 此配置控制是否允许使用 TRUNCATE TABLE 作为回退方案
         * - ⚠️ TRUNCATE 会清空整个表，包括未处理的数据，可能导致数据丢失
         * - ✅ 推荐：将 slow_log 表改为 InnoDB 引擎以支持安全清理
         *
         * 如何改为 InnoDB：
         * SET GLOBAL slow_query_log = 'OFF';
         * ALTER TABLE mysql.slow_log ENGINE = InnoDB;
         * SET GLOBAL slow_query_log = 'ON';
         */
        private Boolean allowTruncate = false;
    }
}
