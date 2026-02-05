package com.dbdoctor.service;

import com.dbdoctor.common.util.EncryptionService;
import com.dbdoctor.entity.AiServiceInstance;
import com.dbdoctor.repository.AiServiceInstanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI服务实例管理服务
 *
 * 核心功能：
 * 1. CRUD操作（创建、读取、更新、删除）
 * 2. 默认实例管理
 * 3. API密钥加密/解密
 *
 * @author DB-Doctor
 * @version 3.1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiServiceInstanceService {

    private final AiServiceInstanceRepository repository;
    private final EncryptionService encryptionService;

    /**
     * 获取所有AI服务实例（包括禁用的）
     *
     * @return 实例列表（默认实例优先，按创建时间倒序）
     */
    public List<AiServiceInstance> findAll() {
        log.debug("查询所有AI服务实例");
        return repository.findAllByOrderByIsDefaultDescCreatedAtDesc();
    }

    /**
     * 获取所有启用的AI服务实例
     *
     * @return 实例列表（默认实例优先）
     */
    public List<AiServiceInstance> findAllEnabled() {
        log.debug("查询所有启用的AI服务实例");
        return repository.findByIsEnabledTrueOrderByIsDefaultDescCreatedAtDesc();
    }

    /**
     * 根据ID查询实例
     *
     * @param id 实例ID
     * @return 实例对象
     * @throws IllegalArgumentException 实例不存在
     */
    public AiServiceInstance findById(Long id) {
        log.debug("查询AI服务实例: id={}", id);
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("AI服务实例不存在: " + id));
    }

    /**
     * 根据实例名称查询
     *
     * @param instanceName 实例名称
     * @return 实例对象
     * @throws IllegalArgumentException 实例不存在
     */
    public AiServiceInstance findByInstanceName(String instanceName) {
        log.debug("查询AI服务实例: instanceName={}", instanceName);
        return repository.findByInstanceName(instanceName)
                .orElseThrow(() -> new IllegalArgumentException("AI服务实例不存在: " + instanceName));
    }

    /**
     * 创建AI服务实例
     *
     * @param instance 实例信息（API密钥未加密）
     * @return 创建后的实例
     * @throws IllegalArgumentException 实例名称已存在
     */
    @Transactional
    public AiServiceInstance createInstance(AiServiceInstance instance) {
        log.info("创建AI服务实例: instanceName={}", instance.getInstanceName());

        // 1. 检查实例名称是否已存在
        if (repository.existsByInstanceName(instance.getInstanceName())) {
            throw new IllegalArgumentException("实例名称已存在: " + instance.getInstanceName());
        }

        // 2. 加密API密钥（如果有）
        if (instance.getApiKey() != null && !instance.getApiKey().isEmpty()) {
            String encryptedApiKey = encryptionService.encrypt(instance.getApiKey());
            instance.setApiKey(encryptedApiKey);
        }

        // 3. 设置默认值
        instance.setIsEnabled(true);
        instance.setIsValid(false);
        instance.setCreatedAt(LocalDateTime.now());
        instance.setUpdatedAt(LocalDateTime.now());

        // 4. 如果是第一个启用的实例，设为默认
        long enabledCount = repository.count();
        if (enabledCount == 0) {
            instance.setIsDefault(true);
        }

        // 5. 保存
        AiServiceInstance saved = repository.save(instance);
        log.info("✅ AI服务实例创建成功: id={}, instanceName={}", saved.getId(), saved.getInstanceName());
        return saved;
    }

    /**
     * 更新AI服务实例
     *
     * @param id 实例ID
     * @param updated 更新后的实例信息
     * @return 更新后的实例
     * @throws IllegalArgumentException 实例不存在
     */
    @Transactional
    public AiServiceInstance updateInstance(Long id, AiServiceInstance updated) {
        log.info("更新AI服务实例: id={}", id);

        AiServiceInstance existing = findById(id);

        // 1. 检查实例名称是否被其他实例使用
        if (!existing.getInstanceName().equals(updated.getInstanceName()) &&
                repository.existsByInstanceName(updated.getInstanceName())) {
            throw new IllegalArgumentException("实例名称已被使用: " + updated.getInstanceName());
        }

        // 2. 更新基本信息
        existing.setInstanceName(updated.getInstanceName());
        existing.setProvider(updated.getProvider());
        existing.setBaseUrl(updated.getBaseUrl());
        existing.setModelName(updated.getModelName());
        existing.setTemperature(updated.getTemperature());
        existing.setMaxTokens(updated.getMaxTokens());
        existing.setTimeoutSeconds(updated.getTimeoutSeconds());
        existing.setDescription(updated.getDescription());
        existing.setCapabilityTags(updated.getCapabilityTags());
        existing.setUpdatedAt(LocalDateTime.now());

        // 3. 如果API密钥有更新，重新加密
        if (updated.getApiKey() != null && !updated.getApiKey().isEmpty()) {
            String encryptedApiKey = encryptionService.encrypt(updated.getApiKey());
            existing.setApiKey(encryptedApiKey);
        }

        // 4. 重置验证状态（因为配置改变了）
        existing.setIsValid(false);
        existing.setLastValidatedAt(null);
        existing.setValidationError(null);

        AiServiceInstance saved = repository.save(existing);
        log.info("✅ AI服务实例更新成功: id={}, instanceName={}", saved.getId(), saved.getInstanceName());
        return saved;
    }

    /**
     * 删除AI服务实例
     *
     * @param id 实例ID
     * @throws IllegalArgumentException 实例不存在或为默认实例
     */
    @Transactional
    public void deleteInstance(Long id) {
        log.info("删除AI服务实例: id={}", id);

        AiServiceInstance instance = findById(id);

        // 不允许删除默认实例
        if (instance.getIsDefault()) {
            throw new IllegalArgumentException("不能删除默认实例，请先设置其他实例为默认");
        }

        repository.deleteById(id);
        log.info("✅ AI服务实例已删除: id={}, instanceName={}", id, instance.getInstanceName());
    }

    /**
     * 设置默认实例
     *
     * @param id 实例ID
     * @throws IllegalArgumentException 实例不存在
     */
    @Transactional
    public void setDefaultInstance(Long id) {
        log.info("设置默认AI服务实例: id={}", id);

        AiServiceInstance instance = findById(id);

        // 1. 清除所有实例的默认标记
        findAllEnabled().forEach(aiInstance -> {
            if (aiInstance.getIsDefault()) {
                aiInstance.setIsDefault(false);
                repository.save(aiInstance);
            }
        });

        // 2. 设置新的默认实例
        instance.setIsDefault(true);
        repository.save(instance);

        log.info("✅ 默认AI服务实例已设置: id={}, instanceName={}", id, instance.getInstanceName());
    }

    /**
     * 启用/禁用实例
     *
     * @param id 实例ID
     * @param enabled 是否启用
     */
    @Transactional
    public void setEnabled(Long id, boolean enabled) {
        log.info("{}AI服务实例: id={}", enabled ? "启用" : "禁用", id);

        AiServiceInstance instance = findById(id);
        instance.setIsEnabled(enabled);
        instance.setUpdatedAt(LocalDateTime.now());

        repository.save(instance);
        log.info("✅ AI服务实例状态已更新: id={}, enabled={}", id, enabled);
    }
}
