package com.dbdoctor.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * DB-Doctor 项目配置属性
 * 统一管理项目相关的配置参数
 *
 * @author DB-Doctor
 * @version 1.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "db-doctor")
public class DbDoctorProperties {

    /**
     * 项目名称
     */
    private String name = "DB-Doctor";

    /**
     * 项目版本
     */
    private String version = "1.0.0";

    /**
     * 项目描述
     */
    private String description = "MySQL 慢查询智能诊疗系统";

    /**
     * AI 配置
     */
    private AiConfig ai = new AiConfig();

    /**
     * 通知配置
     */
    private NotifyConfig notify = new NotifyConfig();

    /**
     * 线程池配置
     */
    private ThreadPoolConfig threadPool = new ThreadPoolConfig();

    /**
     * AI 配置
     */
    @Data
    public static class AiConfig {
        /**
         * 是否启用 AI 分析
         */
        private Boolean enabled = true;

        /**
         * 最大并发分析数量
         */
        private Integer maxConcurrentAnalysis = 4;
    }

    /**
     * 通知配置
     */
    @Data
    public static class NotifyConfig {
        /**
         * 启用的通知渠道
         */
        private String enabledNotifiers = "email,webhook";

        /**
         * 通知间隔（秒）
         */
        private Integer notifyInterval = 3600;

        /**
         * 严重程度阈值
         */
        private Double severityThreshold = 3.0;

        /**
         * 邮件通知配置
         */
        private EmailConfig email = new EmailConfig();

        /**
         * Webhook 通知配置
         */
        private WebhookConfig webhook = new WebhookConfig();
    }

    /**
     * 邮件通知配置
     */
    @Data
    public static class EmailConfig {
        private Boolean enabled = true;
        private String from;
        private java.util.List<String> to;
        private java.util.List<String> cc;
    }

    /**
     * Webhook 通知配置
     */
    @Data
    public static class WebhookConfig {
        private DingTalkConfig dingtalk;
        private FeishuConfig feishu;
        private WecomConfig wecom;
    }

    @Data
    public static class DingTalkConfig {
        private Boolean enabled = false;
        private String webhookUrl;
        private String secret;
    }

    @Data
    public static class FeishuConfig {
        private Boolean enabled = false;
        private String webhookUrl;
    }

    @Data
    public static class WecomConfig {
        private Boolean enabled = false;
        private String webhookUrl;
    }

    /**
     * 线程池配置
     */
    @Data
    public static class ThreadPoolConfig {
        private ExecutorConfig aiAnalysis = new ExecutorConfig();
    }

    /**
     * 执行器配置
     */
    @Data
    public static class ExecutorConfig {
        private Integer coreSize = 4;
        private Integer maxSize = 8;
        private Integer queueCapacity = 100;
    }
}
