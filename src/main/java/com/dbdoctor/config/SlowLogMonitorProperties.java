package com.dbdoctor.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 慢查询监控配置属性
 *
 * @author DB-Doctor
 * @version 2.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "db-doctor.slow-log-monitor")
public class SlowLogMonitorProperties {

    /**
     * 轮询间隔（毫秒）
     */
    private Long pollIntervalMs = 60000L;

    /**
     * 每次查询的最大记录数
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
         */
        private Boolean enabled = true;

        /**
         * 清理任务的 cron 表达式
         */
        private String cronExpression = "0 0 3 * * ?";
    }
}
