package com.dbdoctor.repository;

import com.dbdoctor.entity.DatabaseInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 数据库实例 Repository
 *
 * @author DB-Doctor
 * @version 3.1.0
 */
@Repository
public interface DatabaseInstanceRepository extends JpaRepository<DatabaseInstance, Long> {

    /**
     * 根据实例名称查询
     *
     * @param instanceName 实例名称
     * @return 实例对象
     */
    Optional<DatabaseInstance> findByInstanceName(String instanceName);

    /**
     * 查询所有实例（默认实例优先，按创建时间倒序）
     *
     * @return 所有实例列表
     */
    List<DatabaseInstance> findAllByOrderByIsDefaultDescCreatedAtDesc();

    /**
     * 查询所有启用的实例（默认实例优先，按创建时间倒序）
     *
     * @return 启用的实例列表
     */
    List<DatabaseInstance> findByIsEnabledTrueOrderByIsDefaultDescCreatedAtDesc();

    /**
     * 查询默认实例
     *
     * @return 默认实例
     */
    Optional<DatabaseInstance> findByIsDefaultTrueAndIsEnabledTrue();

    /**
     * 根据环境查询
     *
     * @param environment 环境标识
     * @return 实例列表
     */
    List<DatabaseInstance> findByEnvironmentAndIsEnabledTrueOrderByCreatedAtDesc(String environment);

    /**
     * 检查实例名称是否存在
     *
     * @param instanceName 实例名称
     * @return true=存在，false=不存在
     */
    @Query("SELECT COUNT(d) > 0 FROM DatabaseInstance d WHERE d.instanceName = :instanceName")
    boolean existsByInstanceName(@Param("instanceName") String instanceName);
}
