package com.dbdoctor.service;

import com.dbdoctor.config.SlowLogMonitorProperties;
import com.dbdoctor.model.SlowQueryLog;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 慢查询日志表监控服务
 * 使用定时轮询方式从 mysql.slow_log 表读取慢查询数据
 *
 * 核心机制：
 * 1. 使用 lastCheckTime 作为游标，记录上一次读取到的最后一条日志的时间
 * 2. 启动时初始化为当前时间，避免处理历史旧数据
 * 3. 每次轮询查询 start_time > lastCheckTime 的记录
 * 4. 处理完更新 lastCheckTime 为最新记录的时间
 *
 * 数据源说明：
 * - 使用 targetJdbcTemplate（连接用户的 MySQL）
 * - 只读访问，零侵入
 *
 * @author DB-Doctor
 * @version 2.0.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SlowLogTableMonitor {

    /**
     * 用户 MySQL 的 JdbcTemplate（目标数据源）
     * ⚠️ 注意：必须使用 targetJdbcTemplate，不能使用默认的（连接 H2）
     */
    @Qualifier("targetJdbcTemplate")
    private final JdbcTemplate targetJdbcTemplate;

    private final AnalysisService analysisService;
    private final SlowLogMonitorProperties properties;

    /**
     * 游标：记录上一次读取到的最后一条日志的时间
     * 初始值设为当前时间，避免应用重启后处理历史旧数据
     */
    private Timestamp lastCheckTime;

    /**
     * 初始化方法
     * 启动时记录当前时间，只关注启动后的慢查询
     */
    @PostConstruct
    public void init() {
        this.lastCheckTime = Timestamp.valueOf(LocalDateTime.now());
        log.info("🔍 DB-Doctor 慢查询表监控已启动");
        log.info("   监听时间点: {}", lastCheckTime);
        log.info("   轮询间隔: {} ms", properties.getPollIntervalMs());
        log.info("   每次最大记录数: {}", properties.getMaxRecordsPerPoll());
        log.info("   自动清理: {}", properties.getAutoCleanup().getEnabled() ? "启用 (cron=" + properties.getAutoCleanup().getCronExpression() + ")" : "禁用");
    }

    /**
     * 定时任务：轮询 mysql.slow_log 表
     *
     * 查询优化：
     * 1. TIME_TO_SEC()：直接将时间转换为秒数（Double）
     * 2. CONVERT(sql_text USING utf8)：解决 BLOB 乱码问题
     * 3. WHERE start_time > ?：只查新数据（使用游标）
     * 4. ORDER BY start_time ASC：按时间升序
     * 5. LIMIT：从配置文件读取，防止一次查太多导致内存溢出
     */
    @Scheduled(fixedDelayString = "${db-doctor.slow-log-monitor.poll-interval-ms:60000}")
    public void pollSlowLog() {
        try {
            String sql = String.format("""
                SELECT
                    start_time,
                    user_host,
                    TIME_TO_SEC(query_time) as query_time_sec,
                    TIME_TO_SEC(lock_time) as lock_time_sec,
                    rows_sent,
                    rows_examined,
                    db,
                    CONVERT(sql_text USING utf8) AS sql_content
                FROM mysql.slow_log
                WHERE start_time > ?
                ORDER BY start_time ASC
                LIMIT %d
                """, properties.getMaxRecordsPerPoll());

            List<Map<String, Object>> logs = targetJdbcTemplate.queryForList(sql, lastCheckTime);

            if (logs.isEmpty()) {
                return; // 没有新日志，直接返回
            }

            log.info("🔍 捕获到 {} 条新的慢查询日志", logs.size());

            // 遍历处理每条慢查询日志
            for (Map<String, Object> logEntry : logs) {
                try {
                    // 提取关键字段
                    Timestamp startTime = (Timestamp) logEntry.get("start_time");
                    String dbName = (String) logEntry.get("db");
                    String sqlContent = (String) logEntry.get("sql_content");
                    String userHost = (String) logEntry.get("user_host");

                    // 提取数值类型字段
                    double queryTime = ((Number) logEntry.get("query_time_sec")).doubleValue();
                    double lockTime = ((Number) logEntry.get("lock_time_sec")).doubleValue();
                    long rowsSent = ((Number) logEntry.get("rows_sent")).longValue();
                    long rowsExamined = ((Number) logEntry.get("rows_examined")).longValue();

                    // 更新游标（最关键的一步！）
                    if (startTime.after(lastCheckTime)) {
                        this.lastCheckTime = startTime;
                    }

                    // 数据清洗：如果 SQL 为空，跳过
                    if (sqlContent == null || sqlContent.isBlank()) {
                        log.warn("发现空 SQL 记录，跳过处理。start_time: {}", startTime);
                        continue;
                    }

                    // 构造慢查询日志对象
                    SlowQueryLog slowLog = SlowQueryLog.builder()
                            .startTime(startTime.toLocalDateTime())
                            .userHost(userHost)
                            .dbName(dbName)
                            .sqlText(sqlContent)
                            .queryTime(queryTime)
                            .lockTime(lockTime)
                            .rowsSent(rowsSent)
                            .rowsExamined(rowsExamined)
                            .build();

                    // 交给分析服务处理（包含去重逻辑）
                    analysisService.processSlowQuery(slowLog);

                } catch (Exception e) {
                    log.error("处理单条慢查询日志失败", e);
                    // 继续处理下一条，不中断整个流程
                }
            }

        } catch (Exception e) {
            log.error("❌ 轮询 mysql.slow_log 表失败", e);
        }
    }

    /**
     * 定时清理任务：清理旧的慢查询日志
     *
     * 生产环境的自洁机制：
     * mysql.slow_log 表会无限增长，必须定期清理
     * 使用 TRUNCATE TABLE 清空（CSV 引擎不支持 DELETE WHERE）
     *
     * 注意：此任务是否执行由配置文件中的 db-doctor.slow-log-monitor.auto-cleanup.enabled 控制
     */
    @Scheduled(cron = "${db-doctor.slow-log-monitor.auto-cleanup.cron-expression:0 0 3 * * ?}")
    public void cleanUpSlowLogTable() {
        // 检查是否启用自动清理
        if (!properties.getAutoCleanup().getEnabled()) {
            log.debug("自动清理功能已禁用，跳过清理任务");
            return;
        }

        log.info("🧹 开始清理 mysql.slow_log 表...");
        try {
            // 清理旧数据（TRUNCATE）
            targetJdbcTemplate.execute("TRUNCATE TABLE mysql.slow_log");

            // 重置游标为当前时间
            this.lastCheckTime = Timestamp.valueOf(LocalDateTime.now());

            log.info("✅ 清理完成，游标已重置为: {}", lastCheckTime);
        } catch (Exception e) {
            log.error("❌ 清理日志表失败", e);
        }
    }

    /**
     * 手动重置游标（可选，用于测试或特殊场景）
     *
     * @param timestamp 重置到的时间点
     */
    public void resetCursor(Timestamp timestamp) {
        log.info("🔄 手动重置游标: {} -> {}", lastCheckTime, timestamp);
        this.lastCheckTime = timestamp;
    }

    /**
     * 获取当前游标位置（用于监控）
     */
    public Timestamp getLastCheckTime() {
        return lastCheckTime;
    }
}
