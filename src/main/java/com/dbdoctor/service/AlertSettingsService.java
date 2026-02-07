package com.dbdoctor.service;

import com.dbdoctor.dto.AlertSettingsDTO;
import com.dbdoctor.entity.SystemConfig;
import com.dbdoctor.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 告警设置服务
 *
 * <p>提供告警参数的查询和更新功能，支持热更新</p>
 *
 * @author DB-Doctor
 * @version 3.2.0
 * @since 3.2.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertSettingsService {

    private final SystemConfigRepository configRepository;
    private final CacheManager cacheManager;

    // 配置键常量
    private static final String KEY_SEVERITY_THRESHOLD = "alert.severity-threshold";
    private static final String KEY_COOL_DOWN_HOURS = "alert.cool-down-hours";
    private static final String KEY_DEGRADATION_MULTIPLIER = "alert.degradation-multiplier";

    // 默认值
    private static final Double DEFAULT_SEVERITY_THRESHOLD = 3.0;
    private static final Integer DEFAULT_COOL_DOWN_HOURS = 1;
    private static final Double DEFAULT_DEGRADATION_MULTIPLIER = 1.5;

    /**
     * 查询告警设置
     *
     * @return 告警设置 DTO
     */
    public AlertSettingsDTO getAlertSettings() {
        log.info("[告警设置服务] 查询告警设置");

        // 从数据库读取配置，如果不存在则使用默认值
        Double severityThreshold = getDoubleConfig(KEY_SEVERITY_THRESHOLD, DEFAULT_SEVERITY_THRESHOLD);
        Integer coolDownHours = getIntConfig(KEY_COOL_DOWN_HOURS, DEFAULT_COOL_DOWN_HOURS);
        Double degradationMultiplier = getDoubleConfig(KEY_DEGRADATION_MULTIPLIER, DEFAULT_DEGRADATION_MULTIPLIER);

        return AlertSettingsDTO.builder()
                .severityThreshold(severityThreshold)
                .coolDownHours(coolDownHours)
                .degradationMultiplier(degradationMultiplier)
                .build();
    }

    /**
     * 更新告警设置
     *
     * @param settings 告警设置 DTO
     */
    @Transactional
    public void updateAlertSettings(AlertSettingsDTO settings) {
        log.info("[告警设置服务] 更新告警设置: {}", settings);

        // 保存到数据库
        saveConfig(KEY_SEVERITY_THRESHOLD, settings.getSeverityThreshold().toString());
        saveConfig(KEY_COOL_DOWN_HOURS, settings.getCoolDownHours().toString());
        saveConfig(KEY_DEGRADATION_MULTIPLIER, settings.getDegradationMultiplier().toString());

        // 清除缓存，触发热更新
        clearConfigCache();

        log.info("[告警设置服务] 告警设置已更新并生效（热更新）");
    }

    /**
     * 重置为默认值
     *
     * @return 默认告警设置 DTO
     */
    @Transactional
    public AlertSettingsDTO resetAlertSettings() {
        log.info("[告警设置服务] 重置告警设置为默认值");

        AlertSettingsDTO defaultSettings = AlertSettingsDTO.builder()
                .severityThreshold(DEFAULT_SEVERITY_THRESHOLD)
                .coolDownHours(DEFAULT_COOL_DOWN_HOURS)
                .degradationMultiplier(DEFAULT_DEGRADATION_MULTIPLIER)
                .build();

        updateAlertSettings(defaultSettings);
        return defaultSettings;
    }

    /**
     * 获取 Double 类型配置
     *
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    private Double getDoubleConfig(String key, double defaultValue) {
        return configRepository.findByConfigKey(key)
                .map(config -> {
                    try {
                        return Double.parseDouble(config.getConfigValue());
                    } catch (NumberFormatException e) {
                        log.warn("[告警设置服务] 配置值格式错误: key={}, value={}, 使用默认值: {}",
                                key, config.getConfigValue(), defaultValue);
                        return defaultValue;
                    }
                })
                .orElse(defaultValue);
    }

    /**
     * 获取 Integer 类型配置
     *
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    private Integer getIntConfig(String key, int defaultValue) {
        return configRepository.findByConfigKey(key)
                .map(config -> {
                    try {
                        return Integer.parseInt(config.getConfigValue());
                    } catch (NumberFormatException e) {
                        log.warn("[告警设置服务] 配置值格式错误: key={}, value={}, 使用默认值: {}",
                                key, config.getConfigValue(), defaultValue);
                        return defaultValue;
                    }
                })
                .orElse(defaultValue);
    }

    /**
     * 保存配置
     *
     * @param key 配置键
     * @param value 配置值
     */
    private void saveConfig(String key, String value) {
        SystemConfig config = configRepository.findByConfigKey(key)
                .orElse(new SystemConfig(key));

        config.setConfigValue(value);
        config.setConfigGroup("alert");
        config.setDescription(getConfigDescription(key));
        config.setUpdatedBy("admin");

        configRepository.save(config);
        log.debug("[告警设置服务] 配置已保存: key={}, value={}", key, value);
    }

    /**
     * 获取配置说明
     *
     * @param key 配置键
     * @return 配置说明
     */
    private String getConfigDescription(String key) {
        return switch (key) {
            case KEY_SEVERITY_THRESHOLD -> "严重程度阈值（秒），平均查询耗时低于此值不发送通知";
            case KEY_COOL_DOWN_HOURS -> "冷却期（小时），同一 SQL 两次通知的最小间隔时间";
            case KEY_DEGRADATION_MULTIPLIER -> "性能恶化倍率，触发二次唤醒通知的性能恶化比例";
            default -> "";
        };
    }

    /**
     * 清除配置缓存
     */
    private void clearConfigCache() {
        var cache = cacheManager.getCache("system_config");
        if (cache != null) {
            cache.clear();
            log.info("[告警设置服务] 已清除配置缓存，配置已重新加载");
        }
    }
}
