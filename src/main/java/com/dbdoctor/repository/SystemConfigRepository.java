package com.dbdoctor.repository;

import com.dbdoctor.entity.SystemConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 系统配置 Repository
 *
 * @author DB-Doctor
 * @version 2.4.0
 * @since 2.2.0
 */
@Repository
public interface SystemConfigRepository extends JpaRepository<SystemConfig, Long> {

    /**
     * 根据配置分组查询
     *
     * @param configGroup 配置分组（database/log/ai/notification/scheduler）
     * @return 配置列表
     */
    List<SystemConfig> findByConfigGroup(String configGroup);

    /**
     * 根据配置分组和启用状态查询
     *
     * @param configGroup 配置分组
     * @param isEnabled   是否启用
     * @return 配置列表
     */
    List<SystemConfig> findByConfigGroupAndIsEnabled(String configGroup, Boolean isEnabled);

    /**
     * 根据配置键查询
     *
     * @param configKey 配置键
     * @return 配置对象
     */
    Optional<SystemConfig> findByConfigKey(String configKey);

    /**
     * 查询所有启用的配置
     *
     * @return 配置列表
     */
    List<SystemConfig> findByIsEnabledTrue();

    /**
     * 查询非敏感配置
     *
     * @return 配置列表
     */
    List<SystemConfig> findByIsSensitiveFalse();

    /**
     * 检查必填配置是否已完成
     *
     * @param configGroup 配置分组
     * @return 未配置的必填项数量
     */
    @Query("""
        SELECT COUNT(sc)
        FROM SystemConfig sc
        WHERE sc.configGroup = :configGroup
          AND sc.isRequired = true
          AND (sc.configValue IS NULL OR sc.configValue = '')
        """)
    long countRequiredAndEmptyByGroup(@Param("configGroup") String configGroup);

    /**
     * 查询所有配置分组
     *
     * @return 分组列表
     */
    @Query("SELECT DISTINCT sc.configGroup FROM SystemConfig sc ORDER BY sc.configGroup")
    List<String> findAllGroups();

    // ==================== 兼容旧版本 ====================

    /**
     * @deprecated 使用 {@link #findByConfigGroup(String)} 替代
     */
    @Deprecated
    default List<SystemConfig> findByConfigCategory(String configCategory) {
        return findByConfigGroup(configCategory);
    }
}
