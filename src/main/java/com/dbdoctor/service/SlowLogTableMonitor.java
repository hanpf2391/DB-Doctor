package com.dbdoctor.service;

import com.dbdoctor.check.MySqlEnvChecker;
import com.dbdoctor.config.DataSourceStatusHolder;
import com.dbdoctor.config.SlowLogMonitorProperties;
import com.dbdoctor.lifecycle.ShutdownManager;
import com.dbdoctor.model.SlowQueryLog;
import com.dbdoctor.repository.SlowQueryTemplateRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 慢查询日志表监控服务（自适应轮询版本）
 *
 * 核心机制：
 * 1. 使用 lastCheckTime 作为游标，记录上一次读取到的最后一条日志的时间
 * 2. 启动时初始化为当前时间，避免处理历史旧数据
 * 3. 每次轮询查询 start_time > lastCheckTime 的记录
 * 4. 处理完更新 lastCheckTime 为最新记录的时间
 *
 * 自适应轮询（V2.1 优化）：
 * - 根据慢查询数量自动调整轮询频率
 * - 高负载（>100条/10分钟）：5秒轮询
 * - 中负载（10-100条/10分钟）：15秒轮询
 * - 低负载（<10条/10分钟）：60秒轮询
 * - 避免用户配置错误导致性能问题
 *
 * 优化点（V2.0）：
 * 1. 停机感知：检测到 ShutdownManager.isShuttingDown 时停止扫描
 * 2. 环境感知：动态检查 MySQL 配置，环境不达标时跳过扫描并自动恢复
 * 3. 分批拉取：每次最多读取 maxRecordsPerPoll 条，防止 OOM
 * 4. 设计理念：不补发历史数据，游标始终从"当前时间"开始
 *
 * 数据源说明：
 * - 使用 targetJdbcTemplate（连接用户的 MySQL）
 * - 只读访问，零侵入
 *
 * @author DB-Doctor
 * @version 2.1.0
 */
@Service
@Slf4j
public class SlowLogTableMonitor {

    // ==================== 自适应轮询参数（常量，写死在代码中） ====================

    /**
     * 基础检查间隔：5秒
     * 用于检查是否需要执行轮询
     */
    private static final long BASE_CHECK_INTERVAL_MS = 5000L;

    /**
     * 高负载轮询间隔：5秒
     * 条件：最近10分钟慢查询 > 100条
     */
    private static final long HIGH_LOAD_INTERVAL_MS = 5000L;

    /**
     * 中负载轮询间隔：15秒
     * 条件：最近10分钟慢查询 10-100条
     */
    private static final long MEDIUM_LOAD_INTERVAL_MS = 15000L;

    /**
     * 低负载轮询间隔：60秒
     * 条件：最近10分钟慢查询 < 10条
     */
    private static final long LOW_LOAD_INTERVAL_MS = 60000L;

    /**
     * 负载统计时间窗口：10分钟
     */
    private static final int LOAD_STATISTICS_WINDOW_MINUTES = 10;

    /**
     * 高负载阈值：10分钟内超过此数量视为高负载
     */
    private static final int HIGH_LOAD_THRESHOLD = 100;

    /**
     * 低负载阈值：10分钟内低于此数量视为低负载
     */
    private static final int LOW_LOAD_THRESHOLD = 10;

    // ==================== 依赖注入 ====================

    /**
     * 用户 MySQL 的 JdbcTemplate（目标数据源）
     * ⚠️ 注意：必须使用 targetJdbcTemplate，不能使用默认的（连接 H2）
     */
    @Qualifier("targetJdbcTemplate")
    @Autowired
    private JdbcTemplate targetJdbcTemplate;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private SlowLogMonitorProperties properties;

    /**
     * 慢查询模板Repository（用于统计负载）
     */
    @Autowired
    private SlowQueryTemplateRepository templateRepo;

    /**
     * MySQL 环境检查器（可选）
     * 如果启用了环境检查（db-doctor.env-check.enabled=true），
     * 监控前会先检查环境健康状态
     * required=false 表示如果容器中没有此 Bean 也不报错
     */
    @Autowired(required = false)
    private MySqlEnvChecker envChecker;

    /**
     * 数据源状态持有者（用于记录连接状态）
     */
    @Autowired
    private DataSourceStatusHolder dataSourceStatusHolder;

