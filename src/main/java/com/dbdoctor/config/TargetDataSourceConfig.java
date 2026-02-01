package com.dbdoctor.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.util.Map;

/**
 * 用户目标数据源配置类（动态数据源）
 *
 * <p>从 H2 数据库的 system_config 表读取配置，支持热更新</p>
 *
 * 用途：
 * - 查询 mysql.slow_log 表
 * - 执行 EXPLAIN
 * - 查询 information_schema
 *
 * @author DB-Doctor
 * @version 3.0.0
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class TargetDataSourceConfig {

    private final DynamicDataSourceManager dynamicDataSourceManager;

    /**
     * 创建用户 MySQL 的 DataSource（动态）
     *
     * @return DataSource
     */
    @Bean("targetDataSource")
    @DependsOn("dynamicDataSourceManager") // 确保动态数据源管理器先初始化
    public DataSource targetDataSource() {
        log.info("🔗 [动态数据源] 开始初始化目标数据源 Bean...");

        // 从动态数据源管理器获取
        DataSource dataSource = dynamicDataSourceManager.getTargetDataSource();

        if (dataSource == null) {
            log.warn("⚠️  [动态数据源] 目标数据源未初始化（配置未完成），返回占位符数据源");
            // 返回一个占位符数据源（不会真正连接）
            return createPlaceholderDataSource();
        }

        return dataSource;
    }

    /**
     * 创建专属的 JdbcTemplate（动态委托类）
     *
     * <p>返回一个委托类，每次调用方法时都从 DynamicDataSourceManager 获取最新的 JdbcTemplate</p>
     *
     * @return DelegatingJdbcTemplate
     */
    @Bean("targetJdbcTemplate")
    @DependsOn("dynamicDataSourceManager")
    @Primary // 设置为主 JdbcTemplate
    public JdbcTemplate targetJdbcTemplate() {
        log.info("🔗 [动态数据源] 开始初始化目标 JdbcTemplate Bean（动态委托模式）...");

        // 创建一个委托 JdbcTemplate，每次方法调用都从 DynamicDataSourceManager 获取最新的
        JdbcTemplate realJdbcTemplate = dynamicDataSourceManager.getTargetJdbcTemplate();

        if (realJdbcTemplate != null) {
            log.info("✅ [动态数据源] 目标 JdbcTemplate 初始化完成（支持热更新）");
        } else {
            log.warn("⚠️  [动态数据源] 目标 JdbcTemplate 未初始化（配置未完成），返回委托 JdbcTemplate");
            realJdbcTemplate = new JdbcTemplate(createPlaceholderDataSource());
        }

        // 返回委托类
        return new DelegatingJdbcTemplate(dynamicDataSourceManager, realJdbcTemplate);
    }

    /**
     * 创建占位符数据源（用于配置未完成时）
     *
     * @return 占位符数据源
     */
    private DataSource createPlaceholderDataSource() {
        DriverManagerDataSource placeholderDataSource = new DriverManagerDataSource();
        placeholderDataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        placeholderDataSource.setUrl("jdbc:mysql://placeholder:3306/placeholder");
        placeholderDataSource.setUsername("placeholder");
        placeholderDataSource.setPassword("placeholder");
        return placeholderDataSource;
    }

    /**
     * 动态委托 JdbcTemplate
     *
     * <p>每次方法调用时都从 DynamicDataSourceManager 获取最新的 JdbcTemplate</p>
     * <p>只覆盖实际使用的方法，避免方法签名错误</p>
     */
    private static class DelegatingJdbcTemplate extends JdbcTemplate {

        private final DynamicDataSourceManager dynamicDataSourceManager;
        private JdbcTemplate currentDelegate;

        public DelegatingJdbcTemplate(DynamicDataSourceManager dynamicDataSourceManager, JdbcTemplate initialDelegate) {
            super(initialDelegate.getDataSource());
            this.dynamicDataSourceManager = dynamicDataSourceManager;
            this.currentDelegate = initialDelegate;
        }

        @Override
        public void execute(String sql) {
            getLatestDelegate().execute(sql);
        }

        @Override
        public int update(String sql, Object... args) {
            return getLatestDelegate().update(sql, args);
        }

        @Override
        public java.util.List<Map<String, Object>> queryForList(String sql) {
            return getLatestDelegate().queryForList(sql);
        }

        @Override
        public java.util.List<Map<String, Object>> queryForList(String sql, Object... args) {
            return getLatestDelegate().queryForList(sql, args);
        }

        @Override
        public <T> T queryForObject(String sql, Class<T> requiredType) {
            return getLatestDelegate().queryForObject(sql, requiredType);
        }

        /**
         * 获取最新的 JdbcTemplate 委托对象
         */
        private JdbcTemplate getLatestDelegate() {
            JdbcTemplate latest = dynamicDataSourceManager.getTargetJdbcTemplate();
            if (latest != null && latest != currentDelegate) {
                log.debug("🔄 [动态委托] 检测到 JdbcTemplate 更新，切换到最新实例");
                currentDelegate = latest;
            }
            return currentDelegate;
        }
    }
}
