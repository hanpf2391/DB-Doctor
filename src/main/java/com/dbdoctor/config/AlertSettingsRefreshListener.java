package com.dbdoctor.config;

import com.dbdoctor.entity.SystemConfig;
import com.dbdoctor.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

/**
 * 告警设置热更新监听器
 *
 * <p>应用启动时从数据库加载告警设置，支持热更新</p>
 *
 * @author DB-Doctor
 * @version 3.2.0
 * @since 3.2.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlertSettingsRefreshListener {

    private final SystemConfigRepository configRepository;
    private final DbDoctorProperties properties;

    // 配置键常量
    private static final String KEY_SEVERITY_THRESHOLD = "alert.severity-threshold";
    private static final String KEY_COOL_DOWN_HOURS = "alert.cool-down-hours";
    private static final String KEY_DEGRADATION_MULTIPLIER = "alert.degradation-multiplier";

    // 默认值
    private static final String DEFAULT_SEVERITY_THRESHOLD = "3.0";
    private static final String DEFAULT_COOL_DOWN_HOURS = "1";
    private static final String DEFAULT_DEGRADATION_MULTIPLIER = "1.5";

    /**
     * 应用启动时加载告警设置
     *
     * @param event 应用启动事件
     */
    @EventListener(ApplicationReadyEvent.class)
    public void loadAlertSettings(ApplicationReadyEvent event) {
        log.info("[热更新监听器] 开始加载告警设置...");

        loadConfig(KEY_SEVERITY_THRESHOLD, DEFAULT_SEVERITY_THRESHOLD, value -> {
            properties.getNotify().setSeverityThreshold(Double.parseDouble(value));
            log.info("✓ 严重程度阈值: {} 秒", value);
        });

        loadConfig(KEY_COOL_DOWN_HOURS, DEFAULT_COOL_DOWN_HOURS, value -> {
            properties.getNotify().setCoolDownHours(Integer.parseInt(value));
            log.info("✓ 冷却期: {} 小时", value);
        });

        loadConfig(KEY_DEGRADATION_MULTIPLIER, DEFAULT_DEGRADATION_MULTIPLIER, value -> {
            properties.getNotify().setDegradationMultiplier(Double.parseDouble(value));
            log.info("✓ 性能恶化倍率: {}", value);
        });

        log.info("[热更新监听器] 告警设置加载完成，配置已生效");
    }

    /**
     * 加载单个配置
     *
     * @param key 配置键
     * @param defaultValue 默认值
     * @param setter 配置设置器
     */
    private void loadConfig(String key, String defaultValue, Consumer<String> setter) {
        try {
            String value = configRepository.findByConfigKey(key)
                    .map(SystemConfig::getConfigValue)
                    .orElse(defaultValue);
            setter.accept(value);
        } catch (Exception e) {
            log.error("[热更新监听器] 加载配置失败: key={}, 使用默认值: {}", key, defaultValue, e);
            setter.accept(defaultValue);
        }
    }
}
