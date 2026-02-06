package com.dbdoctor.repository;

import com.dbdoctor.entity.NotificationConfig;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 通知配置 Repository
 *
 * @author DB-Doctor
 * @version 3.2.0
 * @since 3.2.0
 */
@Repository
public interface NotificationConfigRepository extends org.springframework.data.jpa.repository.JpaRepository<NotificationConfig, Long> {

    /**
     * 根据渠道查询配置
     *
     * @param channel 渠道名称
     * @return 配置对象（可能为空）
     */
    Optional<NotificationConfig> findByChannel(String channel);

    /**
     * 根据启用状态查询配置列表
     *
     * @param enabled 是否启用
     * @return 配置列表
     */
    List<NotificationConfig> findByEnabled(Boolean enabled);

    /**
     * 检查渠道是否存在
     *
     * @param channel 渠道名称
     * @return 是否存在
     */
    boolean existsByChannel(String channel);

    /**
     * 查询所有启用的配置
     *
     * @return 启用的配置列表
     */
    List<NotificationConfig> findAllByEnabledTrue();
}
