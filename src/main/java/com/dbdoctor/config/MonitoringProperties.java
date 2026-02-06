package com.dbdoctor.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 监控配置属性
 *
 * <p>从 application.yml 读取监控相关配置</p>
 *
 * @author DB-Doctor
 * @version 3.2.0
 * @since 3.2.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "db-doctor.monitoring")
public class MonitoringProperties {

    /**
     * 健康检查配置
     */
    private HealthCheck healthCheck = new HealthCheck();

    /**
     * 指标采集配置
     */
    private MetricsCollection metricsCollection = new MetricsCollection();

    /**
     * 告警配置
     */
    private Alert alert = new Alert();

    /**
     * 健康检查配置
     */
    @Data
    public static class HealthCheck {
        /**
         * 检查间隔（秒）
         */
        private int intervalSeconds = 60;

        /**
         * 是否启用健康检查
         */
        private boolean enabled = true;
    }

    /**
     * 指标采集配置
     */
    @Data
    public static class MetricsCollection {
        /**
         * 慢查询指标采集
         */
        private SlowQuery slowQuery = new SlowQuery();

        /**
         * AI 分析指标采集
         */
        private AiAnalysis aiAnalysis = new AiAnalysis();

        @Data
        public static class SlowQuery {
            /**
             * 采集间隔（秒）
             */
            private int intervalSeconds = 30;

            /**
             * 是否启用
             */
            private boolean enabled = true;
        }

        @Data
        public static class AiAnalysis {
            /**
             * 采集间隔（秒）
             */
            private int intervalSeconds = 60;

            /**
             * 是否启用
             */
            private boolean enabled = true;
        }
    }

    /**
     * 告警配置
     */
    @Data
    public static class Alert {
        /**
         * 告警规则配置
         */
        private Map<String, AlertRuleConfig> rules;

        /**
         * 冷却期配置
         */
        private CoolDown coolDown = new CoolDown();

        @Data
        public static class AlertRuleConfig {
            /**
             * 是否启用
             */
            private boolean enabled = true;

            /**
             * 阈值
             */
            private double threshold;

            /**
             * 严重程度
             */
            private String severity;
        }

        @Data
        public static class CoolDown {
            /**
             * 默认冷却期（分钟）
             */
            private int defaultMinutes = 30;

            /**
             * 每个规则的冷却期
             */
            private Map<String, Integer> ruleCoolDownMinutes;
        }
    }
}
