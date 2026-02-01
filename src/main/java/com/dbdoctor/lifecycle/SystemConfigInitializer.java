package com.dbdoctor.lifecycle;

import com.dbdoctor.entity.SystemConfig;
import com.dbdoctor.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 系统配置初始化器
 *
 * <p>应用启动时检查并初始化系统配置表中的默认配置项</p>
 *
 * @author DB-Doctor
 * @version 3.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SystemConfigInitializer implements ApplicationRunner {

    private final SystemConfigRepository configRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🔧 系统配置初始化检查开始...");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        try {
            // 1. 数据库配置分组
            initDatabaseConfigs();

            // 2. AI 配置分组
            initAiConfigs();

            // 3. 通知配置分组
            initNotificationConfigs();

            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.info("✅ 系统配置初始化完成");
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        } catch (Exception e) {
            log.error("❌ 系统配置初始化失败", e);
            // 不抛出异常，允许应用继续启动
        }
    }

    /**
     * 初始化数据库配置
     */
    private void initDatabaseConfigs() {
        log.info("📝 初始化数据库配置分组...");

        // 目标数据库连接配置
        createConfigIfNotExists(
            "database.url", "database", "string",
            null, "jdbc:mysql://localhost:3306/information_schema?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true",
            "目标数据库地址", "MySQL 数据库连接 URL（需要连接到 information_schema）",
            true, false, 1, "text", "jdbc:mysql://localhost:3306/information_schema"
        );

        createConfigIfNotExists(
            "database.username", "database", "string",
            null, "db_doctor",
            "数据库用户名", "连接数据库的用户名",
            true, false, 2, "text", "root"
        );

        createConfigIfNotExists(
            "database.password", "database", "password",
            null, "",
            "数据库密码", "连接数据库的密码",
            true, true, 3, "password", "请输入密码"
        );

        createConfigIfNotExists(
            "database.monitored_dbs", "database", "json",
            null, "[]",
            "监听的数据库", "需要监听慢查询的数据库名称列表",
            true, false, 4, "textarea", "[\"db1\", \"db2\", \"db3\"]"
        );

        log.info("✅ 数据库配置初始化完成");
    }

    /**
     * 初始化 AI 配置
     */
    private void initAiConfigs() {
        log.info("📝 初始化 AI 配置分组...");

        createConfigIfNotExists(
            "ai.enabled", "ai", "boolean",
            "false", "false",
            "启用 AI 分析", "是否启用 AI 智能分析功能",
            false, false, 1, "boolean", null
        );

        createConfigIfNotExists(
            "ai.provider", "ai", "string",
            "ollama", "ollama",
            "AI 服务提供商", "AI 服务提供商（openai/ollama）",
            false, false, 2, "select", "ollama"
        );

        createConfigIfNotExists(
            "ai.api_key", "ai", "password",
            null, "",
            "API Key", "OpenAI/Ollama API 密钥",
            true, true, 3, "password", "sk-..."
        );

        createConfigIfNotExists(
            "ai.base_url", "ai", "string",
            "http://localhost:11434", "http://localhost:11434",
            "API Base URL", "AI 服务的基础 URL",
            true, false, 4, "text", "http://localhost:11434"
        );

        createConfigIfNotExists(
            "ai.timeout_seconds", "ai", "number",
            "60", "60",
            "API 超时时间（秒）", "AI API 调用超时时间",
            false, false, 5, "number", "60"
        );

        // 主治医生
        createConfigIfNotExists(
            "ai.diagnosis.model_name", "ai", "string",
            "qwen2.5:7b", "qwen2.5:7b",
            "主治医生模型", "用于慢查询诊断的 AI 模型",
            true, false, 10, "text", "qwen2.5:7b"
        );

        createConfigIfNotExists(
            "ai.diagnosis.temperature", "ai", "number",
            "0.1", "0.1",
            "主治医生温度参数", "控制生成内容的随机性（0-1）",
            false, false, 11, "number", "0.1"
        );

        // 推理专家
        createConfigIfNotExists(
            "ai.reasoning.model_name", "ai", "string",
            "deepseek-r1:7b", "deepseek-r1:7b",
            "推理专家模型", "用于深度推理分析的 AI 模型",
            true, false, 12, "text", "deepseek-r1:7b"
        );

        createConfigIfNotExists(
            "ai.reasoning.temperature", "ai", "number",
            "0.3", "0.3",
            "推理专家温度参数", "控制生成内容的随机性（0-1）",
            false, false, 13, "number", "0.3"
        );

        // 编码专家
        createConfigIfNotExists(
            "ai.coding.model_name", "ai", "string",
            "deepseek-coder:6.7b", "deepseek-coder:6.7b",
            "编码专家模型", "用于生成 SQL 优化建议的 AI 模型",
            true, false, 14, "text", "deepseek-coder:6.7b"
        );

        createConfigIfNotExists(
            "ai.coding.temperature", "ai", "number",
            "0.2", "0.2",
            "编码专家温度参数", "控制生成内容的随机性（0-1）",
            false, false, 15, "number", "0.2"
        );

        log.info("✅ AI 配置初始化完成");
    }

    /**
     * 初始化通知配置
     */
    private void initNotificationConfigs() {
        log.info("📝 初始化通知配置分组...");

        // 邮件通知
        createConfigIfNotExists(
            "notification.email.enabled", "notification", "boolean",
            "false", "false",
            "启用邮件通知", "是否启用邮件通知",
            false, false, 1, "boolean", null
        );

        createConfigIfNotExists(
            "notification.email.smtp_host", "notification", "string",
            "smtp.qq.com", "smtp.qq.com",
            "SMTP 服务器", "邮件发送服务器地址",
            false, false, 2, "text", "smtp.qq.com"
        );

        createConfigIfNotExists(
            "notification.email.smtp_port", "notification", "number",
            "587", "587",
            "SMTP 端口", "邮件发送服务器端口",
            false, false, 3, "number", "587"
        );

        createConfigIfNotExists(
            "notification.email.from", "notification", "string",
            "DB-Doctor <noreply@example.com>", "DB-Doctor <noreply@example.com>",
            "发件人", "发件箱邮箱地址",
            false, false, 4, "text", "your-email@example.com"
        );

        createConfigIfNotExists(
            "notification.email.username", "notification", "string",
            null, "",
            "邮件用户名", "发件箱邮箱地址",
            false, false, 5, "text", "your-email@example.com"
        );

        createConfigIfNotExists(
            "notification.email.password", "notification", "password",
            null, "",
            "邮件密码", "发件箱邮箱密码或授权码",
            false, true, 6, "password", ""
        );

        createConfigIfNotExists(
            "notification.email.to_list", "notification", "json",
            "[]", "[]",
            "收件人列表", "接收通知的邮箱地址列表",
            false, false, 7, "textarea", "[\"admin@example.com\"]"
        );

        log.info("✅ 通知配置初始化完成");
    }

    /**
     * 如果配置不存在则创建
     */
    private void createConfigIfNotExists(
        String configKey, String configGroup, String configType,
        String configValue, String defaultValue,
        String configName, String configDescription,
        Boolean isRequired, Boolean isSensitive,
        Integer displayOrder, String inputType, String uiPlaceholder
    ) {
        if (configRepository.findByConfigKey(configKey).isEmpty()) {
            SystemConfig config = new SystemConfig();
            config.setConfigKey(configKey);
            config.setConfigGroup(configGroup);
            config.setConfigType(configType);
            // 如果 configValue 为 null，则使用 defaultValue（确保配置有值）
            config.setConfigValue(configValue != null ? configValue : defaultValue);
            config.setDefaultValue(defaultValue);
            config.setConfigName(configName);
            config.setConfigDescription(configDescription);
            config.setIsRequired(isRequired);
            config.setIsSensitive(isSensitive);
            config.setIsEnabled(true);
            config.setDisplayOrder(displayOrder);
            config.setInputType(inputType);
            config.setUiPlaceholder(uiPlaceholder);
            config.setCreatedTime(LocalDateTime.now());
            config.setUpdatedTime(LocalDateTime.now());

            configRepository.save(config);
            log.info("✅ 创建配置: {} - {}", configKey, configName);
        } else {
            // 配置已存在，使用 TRACE 级别（比 DEBUG 更低，默认不显示）
            log.trace("配置已存在: {} - {}", configKey, configName);
        }
    }
}
