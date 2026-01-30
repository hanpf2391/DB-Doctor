package com.dbdoctor.model;

import com.dbdoctor.entity.SlowQueryTemplate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 分析上下文（数据快照）
 *
 * 核心作用：
 * 1. 在触发异步任务时，一次性打包所有需要的上下文数据
 * 2. 在整个分析过程中，不再实时查询 H2，保证数据一致性
 * 3. 明确记录时间元数据，用于报告展示
 *
 * 设计理念：
 * - 不可变性：一旦创建，内容不再修改
 * - 时间明确：清晰标注触发时间、分析时间、数据采样时间
 * - 数据完整：包含 AI 分析所需的所有信息
 *
 * @author DB-Doctor
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisContext {

    // === 指纹标识 ===
    /**
     * SQL 指纹（MD5）
     */
    private String sqlFingerprint;

    // === 时间元数据 ===
    /**
     * 触发时间：问题首次被发现并进入 PENDING 状态的时间
     * 作用：标记问题进入系统的时刻
     */
    private LocalDateTime triggerTime;

    /**
     * 分析时间：Worker 线程抢到任务，真正开始分析的时间
     * 作用：记录排队等待时长（analysisTime - triggerTime）
     */
    private LocalDateTime analysisTime;

    /**
     * 数据采样截止时间：读取样本数据时的当前时间
     * 作用：标记统计数据的时效性
     */
    private LocalDateTime dataRangeEndTime;

    // === 数据快照 ===
    /**
     * Template 统计信息快照（在 analysisTime 时刻读取）
     * 作用：AI 分析时的统计数据基准，不会因后续新增样本而变化
     */
    private TemplateStatisticsSnapshot templateStats;

    /**
     * 样本 SQL（用于 AI 分析）
     * 从 SlowQuerySample 中选取的最近一条样本
     */
    private String sampleSql;

    /**
     * 数据库名
     */
    private String dbName;

    /**
     * 表名（如果 SQL 只涉及一张表）
     */
    private String tableName;

    // === 辅助方法 ===

    /**
     * 获取排队等待时长（秒）
     *
     * @return 排队秒数
     */
    public long getQueuingSeconds() {
        if (triggerTime == null || analysisTime == null) {
            return 0;
        }
        return java.time.Duration.between(triggerTime, analysisTime).getSeconds();
    }

    /**
     * 获取数据覆盖范围时长（秒）
     *
     * @return 覆盖秒数
     */
    public long getDataRangeSeconds() {
        if (templateStats == null ||
            templateStats.getFirstSeenTime() == null ||
            templateStats.getLastSeenTime() == null) {
            return 0;
        }
        return java.time.Duration.between(
            templateStats.getFirstSeenTime(),
            templateStats.getLastSeenTime()
        ).getSeconds();
    }

    /**
     * 获取数据新鲜度（秒）
     * 即：统计信息截止时间与分析时间的差值
     *
     * @return 新鲜度秒数
     */
    public long getDataFreshnessSeconds() {
        if (dataRangeEndTime == null || analysisTime == null) {
            return 0;
        }
        return java.time.Duration.between(analysisTime, dataRangeEndTime).getSeconds();
    }

    /**
     * 判断是否为新鲜数据（< 5 分钟）
     *
     * @return true=新鲜，false=陈旧
     */
    public boolean isFreshData() {
        return getDataFreshnessSeconds() < 300;
    }

    // === 内部类：Template 统计信息快照 ===

    /**
     * Template 统计信息快照
     *
     * 作用：在 analysisTime 时刻，从 SlowQueryTemplate 读取的统计信息
     * 特性：不可变，后续新增的 Sample 不会影响此快照
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TemplateStatisticsSnapshot {

        /**
         * 数据库名
         */
        private String dbName;

        /**
         * SQL 模板（脱敏后）
         */
        private String sqlTemplate;

        /**
         * 出现次数
         */
        private Long occurrenceCount;

        /**
         * 平均查询耗时（秒）
         */
        private Double avgQueryTime;

        /**
         * 最大查询耗时（秒）
         */
        private Double maxQueryTime;

        /**
         * 平均锁等待时间（秒）
         */
        private Double avgLockTime;

        /**
         * 最大锁等待时间（秒）
         */
        private Double maxLockTime;

        /**
         * 平均返回行数
         */
        private Double avgRowsSent;

        /**
         * 最大返回行数
         */
        private Long maxRowsSent;

        /**
         * 平均扫描行数
         */
        private Double avgRowsExamined;

        /**
         * 最大扫描行数
         */
        private Long maxRowsExamined;

        /**
         * 首次出现时间
         */
        private LocalDateTime firstSeenTime;

        /**
         * 最后出现时间
         */
        private LocalDateTime lastSeenTime;

        /**
         * 当前状态
         */
        private SlowQueryTemplate.AnalysisStatus status;

        /**
         * 上次通知时间
         */
        private LocalDateTime lastNotifiedTime;

        /**
         * 检查是否为高频 SQL（24小时内出现 > 100 次）
         *
         * @return true=高频，false=低频
         */
        public boolean isHighFrequency() {
            return occurrenceCount != null && occurrenceCount > 100;
        }

        /**
         * 检查是否为严重慢查询（平均耗时 > 3 秒）
         *
         * @return true=严重，false=轻微
         */
        public boolean isSevere() {
            return avgQueryTime != null && avgQueryTime > 3.0;
        }

        /**
         * 检查是否存在锁等待问题
         *
         * @return true=有锁问题，false=无锁问题
         */
        public boolean hasLockIssue() {
            return avgLockTime != null && avgLockTime > 0.1;
        }

        /**
         * 检查是否存在全表扫描（扫描行数 >> 返回行数）
         *
         * @return true=可能全表扫描，false=使用索引
         */
        public boolean hasFullTableScan() {
            if (avgRowsExamined == null || avgRowsSent == null) {
                return false;
            }
            // 扫描行数超过返回行数的 10 倍，疑似全表扫描
            return avgRowsExamined > avgRowsSent * 10;
        }
    }

    // === Builder 辅助方法 ===

    /**
     * 创建用于报告展示的时间窗口描述
     *
     * @return 时间窗口字符串
     */
    public String getTimeRangeDescription() {
        if (templateStats == null ||
            templateStats.getFirstSeenTime() == null ||
            templateStats.getLastSeenTime() == null) {
            return "未知";
        }

        return String.format(
            "%s 至 %s（共 %s）",
            formatDate(templateStats.getFirstSeenTime()),
            formatDate(templateStats.getLastSeenTime()),
            formatDuration(getDataRangeSeconds())
        );
    }

    private String formatDate(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "未知";
        }
        return dateTime.toString().replace('T', ' ').substring(0, 19);
    }

    private String formatDuration(long seconds) {
        if (seconds < 60) {
            return seconds + " 秒";
        } else if (seconds < 3600) {
            return (seconds / 60) + " 分钟";
        } else if (seconds < 86400) {
            return (seconds / 3600) + " 小时";
        } else {
            return (seconds / 86400) + " 天";
        }
    }
}
