package com.dbdoctor.service;

import com.dbdoctor.agent.DBAgent;
import com.dbdoctor.common.util.SqlFingerprintUtil;
import com.dbdoctor.common.util.SqlMaskingUtil;
import com.dbdoctor.config.DbDoctorProperties;
import com.dbdoctor.model.QueryStatisticsDTO;
import com.dbdoctor.model.SlowQueryLog;
import com.dbdoctor.model.AnalysisContext;
import com.dbdoctor.entity.SlowQuerySample;
import com.dbdoctor.entity.SlowQueryTemplate;
import com.dbdoctor.repository.SlowQuerySampleRepository;
import com.dbdoctor.repository.SlowQueryTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * 分析服务（V2.3.0 - 使用 Template + Sample 架构 + 多 Agent 协作）
 * 负责处理慢查询日志并发送通知
 *
 * 核心功能：
 * 1. 从 mysql.slow_log 表接收慢查询数据
 * 2. 计算 SQL 指纹，去重判断
 * 3. 写入 slow_query_template（模板表）和 slow_query_sample（样本表）
 * 4. 调用多 Agent 系统生成分析报告
 * 5. 发送邮件通知
 *
 * 多 Agent 协作：
 * - DiagnosisAgent（主治医生）：初步诊断
 * - ReasoningAgent（推理专家）：深度推理（复杂问题）
 * - CodingAgent（编码专家）：生成优化代码
 *
 * 去重机制：
 * - 使用 SQL 指纹（MD5）判断是否为同一类型的 SQL
 * - 新 SQL：创建 Template + Sample 记录
 * - 老 SQL：只新增 Sample 记录，更新 Template 的 lastSeenTime
 *
 * @author DB-Doctor
 * @version 2.3.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final NotifyService notifyService;
    private final SlowQueryTemplateRepository templateRepo;
    private final SlowQuerySampleRepository sampleRepo;
    private final DbDoctorProperties properties;
    private final DBAgent dbAgent;  // 主治医生（单 Agent 模式，保留用于兼容）
    private final MultiAgentCoordinator multiAgentCoordinator;  // 多 Agent 协调器

    /**
     * 处理慢查询日志（入口方法）
     *
     * @param slowLog 慢查询日志对象
     */
    @Transactional
    public void processSlowQuery(SlowQueryLog slowLog) {
        String rawSql = slowLog.getSqlText();
        String dbName = slowLog.getDbName();

        // 1. 数据清洗
        String cleanedSql = SqlFingerprintUtil.cleanSql(rawSql);
        if (cleanedSql.isBlank()) {
            log.warn("SQL 为空，跳过处理");
            return;
        }

        // 2. 计算 SQL 指纹
        String fingerprint = SqlFingerprintUtil.calculateFingerprint(cleanedSql);

        // 3. 查询 H2 数据库：是否已存在？
        Optional<SlowQueryTemplate> templateOpt = templateRepo.findBySqlFingerprint(fingerprint);

        if (templateOpt.isPresent()) {
            // === 情况 A：老面孔（已分析过） ===
            SlowQueryTemplate template = templateOpt.get();
            handleExistingQuery(template, slowLog, cleanedSql);
        } else {
            // === 情况 B：新面孔（首次发现） ===
            handleNewQuery(fingerprint, cleanedSql, dbName, slowLog);
        }
    }

    /**
     * 处理已存在的慢查询（老面孔）
     *
     * 核心逻辑：
     * - 新增一条 Sample 记录（保留完整历史）
     * - 更新 Template 的统计字段和 lastSeenTime
     * - 触发通知判断
     *
     * @param template 模板记录
     * @param slowLog 慢查询日志
     * @param cleanedSql 清洗后的 SQL
     */
    private void handleExistingQuery(SlowQueryTemplate template, SlowQueryLog slowLog, String cleanedSql) {
        String fingerprint = template.getSqlFingerprint();

        // 1. SQL 脱敏处理（保护敏感数据）
        String maskedSql = SqlMaskingUtil.maskSensitiveData(cleanedSql);

        // 2. 新增 Sample 记录
        SlowQuerySample sample = SlowQuerySample.builder()
                .sqlFingerprint(fingerprint)
                .originalSql(maskedSql)  // 存储脱敏后的 SQL
                .userHost(slowLog.getUserHost())
                .queryTime(slowLog.getQueryTime())
                .lockTime(slowLog.getLockTime())
                .rowsSent(slowLog.getRowsSent())
                .rowsExamined(slowLog.getRowsExamined())
                .capturedAt(slowLog.getStartTime())
                .build();
        sampleRepo.save(sample);

        // 3. 增量更新 Template 的统计字段和时间信息
        updateTemplateStatistics(template, slowLog);

        // 4. 保存更新后的 Template
        templateRepo.save(template);

        log.debug("📋 更新重复 SQL: fingerprint={}, db={}", fingerprint, slowLog.getDbName());

        // 5. 触发报告生成和通知（异步，使用智能通知策略判断）
        generateReportAndNotify(template);
    }

    /**
     * 处理新发现的慢查询（新面孔）
     *
     * 核心逻辑：
     * - 创建一条 Template 记录（包含初始统计信息）
     * - 创建第一条 Sample 记录
     * - 触发通知
     *
     * @param fingerprint SQL 指纹
     * @param cleanedSql 清洗后的 SQL
     * @param dbName 数据库名
     * @param slowLog 慢查询日志
     */
    private void handleNewQuery(String fingerprint, String cleanedSql, String dbName, SlowQueryLog slowLog) {
        // 提取表名（可选）
        String tableName = extractTableName(cleanedSql);

        // 1. 提取 SQL 模板（Druid 参数化，把真实值替换成 ?）
        String sqlTemplate = SqlFingerprintUtil.extractTemplate(cleanedSql);

        // 2. SQL 脱敏处理（用于 Sample 表存储）
        String maskedSql = SqlMaskingUtil.maskSensitiveData(cleanedSql);

        // 3. 创建 Template 记录（初始化统计字段）
        SlowQueryTemplate template = SlowQueryTemplate.builder()
                .sqlFingerprint(fingerprint)
                .sqlTemplate(sqlTemplate)  // ← 存储参数化后的模板（全是 ?）
                .dbName(dbName)
                .tableName(tableName)
                .firstSeenTime(LocalDateTime.now())
                .lastSeenTime(LocalDateTime.now())
                .status(SlowQueryTemplate.AnalysisStatus.PENDING)
                // 初始化统计字段（首次出现，所有值都来自第一条样本）
                .occurrenceCount(1L)
                .avgQueryTime(slowLog.getQueryTime())
                .maxQueryTime(slowLog.getQueryTime())
                .avgLockTime(slowLog.getLockTime())
                .maxLockTime(slowLog.getLockTime())
                .avgRowsSent(slowLog.getRowsSent() != null ? slowLog.getRowsSent().doubleValue() : 0.0)
                .maxRowsSent(slowLog.getRowsSent())
                .avgRowsExamined(slowLog.getRowsExamined() != null ? slowLog.getRowsExamined().doubleValue() : 0.0)
                .maxRowsExamined(slowLog.getRowsExamined())
                .build();

        template = templateRepo.save(template);

        // 4. 创建第一条 Sample 记录
        SlowQuerySample sample = SlowQuerySample.builder()
                .sqlFingerprint(fingerprint)
                .originalSql(maskedSql)  // ← 存储脱敏后的原始 SQL
                .userHost(slowLog.getUserHost())
                .queryTime(slowLog.getQueryTime())
                .lockTime(slowLog.getLockTime())
                .rowsSent(slowLog.getRowsSent())
                .rowsExamined(slowLog.getRowsExamined())
                .capturedAt(slowLog.getStartTime())
                .build();
        sampleRepo.save(sample);

        log.info("✨ 新发现慢查询: fingerprint={}, db={}, table={}",
                fingerprint, dbName, tableName);

        // 5. 触发报告生成和通知（异步）
        generateReportAndNotify(template);
    }

    /**
     * 异步生成报告并发送通知（V2.3.0 - 使用多 Agent 协调器）
     *
     * @param template 模板记录
     */
    @Async("analysisExecutor")
    @Transactional
    public void generateReportAndNotify(SlowQueryTemplate template) {
        String fingerprint = template.getSqlFingerprint();
        long startTime = System.currentTimeMillis();

        try {
            log.info("📋 开始多 Agent 协作分析: fingerprint={}, db={}, table={}",
                    fingerprint, template.getDbName(), template.getTableName());

            // 1. 创建数据快照（AnalysisContext）
            AnalysisContext context = buildAnalysisContext(template);
            log.info("📸 数据快照创建完成: triggerTime={}, analysisTime={}, dataRange={}",
                    context.getTriggerTime(), context.getAnalysisTime(), context.getTimeRangeDescription());

            // 2. 调用多 Agent 协调器进行协作分析
            log.info("🤖 调用多 Agent 协调器进行协作分析...");
            String aiReport = multiAgentCoordinator.analyze(context);

            log.info("✅ 多 Agent 协作分析完成: fingerprint={}, 报告长度={} 字符", fingerprint, aiReport.length());

            // 3. 保存报告到 Template
            template.setAiAnalysisReport(aiReport);
            template.setStatus(SlowQueryTemplate.AnalysisStatus.SUCCESS);

            // 4. 判断是否需要通知，标记通知状态
            QueryStatisticsDTO stats = buildStatisticsFromTemplate(template);
            if (shouldNotify(template, stats)) {
                // 标记为等待通知状态，由定时任务批量发送
                template.setNotificationStatus(com.dbdoctor.common.enums.NotificationStatus.WAITING);
                log.info("📬 标记为等待通知状态: fingerprint={}", fingerprint);
            } else {
                // 不需要通知，直接标记为已发送
                template.setNotificationStatus(com.dbdoctor.common.enums.NotificationStatus.SENT);
            }

            templateRepo.save(template);

            long duration = System.currentTimeMillis() - startTime;
            log.info("✅ 报告生成完成: fingerprint={}, 总耗时={}ms", fingerprint, duration);

        } catch (Exception e) {
            log.error("❌ 多 Agent 协作分析失败: fingerprint={}", fingerprint, e);

            // 标记状态为 ERROR，不保存报告
            template.setStatus(SlowQueryTemplate.AnalysisStatus.ERROR);
            template.setAiAnalysisReport(null); // 不保存错误报告
            templateRepo.save(template);

            log.warn("⚠️ AI分析失败，跳过报告生成: fingerprint={}, error={}", fingerprint, e.getMessage());
        }
    }

    /**
     * 构建分析上下文（数据快照）
     *
     * @param template Template 记录
     * @return AnalysisContext
     */
    private AnalysisContext buildAnalysisContext(SlowQueryTemplate template) {
        // 1. 读取最近一条样本 SQL
        String sampleSql = sampleRepo.findRecentSamplesByFingerprint(
                template.getSqlFingerprint(), 1
        ).stream()
         .findFirst()
         .map(SlowQuerySample::getOriginalSql)
         .orElse(template.getSqlTemplate());

        // 2. 构建 Template 统计信息快照
        AnalysisContext.TemplateStatisticsSnapshot statsSnapshot =
                AnalysisContext.TemplateStatisticsSnapshot.builder()
                        .dbName(template.getDbName())
                        .sqlTemplate(template.getSqlTemplate())
                        .occurrenceCount(template.getOccurrenceCount())
                        .avgQueryTime(template.getAvgQueryTime())
                        .maxQueryTime(template.getMaxQueryTime())
                        .avgLockTime(template.getAvgLockTime())
                        .maxLockTime(template.getMaxLockTime())
                        .avgRowsSent(template.getAvgRowsSent() != null ? template.getAvgRowsSent().doubleValue() : 0)
                        .maxRowsSent(template.getMaxRowsSent())
                        .avgRowsExamined(template.getAvgRowsExamined() != null ? template.getAvgRowsExamined().doubleValue() : 0)
                        .maxRowsExamined(template.getMaxRowsExamined())
                        .firstSeenTime(template.getFirstSeenTime())
                        .lastSeenTime(template.getLastSeenTime())
                        .status(template.getStatus())
                        .lastNotifiedTime(template.getLastNotifiedTime())
                        .build();

        // 3. 构建 AnalysisContext
        LocalDateTime now = LocalDateTime.now();

        return AnalysisContext.builder()
                .sqlFingerprint(template.getSqlFingerprint())
                .triggerTime(template.getFirstSeenTime())  // 使用首次发现时间作为触发时间
                .analysisTime(now)                         // 当前分析时间
                .dataRangeEndTime(now)                      // 数据采样截止时间
                .templateStats(statsSnapshot)
                .sampleSql(sampleSql)
                .dbName(template.getDbName())
                .tableName(template.getTableName())
                .build();
    }

    /**
     * 判断是否需要通知
     *
     * @param template 模板记录
     * @param stats 统计信息
     * @return true-需要通知，false-跳过通知
     */
    private boolean shouldNotify(SlowQueryTemplate template, QueryStatisticsDTO stats) {
        // 1. 检查严重程度阈值
        if (stats.getAvgQueryTime() < properties.getNotify().getSeverityThreshold()) {
            log.debug("跳过通知：平均耗时低于阈值 ({} < {})",
                    stats.getAvgQueryTime(), properties.getNotify().getSeverityThreshold());
            return false;
        }

        // 2. 智能通知策略判断
        int coolDownHours = properties.getNotify().getCoolDownHours();
        double degradationMultiplier = properties.getNotify().getDegradationMultiplier();

        return template.shouldNotify(coolDownHours, degradationMultiplier, stats.getAvgQueryTime());
    }

    /**
     * 提取表名（简单实现）
     *
     * @param sql SQL 语句
     * @return 表名
     */
    private String extractTableName(String sql) {
        try {
            String upperSql = sql.toUpperCase().replaceAll("\\s+", " ");

            // FROM table_name
            int fromIndex = upperSql.indexOf(" FROM ");
            if (fromIndex > 0) {
                int start = fromIndex + 6;
                int end = upperSql.indexOf(' ', start);
                if (end == -1) end = upperSql.indexOf('(', start);
                if (end == -1) end = upperSql.length();
                return sql.substring(start, end).trim().replaceAll("[`;\"]", "");
            }

            // UPDATE table_name
            int updateIndex = upperSql.indexOf("UPDATE ");
            if (updateIndex == 0) {
                int start = 7;
                int end = upperSql.indexOf(' ', start);
                if (end == -1) end = upperSql.length();
                return sql.substring(start, end).trim().replaceAll("[`;\"]", "");
            }

            // INSERT INTO table_name
            int insertIndex = upperSql.indexOf("INSERT INTO ");
            if (insertIndex == 0) {
                int start = 12;
                int end = upperSql.indexOf(' ', start);
                if (end == -1) end = upperSql.indexOf('(', start);
                if (end == -1) end = upperSql.length();
                return sql.substring(start, end).trim().replaceAll("[`;\"]", "");
            }

        } catch (Exception e) {
            log.debug("提取表名失败: {}", e.getMessage());
        }

        return "unknown";
    }

    /**
     * 格式化时间
     *
     * @param time 时间
     * @return 格式化后的字符串
     */
    private String formatTime(LocalDateTime time) {
        if (time == null) {
            return "未知";
        }
        return time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * 增量更新 Template 的统计字段
     *
     * 核心逻辑：
     * - 使用增量算法更新平均值：新平均值 = (旧平均值 * 旧数量 + 新值) / (旧数量 + 1)
     * - 更新最大值：如果新值 > 旧最大值，则更新
     * - 更新出现次数：旧数量 + 1
     *
     * @param template 模板记录
     * @param slowLog 新的慢查询日志
     */
    private void updateTemplateStatistics(SlowQueryTemplate template, SlowQueryLog slowLog) {
        // 更新时间信息
        template.setLastSeenTime(LocalDateTime.now());

        // 获取当前统计值
        Long oldCount = template.getOccurrenceCount();
        if (oldCount == null) {
            oldCount = 0L;
        }

        // 1. 更新出现次数
        template.setOccurrenceCount(oldCount + 1);

        // 2. 更新查询耗时统计
        Double oldAvgQueryTime = template.getAvgQueryTime();
        Double oldMaxQueryTime = template.getMaxQueryTime();
        double newQueryTime = slowLog.getQueryTime();

        if (oldAvgQueryTime != null && oldCount > 0) {
            // 增量更新平均值
            template.setAvgQueryTime((oldAvgQueryTime * oldCount + newQueryTime) / (oldCount + 1));
        } else {
            // 首次设置
            template.setAvgQueryTime(newQueryTime);
        }

        // 更新最大值
        if (oldMaxQueryTime == null || newQueryTime > oldMaxQueryTime) {
            template.setMaxQueryTime(newQueryTime);
        }

        // 3. 更新锁等待时间统计
        Double oldAvgLockTime = template.getAvgLockTime();
        Double oldMaxLockTime = template.getMaxLockTime();
        double newLockTime = slowLog.getLockTime();

        if (oldAvgLockTime != null && oldCount > 0) {
            template.setAvgLockTime((oldAvgLockTime * oldCount + newLockTime) / (oldCount + 1));
        } else {
            template.setAvgLockTime(newLockTime);
        }

        if (oldMaxLockTime == null || newLockTime > oldMaxLockTime) {
            template.setMaxLockTime(newLockTime);
        }

        // 4. 更新返回行数统计
        Double oldAvgRowsSent = template.getAvgRowsSent();
        Long oldMaxRowsSent = template.getMaxRowsSent();
        Long newRowsSent = slowLog.getRowsSent();

        if (newRowsSent != null) {
            if (oldAvgRowsSent != null && oldCount > 0) {
                template.setAvgRowsSent((oldAvgRowsSent * oldCount + newRowsSent) / (oldCount + 1));
            } else {
                template.setAvgRowsSent(newRowsSent.doubleValue());
            }

            if (oldMaxRowsSent == null || newRowsSent > oldMaxRowsSent) {
                template.setMaxRowsSent(newRowsSent);
            }
        }

        // 5. 更新扫描行数统计
        Double oldAvgRowsExamined = template.getAvgRowsExamined();
        Long oldMaxRowsExamined = template.getMaxRowsExamined();
        Long newRowsExamined = slowLog.getRowsExamined();

        if (newRowsExamined != null) {
            if (oldAvgRowsExamined != null && oldCount > 0) {
                template.setAvgRowsExamined((oldAvgRowsExamined * oldCount + newRowsExamined) / (oldCount + 1));
            } else {
                template.setAvgRowsExamined(newRowsExamined.doubleValue());
            }

            if (oldMaxRowsExamined == null || newRowsExamined > oldMaxRowsExamined) {
                template.setMaxRowsExamined(newRowsExamined);
            }
        }
    }

    /**
     * 从 Template 构建 QueryStatisticsDTO 对象
     *
     * @param template 模板记录
     * @return 统计信息 DTO
     */
    private QueryStatisticsDTO buildStatisticsFromTemplate(SlowQueryTemplate template) {
        return QueryStatisticsDTO.builder()
                .fingerprint(template.getSqlFingerprint())
                .dbName(template.getDbName())
                .tableName(template.getTableName())
                .firstSeenTime(template.getFirstSeenTime())
                .lastSeenTime(template.getLastSeenTime())
                .occurrenceCount(template.getOccurrenceCount() != null ? template.getOccurrenceCount() : 0L)
                .avgQueryTime(template.getAvgQueryTime() != null ? template.getAvgQueryTime() : 0.0)
                .maxQueryTime(template.getMaxQueryTime() != null ? template.getMaxQueryTime() : 0.0)
                .avgLockTime(template.getAvgLockTime() != null ? template.getAvgLockTime() : 0.0)
                .maxLockTime(template.getMaxLockTime() != null ? template.getMaxLockTime() : 0.0)
                .avgRowsSent(template.getAvgRowsSent())
                .maxRowsSent(template.getMaxRowsSent() != null ? template.getMaxRowsSent() : 0L)
                .avgRowsExamined(template.getAvgRowsExamined())
                .maxRowsExamined(template.getMaxRowsExamined() != null ? template.getMaxRowsExamined() : 0L)
                .build();
    }
}
