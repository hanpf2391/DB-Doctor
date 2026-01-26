package com.dbdoctor.check;

import com.dbdoctor.config.DbDoctorProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * MySQL 环境检查器
 * 启动时检查目标 MySQL 的慢查询配置是否符合 DB-Doctor 运行要求
 *
 * 核心功能：
 * 1. 启动时执行完整检查（生成详细报告）
 * 2. 运行时提供快速检查（轻量级，供监控线程调用）
 * 3. 动态感知环境变化，自动恢复监控
 *
 * 使用方式：
 * - 在 application.yml 中配置 db-doctor.env-check.enabled=true
 * - 配置检查失败后的处理策略（fail-on-error）
 * - 启动项目即可自动检查环境
 *
 * @author DB-Doctor
 * @version 2.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "db-doctor.env-check", name = "enabled", havingValue = "true")
public class MySqlEnvChecker implements ApplicationRunner {

    private final DbDoctorProperties properties;
    private final JdbcTemplate jdbcTemplate;

    private final List<CheckResult> checkResults = new ArrayList<>();

    /**
     * 缓存环境健康状态（避免每次查询数据库）
     */
    private final AtomicBoolean isHealthy = new AtomicBoolean(false);

    @Override
    public void run(ApplicationArguments args) {
        log.info("========================================");
        log.info("🚀 开始 MySQL 环境准入检测...");
        log.info("========================================");

        // 清空上次检查结果
        checkResults.clear();

        // 执行各项检查
        checkSlowQueryLog();
        checkLogOutput();
        checkLongQueryTime();
        checkSlowLogTableAccess();

        // 生成诊断报告
        generateReport();

        // 根据检查结果决定是否阻止启动
        handleCheckResult();
    }

    /**
     * 检查 slow_query_log 是否开启
     */
    private void checkSlowQueryLog() {
        try {
            String value = queryVariable("slow_query_log");
            boolean isEnabled = "ON".equalsIgnoreCase(value);

            if (isEnabled) {
                addCheckResult("slow_query_log", CheckStatus.PASS, "已开启",
                    "慢查询日志已启用，可以捕获慢查询");
            } else {
                addCheckResult("slow_query_log", CheckStatus.FAIL, "未开启",
                    "慢查询日志未启用！DB-Doctor 无法捕获慢查询。\n修复命令：SET GLOBAL slow_query_log = 'ON';");
            }
        } catch (Exception e) {
            addCheckResult("slow_query_log", CheckStatus.ERROR, "检查失败",
                "无法查询 slow_query_log 状态：" + e.getMessage());
        }
    }

    /**
     * 检查 log_output 是否包含 TABLE
     */
    private void checkLogOutput() {
        try {
            String value = queryVariable("log_output");
            boolean containsTable = value != null && value.contains("TABLE");

            if (containsTable) {
                addCheckResult("log_output", CheckStatus.PASS, value,
                    "日志输出方式支持 TABLE，可以从 mysql.slow_log 表读取");
            } else {
                addCheckResult("log_output", CheckStatus.FAIL, value,
                    "log_output 不包含 TABLE！DB-Doctor 需要从 mysql.slow_log 表读取数据。\n" +
                    "修复命令：SET GLOBAL log_output = 'TABLE';\n" +
                    "注意：如果使用云数据库（RDS），请前往控制台参数设置页面修改。");
            }
        } catch (Exception e) {
            addCheckResult("log_output", CheckStatus.ERROR, "检查失败",
                "无法查询 log_output 状态：" + e.getMessage());
        }
    }

