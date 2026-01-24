package com.dbdoctor.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * 用户目标数据源配置类
 * 配置用户的 MySQL 数据源（只读访问）
 *
 * 用途：
 * - 查询 mysql.slow_log 表
 * - 执行 EXPLAIN
 * - 查询 information_schema
 *
 * @author DB-Doctor
 * @version 2.0.0
 */
@Slf4j
@Configuration
public class TargetDataSourceConfig {

    /**
     * 读取配置文件里 db-doctor.target-db 开头的配置
     *
     * @return DataSource 配置属性
     */
    @Bean
    @ConfigurationProperties(prefix = "db-doctor.target-db")
    public DataSourceProperties targetDataSourceProperties() {
        return new DataSourceProperties();
    }

    /**
     * 创建用户 MySQL 的 DataSource
     *
     * @return DataSource
     */
    @Bean("targetDataSource")
    public DataSource targetDataSource() {
        DataSourceProperties properties = targetDataSourceProperties();

        log.info("🔗 初始化用户目标数据源: url={}, username={}",
                properties.getUrl(), properties.getUsername());

        return DataSourceBuilder.create()
                .url(properties.getUrl())
                .username(properties.getUsername())
                .password(properties.getPassword())
                .driverClassName(properties.getDriverClassName())
                .build();
    }

    /**
     * 创建专属的 JdbcTemplate
     * 以后要查用户的库，就注入这个 Bean
     *
     * @param targetDataSource 用户 MySQL 数据源
     * @return JdbcTemplate
     */
    @Bean("targetJdbcTemplate")
    public JdbcTemplate targetJdbcTemplate(
            @Qualifier("targetDataSource") DataSource targetDataSource
    ) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(targetDataSource);
        log.info("✅ 用户目标 JdbcTemplate 初始化完成");
        return jdbcTemplate;
    }

    /**
     * DataSource 配置属性类
     */
    public static class DataSourceProperties {
        private String url;
        private String username;
        private String password;
        private String driverClassName;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getDriverClassName() {
            return driverClassName;
        }

        public void setDriverClassName(String driverClassName) {
            this.driverClassName = driverClassName;
        }

        public DataSource initializeDataSourceBuilder() {
            return DataSourceBuilder.create()
                    .url(url)
                    .username(username)
                    .password(password)
                    .driverClassName(driverClassName)
                    .build();
        }
    }
}
