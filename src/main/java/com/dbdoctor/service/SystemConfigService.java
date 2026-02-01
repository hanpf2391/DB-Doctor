package com.dbdoctor.service;

import com.dbdoctor.entity.SystemConfig;
import com.dbdoctor.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 系统配置服务
 *
 * <p>核心功能：</p>
 * <ul>
 *   <li>配置的增删改查</li>
 *   <li>敏感信息加密存储</li>
 *   <li>配置热加载（基于缓存）</li>
 *   <li>配置校验</li>
 *   <li>数据库连接测试</li>
 * </ul>
 *
 * @author DB-Doctor
 * @version 2.4.0
 * @since 2.4.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemConfigService {

    private final SystemConfigRepository configRepository;

    /**
     * 配置缓存名称
     */
    public static final String CACHE_NAME = "system_config";

    /**
     * 加密密钥（使用 MD5 哈希生成固定 16 字节密钥）
     * AES 要求密钥长度必须是 16/24/32 字节
     */
    private static final String ENCRYPTION_KEY = "DBDoctor2024Secret";

    /**
     * 获取所有配置（按分组和显示顺序排序）
     *
     * @return 配置列表
     */
    public List<SystemConfig> findAll() {
        return configRepository.findAll().stream()
                .sorted(Comparator.comparing(SystemConfig::getConfigGroup)
                        .thenComparing(SystemConfig::getDisplayOrder))
                .collect(Collectors.toList());
    }

    /**
     * 根据分组获取配置
     *
     * @param group 配置分组
     * @return 配置列表
     */
    @Cacheable(value = CACHE_NAME, key = "#group")
    public List<SystemConfig> findByGroup(String group) {
        return configRepository.findByConfigGroup(group).stream()
                .sorted(Comparator.comparing(SystemConfig::getDisplayOrder))
                .collect(Collectors.toList());
    }

    /**
     * 获取单个配置（支持缓存）
     *
     * @param configKey 配置键
     * @return 配置值
     */
    @Cacheable(value = CACHE_NAME, key = "#configKey")
    public String getString(String configKey) {
        return configRepository.findByConfigKey(configKey)
                .map(SystemConfig::getConfigValue)
                .orElse(null);
    }

    /**
     * 获取整数配置
     *
     * @param configKey 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    public Integer getInteger(String configKey, Integer defaultValue) {
        String value = getString(configKey);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            log.warn("[配置服务] 配置值不是有效整数: {} = {}", configKey, value);
            return defaultValue;
        }
    }

    /**
     * 获取长整数配置
     *
     * @param configKey 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    public Long getLong(String configKey, Long defaultValue) {
        String value = getString(configKey);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            log.warn("[配置服务] 配置值不是有效长整数: {} = {}", configKey, value);
            return defaultValue;
        }
    }

    /**
     * 获取布尔配置
     *
     * @param configKey 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    public Boolean getBoolean(String configKey, Boolean defaultValue) {
        String value = getString(configKey);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }

    /**
     * 获取 JSON 配置
     *
     * @param configKey 配置键
     * @return 配置值
     */
    public String getJson(String configKey) {
        return getString(configKey);
    }

    /**
     * 更新配置（清除缓存）
     *
     * @param configKey 配置键
     * @param configValue 配置值
     * @param updatedBy 更新者
     */
    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public void updateConfig(String configKey, String configValue, String updatedBy) {
        SystemConfig config = configRepository.findByConfigKey(configKey)
                .orElseThrow(() -> new IllegalArgumentException("配置不存在: " + configKey));

        // 敏感信息加密
        String finalValue = configValue;
        if (Boolean.TRUE.equals(config.getIsSensitive()) && configValue != null && !configValue.isEmpty()) {
            finalValue = encrypt(configValue);
            log.debug("[配置服务] 敏感配置已加密: {}", configKey);
        }

        config.setConfigValue(finalValue);
        config.setUpdatedBy(updatedBy);
        configRepository.save(config);

        log.info("[配置服务] 配置已更新: {} by {}", configKey, updatedBy);
    }

    /**
     * 批量更新配置
     *
     * @param configs 配置Map
     * @param updatedBy 更新者
     * @return 更新结果（包含成功数量和失败列表）
     */
    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public Map<String, Object> batchUpdateConfigs(Map<String, String> configs, String updatedBy) {
        Map<String, Object> result = new HashMap<>();
        List<String> failedConfigs = new ArrayList<>();
        int successCount = 0;

        for (Map.Entry<String, String> entry : configs.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            try {
                updateConfig(key, value, updatedBy);
                successCount++;
            } catch (Exception e) {
                log.error("[配置服务] 更新配置失败: {} = {}", key, value, e);
                failedConfigs.add(key + ": " + e.getMessage());
            }
        }

        // 如果有失败的配置，抛出异常回滚事务
        if (!failedConfigs.isEmpty()) {
            throw new RuntimeException(
                String.format("配置保存失败！成功: %d 项, 失败: %d 项。失败详情: %s",
                    successCount,
                    failedConfigs.size(),
                    String.join("; ", failedConfigs))
            );
        }

        result.put("success", true);
        result.put("updatedCount", successCount);
        result.put("message", String.format("批量更新成功，共更新 %d 项配置", successCount));

        return result;
    }

    /**
     * 测试数据库连接
     *
     * @param url 数据库URL
     * @param username 用户名
     * @param password 密码
     * @return 测试结果
     */
    public Map<String, Object> testDatabaseConnection(String url, String username, String password) {
        Map<String, Object> result = new HashMap<>();
        Connection conn = null;

        try {
            // 加载 MySQL 驱动
            Class.forName("com.mysql.cj.jdbc.Driver");

            // 创建连接
            conn = DriverManager.getConnection(url, username, password);

            // 测试查询
            DatabaseMetaData meta = conn.getMetaData();
            String dbVersion = meta.getDatabaseProductVersion();

            // 获取所有数据库列表
            List<String> databases = new ArrayList<>();
            try (ResultSet rs = meta.getCatalogs()) {
                while (rs.next()) {
                    String dbName = rs.getString("TABLE_CAT");
                    // 过滤系统数据库
                    if (!"information_schema".equalsIgnoreCase(dbName) &&
                        !"mysql".equalsIgnoreCase(dbName) &&
                        !"performance_schema".equalsIgnoreCase(dbName) &&
                        !"sys".equalsIgnoreCase(dbName)) {
                        databases.add(dbName);
                    }
                }
            }
            Collections.sort(databases); // 按名称排序

            result.put("success", true);
            result.put("message", "数据库连接成功");
            result.put("dbVersion", dbVersion);
            result.put("username", username);
            result.put("databases", databases); // 返回数据库列表

            log.info("[配置服务] 数据库连接测试成功: {}, 找到 {} 个业务数据库", url, databases.size());

        } catch (ClassNotFoundException e) {
            result.put("success", false);
            result.put("message", "MySQL 驱动未找到: " + e.getMessage());
            log.error("[配置服务] 数据库连接测试失败: 驱动未找到", e);

        } catch (SQLException e) {
            result.put("success", false);
            result.put("message", "数据库连接失败: " + e.getMessage());
            result.put("sqlState", e.getSQLState());
            result.put("errorCode", e.getErrorCode());
            log.error("[配置服务] 数据库连接测试失败", e);

        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    log.warn("[配置服务] 关闭数据库连接失败", e);
                }
            }
        }

        return result;
    }

    /**
     * 检查配置分组的完整性
     *
     * @param group 配置分组
     * @return 检查结果
     */
    public Map<String, Object> checkGroupCompleteness(String group) {
        Map<String, Object> result = new HashMap<>();

        long emptyRequiredCount = configRepository.countRequiredAndEmptyByGroup(group);
        boolean isComplete = emptyRequiredCount == 0;

        result.put("group", group);
        result.put("isComplete", isComplete);
        result.put("missingRequiredCount", emptyRequiredCount);

        if (!isComplete) {
            List<SystemConfig> missingConfigs = configRepository.findByConfigGroup(group).stream()
                    .filter(c -> Boolean.TRUE.equals(c.getIsRequired()))
                    .filter(c -> c.getConfigValue() == null || c.getConfigValue().isEmpty())
                    .toList();
            result.put("missingConfigs", missingConfigs);
        }

        return result;
    }

    /**
     * 检查所有配置的完整性
     *
     * @return 检查结果
     */
    public Map<String, Object> checkAllCompleteness() {
        Map<String, Object> result = new HashMap<>();

        List<String> groups = configRepository.findAllGroups();
        Map<String, Boolean> completeness = new HashMap<>();

        for (String group : groups) {
            Map<String, Object> groupResult = checkGroupCompleteness(group);
            completeness.put(group, (Boolean) groupResult.get("isComplete"));
        }

        boolean allComplete = completeness.values().stream().allMatch(v -> v);

        result.put("allComplete", allComplete);
        result.put("groups", completeness);

        return result;
    }

    /**
     * 加密敏感信息
     *
     * @param plainText 明文
     * @return 密文（Base64编码）
     */
    private String encrypt(String plainText) {
        try {
            // 使用 MD5 哈希生成固定 16 字节密钥
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] keyBytes = md.digest(ENCRYPTION_KEY.getBytes(StandardCharsets.UTF_8));
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");

            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            log.error("[配置服务] 加密失败", e);
            throw new RuntimeException("加密失败", e);
        }
    }

    /**
     * 解密敏感信息
     *
     * @param cipherText 密文（Base64编码）
     * @return 明文
     */
    private String decrypt(String cipherText) {
        try {
            // 使用 MD5 哈希生成固定 16 字节密钥
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] keyBytes = md.digest(ENCRYPTION_KEY.getBytes(StandardCharsets.UTF_8));
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");

            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(cipherText));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("[配置服务] 解密失败", e);
            throw new RuntimeException("解密失败", e);
        }
    }

    /**
     * 获取解密后的配置值
     *
     * @param configKey 配置键
     * @return 解密后的值（如果 configValue 为空，则尝试使用 defaultValue）
     */
    public String getDecryptedValue(String configKey) {
        Optional<SystemConfig> configOpt = configRepository.findByConfigKey(configKey);
        if (configOpt.isEmpty()) {
            return null;
        }

        SystemConfig config = configOpt.get();
        String configValue = config.getConfigValue();

        // 如果 configValue 为空，尝试使用 defaultValue
        if (configValue == null || configValue.isEmpty()) {
            configValue = config.getDefaultValue();
            if (configValue == null || configValue.isEmpty()) {
                return null;
            }
        }

        if (Boolean.TRUE.equals(config.getIsSensitive())) {
            return decrypt(configValue);
        }

        return configValue;
    }

    /**
     * 获取数据库配置（用于创建 DataSource）
     *
     * @return 数据库配置Map
     */
    public Map<String, String> getDatabaseConfig() {
        Map<String, String> config = new HashMap<>();

        String url = getDecryptedValue("database.url");
        String username = getDecryptedValue("database.username");
        String password = getDecryptedValue("database.password");

        if (url != null && !url.isEmpty()) {
            config.put("url", url);
        }
        if (username != null && !username.isEmpty()) {
            config.put("username", username);
        }
        if (password != null && !password.isEmpty()) {
            config.put("password", password);
        }

        return config;
    }

    /**
     * 获取监听的数据库列表
     *
     * @return 数据库名称列表
     */
    public List<String> getMonitoredDatabases() {
        String json = getJson("database.monitored_dbs");
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyList();
        }

        try {
            // 简单的 JSON 解析（生产环境建议使用 Jackson/Gson）
            json = json.trim();
            if (json.startsWith("[") && json.endsWith("]")) {
                json = json.substring(1, json.length() - 1);
                String[] dbs = json.split(",");
                return Arrays.stream(dbs)
                        .map(String::trim)
                        .map(s -> s.replace("\"", "").replace("'", ""))
                        .filter(s -> !s.isEmpty())
                        .toList();
            }
        } catch (Exception e) {
            log.error("[配置服务] 解析监听数据库列表失败: {}", json, e);
        }

        return Collections.emptyList();
    }
}
