package com.dbdoctor.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * DB-Doctor 项目配置属性（带验证）
 * 统一管理项目相关的配置参数
 * 使用 JSR-303/380 Bean Validation 进行配置验证
 *
 * @author DB-Doctor
 * @version 1.0.0
 */
@Data
@Component
@Validated
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
    @Valid
    private AiConfig ai = new AiConfig();

    /**
     * 通知配置
     */
    @Valid
    private NotifyConfig notify = new NotifyConfig();

    /**
     * 线程池配置
     */
    @Valid
    private ThreadPoolConfig threadPool = new ThreadPoolConfig();

    /**
     * 重试配置
     */
    @Valid
    private RetryConfig retry = new RetryConfig();

    /**
     * 停机配置
     */
    @Valid
    private ShutdownConfig shutdown = new ShutdownConfig();

    /**
     * 环境检查配置
     */
    @Valid
    private EnvCheckConfig envCheck = new EnvCheckConfig();

    /**
     * 监控配置（v2.3.0）
     */
    @Valid
    private MonitoringConfig monitoring = new MonitoringConfig();

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
         * 范围：1-50
         */
        @Min(value = 1, message = "最大并发分析数量至少为 1")
        @Max(value = 50, message = "最大并发分析数量最多为 50")
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
         * 范围：60-86400（1分钟-1天）
         */
        @Min(value = 60, message = "通知间隔至少 60 秒")
        @Max(value = 86400, message = "通知间隔最多 86400 秒（1天）")
        private Integer notifyInterval = 3600;

        /**
         * 严重程度阈值
         * 范围：1.0-10.0
         */
        @DecimalMin(value = "1.0", message = "严重程度阈值至少 1.0")
        @DecimalMax(value = "10.0", message = "严重程度阈值最多 10.0")
        private Double severityThreshold = 3.0;

        /**
         * 冷却期时间（小时）- 防止频繁通知
         * 默认值：1 小时
         * 范围：1-168（1小时-1周）
         */
        @Min(value = 1, message = "冷却期至少 1 小时")
        @Max(value = 168, message = "冷却期最长 1 周（168 小时）")
        private Integer coolDownHours = 1;

        /**
         * 性能恶化倍率 - 触发二次唤醒通知
         * 当平均耗时恶化超过此倍率时，即使在冷却期内也会立即通知
         * 默认值：1.5（即耗时增加 50%）
         * 范围：1.1-10.0
         */
        @DecimalMin(value = "1.1", message = "性能恶化倍率至少 1.1")
        @DecimalMax(value = "10.0", message = "性能恶化倍率最多 10.0")
        private Double degradationMultiplier = 1.5;

        /**
         * 高频异常阈值 - 24小时内的出现次数
         * 当一天内出现次数超过此阈值时，立即通知
         * 默认值：100 次
         * 范围：1-10000
         */
        @Min(value = 1, message = "高频阈值至少 1")
        @Max(value = 10000, message = "高频阈值最多 10000")
        private Integer highFrequencyThreshold = 100;

        /**
         * 邮件通知配置
         */
        @Valid
        private EmailConfig email = new EmailConfig();

        /**
         * Webhook 通知配置
         */
        @Valid
        private WebhookConfig webhook = new WebhookConfig();
    }

    /**
     * 邮件通知配置
     */
    @Data
    public static class EmailConfig {
        private Boolean enabled = true;

        @Email(message = "发件人邮箱格式不正确")
        private String from;

        private java.util.List<@Email(message = "收件人邮箱格式不正确") String> to;
        private java.util.List<@Email(message = "抄送人邮箱格式不正确") String> cc;
    }

    /**
     * Webhook 通知配置
     */
    @Data
    public static class WebhookConfig {
        @Valid
        private DingTalkConfig dingtalk;
        @Valid
        private FeishuConfig feishu;
        @Valid
        private WecomConfig wecom;
    }

    @Data
    public static class DingTalkConfig {
        private Boolean enabled = false;

        @Pattern(regexp = "^https?://.*", message = "钉钉 Webhook URL 格式不正确")
        private String webhookUrl;

        private String secret;
    }

    @Data
    public static class FeishuConfig {
        private Boolean enabled = false;

        @Pattern(regexp = "^https?://.*", message = "飞书 Webhook URL 格式不正确")
        private String webhookUrl;
    }

    @Data
    public static class WecomConfig {
        private Boolean enabled = false;

        @Pattern(regexp = "^https?://.*", message = "企业微信 Webhook URL 格式不正确")
        private String webhookUrl;
    }

    /**
     * 线程池配置
     */
    @Data
    public static class ThreadPoolConfig {
        @Valid
        private ExecutorConfig aiAnalysis = new ExecutorConfig();
    }

    /**
     * 执行器配置
     */
    @Data
    public static class ExecutorConfig {
        /**
         * 核心线程数
         * 范围：1-64
         */
        @Min(value = 1, message = "核心线程数至少为 1")
        @Max(value = 64, message = "核心线程数最多为 64")
        private Integer coreSize = 4;

        /**
         * 最大线程数
         * 范围：1-256
         */
        @Min(value = 1, message = "最大线程数至少为 1")
        @Max(value = 256, message = "最大线程数最多为 256")
        private Integer maxSize = 16;

        /**
         * 队列容量
         * 范围：10-10000
         */
        @Min(value = 10, message = "队列容量至少为 10")
        @Max(value = 10000, message = "队列容量最多为 10000")
        private Integer queueCapacity = 200;
    }

    /**
     * 重试配置
     */
    @Data
    public static class RetryConfig {
        /**
         * 是否启用 PENDING 补扫
         */
        private Boolean enabled = true;

        /**
         * 最大重试次数
         * 范围：1-10
         */
        @Min(value = 1, message = "最大重试次数至少为 1")
        @Max(value = 10, message = "最大重试次数最多为 10")
        private Integer maxAttempts = 3;

        /**
         * 补扫间隔（毫秒），默认 10 分钟
         * 范围：1000-3600000（1秒-1小时）
         */
        @Min(value = 1000, message = "补扫间隔至少 1000 毫秒")
        @Max(value = 3600000, message = "补扫间隔最多 3600000 毫秒（1小时）")
        private Long pendingIntervalMs = 600000L;
    }

    /**
     * 停机配置
     */
    @Data
    public static class ShutdownConfig {
        /**
         * 等待任务完成的最长时间（秒）
         * 范围：1-300（1秒-5分钟）
         */
        @Min(value = 1, message = "等待时间至少 1 秒")
        @Max(value = 300, message = "等待时间最多 300 秒（5分钟）")
        private Integer awaitTerminationSeconds = 50;

        /**
         * 停机时是否清空队列
         */
        private Boolean clearQueueOnShutdown = true;
    }

    /**
     * 环境检查配置
     */
    @Data
    public static class EnvCheckConfig {
        /**
         * 是否启用环境检查（默认 false）
         */
        private Boolean enabled = false;

        /**
         * 检查失败时是否阻止应用启动（默认 false）
         * true: 检查失败抛出异常，应用启动终止
         * false: 检查失败只记录警告日志，应用继续启动
         */
        private Boolean failOnError = false;

        /**
         * 是否尝试自动修复问题（默认 false）
         * true: 尝试执行 SET GLOBAL 修复配置（需要 SUPER 权限）
         * false: 只检查，不修复
         */
        private Boolean autoFix = false;
    }

    /**
     * 监控配置（v2.3.0）
     */
    @Data
    public static class MonitoringConfig {
        /**
         * 是否启用 AI 监控
         */
        private Boolean enabled = true;

        /**
         * 是否存储提示词和响应（调试用，会占用大量存储空间）
         */
        private Boolean savePromptResponse = false;

        /**
         * 数据保留天数（默认 90 天）
         * 范围：1-365
         */
        @Min(value = 1, message = "数据保留天数至少为 1 天")
        @Max(value = 365, message = "数据保留天数最多 365 天")
        private Integer retentionDays = 90;

        /**
         * 自动清理配置
         */
        @Valid
        private AutoCleanupConfig autoCleanup = new AutoCleanupConfig();

        /**
         * 成本计算配置
         */
        @Valid
        private CostCalculationConfig costCalculation = new CostCalculationConfig();
    }

    /**
     * 自动清理配置
     */
    @Data
    public static class AutoCleanupConfig {
        /**
         * 是否启用自动清理（默认 false）
         */
        private Boolean enabled = false;

        /**
         * 清理任务的 cron 表达式
         * 默认值：每天凌晨 2 点
         */
        private String cronExpression = "0 0 2 * * ?";
    }

    /**
     * 成本计算配置
     */
    @Data
    public static class CostCalculationConfig {
        /**
         * 是否启用成本计算
         */
        private Boolean enabled = false;

        /**
         * 各模型的 Token 单价（元/1K tokens）
         */
        private java.util.Map<String, Double> prices = new java.util.HashMap<>();
    }
}
