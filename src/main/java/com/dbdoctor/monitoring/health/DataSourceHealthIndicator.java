package com.dbdoctor.monitoring.health;

import com.dbdoctor.service.DatabaseInstanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

/**
 * 数据源健康指标检查器
 *
 * <p>检查数据库连接池和数据库连接状态</p>
 *
 * @author DB-Doctor
 * @version 3.2.0
 * @since 3.2.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSourceHealthIndicator implements HealthIndicator {

    private final DataSource dataSource;

    @Override
    public String getName() {
        return "datasource";
    }

    @Override
    public String getDisplayName() {
        return "数据源";
    }

    @Override
    public IndicatorStatus check() {
        try {
            Map<String, Object> details = new HashMap<>();

            // 尝试获取数据库连接
            try (Connection connection = dataSource.getConnection()) {
                boolean isValid = connection.isValid(5); // 5秒超时

                if (isValid) {
                    String databaseProductName = connection.getMetaData().getDatabaseProductName();
                    String databaseProductVersion = connection.getMetaData().getDatabaseProductVersion();
                    String url = connection.getMetaData().getURL();

                    details.put("database", databaseProductName);
                    details.put("version", databaseProductVersion);
                    details.put("url", url);
                    details.put("status", "UP");

                    return IndicatorStatus.healthy("数据库连接正常", details);
                } else {
                    details.put("status", "DOWN");
                    details.put("error", "连接验证失败");
                    return IndicatorStatus.unhealthy("数据库连接验证失败", details);
                }
            }
        } catch (Exception e) {
            log.error("[健康检查] 数据源健康检查失败", e);

            Map<String, Object> details = new HashMap<>();
            details.put("status", "DOWN");
            details.put("error", e.getMessage());

            return IndicatorStatus.unhealthy("数据库连接失败: " + e.getMessage(), details);
        }
    }
}
