package com.dbdoctor.config;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * 主数据源配置类（H2）
 * 用于存储 DB-Doctor 的元数据（分析历史、SQL 指纹等）
 *
 * @author DB-Doctor
 * @version 2.0.0
 */
@Slf4j
@Configuration
public class PrimaryDataSourceConfig {

    /**
     * H2 数据源属性配置
     * 绑定到 spring.datasource.* 配置
     *
     * @return DataSourceProperties
     */
    @Bean
    @Primary
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSourceProperties primaryDataSourceProperties() {
        return new DataSourceProperties();
    }

    /**
     * 创建 H2 数据源（主数据源）
     * 使用 @Primary 确保这是默认的数据源
     *
     * @return DataSource
     */
    @Bean
    @Primary
    public DataSource primaryDataSource() {
        DataSourceProperties properties = primaryDataSourceProperties();

        log.info("🔗 初始化 H2 主数据源: url={}", properties.getUrl());

        return properties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }
}
