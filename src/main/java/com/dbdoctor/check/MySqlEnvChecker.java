package com.dbdoctor.check;

import com.dbdoctor.common.util.EncryptionService;
import com.dbdoctor.model.EnvCheckReport;
import com.dbdoctor.model.EnvCheckReport.CheckItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * MySQL 环境检查器（增强版）
 *
 * 核心功能：
 * 1. 连接测试（基础连接验证）
 * 2. 完整环境检查（4项必选检查）
 * 3. 详细错误报告（含修复命令）
 *
 * 使用场景：
 * - 前端"测试连接"按钮
 * - 配置保存前的验证
 * - 应用启动时的环境检查
 *
 * @author DB-Doctor
 * @version 3.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MySqlEnvChecker {

    private final EncryptionService encryptionService;

    /**
     * 执行完整的环境检查（含连接测试）
     *
     * @param url      JDBC URL
     * @param username 用户名
     * @param password 明文密码（调用方已解密）
     * @return 环境检查报告
     */
    public EnvCheckReport checkFully(String url, String username, String password) {
        log.info("========================================");
        log.info("🔍 开始完整环境检查...");
        log.info("========================================");
        log.info("URL: {}", url);
        log.info("Username: {}", username);

        EnvCheckReport report = EnvCheckReport.builder()
            .items(new ArrayList<>())
            .build();

        try {
            // 1. 基础连接测试
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.info("1️⃣  测试数据库连接...");
            JdbcTemplate testJdbcTemplate = testConnection(url, username, password, report);
            report.setConnectionSuccess(true);
            log.info("✅ 数据库连接成功");

            // 1.5 查询所有可用数据库
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.info("1️⃣.5️⃣  查询可用数据库列表...");
            List<String> databases = queryAvailableDatabases(testJdbcTemplate);
            report.setAvailableDatabases(databases);
            log.info("✅ 已加载 {} 个数据库", databases.size());

            // 2. 检查 slow_query_log
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.info("2️⃣  检查 slow_query_log...");
            checkSlowQueryLog(testJdbcTemplate, report);

            // 3. 检查 log_output
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.info("3️⃣  检查 log_output...");
            checkLogOutput(testJdbcTemplate, report);

            // 4. 检查 long_query_time
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.info("4️⃣  检查 long_query_time...");
            checkLongQueryTime(testJdbcTemplate, report);

            // 5. 检查 slow_log 表访问权限
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.info("5️⃣  检查 mysql.slow_log 访问权限...");
            checkSlowLogTableAccess(testJdbcTemplate, report);

            // 6. 生成总结
            generateSummary(report);

            log.info("========================================");
            log.info("📋 环境检查完成");
            log.info("========================================");

            return report;

        } catch (Exception e) {
            log.error("❌ 环境检查失败", e);
            report.setConnectionSuccess(false);
            report.setStatus(EnvCheckReport.CheckStatus.CRITICAL);
            report.setSummary("环境检查失败：" + e.getMessage());
            return report;
        }
    }

    /**
     * 测试数据库连接
     */
    private JdbcTemplate testConnection(String url, String username, String password, EnvCheckReport report) {
        try {
            // 尝试解密密码（如果是密文则解密，如果是明文则保持不变）
            String actualPassword = encryptionService.decrypt(password);
            log.debug("密码处理完成，长度: {}", actualPassword != null ? actualPassword.length() : 0);

            // 创建临时数据源测试连接
            com.zaxxer.hikari.HikariDataSource dataSource = new com.zaxxer.hikari.HikariDataSource();
            dataSource.setJdbcUrl(url);
            dataSource.setUsername(username);
            dataSource.setPassword(actualPassword); // 使用解密后的密码
            dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
            dataSource.setConnectionTimeout(10000); // 10秒超时

            // 测试连接
            dataSource.getConnection().close();

            report.setConnectionSuccess(true);
            return new JdbcTemplate(dataSource);

        } catch (SQLException e) {
            report.setConnectionSuccess(false);
            report.setConnectionError(parseConnectionError(e));
            report.setStatus(EnvCheckReport.CheckStatus.CRITICAL);
            report.setSummary("数据库连接失败：" + parseConnectionError(e));
            throw new RuntimeException("数据库连接失败", e);
        }
    }

    /**
     * 解析连接错误（用户友好的错误信息）
     */
    private String parseConnectionError(SQLException e) {
        String message = e.getMessage();
        log.debug("原始 SQL 异常: {}", message);

        // 常见错误码
        if (message.contains("Access denied") || message.contains("1045")) {
            return "用户名或密码错误，请检查连接配置";
        }

        if (message.contains("Unknown database") || message.contains("1049")) {
            return "数据库不存在，请检查数据库名称";
        }

        if (message.contains("Communications link failure") || message.contains("08S01")) {
            return "无法连接到数据库服务器，请检查：\n" +
                   "1. 数据库服务是否启动\n" +
                   "2. 主机地址和端口是否正确\n" +
                   "3. 防火墙是否阻止连接";
        }

        if (message.contains("Connection timed out") || message.contains("SQLTimeoutException")) {
            return "连接超时，请检查网络连接和数据库状态";
        }

        if (message.contains("Invalid connection string")) {
            return "JDBC URL 格式错误，正确格式：jdbc:mysql://host:port/database";
        }

        // 默认错误
        return "连接失败: " + message;
    }

    /**
     * 检查 slow_query_log
     */
    private void checkSlowQueryLog(JdbcTemplate jdbcTemplate, EnvCheckReport report) {
        try {
            String value = queryVariable(jdbcTemplate, "slow_query_log");
            boolean isEnabled = "ON".equalsIgnoreCase(value);

            CheckItem item = CheckItem.builder()
                .name(CheckItem.Constants.SLOW_QUERY_LOG)
                .required(true)
                .passed(isEnabled)
                .currentValue(value)
                .build();

            if (isEnabled) {
                item.setErrorMessage("慢查询日志已启用");
                log.info("✅ slow_query_log = ON");
            } else {
                item.setErrorMessage("慢查询日志未启用，DB-Doctor 无法捕获慢查询");
                item.setFixCommand("SET GLOBAL slow_query_log = 'ON';");
                item.setErrorCode(CheckItem.Constants.ERROR_CODE_NOT_ENABLED);
                item.setHelpUrl("https://dev.mysql.com/doc/refman/8.0/en/slow-query-log.html");
                log.warn("❌ slow_query_log = {}", value);
            }

            report.addItem(item);

        } catch (Exception e) {
            log.error("检查 slow_query_log 失败", e);
            report.addItem(CheckItem.builder()
                .name(CheckItem.Constants.SLOW_QUERY_LOG)
                .required(true)
                .passed(false)
                .errorMessage("检查失败：" + e.getMessage())
                .build());
        }
    }

    /**
     * 检查 log_output
     */
    private void checkLogOutput(JdbcTemplate jdbcTemplate, EnvCheckReport report) {
        try {
            String value = queryVariable(jdbcTemplate, "log_output");
            boolean containsTable = value != null && value.contains("TABLE");

            CheckItem item = CheckItem.builder()
                .name(CheckItem.Constants.LOG_OUTPUT)
                .required(true)
                .passed(containsTable)
                .currentValue(value)
                .build();

            if (containsTable) {
                item.setErrorMessage("日志输出方式支持 TABLE");
                log.info("✅ log_output = {} (包含 TABLE)", value);
            } else {
                item.setErrorMessage("log_output 不包含 TABLE，无法从 mysql.slow_log 表读取数据");
                item.setFixCommand("SET GLOBAL log_output = 'TABLE';");
                item.setErrorCode(CheckItem.Constants.ERROR_CODE_NO_TABLE);
                item.setHelpUrl("https://dev.mysql.com/doc/refman/8.0/en/slow-query-log.html");
                log.warn("❌ log_output = {} (不包含 TABLE)", value);
            }

            report.addItem(item);

        } catch (Exception e) {
            log.error("检查 log_output 失败", e);
            report.addItem(CheckItem.builder()
                .name(CheckItem.Constants.LOG_OUTPUT)
                .required(true)
                .passed(false)
                .errorMessage("检查失败：" + e.getMessage())
                .build());
        }
    }

    /**
     * 检查 long_query_time
     */
    private void checkLongQueryTime(JdbcTemplate jdbcTemplate, EnvCheckReport report) {
        try {
            String value = queryVariable(jdbcTemplate, "long_query_time");
            double threshold = Double.parseDouble(value);

            CheckItem.CheckItemBuilder itemBuilder = CheckItem.builder()
                .name(CheckItem.Constants.LONG_QUERY_TIME)
                .required(true)
                .currentValue(value + " 秒");

            // 合理范围：0.1 - 10 秒
            if (threshold >= 0.1 && threshold <= 10.0) {
                itemBuilder.passed(true)
                    .errorMessage("慢查询阈值设置合理");
                log.info("✅ long_query_time = {} 秒 (合理)", value);
            } else {
                itemBuilder.passed(false)
                    .errorMessage(String.format("慢查询阈值不合理（%.2f秒），建议设置为 1-2 秒", threshold))
                    .fixCommand("SET GLOBAL long_query_time = 1.0;")
                    .errorCode(CheckItem.Constants.ERROR_CODE_THRESHOLD);
                log.warn("⚠️  long_query_time = {} 秒 (不合理)", value);
            }

            report.addItem(itemBuilder.build());

        } catch (Exception e) {
            log.error("检查 long_query_time 失败", e);
            report.addItem(CheckItem.builder()
                .name(CheckItem.Constants.LONG_QUERY_TIME)
                .required(true)
                .passed(false)
                .errorMessage("检查失败：" + e.getMessage())
                .build());
        }
    }

    /**
     * 检查 slow_log 表访问权限
     */
    private void checkSlowLogTableAccess(JdbcTemplate jdbcTemplate, EnvCheckReport report) {
        try {
            // 尝试查询表是否存在
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'mysql' AND TABLE_NAME = 'slow_log'",
                Integer.class);

            if (count == null || count == 0) {
                // 表不存在
                report.addItem(CheckItem.builder()
                    .name(CheckItem.Constants.SLOW_LOG_ACCESS)
                    .required(true)
                    .passed(false)
                    .currentValue("表不存在")
                    .errorMessage("mysql.slow_log 表不存在，可能是 log_output 未设置为 TABLE")
                    .fixCommand("SET GLOBAL log_output = 'TABLE';")
                    .errorCode(CheckItem.Constants.ERROR_CODE_NO_PERMISSION)
                    .build());
                log.warn("❌ mysql.slow_log 表不存在");
                return;
            }

            // 表存在，尝试读取
            jdbcTemplate.queryForMap("SELECT * FROM mysql.slow_log LIMIT 1");

            report.addItem(CheckItem.builder()
                .name(CheckItem.Constants.SLOW_LOG_ACCESS)
                .required(true)
                .passed(true)
                .currentValue("有权限")
                .errorMessage("可以读取 mysql.slow_log 表")
                .build());
            log.info("✅ mysql.slow_log 访问权限正常");

        } catch (DataAccessException e) {
            log.error("检查 mysql.slow_log 访问权限失败", e);
            report.addItem(CheckItem.builder()
                .name(CheckItem.Constants.SLOW_LOG_ACCESS)
                .required(true)
                .passed(false)
                .currentValue("无权限")
                .errorMessage("无法读取 mysql.slow_log 表：" + e.getMessage())
                .fixCommand("GRANT SELECT ON mysql.slow_log TO 'your_user'@'your_host';")
                .errorCode(CheckItem.Constants.ERROR_CODE_NO_PERMISSION)
                .build());
        }
    }

    /**
     * 生成总结信息
     */
    private void generateSummary(EnvCheckReport report) {
        if (!report.isOverallPassed()) {
            report.setStatus(EnvCheckReport.CheckStatus.CRITICAL);
            report.setSummary("环境检查未通过，请修复以下问题后重试");
            return;
        }

        // 检查是否有警告项（通过但有建议）
        boolean hasWarnings = report.getItems().stream()
            .anyMatch(item -> !item.isPassed() && !item.isRequired());

        if (hasWarnings) {
            report.setStatus(EnvCheckReport.CheckStatus.FAILED);
            report.setSummary("环境检查通过，但有一些建议优化项");
        } else {
            report.setStatus(EnvCheckReport.CheckStatus.PASSED);
            report.setSummary("环境检查全部通过，可以正常使用");
        }
    }

    /**
     * 查询 MySQL 系统变量
     */
    private String queryVariable(JdbcTemplate jdbcTemplate, String varName) {
        return jdbcTemplate.queryForObject(
            "SHOW VARIABLES LIKE ?",
            (rs, rowNum) -> rs.getString("Value"),
            varName);
    }

    /**
     * 查询所有可用数据库
     */
    private List<String> queryAvailableDatabases(JdbcTemplate jdbcTemplate) {
        try {
            List<String> databases = jdbcTemplate.queryForList(
                "SHOW DATABASES",
                String.class
            );
            log.debug("查询到 {} 个数据库", databases.size());
            return databases;
        } catch (Exception e) {
            log.error("查询数据库列表失败", e);
            return new ArrayList<>();
        }
    }
}
