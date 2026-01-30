package com.dbdoctor.service;

import com.dbdoctor.model.HotReloadResult;

import java.util.Map;

/**
 * 系统配置服务接口
 *
 * @author DB-Doctor
 * @version 2.2.0
 */
public interface ConfigService {

    /**
     * 获取单个配置值
     *
     * @param key 配置键
     * @return 配置值，不存在返回 null
     */
    String getConfig(String key);

    /**
     * 获取单个配置值，支持默认值
     *
     * @param key         配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    String getConfig(String key, String defaultValue);

    /**
     * 获取所有配置（敏感信息脱敏）
     *
     * @return 配置键值对
     */
    Map<String, String> getAllConfigs();

    /**
     * 根据分类获取配置
     *
     * @param category 配置分类 (DB, AI, NOTIFY, SYSTEM)
     * @return 配置键值对
     */
    Map<String, String> getConfigsByCategory(String category);

    /**
     * 保存单个配置
     *
     * @param key   配置键
     * @param value 配置值
     */
    void saveConfig(String key, String value);

    /**
     * 批量保存配置
     *
     * @param configs 配置键值对
     */
    void saveConfigs(Map<String, String> configs);

    /**
     * 保存配置并触发热重载
     *
     * @param category 配置分类
     * @param configs  配置键值对
     * @return 热重载结果
     */
    HotReloadResult saveAndRefresh(String category, Map<String, String> configs);

    /**
     * 删除配置
     *
     * @param key 配置键
     */
    void deleteConfig(String key);

    /**
     * 检查配置是否存在
     *
     * @param key 配置键
     * @return true=存在, false=不存在
     */
    boolean exists(String key);

    /**
     * 脱敏处理（敏感信息隐藏）
     *
     * @param value 原始值
     * @return 脱敏后的值
     */
    String maskSensitiveValue(String value);
}
