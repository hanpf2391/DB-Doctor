package com.dbdoctor.service;

import com.dbdoctor.check.MySqlEnvChecker;
import com.dbdoctor.common.util.EncryptionService;
import com.dbdoctor.entity.DatabaseInstance;
import com.dbdoctor.model.EnvCheckReport;
import com.dbdoctor.repository.DatabaseInstanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 数据库实例管理服务
 *
 * 核心功能：
 * 1. CRUD操作（创建、读取、更新、删除）
 * 2. 连接验证（测试数据库连接）
 * 3. 默认实例管理
 * 4. 密码加密/解密
 *
 * @author DB-Doctor
 * @version 3.1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseInstanceService {

    private final DatabaseInstanceRepository repository;
    private final MySqlEnvChecker envChecker;
    private final EncryptionService encryptionService;

    /**
     * 获取所有启用的数据库实例
     *
     * @return 实例列表（默认实例优先）
     */
    public List<DatabaseInstance> findAllEnabled() {
        log.debug("查询所有启用的数据库实例");
        return repository.findByIsEnabledTrueOrderByIsDefaultDescCreatedAtDesc();
    }

    /**
     * 根据ID查询实例
     *
     * @param id 实例ID
     * @return 实例对象
     * @throws IllegalArgumentException 实例不存在
     */
    public DatabaseInstance findById(Long id) {
        log.debug("查询数据库实例: id={}", id);
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("数据库实例不存在: " + id));
    }

    /**
     * 根据实例名称查询
     *
     * @param instanceName 实例名称
     * @return 实例对象
     * @throws IllegalArgumentException 实例不存在
     */
    public DatabaseInstance findByInstanceName(String instanceName) {
        log.debug("查询数据库实例: instanceName={}", instanceName);
        return repository.findByInstanceName(instanceName)
                .orElseThrow(() -> new IllegalArgumentException("数据库实例不存在: " + instanceName));
    }

    /**
     * 创建数据库实例
     *
     * @param instance 实例信息（密码未加密）
     * @return 创建后的实例
     * @throws IllegalArgumentException 实例名称已存在
     */
    @Transactional
    public DatabaseInstance createInstance(DatabaseInstance instance) {
        log.info("创建数据库实例: instanceName={}", instance.getInstanceName());

        // 1. 检查实例名称是否已存在
        if (repository.existsByInstanceName(instance.getInstanceName())) {
            throw new IllegalArgumentException("实例名称已存在: " + instance.getInstanceName());
        }

        // 2. 加密密码
        String encryptedPassword = encryptionService.encrypt(instance.getPassword());
        instance.setPassword(encryptedPassword);

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
        DatabaseInstance saved = repository.save(instance);
        log.info("✅ 数据库实例创建成功: id={}, instanceName={}", saved.getId(), saved.getInstanceName());
        return saved;
    }

    /**
     * 更新数据库实例
     *
     * @param id 实例ID
     * @param updated 更新后的实例信息
     * @return 更新后的实例
     * @throws IllegalArgumentException 实例不存在
     */
    @Transactional
    public DatabaseInstance updateInstance(Long id, DatabaseInstance updated) {
        log.info("更新数据库实例: id={}", id);

        DatabaseInstance existing = findById(id);

        // 1. 检查实例名称是否被其他实例使用
        if (!existing.getInstanceName().equals(updated.getInstanceName()) &&
                repository.existsByInstanceName(updated.getInstanceName())) {
            throw new IllegalArgumentException("实例名称已被使用: " + updated.getInstanceName());
        }

        // 2. 更新基本信息
        existing.setInstanceName(updated.getInstanceName());
        existing.setInstanceType(updated.getInstanceType());
        existing.setUrl(updated.getUrl());
        existing.setUsername(updated.getUsername());
        existing.setDescription(updated.getDescription());
        existing.setEnvironment(updated.getEnvironment());
        existing.setTags(updated.getTags());
        existing.setUpdatedAt(LocalDateTime.now());

        // 3. 如果密码有更新，重新加密
        if (updated.getPassword() != null && !updated.getPassword().isEmpty()) {
            String encryptedPassword = encryptionService.encrypt(updated.getPassword());
            existing.setPassword(encryptedPassword);
        }

        // 4. 重置验证状态（因为配置改变了）
        existing.setIsValid(false);
        existing.setLastValidatedAt(null);
        existing.setValidationError(null);

        DatabaseInstance saved = repository.save(existing);
        log.info("✅ 数据库实例更新成功: id={}, instanceName={}", saved.getId(), saved.getInstanceName());
        return saved;
    }

    /**
     * 删除数据库实例
     *
     * @param id 实例ID
     * @throws IllegalArgumentException 实例不存在或为默认实例
     */
    @Transactional
    public void deleteInstance(Long id) {
        log.info("删除数据库实例: id={}", id);

        DatabaseInstance instance = findById(id);

        // 不允许删除默认实例
        if (instance.getIsDefault()) {
            throw new IllegalArgumentException("不能删除默认实例，请先设置其他实例为默认");
        }

        repository.deleteById(id);
        log.info("✅ 数据库实例已删除: id={}, instanceName={}", id, instance.getInstanceName());
    }

    /**
     * 验证数据库连接
     *
     * @param id 实例ID
     * @return 验证报告
     * @throws IllegalArgumentException 实例不存在
     */
    @Transactional
    public EnvCheckReport validateConnection(Long id) {
        log.info("验证数据库连接: id={}", id);

        DatabaseInstance instance = findById(id);

        try {
            // 1. 解密密码
            String decryptedPassword = encryptionService.decrypt(instance.getPassword());

            // 2. 执行环境检查
            EnvCheckReport report = envChecker.checkFully(
                    instance.getUrl(),
                    instance.getUsername(),
                    decryptedPassword
            );

            // 3. 更新验证状态
            instance.setIsValid(report.isOverallPassed());
            instance.setLastValidatedAt(LocalDateTime.now());
            instance.setValidationError(
                    report.isOverallPassed() ? null : report.getSummary()
            );

            repository.save(instance);

            if (report.isOverallPassed()) {
                log.info("✅ 数据库实例验证成功: instanceName={}", instance.getInstanceName());
            } else {
                log.warn("⚠️  数据库实例验证失败: instanceName={}, error={}",
                    instance.getInstanceName(), report.getSummary());
            }

            return report;

        } catch (Exception e) {
            log.error("❌ 验证数据库连接失败: instanceName={}", instance.getInstanceName(), e);

            // 更新失败状态
            instance.setIsValid(false);
            instance.setLastValidatedAt(LocalDateTime.now());
            instance.setValidationError("验证失败: " + e.getMessage());

            repository.save(instance);

            throw new RuntimeException("验证失败: " + e.getMessage(), e);
        }
    }

    /**
     * 设置默认实例
     *
     * @param id 实例ID
     * @throws IllegalArgumentException 实例不存在
     */
    @Transactional
    public void setDefaultInstance(Long id) {
        log.info("设置默认数据库实例: id={}", id);

        DatabaseInstance instance = findById(id);

        // 1. 清除所有实例的默认标记
        findAllEnabled().forEach(dbInstance -> {
            if (dbInstance.getIsDefault()) {
                dbInstance.setIsDefault(false);
                repository.save(dbInstance);
            }
        });

        // 2. 设置新的默认实例
        instance.setIsDefault(true);
        repository.save(instance);

        log.info("✅ 默认数据库实例已设置: id={}, instanceName={}", id, instance.getInstanceName());
    }

    /**
     * 启用/禁用实例
     *
     * @param id 实例ID
     * @param enabled 是否启用
     */
    @Transactional
    public void setEnabled(Long id, boolean enabled) {
        log.info("{}数据库实例: id={}", enabled ? "启用" : "禁用", id);

        DatabaseInstance instance = findById(id);
        instance.setIsEnabled(enabled);
        instance.setUpdatedAt(LocalDateTime.now());

        repository.save(instance);
        log.info("✅ 数据库实例状态已更新: id={}, enabled={}", id, enabled);
    }
}
