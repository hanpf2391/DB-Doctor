package com.dbdoctor.service;

import com.dbdoctor.entity.AiServiceInstance;
import com.dbdoctor.entity.SystemConfig;
import com.dbdoctor.repository.AiServiceInstanceRepository;
import com.dbdoctor.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI 配置管理服务
 *
 * 核心功能：
 * 1. 从数据库读取 AI 配置（支持 ai_service_instance 实例配置）
 * 2. 优先使用实例ID查询 ai_service_instance 表
 * 3. 提供配置缓存和热加载
 * 4. 支持运行时动态切换 AI 启用状态
 *
 * @author DB-Doctor
 * @version 3.1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiConfigManagementService {

    private final SystemConfigRepository systemConfigRepository;
    private final AiServiceInstanceRepository aiServiceInstanceRepository;

    /**
     * 配置缓存（本地缓存，避免频繁查询数据库）
     * Key: configKey, Value: configValue
     */
    private final Map<String, String> configCache = new ConcurrentHashMap<>();

    /**
     * AI 服务实例缓存（ID → Instance）
     */
    private final Map<Long, AiServiceInstance> instanceCache = new ConcurrentHashMap<>();

    /**
     * 缓存刷新时间戳
     */
    private volatile long lastRefreshTime = 0;

    /**
     * 缓存有效期（秒），默认 30 秒
     */
    private static final long CACHE_TTL_SECONDS = 30;

    /**
     * 检查 AI 功能是否启用
     *
     * @return true=启用，false=禁用
     */
    public boolean isAiEnabled() {
        String value = getConfigValue("ai.enabled");
        return "true".equalsIgnoreCase(value);
    }

    /**
     * 获取 AI 服务提供商（从实例配置读取）
     *
     * @return 提供商（ollama/openai/deepseek/aliyun）
     */
    public String getAiProvider() {
        // 优先从实例配置获取
        AiServiceInstance instance = getDiagnosisInstance();
        if (instance != null) {
            return instance.getProvider();
        }
        // 降级到旧配置
        return getConfigValue("ai.provider");
    }

    /**
     * 获取 API Key（从实例配置读取）
     *
     * @return API Key
     */
    public String getApiKey() {
        // 优先从实例配置获取
        AiServiceInstance instance = getDiagnosisInstance();
        if (instance != null) {
            return instance.getApiKey();
        }
        // 降级到旧配置
        return getConfigValue("ai.api_key");
    }

    /**
     * 获取 Base URL（从实例配置读取）
     *
     * @return Base URL
     */
    public String getBaseUrl() {
        // 优先从实例配置获取
        AiServiceInstance instance = getDiagnosisInstance();
        if (instance != null) {
            return instance.getBaseUrl();
        }
        // 降级到旧配置
        return getConfigValue("ai.base_url");
    }

    /**
     * 获取超时时间
     *
     * @return 超时时间（秒）
     */
    public Long getTimeoutSeconds() {
        // 优先从实例配置获取
        AiServiceInstance instance = getDiagnosisInstance();
        if (instance != null && instance.getTimeoutSeconds() != null) {
            return instance.getTimeoutSeconds().longValue();
        }
        // 降级到旧配置
        String value = getConfigValue("ai.timeout_seconds");
        if (value != null && !value.isEmpty()) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException e) {
                log.warn("[AI配置] 解析超时时间失败: {}", value);
            }
        }
        return 60L; // 默认 60 秒
    }

    /**
     * 获取主治医生模型名称（从实例配置读取）
     *
     * @return 模型名称
     */
    public String getDiagnosisModelName() {
        // 优先从实例配置获取
        AiServiceInstance instance = getDiagnosisInstance();
        if (instance != null && instance.getModelName() != null) {
            return instance.getModelName();
        }
        // 降级到旧配置
        return getConfigValue("ai.diagnosis.model_name");
    }

    /**
     * 获取推理专家模型名称（从实例配置读取）
     *
     * @return 模型名称
     */
    public String getReasoningModelName() {
        // 优先从实例配置获取
        AiServiceInstance instance = getReasoningInstance();
        if (instance != null && instance.getModelName() != null) {
            return instance.getModelName();
        }
        // 降级到旧配置
        return getConfigValue("ai.reasoning.model_name");
    }

    /**
     * 获取编码专家模型名称（从实例配置读取）
     *
     * @return 模型名称
     */
    public String getCodingModelName() {
        // 优先从实例配置获取
        AiServiceInstance instance = getCodingInstance();
        if (instance != null && instance.getModelName() != null) {
            return instance.getModelName();
        }
        // 降级到旧配置
        return getConfigValue("ai.coding.model_name");
    }

    /**
     * 获取成本计算开关
     *
     * @return true=启用，false=禁用
     */
    public boolean isCostCalculationEnabled() {
        String value = getConfigValue("monitoring.cost_calculation_enabled");
        return "true".equalsIgnoreCase(value);
    }

    /**
     * 获取模型定价配置
     *
     * @return 定价配置 Map
     */
    public Map<String, Object> getModelPricing() {
        Map<String, Object> pricing = new HashMap<>();

        // 从数据库读取模型定价
        String gpt4oPrice = getConfigValue("cost.model_pricing.gpt-4o");
        if (gpt4oPrice != null) {
            pricing.put("gpt-4o", parsePriceConfig(gpt4oPrice, "openai"));
        }

        String gpt4oMiniPrice = getConfigValue("cost.model_pricing.gpt-4o-mini");
        if (gpt4oMiniPrice != null) {
            pricing.put("gpt-4o-mini", parsePriceConfig(gpt4oMiniPrice, "openai"));
        }

        String qwenPrice = getConfigValue("cost.model_pricing.qwen");
        if (qwenPrice != null) {
            pricing.put("qwen", parsePriceConfig(qwenPrice, "ollama"));
        }

        // 如果数据库中没有定价配置，返回空 Map（使用默认定价）
        return pricing;
    }

    /**
     * 解析价格配置字符串
     *
     * @param priceStr 价格字符串（格式：input_price,output_price,provider）
     * @param defaultProvider 默认提供商
     * @return 价格配置对象
     */
    private Map<String, Object> parsePriceConfig(String priceStr, String defaultProvider) {
        Map<String, Object> config = new HashMap<>();
        String[] parts = priceStr.split(",");

        config.put("input-price", parts.length > 0 ? Double.parseDouble(parts[0]) : 0.0);
        config.put("output-price", parts.length > 1 ? Double.parseDouble(parts[1]) : 0.0);
        config.put("provider", parts.length > 2 ? parts[2] : defaultProvider);

        return config;
    }

    /**
     * 获取主治医生的 AI 服务实例
     *
     * @return AI 服务实例
     */
    private AiServiceInstance getDiagnosisInstance() {
        String instanceIdStr = getConfigValue("ai.diagnosis.instance_id");
        if (instanceIdStr != null && !instanceIdStr.isEmpty()) {
            try {
                Long instanceId = Long.parseLong(instanceIdStr);
                return getInstanceById(instanceId);
            } catch (NumberFormatException e) {
                log.warn("[AI配置] 解析主治医生实例ID失败: {}", instanceIdStr);
            }
        }
        return null;
    }

    /**
     * 获取推理专家的 AI 服务实例
     *
     * @return AI 服务实例
     */
    private AiServiceInstance getReasoningInstance() {
        String instanceIdStr = getConfigValue("ai.reasoning.instance_id");
        if (instanceIdStr != null && !instanceIdStr.isEmpty()) {
            try {
                Long instanceId = Long.parseLong(instanceIdStr);
                return getInstanceById(instanceId);
            } catch (NumberFormatException e) {
                log.warn("[AI配置] 解析推理专家实例ID失败: {}", instanceIdStr);
            }
        }
        return null;
    }

    /**
     * 获取编码专家的 AI 服务实例
     *
     * @return AI 服务实例
     */
    private AiServiceInstance getCodingInstance() {
        String instanceIdStr = getConfigValue("ai.coding.instance_id");
        if (instanceIdStr != null && !instanceIdStr.isEmpty()) {
            try {
                Long instanceId = Long.parseLong(instanceIdStr);
                return getInstanceById(instanceId);
            } catch (NumberFormatException e) {
                log.warn("[AI配置] 解析编码专家实例ID失败: {}", instanceIdStr);
            }
        }
        return null;
    }

    /**
     * 根据 ID 获取 AI 服务实例（带缓存）
     *
     * @param instanceId 实例ID
     * @return AI 服务实例
     */
    private AiServiceInstance getInstanceById(Long instanceId) {
        // 检查缓存是否过期
        long now = System.currentTimeMillis();
        if (now - lastRefreshTime > CACHE_TTL_SECONDS * 1000) {
            refreshCache();
        }

        // 从缓存获取
        AiServiceInstance instance = instanceCache.get(instanceId);
        if (instance == null) {
            // 缓存未命中，从数据库查询
            Optional<AiServiceInstance> opt = aiServiceInstanceRepository.findById(instanceId);
            if (opt.isPresent()) {
                instance = opt.get();
                instanceCache.put(instanceId, instance);
                log.debug("[AI配置] 从数据库加载 AI 服务实例: id={}, provider={}, model={}",
                        instanceId, instance.getProvider(), instance.getModelName());
            } else {
                log.warn("[AI配置] 未找到 AI 服务实例: id={}", instanceId);
            }
        }
        return instance;
    }

    /**
     * 获取配置值（带缓存）
     *
     * @param configKey 配置键
     * @return 配置值（不存在返回 null）
     */
    private String getConfigValue(String configKey) {
        // 检查缓存是否过期
        long now = System.currentTimeMillis();
        if (now - lastRefreshTime > CACHE_TTL_SECONDS * 1000) {
            refreshCache();
        }

        return configCache.get(configKey);
    }

    /**
     * 刷新配置缓存（从数据库重新加载）
     * 可以在配置更新后手动调用，实现热加载
     */
    public void refreshCache() {
        log.debug("[AI配置] 刷新配置缓存...");

        try {
            // 清空实例缓存
            instanceCache.clear();

            // 从数据库加载所有系统配置
            Iterable<SystemConfig> configs = systemConfigRepository.findAll();

            configCache.clear();
            for (SystemConfig config : configs) {
                configCache.put(config.getConfigKey(), config.getConfigValue());
            }

            lastRefreshTime = System.currentTimeMillis();
            log.debug("[AI配置] 缓存刷新完成，共加载 {} 项系统配置", configCache.size());
        } catch (Exception e) {
            log.error("[AI配置] 刷新缓存失败", e);
        }
    }

    /**
     * 清除缓存（用于测试或强制重新加载）
     */
    @CacheEvict(value = "ai-config", allEntries = true)
    public void clearCache() {
        configCache.clear();
        instanceCache.clear();
        lastRefreshTime = 0;
        log.info("[AI配置] 缓存已清除");
    }
}
