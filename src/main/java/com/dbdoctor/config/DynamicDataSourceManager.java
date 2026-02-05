package com.dbdoctor.config;

import com.dbdoctor.check.MySqlEnvChecker;
import com.dbdoctor.common.util.EncryptionService;
import com.dbdoctor.entity.DatabaseInstance;
import com.dbdoctor.model.EnvCheckReport;
import com.dbdoctor.repository.DatabaseInstanceRepository;
import com.dbdoctor.service.SystemConfigService;
import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 动态数据源管理器
 *
 * <p>核心功能：</p>
 * <ul>
 *   <li>从 H2 数据库的 system_config 表读取目标数据库配置</li>
 *   <li>支持热更新：修改配置后立即生效，无需重启</li>
 *   <li>热更新前进行环境检查，确保配置正确</li>
 *   <li>线程安全的动态数据源切换</li>
 * </ul>
 *
 * @author DB-Doctor
 * @version 3.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicDataSourceManager {

    private final SystemConfigService configService;
    private final MySqlEnvChecker envChecker;
    private final DatabaseInstanceRepository databaseInstanceRepository;
    private final EncryptionService encryptionService;

    /**
     * 动态数据源的原子引用（线程安全）
     */
    private final AtomicReference<HikariDataSource> targetDataSource = new AtomicReference<>();
    private final AtomicReference<JdbcTemplate> targetJdbcTemplate = new AtomicReference<>();

    /**
     * 初始化动态数据源（从数据库读取配置）
     *
     * @return 初始化后的 JdbcTemplate
     */
    public JdbcTemplate initializeTargetDataSource() {
        log.info("🔄 [动态数据源] 开始从数据库读取目标数据库配置...");

        try {
            // 从数据库读取配置（支持两种方式）
            DatabaseConfig config = loadDatabaseConfig();
            if (config == null) {
                log.warn("⚠️  [动态数据源] 无法加载数据库配置，目标数据源未初始化");
                return null;
            }

            String url = config.url;
            String username = config.username;
            String password = config.password;

            // 验证必需配置
            if (url == null || url.trim().isEmpty()) {
                log.warn("⚠️  [动态数据源] database.url 配置为空，目标数据源未初始化");
                return null;
            }

            if (username == null || username.trim().isEmpty()) {
                log.warn("⚠️  [动态数据源] database.username 配置为空，目标数据源未初始化");
                return null;
            }

            if (password == null || password.trim().isEmpty()) {
                log.warn("⚠️  [动态数据源] database.password 配置为空，目标数据源未初始化");
                return null;
            }

            // 创建 HikariCP 数据源
            HikariDataSource dataSource = new HikariDataSource();
            dataSource.setJdbcUrl(url);
            dataSource.setUsername(username);
            dataSource.setPassword(password);
            dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");

            // 连接池配置（默认值）
            dataSource.setMaximumPoolSize(10);
            dataSource.setMinimumIdle(2);
            dataSource.setConnectionTimeout(30000); // 30秒
            dataSource.setIdleTimeout(600000); // 10分钟
            dataSource.setMaxLifetime(1800000); // 30分钟

            // 初始化数据源
            dataSource.getConnection().close(); // 测试连接

            // 保存到原子引用
            targetDataSource.set(dataSource);

            // 创建 JdbcTemplate
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            targetJdbcTemplate.set(jdbcTemplate);

            log.info("✅ [动态数据源] 目标数据源初始化成功");
            log.info("   JdbcTemplate hashCode: {}", jdbcTemplate.hashCode());
            log.info("   URL: {}", url);
            log.info("   Username: {}", username);

            return jdbcTemplate;

        } catch (Exception e) {
            log.error("❌ [动态数据源] 初始化目标数据源失败", e);
            log.warn("⚠️  [动态数据源] 目标数据源未初始化，部分功能可能无法使用");
            return null;
        }
    }

    /**
     * 获取当前的目标数据源
     *
     * @return HikariDataSource
     */
    public HikariDataSource getTargetDataSource() {
        return targetDataSource.get();
    }

    /**
     * 获取当前的 JdbcTemplate
     *
     * @return JdbcTemplate
     */
    public JdbcTemplate getTargetJdbcTemplate() {
        return targetJdbcTemplate.get();
    }

    /**
     * 热更新数据源（配置修改后调用）
     *
     * 新增：热更新前进行环境检查，确保配置正确
     *
     * @return 是否更新成功
     */
    public boolean reloadDataSource() {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🔄 [动态数据源] 收到热更新请求");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        // 记录旧的 JdbcTemplate hashCode
        JdbcTemplate oldJdbcTemplate = targetJdbcTemplate.get();
        if (oldJdbcTemplate != null) {
            log.info("📌 [动态数据源] 旧 JdbcTemplate hashCode: {}", oldJdbcTemplate.hashCode());
        } else {
            log.info("📌 [动态数据源] 旧 JdbcTemplate: null（数据源未初始化）");
        }

        try {
            // 1. 读取新配置（支持两种方式：database_instances 表或 system_config 表）
            String url, username, password;

            // 优先从 database_instances 表读取（新功能）
            String instanceIdStr = configService.getString("database.instance_id");
            if (instanceIdStr != null && !instanceIdStr.trim().isEmpty()) {
                try {
                    Long instanceId = Long.parseLong(instanceIdStr);
                    DatabaseInstance instance = databaseInstanceRepository.findById(instanceId).orElse(null);

                    if (instance != null) {
                        log.info("📋 [动态数据源] 从数据库实例加载配置: {}", instance.getInstanceName());
                        url = instance.getUrl();
                        username = instance.getUsername();
                        // 直接使用加密密码（checkFully 内部会解密）
                        password = instance.getPassword();

                        if (url == null || url.trim().isEmpty()) {
                            log.error("❌ [动态数据源] 数据库实例的 URL 为空: id={}", instanceId);
                            return false;
                        }
                    } else {
                        log.error("❌ [动态数据源] 数据库实例不存在: id={}", instanceId);
                        return false;
                    }
                } catch (NumberFormatException e) {
                    log.error("❌ [动态数据源] database.instance_id 格式错误: {}", instanceIdStr);
                    return false;
                }
            } else {
                // 兼容旧方式：从 system_config 表读取（获取加密值）
                log.info("📋 [动态数据源] 从 system_config 表加载配置");
                url = configService.getString("database.url");
                username = configService.getString("database.username");
                password = configService.getString("database.password"); // 获取加密值，不解密

                if (url == null || url.trim().isEmpty()) {
                    log.error("❌ [动态数据源] database.url 配置为空");
                    return false;
                }
            }

            // 2. 进行环境检查（热更新前强制检查）
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.info("🔍 [动态数据源] 热更新前进行环境检查...");
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            EnvCheckReport report = envChecker.checkFully(url, username, password);

            if (!report.isOverallPassed()) {
                log.error("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                log.error("❌ [动态数据源] 环境检查未通过，拒绝热更新");
                log.error("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

                // 打印未通过的检查项
                if (report.getItems() != null) {
                    report.getItems().stream()
                        .filter(item -> !item.isPassed())
                        .forEach(item -> {
                            log.error("❌ {} = {}", item.getName(), item.getCurrentValue());
                            log.error("   建议：{}", item.getErrorMessage());
                            if (item.getFixCommand() != null) {
                                log.error("   修复：{}", item.getFixCommand());
                            }
                        });
                }

                log.error("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                log.error("💡 [动态数据源] 请修复上述问题后重试");
                log.error("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                return false;
            }

            log.info("✅ [动态数据源] 环境检查全部通过，继续热更新...");

            // 3. 关闭旧数据源
            HikariDataSource oldDataSource = targetDataSource.get();
            if (oldDataSource != null && !oldDataSource.isClosed()) {
                log.info("🔌 [动态数据源] 关闭旧数据源...");
                oldDataSource.close();
                log.info("✅ [动态数据源] 旧数据源已关闭");
            }

            // 4. 重新初始化数据源
            JdbcTemplate newJdbcTemplate = initializeTargetDataSource();

            if (newJdbcTemplate != null) {
                log.info("✅ [动态数据源] 数据源热更新成功！配置已生效");
                log.info("📊 [动态数据源] JdbcTemplate 已更新:");
                log.info("   旧 hashCode: {}", oldJdbcTemplate != null ? oldJdbcTemplate.hashCode() : "null");
                log.info("   新 hashCode: {}", newJdbcTemplate.hashCode());
                log.info("   是否同一实例: {}", (oldJdbcTemplate == newJdbcTemplate));
                log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                return true;
            } else {
                log.warn("⚠️  [动态数据源] 数据源热更新失败：新数据源初始化失败");
                return false;
            }

        } catch (Exception e) {
            log.error("❌ [动态数据源] 数据源热更新失败", e);
            log.error("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            return false;
        }
    }

    /**
     * 检查数据源是否已初始化
     *
     * @return 是否已初始化
     */
    public boolean isInitialized() {
        return targetDataSource.get() != null;
    }

    /**
     * 从数据库加载配置（支持两种方式）
     * 1. 优先从 database_instances 表读取（新功能）
     * 2. 兼容从 system_config 表读取（旧功能）
     *
     * @return 数据库配置，如果无法加载则返回 null
     */
    private DatabaseConfig loadDatabaseConfig() {
        // 优先从 database_instances 表读取（新功能）
        String instanceIdStr = configService.getString("database.instance_id");
        if (instanceIdStr != null && !instanceIdStr.trim().isEmpty()) {
            try {
                Long instanceId = Long.parseLong(instanceIdStr);
                DatabaseInstance instance = databaseInstanceRepository.findById(instanceId).orElse(null);

                if (instance != null) {
                    log.info("📋 [动态数据源] 从数据库实例加载配置: {}", instance.getInstanceName());
                    // 解密密码（用于创建数据源）
                    String encryptedPassword = instance.getPassword();
                    String password = encryptedPassword != null ? encryptionService.decrypt(encryptedPassword) : null;

                    return new DatabaseConfig(instance.getUrl(), instance.getUsername(), password);
                } else {
                    log.error("❌ [动态数据源] 数据库实例不存在: id={}", instanceId);
                    return null;
                }
            } catch (NumberFormatException e) {
                log.error("❌ [动态数据源] database.instance_id 格式错误: {}", instanceIdStr);
                return null;
            }
        }

        // 兼容旧方式：从 system_config 表读取
        log.info("📋 [动态数据源] 从 system_config 表加载配置");
        String url = configService.getDecryptedValue("database.url");
        String username = configService.getDecryptedValue("database.username");
        String password = configService.getDecryptedValue("database.password");

        if (url != null && !url.trim().isEmpty()) {
            return new DatabaseConfig(url, username, password);
        }

        return null;
    }

    /**
     * 数据库配置封装类
     */
    private static class DatabaseConfig {
        final String url;
        final String username;
        final String password;

        DatabaseConfig(String url, String username, String password) {
            this.url = url;
            this.username = username;
            this.password = password;
        }
    }

    /**
     * 销毁数据源（应用关闭时调用）
     */
    public void destroy() {
        log.info("🛑 [动态数据源] 开始销毁动态数据源...");
        HikariDataSource dataSource = targetDataSource.get();
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            log.info("✅ [动态数据源] 数据源已销毁");
        }
    }
}
