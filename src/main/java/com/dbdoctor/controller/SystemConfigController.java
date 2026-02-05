package com.dbdoctor.controller;

import com.dbdoctor.common.Result;
import com.dbdoctor.config.DynamicDataSourceManager;
import com.dbdoctor.entity.SystemConfig;
import com.dbdoctor.service.SystemConfigService;
import com.dbdoctor.service.AiConfigManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统配置管理 Controller
 *
 * <p>提供配置管理的 REST API</p>
 *
 * @author DB-Doctor
 * @version 2.4.0
 * @since 2.4.0
 */
@Slf4j
@RestController
@RequestMapping("/api/system/config")
@RequiredArgsConstructor
public class SystemConfigController {

    private final SystemConfigService configService;
    private final DynamicDataSourceManager dynamicDataSourceManager;
    private final AiConfigManagementService aiConfigService;

    /**
     * 获取所有配置（按分组和显示顺序排序）
     *
     * @return 配置Map（前端期待的格式：{ configs: { "key": "value" } }）
     */
    @GetMapping
    public Result<Map<String, Object>> getAllConfigs() {
        List<SystemConfig> configList = configService.findAll();

        // 转换为前端期待的格式：{ configs: { "database.instance_id": "2", ... } }
        Map<String, String> configsMap = new HashMap<>();
        for (SystemConfig config : configList) {
            configsMap.put(config.getConfigKey(), config.getConfigValue());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("configs", configsMap);

        return Result.success(result);
    }

    /**
     * 根据分组获取配置
     *
     * @param group 配置分组（database/log/ai/notification/scheduler）
     * @return 配置列表
     */
    @GetMapping("/group/{group}")
    public Result<List<SystemConfig>> getConfigsByGroup(@PathVariable String group) {
        List<SystemConfig> configs = configService.findByGroup(group);
        return Result.success(configs);
    }

    /**
     * 获取单个配置
     *
     * @param configKey 配置键
     * @return 配置值
     */
    @GetMapping("/value/{configKey}")
    public Result<String> getConfigValue(@PathVariable String configKey) {
        String value = configService.getString(configKey);
        return Result.success(value);
    }

    /**
     * 更新配置
     *
     * @param requestBody 请求体（包含 configKey 和 configValue）
     * @return 操作结果
     */
    @PostMapping("/update")
    public Result<Void> updateConfig(@RequestBody Map<String, String> requestBody) {
        String configKey = requestBody.get("configKey");
        String configValue = requestBody.get("configValue");
        String updatedBy = requestBody.getOrDefault("updatedBy", "system");

        if (configKey == null || configKey.trim().isEmpty()) {
            return Result.error("配置键不能为空");
        }

        try {
            configService.updateConfig(configKey, configValue, updatedBy);
            return Result.success();
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("[配置API] 更新配置失败: {} = {}", configKey, configValue, e);
            return Result.error("配置更新失败: " + e.getMessage());
        }
    }

    /**
     * 批量更新配置
     *
     * @param requestBody 请求体（包含 configs Map 和 updatedBy）
     * @return 操作结果
     */
    @PostMapping("/batch-update")
    public Result<Map<String, Object>> batchUpdateConfigs(@RequestBody Map<String, Object> requestBody) {
        @SuppressWarnings("unchecked")
        Map<String, String> configs = (Map<String, String>) requestBody.get("configs");
        String updatedBy = (String) requestBody.getOrDefault("updatedBy", "system");

        if (configs == null || configs.isEmpty()) {
            return Result.error("配置列表不能为空");
        }

        try {
            // 检查是否更新了数据库配置
            boolean hasDatabaseConfig = configs.keySet().stream()
                .anyMatch(key -> key.startsWith("database.url") ||
                               key.startsWith("database.username") ||
                               key.startsWith("database.password") ||
                               key.startsWith("database.monitored_dbs"));

            // 检查是否更新了 AI 配置
            boolean hasAiConfig = configs.keySet().stream()
                .anyMatch(key -> key.startsWith("ai.") ||
                               key.startsWith("cost.") ||
                               key.startsWith("monitoring."));

            // 更新配置
            Map<String, Object> result = configService.batchUpdateConfigs(configs, updatedBy);

            // 热加载处理
            if (hasDatabaseConfig) {
                log.info("🔄 [热部署] 检测到数据库配置更新，开始热加载数据源...");
                boolean reloadSuccess = dynamicDataSourceManager.reloadDataSource();

                if (reloadSuccess) {
                    result.put("hotReload", true);
                    result.put("hotReloadMessage", "✅ 配置已保存，数据源已热更新，无需重启服务！");
                } else {
                    result.put("hotReload", false);
                    result.put("hotReloadMessage", "⚠️  配置已保存，但数据源热更新失败，请重启服务");
                }
            } else if (hasAiConfig) {
                log.info("🔄 [热部署] 检测到 AI 配置更新，刷新 AI 配置缓存...");
                aiConfigService.refreshCache();
                result.put("hotReload", true);
                result.put("hotReloadMessage", "✅ AI 配置已保存并热加载，无需重启服务！");
            } else {
                result.put("hotReload", false);
                result.put("hotReloadMessage", "ℹ️  配置已保存，无需重启");
            }

            return Result.success(result);
        } catch (RuntimeException e) {
            log.error("[配置API] 批量更新配置失败", e);
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("[配置API] 批量更新配置失败", e);
            return Result.error("配置更新失败: " + e.getMessage());
        }
    }

    /**
     * 测试数据库连接
     *
     * @param requestBody 请求体（包含 url, username, password）
     * @return 测试结果
     */
    @PostMapping("/test-database")
    public Result<Map<String, Object>> testDatabaseConnection(@RequestBody Map<String, String> requestBody) {
        String url = requestBody.get("url");
        String username = requestBody.get("username");
        String password = requestBody.get("password");

        if (url == null || url.trim().isEmpty()) {
            return Result.error("数据库URL不能为空");
        }
        if (username == null || username.trim().isEmpty()) {
            return Result.error("用户名不能为空");
        }

        Map<String, Object> result = configService.testDatabaseConnection(url, username, password);

        if ((Boolean) result.get("success")) {
            return Result.success(result);
        } else {
            return Result.error((String) result.get("message"));
        }
    }

    /**
     * 检查配置分组的完整性
     *
     * @param group 配置分组
     * @return 检查结果
     */
    @GetMapping("/check-completeness/{group}")
    public Result<Map<String, Object>> checkGroupCompleteness(@PathVariable String group) {
        Map<String, Object> result = configService.checkGroupCompleteness(group);
        return Result.success(result);
    }

    /**
     * 检查所有配置的完整性
     *
     * @return 检查结果
     */
    @GetMapping("/check-completeness")
    public Result<Map<String, Object>> checkAllCompleteness() {
        Map<String, Object> result = configService.checkAllCompleteness();
        return Result.success(result);
    }

    /**
     * 获取数据库配置（用于前端显示，脱敏处理）
     *
     * @return 数据库配置
     */
    @GetMapping("/database")
    public Result<Map<String, String>> getDatabaseConfig() {
        Map<String, String> config = configService.getDatabaseConfig();

        // 脱敏处理：密码完全不返回明文
        if (config.containsKey("password")) {
            String password = config.get("password");
            if (password != null && !password.isEmpty()) {
                // 不返回任何明文，只显示占位符
                config.put("password", "****");
            } else {
                config.remove("password");
            }
        }

        return Result.success(config);
    }

    /**
     * 获取监听的数据库列表
     *
     * @return 数据库名称列表
     */
    @GetMapping("/monitored-databases")
    public Result<List<String>> getMonitoredDatabases() {
        List<String> databases = configService.getMonitoredDatabases();
        return Result.success(databases);
    }

    /**
     * 检查系统是否已初始化（用于首次启动引导）
     *
     * @return 初始化状态
     */
    @GetMapping("/initialization-status")
    public Result<Map<String, Object>> getInitializationStatus() {
        Map<String, Object> allStatus = configService.checkAllCompleteness();
        @SuppressWarnings("unchecked")
        Map<String, Boolean> groups = (Map<String, Boolean>) allStatus.get("groups");

        Map<String, Object> result = new HashMap<>();

        // 数据库配置是必须的
        boolean databaseConfigured = groups.getOrDefault("database", false);
        result.put("databaseConfigured", databaseConfigured);

        // AI 配置是可选的
        boolean aiConfigured = groups.getOrDefault("ai", true); // 如果没有ai分组，认为已配置
        result.put("aiConfigured", aiConfigured);

        // 通知配置是可选的
        boolean notificationConfigured = groups.getOrDefault("notification", true);
        result.put("notificationConfigured", notificationConfigured);

        // 是否可以启动系统（至少数据库配置完成）
        boolean canStart = databaseConfigured;
        result.put("canStart", canStart);

        // 是否完全配置完成
        boolean fullyConfigured = (Boolean) allStatus.get("allComplete");
        result.put("fullyConfigured", fullyConfigured);

        return Result.success(result);
    }
}
