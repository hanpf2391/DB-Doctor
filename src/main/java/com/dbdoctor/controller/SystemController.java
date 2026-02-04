package com.dbdoctor.controller;

import com.dbdoctor.config.DataSourceStatusHolder;
import com.dbdoctor.repository.SlowQuerySampleRepository;
import com.dbdoctor.repository.SlowQueryTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统信息控制器
 *
 * @author DB-Doctor
 * @version 2.2.0
 */
@Slf4j
@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
public class SystemController {

    private final SlowQueryTemplateRepository templateRepository;
    private final SlowQuerySampleRepository sampleRepository;
    private final DataSourceStatusHolder dataSourceStatusHolder;
    private final com.dbdoctor.config.DynamicDataSourceManager dynamicDataSourceManager;

    /**
     * 目标 MySQL 数据源的 JdbcTemplate（动态委托）
     * 使用 @Qualifier 明确指定使用 targetJdbcTemplate，而不是 H2 的 JdbcTemplate
     */
    @org.springframework.beans.factory.annotation.Qualifier("targetJdbcTemplate")
    private final JdbcTemplate targetJdbcTemplate;

    @Value("${db-doctor.version:2.2.0}")
    private String version;

    @Value("${db-doctor.build-time:未知}")
    private String buildTime;

    @Value("${git.commit.id.abbrev:unknown}")
    private String gitCommit;

    /**
     * 获取系统信息
     *
     * @return 系统信息
     */
    @GetMapping("/info")
    public Map<String, Object> getSystemInfo() {
        return Map.of(
                "code", 200,
                "message", "success",
                "data", Map.of(
                        "version", "v" + version,
                        "buildTime", buildTime,
                        "gitCommit", gitCommit
                )
        );
    }

    /**
     * 获取今日概览统计
     *
     * @return 今日统计数据
     */
    @GetMapping("/overview")
    public Map<String, Object> getTodayOverview() {
        log.info("查询今日概览统计");

        // 今日分析总数（所有模板）
        long totalTemplates = templateRepository.count();

        // SQL样本总数
        long totalSqlSamples = sampleRepository.count();

        // 高危 SQL 数（严重级别）
        long highRiskCount = templateRepository.countBySeverityLevel(
                com.dbdoctor.common.enums.SeverityLevel.CRITICAL
        );

        // 待分析任务数（状态为 PENDING）
        long pendingTasks = templateRepository.countByStatus(
                com.dbdoctor.entity.SlowQueryTemplate.AnalysisStatus.PENDING
        );

        // 平均耗时
        double avgQueryTime = 0.0;
        // TODO: 计算平均耗时

        Map<String, Object> data = new HashMap<>();
        data.put("templateTotal", totalTemplates);
        data.put("sqlTotal", totalSqlSamples);
        data.put("todayTotal", totalTemplates);
        data.put("highRiskCount", highRiskCount);
        data.put("avgQueryTime", avgQueryTime);
        data.put("pendingTasks", pendingTasks);
        data.put("date", LocalDate.now().toString());

        return Map.of(
                "code", 200,
                "message", "success",
                "data", data
        );
    }

    /**
     * 获取模板-SQL关联统计
     *
     * @return 模板及其对应的SQL样本数量
     */
    @GetMapping("/template-sql-stats")
    public Map<String, Object> getTemplateSqlStats() {
        log.info("查询模板-SQL关联统计");

        // 查询所有模板
        var templates = templateRepository.findAll();

        // 构建模板-SQL统计列表
        var stats = templates.stream()
                .map(template -> {
                    long sqlCount = sampleRepository.countBySqlFingerprint(
                            template.getSqlFingerprint()
                    );

                    Map<String, Object> stat = new HashMap<>();
                    stat.put("id", template.getId());
                    stat.put("fingerprint", template.getSqlFingerprint());
                    stat.put("dbName", template.getDbName() != null ? template.getDbName() : "");
                    stat.put("tableName", template.getTableName() != null ? template.getTableName() : "");
                    stat.put("sqlTemplate", template.getSqlTemplate() != null
                            ? truncateSql(template.getSqlTemplate(), 100) : "");
                    stat.put("sqlCount", sqlCount);
                    stat.put("severityLevel", template.getSeverityLevel() != null
                            ? template.getSeverityLevel().getDisplayName() : "🟢 正常");
                    stat.put("lastSeenTime", template.getLastSeenTime() != null
                            ? template.getLastSeenTime().toString() : "");

                    return stat;
                })
                .toList();

        return Map.of(
                "code", 200,
                "message", "success",
                "data", Map.of(
                        "total", templates.size(),
                        "records", stats
                )
        );
    }

    /**
     * 截断SQL语句
     */
    private String truncateSql(String sql, int maxLength) {
        if (sql == null) return "";
        if (sql.length() <= maxLength) return sql;
        return sql.substring(0, maxLength) + "...";
    }

    /**
     * 获取队列状态
     *
     * @return 队列状态信息
     */
    @GetMapping("/queue-status")
    public Map<String, Object> getQueueStatus() {
        log.info("查询队列状态");

        // 待分析任务数
        long pendingTasks = templateRepository.countByStatus(
                com.dbdoctor.entity.SlowQueryTemplate.AnalysisStatus.PENDING
        );

        // 正在分析任务数（这里用 SUCCESS 表示正在处理或已完成的）
        long processingTasks = templateRepository.countByStatus(
                com.dbdoctor.entity.SlowQueryTemplate.AnalysisStatus.SUCCESS
        );

        // AI 服务状态（简单实现：假设在线）
        boolean aiOnline = true; // TODO: 实际检测 AI 服务状态

        return Map.of(
                "code", 200,
                "message", "success",
                "data", Map.of(
                        "pendingTasks", pendingTasks,
                        "processingTasks", processingTasks,
                        "aiServiceStatus", aiOnline ? "online" : "offline"
                )
        );
    }

    /**
     * 获取数据源连接状态
     *
     * @return 连接状态信息
     */
    @GetMapping("/datasource-status")
    public Map<String, Object> getDataSourceStatus() {
        log.info("查询数据源连接状态");

        DataSourceStatusHolder.DataSourceStatusVO status = dataSourceStatusHolder.getStatus();

        Map<String, Object> data = new HashMap<>();
        data.put("connected", status.isConnected());
        data.put("lastError", status.getLastError());
        data.put("lastCheckTime", status.getLastCheckTime());
        data.put("lastSuccessTime", status.getLastSuccessTime());

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "success");
        result.put("data", data);

        return result;
    }

    /**
     * 获取当前连接的数据库列表
     *
     * @return 数据库列表
     */
    @GetMapping("/available-databases")
    public Map<String, Object> getAvailableDatabases() {
        log.info("查询可用数据库列表");

        // 检查数据源是否已初始化
        if (!dynamicDataSourceManager.isInitialized()) {
            log.info("目标数据源未初始化，返回空数据库列表");
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", "数据源未初始化，请先配置数据库连接");
            result.put("data", new ArrayList<>());
            return result;
        }

        try {
            // 使用目标数据源（MySQL）查询数据库列表
            List<String> databases = targetJdbcTemplate.queryForList("SHOW DATABASES", String.class);

            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", databases);

            return result;
        } catch (Exception e) {
            log.error("查询数据库列表失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("code", 500);
            result.put("message", "查询失败: " + e.getMessage());
            result.put("data", new ArrayList<>());
            return result;
        }
    }
}
