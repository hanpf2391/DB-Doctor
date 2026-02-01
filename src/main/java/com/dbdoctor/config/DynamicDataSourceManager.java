package com.dbdoctor.config;

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
            // 从 H2 数据库读取配置
            String url = configService.getDecryptedValue("database.url");
            String username = configService.getDecryptedValue("database.username");
            String password = configService.getDecryptedValue("database.password");

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
     * @return 是否更新成功
     */
    public boolean reloadDataSource() {
        log.info("🔄 [动态数据源] 收到热更新请求，开始重新加载数据源...");

        try {
            // 关闭旧数据源
            HikariDataSource oldDataSource = targetDataSource.get();
            if (oldDataSource != null && !oldDataSource.isClosed()) {
                log.info("🔌 [动态数据源] 关闭旧数据源...");
                oldDataSource.close();
                log.info("✅ [动态数据源] 旧数据源已关闭");
            }

            // 重新初始化数据源
            JdbcTemplate newJdbcTemplate = initializeTargetDataSource();

            if (newJdbcTemplate != null) {
                log.info("✅ [动态数据源] 数据源热更新成功！配置已生效");
                return true;
            } else {
                log.warn("⚠️  [动态数据源] 数据源热更新失败：新数据源初始化失败");
                return false;
            }

        } catch (Exception e) {
            log.error("❌ [动态数据源] 数据源热更新失败", e);
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
