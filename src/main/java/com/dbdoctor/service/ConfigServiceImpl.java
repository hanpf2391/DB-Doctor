package com.dbdoctor.service;

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
 * @version 3.1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigServiceImpl implements ConfigService {

    private final SystemConfigRepository configRepo;
    private final AiConfigManagementService aiConfigService;

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
        List<SystemConfig> configs = configRepo.findByConfigGroup(category);

        return configs.stream()
                .collect(Collectors.toMap(
                        SystemConfig::getConfigKey,
                        config -> config.getIsSensitive() ? MASK : config.getConfigValue()
                ));
    }

    @Override
    @Transactional
    public void saveConfig(String key, String value) {
        SystemConfig config = configRepo.findByConfigKey(key)
                .orElse(new SystemConfig());

        config.setConfigKey(key);
        config.setConfigValue(value);
        config.setConfigGroup(extractCategory(key));

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
        configRepo.findByConfigKey(key).ifPresent(config -> {
            configRepo.delete(config);
            log.info("配置已删除: {}", key);
        });
    }

    @Override
    public boolean exists(String key) {
        return configRepo.findByConfigKey(key).isPresent();
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
     *
     * 说明：AI 配置现在通过 AiConfigManagementService 管理，支持自动缓存刷新
     * AI Bean 在使用时自动从数据库读取最新配置
     */
    private HotReloadResult reloadAiConfig(Map<String, String> configs) {
        log.info("🔄 开始重新加载 AI 配置...");

        try {
            // 刷新 AI 配置缓存
            aiConfigService.refreshCache();

            List<String> refreshedBeans = Arrays.asList(
                    "aiConfigCache"
            );

            log.info("✅ AI 配置缓存刷新成功");
            return HotReloadResult.success(refreshedBeans);

        } catch (Exception e) {
            log.error("❌ AI 配置缓存刷新失败", e);
            return HotReloadResult.needRestart("AI 配置加载失败: " + e.getMessage());
        }
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