    /**
     * 动态数据源管理器（用于检查数据源是否已初始化）
     */
    @Autowired
    private com.dbdoctor.config.DynamicDataSourceManager dynamicDataSourceManager;

    // ==================== 状态字段 ====================

    /**
     * 游标：记录上一次读取到的最后一条日志的时间
     * 初始值设为当前时间，避免应用重启后处理历史旧数据
     */
    private Timestamp lastCheckTime;

    /**
     * 上次轮询时间（用于自适应轮询）
     */
    private final AtomicLong lastPollTime = new AtomicLong(System.currentTimeMillis());

    /**
     * 初始化方法
     * 启动时记录当前时间，只关注启动后的慢查询
     */
    @PostConstruct
    public void init() {
        // ✅ 游标初始化为当前时间（不补发历史数据）
        this.lastCheckTime = Timestamp.valueOf(LocalDateTime.now());

        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🔍 DB-Doctor 慢查询表监控已启动");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("   📢 设计理念：实时监控，不补发历史数据");
        log.info("   ⏰ 监听起始时间: {}", lastCheckTime);
        log.info("   🔄 自适应轮询：已启用");
        log.info("      ├─ 高负载（>100条/10分钟）: {} 秒", HIGH_LOAD_INTERVAL_MS / 1000);
        log.info("      ├─ 中负载（10-100条/10分钟）: {} 秒", MEDIUM_LOAD_INTERVAL_MS / 1000);
        log.info("      └─ 低负载（<10条/10分钟）: {} 秒", LOW_LOAD_INTERVAL_MS / 1000);
        log.info("   📦 每次最大记录数: {}", properties.getMaxRecordsPerPoll());
        log.info("   🧹 自动清理: {}", properties.getAutoCleanup().getEnabled() ? "启用 (cron=" + properties.getAutoCleanup().getCronExpression() + ")" : "禁用");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    /**
     * 自适应轮询：基础检查任务
     *
     * 每5秒检查一次是否需要执行轮询
     * 根据慢查询数量动态决定是否真正执行轮询
     */
    @Scheduled(fixedDelay = BASE_CHECK_INTERVAL_MS)
    public void adaptivePoll() {
        // 1. 停机感知逻辑
        if (ShutdownManager.isShuttingDown) {
            log.debug("正在停机中，跳过本次慢日志扫描");
            return;
        }

        // 2. 数据源初始化检查（防止首次启动时尝试连接占位符数据源）
        if (!dynamicDataSourceManager.isInitialized()) {
            log.debug("目标数据源未初始化，跳过本次慢日志扫描（请先配置数据库连接）");
            return;
        }

        // 3. 环境感知逻辑（动态门禁）
        // ⚠️ 已禁用自动环境检查，改为用户手动触发
        // if (envChecker != null) {
        //     boolean isHealthy = envChecker.checkQuickly();
        //     if (!isHealthy) {
        //         log.debug("环境检查未通过，跳过本次检查");
        //         return;
        //     }
        // }

        // 4. 计算当前应该使用的轮询间隔
        long interval = calculateAdaptiveInterval();

        // 5. 判断是否需要执行轮询
        long elapsed = System.currentTimeMillis() - lastPollTime.get();
        if (elapsed >= interval) {
            // 执行轮询
            pollSlowLog();

            // 更新上次轮询时间
            lastPollTime.set(System.currentTimeMillis());
        } else {
            // 跳过本次轮询
            log.trace("跳过轮询：距上次 {}ms，需等待 {}ms",
                elapsed, interval - elapsed);
        }
    }

    /**
     * 计算自适应轮询间隔
     *
     * @return 应该使用的轮询间隔（毫秒）
     */
    private long calculateAdaptiveInterval() {
        // 统计最近10分钟的慢查询数量
        int recentCount = countRecentSlowQueries(LOAD_STATISTICS_WINDOW_MINUTES);

        // 根据负载返回对应的间隔
        if (recentCount > HIGH_LOAD_THRESHOLD) {
            return HIGH_LOAD_INTERVAL_MS;
        } else if (recentCount >= LOW_LOAD_THRESHOLD) {
            return MEDIUM_LOAD_INTERVAL_MS;
        } else {
            return LOW_LOAD_INTERVAL_MS;
        }
    }

    /**
     * 统计最近 N 分钟的慢查询数量
     *
     * @param minutes 时间窗口（分钟）
     * @return 慢查询数量
     */
    private int countRecentSlowQueries(int minutes) {
        try {
            LocalDateTime cutoff = LocalDateTime.now().minusMinutes(minutes);
            Long count = templateRepo.countByLastSeenTimeAfter(cutoff);
            return count != null ? count.intValue() : 0;
        } catch (Exception e) {
            log.warn("统计慢查询数量失败: {}", e.getMessage());
            return 0;  // 查询失败时返回0，使用低负载策略
        }
    }

    /**
     * 定时任务：轮询 mysql.slow_log 表
     *
     * 查询优化：
     * 1. TIME_TO_SEC()：直接将时间转换为秒数
     * 2. CONVERT(sql_text USING utf8)：解决 BLOB 乱码问题
     * 3. WHERE start_time > ?：只查新数据（使用游标）
     * 4. ORDER BY start_time ASC：按时间升序
     * 5. LIMIT：从配置文件读取，防止一次查太多导致内存溢出
     */
    private void pollSlowLog() {
        // 统计当前负载并记录日志
        int recentCount = countRecentSlowQueries(LOAD_STATISTICS_WINDOW_MINUTES);
        long interval = calculateAdaptiveInterval();

        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🔄 开始轮询 mysql.slow_log 表");
        log.info("   📊 最近10分钟慢查询: {} 条", recentCount);
        log.info("   ⏱️  当前轮询间隔: {} 秒", interval / 1000);
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        // 2. 环境感知逻辑（动态门禁）
        // ⚠️ 已禁用自动环境检查，改为用户手动触发
        // if (envChecker != null) {
        //     boolean isHealthy = envChecker.checkQuickly();
        //
        //     if (!isHealthy) {
        //         log.warn("========================================");
        //         log.warn("🛑 [环境待就绪] 慢查询监控暂停中");
        //         log.warn("📋 {}", envChecker.getDiagnosticInfo());
        //         log.warn("💡 提示：请在目标数据库执行以下修复语句，程序会自动感知并恢复监控");
        //         log.warn("   SET GLOBAL slow_query_log = 'ON';");
        //         log.warn("   SET GLOBAL log_output = 'TABLE';");
        //         log.warn("========================================");
        //         return; // 环境不健康，跳过本次扫描
        //     }
        //
        //     // 环境健康，继续正常扫描
        //     log.debug("✅ 环境检查通过，开始扫描慢查询日志");
        // }

        // 3. 执行慢查询日志扫描
        try {
            String sql = String.format("""
                SELECT
                    start_time,
                    user_host,
                    TIME_TO_SEC(query_time) + MICROSECOND(query_time)/1000000.0 as query_time_sec,
                    TIME_TO_SEC(lock_time) + MICROSECOND(lock_time)/1000000.0 as lock_time_sec,
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

            // ✅ 查询成功：更新数据源状态为已连接
            dataSourceStatusHolder.updateSuccess();

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

                    // ⚠️ 跨库查询处理：如果 db 字段为空，尝试从 SQL 中提取数据库名
                    if ((dbName == null || dbName.trim().isEmpty()) && sqlContent != null && !sqlContent.isBlank()) {
                        dbName = extractDatabaseFromSql(sqlContent);
                        if (dbName != null && !dbName.trim().isEmpty()) {
                            log.debug("🔍 [跨库查询] 从 SQL 中提取数据库名: {}", dbName);
                        }
                    }

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
            // 如果不在停机阶段，才记录错误日志
            if (!ShutdownManager.isShuttingDown) {
                log.error("❌ 轮询 mysql.slow_log 表失败", e);

                // 判断是否是连接错误，更新状态
                if (isConnectionError(e)) {
                    dataSourceStatusHolder.updateFailure(e.getMessage());
                }
            }
        }
    }

    /**
     * 判断异常是否为连接错误
     *
     * @param e 异常
     * @return 是否为连接错误
     */
    private boolean isConnectionError(Exception e) {
        String message = e.getMessage();
        if (message == null) {
            return false;
        }

        return message.contains("CommunicationsException") ||
               message.contains("UnknownHostException") ||
               message.contains("Connection refused") ||
               message.contains("Communications link failure") ||
               message.contains("placeholder") ||
               message.contains("could not create connection to database server") ||
               message.contains("No operations allowed after connection closed");
    }

    /**
     * 定时清理任务：安全清理已处理的慢查询日志
     *
     * 安全清理机制：
     * 1. 只删除游标之前的数据（已处理的数据）
     * 2. 保留游标之后的数据（正在处理/未处理的数据）
     * 3. 避免数据丢失，确保业务连续性
     *
     * 清理策略：
     * - 如果 slow_log 表是 InnoDB 引擎：使用 DELETE WHERE start_time < 游标
     * - 如果 slow_log 表是 CSV 引擎（MySQL 默认）：TRUNCATE TABLE（⚠️ 会丢失数据，需手动改为 InnoDB）
     *
     * 注意：
     * 1. 默认关闭，需在配置文件中启用（db-doctor.slow-log-monitor.auto-cleanup.enabled=true）
     * 2. 执行时间可配置（默认每天凌晨 3 点）
     * 3. 建议将 slow_log 表改为 InnoDB 引擎以支持安全清理
     */
    @Scheduled(cron = "${db-doctor.slow-log-monitor.auto-cleanup.cron-expression:0 0 3 * * ?}")
    public void cleanUpSlowLogTable() {
        // 检查数据源是否已初始化
        if (!dynamicDataSourceManager.isInitialized()) {
            log.debug("目标数据源未初始化，跳过慢日志表清理（请先配置数据库连接）");
            return;
        }

        // 检查是否启用自动清理
        if (!properties.getAutoCleanup().getEnabled()) {
            log.debug("自动清理功能已禁用（默认关闭），如需启用请在配置文件中设置 db-doctor.slow-log-monitor.auto-cleanup.enabled=true");
            return;
        }

        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🧹 开始安全清理 mysql.slow_log 表...");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        try {
            // 策略 1: 尝试安全删除（基于游标）
            boolean safeCleanupSuccess = trySafeCleanup();

            if (!safeCleanupSuccess) {
                // 策略 2: 如果安全删除失败，回退到 TRUNCATE（需用户确认）
                fallbackToTruncate();
            }

        } catch (Exception e) {
            log.error("❌ 清理日志表失败", e);
        }
    }

    /**
     * 尝试安全清理（基于游标）
     *
     * @return true-成功, false-失败（需要回退到 TRUNCATE）
     */
    private boolean trySafeCleanup() {
        try {
            // 检查表引擎
            String tableEngine = checkTableEngine();

            if ("InnoDB".equals(tableEngine)) {
                // ✅ InnoDB 引擎，使用安全删除
                return safeDeleteByCursor();
            } else {
                // ⚠️ CSV 引擎，不支持 DELETE WHERE
                log.warn("⚠️  检测到 mysql.slow_log 表引擎为: {}", tableEngine);
                log.warn("⚠️  CSV 引擎不支持 DELETE WHERE 操作，无法执行安全清理");
                log.warn("💡 建议：执行以下命令将表改为 InnoDB 引擎");
                log.warn("   SET GLOBAL slow_query_log = 'OFF';");
                log.warn("   ALTER TABLE mysql.slow_log ENGINE = InnoDB;");
                log.warn("   SET GLOBAL slow_query_log = 'ON';");
                return false;
            }

        } catch (Exception e) {
            log.error("检查表引擎失败", e);
            return false;
        }
    }

    /**
     * 安全删除（基于游标）
     * 只删除游标之前的数据，保留游标之后的数据
     *
     * @return true-成功
     */
    private boolean safeDeleteByCursor() {
        log.info("📍 当前游标位置: {}", lastCheckTime);

        // 使用 DELETE WHERE 删除游标之前的数据
        String sql = "DELETE FROM mysql.slow_log WHERE start_time < ?";
        int deleted = targetJdbcTemplate.update(sql, lastCheckTime);

        log.info("✅ 安全清理完成");
        log.info("   🗑️  删除记录数: {}", deleted);
        log.info("   📍 游标位置保持不变: {}", lastCheckTime);
        log.info("   🛡️  安全保证: 未删除游标之后的数据");

        return true;
    }

    /**
     * 回退方案：使用 TRUNCATE
     * ⚠️ 会清空整个表，包括未处理的数据
     */
    private void fallbackToTruncate() {
        // 检查是否允许回退到 TRUNCATE
        if (!properties.getAutoCleanup().getAllowTruncate()) {
            log.warn("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.warn("⛔ 安全清理失败，且配置禁止回退到 TRUNCATE");
            log.warn("💡 请执行以下操作：");
            log.warn("   1. 将 mysql.slow_log 表改为 InnoDB 引擎：");
            log.warn("      ALTER TABLE mysql.slow_log ENGINE = InnoDB;");
            log.warn("   2. 或者在配置文件中允许回退：");
            log.warn("      db-doctor.slow-log-monitor.auto-cleanup.allow-truncate=true");
            log.warn("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            return;
        }

        log.warn("⚠️  回退到 TRUNCATE 模式");
        log.warn("⚠️  这将清空整个表，包括未处理的数据");

        try {
            // TRUNCATE TABLE
            targetJdbcTemplate.execute("TRUNCATE TABLE mysql.slow_log");

            // 重置游标为当前时间
            this.lastCheckTime = Timestamp.valueOf(LocalDateTime.now());

            log.warn("✅ TRUNCATE 完成");
            log.warn("   🗑️  已清空整个表");
            log.warn("   📍 游标已重置为: {}", lastCheckTime);

        } catch (Exception e) {
            log.error("❌ TRUNCATE 失败", e);
        }
    }

    /**
     * 检查 mysql.slow_log 表的引擎
     *
     * @return 表引擎（InnoDB/CSV）
     */
    private String checkTableEngine() {
        String sql = """
            SELECT ENGINE
            FROM information_schema.TABLES
            WHERE TABLE_SCHEMA = 'mysql'
            AND TABLE_NAME = 'slow_log'
            """;

        return targetJdbcTemplate.queryForObject(sql, String.class);
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

    /**
     * 从 SQL 语句中提取数据库名
     * 支持格式：database.table 或 `database`.`table`
     *
     * @param sql SQL 语句
     * @return 数据库名，如果无法提取则返回 null
     */
    private String extractDatabaseFromSql(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return null;
        }

        try {
            // 转大写并移除多余空格，便于解析
            String normalizedSql = sql.toUpperCase().replaceAll("\\s+", " ");

            // 查找 FROM 子句的位置
            int fromIndex = normalizedSql.indexOf(" FROM ");
            if (fromIndex == -1) {
                // 如果没有 FROM，尝试查找 UPDATE
                fromIndex = normalizedSql.indexOf(" UPDATE ");
                if (fromIndex == -1) {
                    // 如果也没有 UPDATE，尝试查找 INSERT INTO
                    fromIndex = normalizedSql.indexOf(" INTO ");
                    if (fromIndex == -1) {
                        return null;
                    }
                    fromIndex += 6; // " INTO ".length()
                } else {
                    fromIndex += 7; // " UPDATE ".length()
                }
            } else {
                fromIndex += 6; // " FROM ".length()
            }

            // 提取 FROM/UPDATE/INTO 之后的部分（到下一个关键字之前）
            String afterFrom = normalizedSql.substring(fromIndex).trim();

            // 查找第一个表引用（可能带有数据库名前缀）
            int spaceIndex = afterFrom.indexOf(' ');
            int commaIndex = afterFrom.indexOf(',');
            int joinIndex = afterFrom.indexOf(" JOIN ");

            // 取最近的分隔符
            int endIndex = afterFrom.length();
            if (spaceIndex > 0 && spaceIndex < endIndex) endIndex = spaceIndex;
            if (commaIndex > 0 && commaIndex < endIndex) endIndex = commaIndex;
            if (joinIndex > 0 && joinIndex < endIndex) endIndex = joinIndex;

            String firstTableRef = afterFrom.substring(0, endIndex).trim();

            // 移除可能的别名（AS 或空格后的别名）
            if (firstTableRef.contains(" AS ")) {
                firstTableRef = firstTableRef.substring(0, firstTableRef.indexOf(" AS ")).trim();
            }

            // 提取数据库名（支持带反引号和不带反引号）
            String dbName = null;

            // 匹配 `database`.`table` 或 database.table
            if (firstTableRef.contains(".")) {
                String[] parts = firstTableRef.split("\\.");
                if (parts.length >= 2) {
                    dbName = parts[0].replaceAll("`", "").trim();
                }
            }

            return dbName;

        } catch (Exception e) {
            log.warn("⚠️ [SQL解析] 提取数据库名失败: {}", e.getMessage());
            return null;
        }
    }
}
