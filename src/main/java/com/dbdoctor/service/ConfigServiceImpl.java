package com.dbdoctor.service;

import com.dbdoctor.config.AiConfig;
import com.dbdoctor.config.AiProperties;
import com.dbdoctor.entity.SystemConfig;
import com.dbdoctor.model.HotReloadResult;
import com.dbdoctor.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 系统配置服务实现
 *
 * @author DB-Doctor
 * @version 2.2.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigServiceImpl implements ConfigService {

    private final SystemConfigRepository configRepo;
    private final AiConfig aiConfig;

    private static final String MASK = "******";

    @Override
    public String getConfig(String key) {
        return configRepo.findByConfigKey(key)
                .map(SystemConfig::getConfigValue)
                .orElse(null);
    }

    @Override
    public String getConfig(String key, String defaultValue) {
        String value = getConfig(key);
        return value != null ? value : defaultValue;
    }

    @Override
    public Map<String, String> getAllConfigs() {
        List<SystemConfig> configs = configRepo.findAll();

        return configs.stream()
                .collect(Collectors.toMap(
                        SystemConfig::getConfigKey,
                        config -> config.getIsSensitive() ? MASK : config.getConfigValue()
                ));
    }

    @Override
    public Map<String, String> getConfigsByCategory(String category) {
        List<SystemConfig> configs = configRepo.findByConfigCategory(category);

        return configs.stream()
                .collect(Collectors.toMap(
                        SystemConfig::getConfigKey,
                        config -> config.getIsSensitive() ? MASK : config.getConfigValue()
                ));
    }

    @Override
    @Transactional
    public void saveConfig(String key, String value) {
        SystemConfig config = configRepo.findById(key)
                .orElse(new SystemConfig());

        config.setConfigKey(key);
        config.setConfigValue(value);
        config.setConfigCategory(extractCategory(key));

        configRepo.save(config);
        log.info("配置已保存: {} = {}", key, maskSensitiveValue(value));
    }

    @Override
    @Transactional
    public void saveConfigs(Map<String, String> configs) {
        configs.forEach(this::saveConfig);
    }

    @Override
    @Transactional
    public HotReloadResult saveAndRefresh(String category, Map<String, String> configs) {
        log.info("保存配置并触发热重载: category={}, configs={}", category, configs.keySet());

        // 1. 保存到数据库
        saveConfigs(configs);

        // 2. 触发热重载
        return triggerHotReload(category, configs);
    }

    @Override
    @Transactional
    public void deleteConfig(String key) {
        configRepo.deleteById(key);
        log.info("配置已删除: {}", key);
    }

    @Override
    public boolean exists(String key) {
        return configRepo.existsById(key);
    }

    @Override
    public String maskSensitiveValue(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        // API Key 格式: sk-xxx
        if (value.startsWith("sk-")) {
            return value.substring(0, Math.min(5, value.length())) + MASK;
        }

        // 密码: 全部隐藏
        return MASK;
    }

    // === 私有方法 ===

    /**
     * 从配置键提取分类
     */
    private String extractCategory(String key) {
        if (key.startsWith("target.db.") || key.startsWith("db.")) {
            return "DB";
        } else if (key.startsWith("ai.")) {
            return "AI";
        } else if (key.startsWith("notify.") || key.startsWith("monitor.")) {
            return "NOTIFY";
        } else {
            return "SYSTEM";
        }
    }

    /**
     * 触发热重载
     */
    private HotReloadResult triggerHotReload(String category, Map<String, String> configs) {
        List<String> refreshedBeans = new ArrayList<>();

        if ("AI".equals(category)) {
            return reloadAiConfig(configs);
        } else if ("DB".equals(category)) {
            return reloadDatabaseConfig(configs);
        } else if ("NOTIFY".equals(category)) {
            return reloadNotifyConfig(configs);
        }

        return HotReloadResult.success(refreshedBeans);
    }

    /**
     * 重新加载 AI 配置
     */
    private HotReloadResult reloadAiConfig(Map<String, String> configs) {
        log.info("开始重新加载 AI 配置...");

        try {
            // 1. 从数据库构建新的配置对象
            AiProperties newConfig = buildAiConfigFromDb();

            // 2. 调用 AiConfig 刷新 Bean
            aiConfig.refreshAiConfig(newConfig);

            List<String> refreshedBeans = Arrays.asList(
                    "diagnosisAgent",
                    "reasoningAgent",
                    "codingAgent"
            );

            log.info("AI 配置重新加载成功");
            return HotReloadResult.success(refreshedBeans);

        } catch (Exception e) {
            log.error("AI 配置重新加载失败", e);
            return HotReloadResult.needRestart("AI 配置加载失败: " + e.getMessage());
        }
    }

    /**
     * 从数据库构建 AI 配置对象
     */
    private AiProperties buildAiConfigFromDb() {
        AiProperties aiProps = new AiProperties();

        // 读取 AI 开关
        aiProps.setEnabled(Boolean.parseBoolean(getConfig("ai.enabled", "true")));

        // 读取主治医生配置
        aiProps.setDiagnosis(new AiProperties.AgentConfig(
                getConfig("ai.diagnosis.provider", "ollama"),
                getConfig("ai.diagnosis.base-url", "http://localhost:11434"),
                getConfig("ai.diagnosis.model-name", "qwen2.5:7b"),
                Double.parseDouble(getConfig("ai.diagnosis.temperature", "0.0")),
                getConfig("ai.diagnosis.api-key", "")
        ));

        // 读取推理专家配置
        aiProps.setReasoning(new AiProperties.AgentConfig(
                getConfig("ai.reasoning.provider", "ollama"),
                getConfig("ai.reasoning.base-url", "http://localhost:11434"),
                getConfig("ai.reasoning.model-name", "qwen2.5:7b"),
                Double.parseDouble(getConfig("ai.reasoning.temperature", "0.0")),
                getConfig("ai.reasoning.api-key", "")
        ));

        // 读取编码专家配置
        aiProps.setCoding(new AiProperties.AgentConfig(
                getConfig("ai.coding.provider", "ollama"),
                getConfig("ai.coding.base-url", "http://localhost:11434"),
                getConfig("ai.coding.model-name", "qwen2.5-coder:7b"),
                Double.parseDouble(getConfig("ai.coding.temperature", "0.0")),
                getConfig("ai.coding.api-key", "")
        ));

        return aiProps;
    }

    /**
     * 重新加载数据库配置（需要重建连接池）
     */
    private HotReloadResult reloadDatabaseConfig(Map<String, String> configs) {
        log.warn("检测到数据库配置变更，需要重启服务才能生效");

        // TODO: 在下一阶段实现连接池重建逻辑
        return HotReloadResult.needRestart("数据库配置已更改，请重启服务以应用新配置");
    }

    /**
     * 重新加载通知配置
     */
    private HotReloadResult reloadNotifyConfig(Map<String, String> configs) {
        log.info("通知配置已更新，下次发送通知时使用新配置");

        return HotReloadResult.success(Arrays.asList("notificationService"));
    }
}
