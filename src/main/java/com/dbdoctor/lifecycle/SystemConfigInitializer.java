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

        // 数据库实例ID（基础设置用）
        createConfigIfNotExists(
            "database.instance_id", "database", "number",
            null, null,
            "数据库实例ID", "当前使用的数据库实例ID",
            false, false, 5, "number", "数据库实例ID"
        );

        // 数据库实例名称（基础设置用）
        createConfigIfNotExists(
            "database.instance_name", "database", "string",
            null, null,
            "数据库实例名称", "当前使用的数据库实例名称",
            false, false, 6, "text", "数据库实例名称"
        );

        log.info("✅ 数据库配置初始化完成");
    }

    /**
     * 初始化 AI 配置
     *
     * 配置策略：
     * - 使用 ai_service_instance 表存储实例配置
     * - system_config 只存储实例ID引用
     * - 旧配置方式（直接存储 provider、base_url 等）已废弃
     */
    private void initAiConfigs() {
        log.info("📝 初始化 AI 配置分组...");

        // AI 启用开关
        createConfigIfNotExists(
            "ai.enabled", "ai", "boolean",
            "false", "false",
            "启用 AI 分析", "是否启用 AI 智能分析功能",
            false, false, 1, "boolean", null
        );

        // AI 服务实例配置（新方式）
        createConfigIfNotExists(
            "ai.diagnosis.instance_id", "ai", "number",
            null, "",
            "主治医生实例ID", "主治医生AI Agent使用的服务实例ID",
            false, false, 20, "number", "AI服务实例ID"
        );

        createConfigIfNotExists(
            "ai.diagnosis.instance_name", "ai", "string",
            null, "",
            "主治医生实例名称", "主治医生AI Agent使用的服务实例名称",
            false, false, 21, "text", "AI服务实例名称"
        );

        createConfigIfNotExists(
            "ai.reasoning.instance_id", "ai", "number",
            null, "",
            "推理专家实例ID", "推理专家AI Agent使用的服务实例ID",
            false, false, 22, "number", "AI服务实例ID"
        );

        createConfigIfNotExists(
            "ai.reasoning.instance_name", "ai", "string",
            null, "",
            "推理专家实例名称", "推理专家AI Agent使用的服务实例名称",
            false, false, 23, "text", "AI服务实例名称"
        );

        createConfigIfNotExists(
            "ai.coding.instance_id", "ai", "number",
            null, "",
            "编码专家实例ID", "编码专家AI Agent使用的服务实例ID",
            false, false, 24, "number", "AI服务实例ID"
        );

        createConfigIfNotExists(
            "ai.coding.instance_name", "ai", "string",
            null, "",
            "编码专家实例名称", "编码专家AI Agent使用的服务实例名称",
            false, false, 25, "text", "AI服务实例名称"
        );

        // ============ 基础设置使用的实例配置 ============

        // 主治医生 - 实例ID
        createConfigIfNotExists(
            "ai.diagnosis.instance_id", "ai", "number",
            null, null,
            "主治医生实例ID", "主治医生AI Agent使用的服务实例ID",
            false, false, 20, "number", "AI服务实例ID"
        );

        // 主治医生 - 实例名称
        createConfigIfNotExists(
            "ai.diagnosis.instance_name", "ai", "string",
            null, null,
            "主治医生实例名称", "主治医生AI Agent使用的服务实例名称",
            false, false, 21, "text", "AI服务实例名称"
        );

        // 推理专家 - 实例ID
        createConfigIfNotExists(
            "ai.reasoning.instance_id", "ai", "number",
            null, null,
            "推理专家实例ID", "推理专家AI Agent使用的服务实例ID",
            false, false, 22, "number", "AI服务实例ID"
        );

        // 推理专家 - 实例名称
        createConfigIfNotExists(
            "ai.reasoning.instance_name", "ai", "string",
            null, null,
            "推理专家实例名称", "推理专家AI Agent使用的服务实例名称",
            false, false, 23, "text", "AI服务实例名称"
        );

        // 编码专家 - 实例ID
        createConfigIfNotExists(
            "ai.coding.instance_id", "ai", "number",
            null, null,
            "编码专家实例ID", "编码专家AI Agent使用的服务实例ID",
            false, false, 24, "number", "AI服务实例ID"
        );

        // 编码专家 - 实例名称
        createConfigIfNotExists(
            "ai.coding.instance_name", "ai", "string",
            null, null,
            "编码专家实例名称", "编码专家AI Agent使用的服务实例名称",
            false, false, 25, "text", "AI服务实例名称"
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
