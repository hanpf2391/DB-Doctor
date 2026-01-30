package com.dbdoctor.repository;

import com.dbdoctor.entity.SystemConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 系统配置 Repository
 *
 * @author DB-Doctor
 * @version 2.2.0
 */
@Repository
public interface SystemConfigRepository extends JpaRepository<SystemConfig, String> {

    /**
     * 根据配置分类查询
     */
    List<SystemConfig> findByConfigCategory(String configCategory);

    /**
     * 根据配置键查询
     */
    Optional<SystemConfig> findByConfigKey(String configKey);

    /**
     * 查询非敏感配置
     */
    List<SystemConfig> findByIsSensitiveFalse();
}