    /**
     * 检查 long_query_time 是否合理
     */
    private void checkLongQueryTime() {
        try {
            String value = queryVariable("long_query_time");
            double threshold = Double.parseDouble(value);

            // 警告阈值：超过 10 秒认为不合理
            if (threshold > 10.0) {
                addCheckResult("long_query_time", CheckStatus.WARN, value + " 秒",
                    "慢查询阈值过高（" + threshold + "秒），可能捕获不到有价值的慢查询。\n" +
                    "建议设置为 1-2 秒。\n" +
                    "修复命令：SET GLOBAL long_query_time = 1.0;");
            } else if (threshold < 0.1) {
                addCheckResult("long_query_time", CheckStatus.WARN, value + " 秒",
                    "慢查询阈值过低（" + threshold + "秒），可能产生大量日志。\n" +
                    "建议设置为 1-2 秒。\n" +
                    "修复命令：SET GLOBAL long_query_time = 1.0;");
            } else {
                addCheckResult("long_query_time", CheckStatus.PASS, value + " 秒",
                    "慢查询阈值设置合理");
            }
        } catch (Exception e) {
            addCheckResult("long_query_time", CheckStatus.ERROR, "检查失败",
                "无法查询 long_query_time 状态：" + e.getMessage());
        }
    }

    /**
     * 检查是否具有读取 mysql.slow_log 表的权限
     */
    private void checkSlowLogTableAccess() {
        try {
            // 尝试查询慢查询日志表
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'mysql' AND TABLE_NAME = 'slow_log'",
                Integer.class);

            if (count != null && count > 0) {
                // 表存在，尝试读取
                jdbcTemplate.queryForMap("SELECT * FROM mysql.slow_log LIMIT 1");
                addCheckResult("mysql.slow_log 访问权限", CheckStatus.PASS, "有权限",
                    "可以读取 mysql.slow_log 表");
            } else {
                addCheckResult("mysql.slow_log 访问权限", CheckStatus.WARN, "表不存在",
                    "mysql.slow_log 表不存在或无法访问\n" +
                    "可能原因：\n" +
                    "1. MySQL 版本不支持表模式慢查询日志\n" +
                    "2. log_output 未设置为 TABLE");
            }
        } catch (Exception e) {
            addCheckResult("mysql.slow_log 访问权限", CheckStatus.FAIL, "无权限",
                "无法读取 mysql.slow_log 表：" + e.getMessage() + "\n" +
                "可能原因：\n" +
                "1. 当前数据库用户没有 SELECT 权限\n" +
                "2. log_output 未设置为 TABLE\n" +
                "解决方案：\n" +
                "1. 授予 SELECT 权限：GRANT SELECT ON mysql.slow_log TO 'your_user'@'your_host';\n" +
                "2. 设置 log_output = 'TABLE'");
        }
    }

    /**
     * 生成诊断报告
     */
    private void generateReport() {
        log.info("");
        log.info("========================================");
        log.info("📋 环境检查报告");
        log.info("========================================");

        int passCount = 0;
        int warnCount = 0;
        int failCount = 0;
        int errorCount = 0;

        for (CheckResult result : checkResults) {
            switch (result.status()) {
                case PASS -> {
                    log.info("✅ PASS | {} | {}", result.item(), result.value());
                    passCount++;
                }
                case WARN -> {
                    log.warn("⚠️  WARN | {} | {}", result.item(), result.value());
                    log.warn("   建议：{}", result.suggestion());
                    warnCount++;
                }
                case FAIL -> {
                    log.error("❌ FAIL | {} | {}", result.item(), result.value());
                    log.error("   建议：{}", result.suggestion());
                    failCount++;
                }
                case ERROR -> {
                    log.error("🔥 ERROR | {} | {}", result.item(), result.value());
                    log.error("   错误：{}", result.suggestion());
                    errorCount++;
                }
            }
        }

        log.info("========================================");
        log.info("检查结果：通过 {}，警告 {}，失败 {}，错误 {}",
            passCount, warnCount, failCount, errorCount);
        log.info("========================================");
        log.info("");
    }

    /**
     * 根据检查结果决定是否阻止启动
     */
    private void handleCheckResult() {
        boolean hasFail = checkResults.stream().anyMatch(r -> r.status() == CheckStatus.FAIL);
        boolean hasError = checkResults.stream().anyMatch(r -> r.status() == CheckStatus.ERROR);

        if (hasFail || hasError) {
            boolean failOnError = properties.getEnvCheck().getFailOnError();

            if (failOnError) {
                log.error("========================================");
                log.error("❌ 环境检查未通过，应用启动终止！");
                log.error("========================================");
                log.error("");
                log.error("💡 快速修复指南：");
                log.error("1. 手动执行上述修复命令（需要 SUPER 权限）");
                log.error("2. 或在配置文件中设置 db-doctor.env-check.fail-on-error=false");
                log.error("3. 或配置 db-doctor.env-check.auto-fix=true（尝试自动修复）");
                log.error("");

                throw new RuntimeException("MySQL 环境检查未通过，应用启动终止");
            } else {
                log.warn("========================================");
                log.warn("⚠️  环境检查未通过，但应用继续启动（fail-on-error=false）");
                log.warn("⚠️  慢查询监控功能可能无法正常工作！");
                log.warn("========================================");
            }
        } else {
            log.info("========================================");
            log.info("✅ 环境检查通过，DB-Doctor 可以正常工作！");
            log.info("========================================");
        }
    }

    /**
     * 查询 MySQL 系统变量
     */
    private String queryVariable(String varName) {
        return jdbcTemplate.queryForObject(
            "SHOW VARIABLES LIKE '" + varName + "'",
            (rs, rowNum) -> rs.getString("Value"));
    }

    /**
     * 添加检查结果
     */
    private void addCheckResult(String item, CheckStatus status, String value, String suggestion) {
        checkResults.add(new CheckResult(item, status, value, suggestion));
    }

    // ========================================
    // 运行时快速检查方法（供监控线程调用）
    // ========================================

    /**
     * 快速检查环境是否健康（轻量级）
     * 供 SlowLogTableMonitor 在每次轮询前调用
     *
     * @return true-环境健康可以监控，false-环境不健康跳过本次监控
     */
    public boolean checkQuickly() {
        try {
            // 只检查最核心的两个指标（轻量级查询）
            String slowQueryLog = queryVariable("slow_query_log");
            String logOutput = queryVariable("log_output");

            boolean isSlowLogOn = "ON".equalsIgnoreCase(slowQueryLog);
            boolean isTableMode = logOutput != null && logOutput.contains("TABLE");

            boolean healthy = isSlowLogOn && isTableMode;

            // 更新缓存
            boolean oldValue = isHealthy.getAndSet(healthy);

            // 如果状态发生变化，打印日志
            if (oldValue != healthy) {
                if (healthy) {
                    log.info("========================================");
                    log.info("🎉 环境已恢复健康！慢查询监控自动激活");
                    log.info("========================================");
                } else {
                    log.warn("========================================");
                    log.warn("⚠️  环境状态变化：从不健康转为健康，或从健康转为不健康");
                    log.warn("⚠️  slow_query_log: {}", slowQueryLog);
                    log.warn("⚠️  log_output: {}", logOutput);
                    log.warn("========================================");
                }
            }

            return healthy;

        } catch (Exception e) {
            log.debug("快速检查环境失败: {}", e.getMessage());
            isHealthy.set(false);
            return false;
        }
    }

    /**
     * 获取当前环境健康状态（缓存值）
     */
    public boolean isHealthy() {
        return isHealthy.get();
    }

    /**
     * 获取环境诊断信息（用于日志输出）
     *
     * @return 诊断信息字符串
     */
    public String getDiagnosticInfo() {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("环境诊断: ");

            String slowQueryLog = queryVariable("slow_query_log");
            String logOutput = queryVariable("log_output");

            if (!"ON".equalsIgnoreCase(slowQueryLog)) {
                sb.append("slow_query_log=").append(slowQueryLog).append(" (未开启)");
            }

            if (logOutput == null || !logOutput.contains("TABLE")) {
                if (sb.length() > 5) sb.append(" | ");
                sb.append("log_output=").append(logOutput).append(" (未包含TABLE)");
            }

            if (sb.length() == 5) {
                return "环境健康";
            }

            return sb.toString();

        } catch (Exception e) {
            return "无法获取环境信息: " + e.getMessage();
        }
    }

    /**
     * 检查状态枚举
     */
    private enum CheckStatus {
        PASS,   // 通过
        WARN,   // 警告
        FAIL,   // 失败
        ERROR   // 错误
    }

    /**
     * 检查结果记录
     */
    private record CheckResult(
        String item,          // 检查项
        CheckStatus status,   // 状态
        String value,         // 当前值
        String suggestion     // 建议
    ) {}
}
