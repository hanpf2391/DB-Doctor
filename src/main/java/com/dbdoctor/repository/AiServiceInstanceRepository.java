package com.dbdoctor.repository;

import com.dbdoctor.entity.AiServiceInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * AI服务实例 Repository
 *
 * @author DB-Doctor
 * @version 3.1.0
 */
@Repository
public interface AiServiceInstanceRepository extends JpaRepository<AiServiceInstance, Long> {

    /**
     * 根据实例名称查询
     *
     * @param instanceName 实例名称
     * @return 实例对象
     */
    Optional<AiServiceInstance> findByInstanceName(String instanceName);

    /**
     * 查询所有实例（默认实例优先，按创建时间倒序）
     *
     * @return 所有实例列表
     */
    List<AiServiceInstance> findAllByOrderByIsDefaultDescCreatedAtDesc();

    /**
     * 查询所有启用的实例（默认实例优先，按创建时间倒序）
     *
     * @return 启用的实例列表
     */
    List<AiServiceInstance> findByIsEnabledTrueOrderByIsDefaultDescCreatedAtDesc();

    /**
     * 根据Provider查询
     *
     * @param provider 提供商
     * @return 实例列表
     */
    List<AiServiceInstance> findByProviderAndIsEnabledTrueOrderByCreatedAtDesc(String provider);

    /**
     * 检查实例名称是否存在
     *
     * @param instanceName 实例名称
     * @return true=存在，false=不存在
     */
    @Query("SELECT COUNT(a) > 0 FROM AiServiceInstance a WHERE a.instanceName = :instanceName")
    boolean existsByInstanceName(@Param("instanceName") String instanceName);

    /**
     * 查询默认实例
     *
     * @return 默认实例
     */
    Optional<AiServiceInstance> findByIsDefaultTrueAndIsEnabledTrue();
}
