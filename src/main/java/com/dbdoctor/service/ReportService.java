package com.dbdoctor.service;

import com.dbdoctor.common.enums.SeverityLevel;
import com.dbdoctor.entity.SlowQueryTemplate;
import com.dbdoctor.repository.SlowQuerySampleRepository;
import com.dbdoctor.repository.SlowQueryTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 慢查询报表服务
 *
 * @author DB-Doctor
 * @version 2.2.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final SlowQueryTemplateRepository templateRepository;
    private final SlowQuerySampleRepository sampleRepository;
    private final AnalysisService analysisService;

    /**
     * 分页查询慢查询报表
     *
     * @param page 页码（从 1 开始）
     * @param size 每页数量
     * @param dbName 数据库名筛选（可选）
     * @param severityLevel 严重程度筛选（可选）
     * @return 报表数据
     */
    public Map<String, Object> getReports(int page, int size, String dbName, String severityLevel) {
        log.info("查询报表列表: page={}, size={}, dbName={}, severity={}", page, size, dbName, severityLevel);

        // 处理空字符串参数，转换为 null（避免 SQL 查询时 t.dbName = '' 的问题）
        if (dbName != null && dbName.trim().isEmpty()) {
            dbName = null;
        }

        // 转换严重程度字符串为枚举
        SeverityLevel severity = null;
        if (severityLevel != null && !severityLevel.trim().isEmpty()) {
            try {
                severity = SeverityLevel.valueOf(severityLevel.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("无效的严重程度值: {}", severityLevel);
            }
        }

        // 创建分页参数（按最后见到时间倒序）
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "lastSeenTime"));

        // 执行查询
        Page<SlowQueryTemplate> result = templateRepository.findByFilters(dbName, severity, pageable);

        // 转换为 DTO
        var records = result.getContent().stream()
                .map(this::convertToDto)
                .toList();

        return Map.of(
                "total", result.getTotalElements(),
                "page", page,
                "size", size,
                "records", records
        );
    }

    /**
     * 获取报告详情
     *
     * @param id 模板 ID
     * @return 报告详情
     */
    public Map<String, Object> getReportDetail(Long id) {
        log.info("查询报告详情: id={}", id);

        Optional<SlowQueryTemplate> templateOpt = templateRepository.findById(id);

        if (templateOpt.isEmpty()) {
            log.warn("报告不存在: id={}", id);
            return null;
        }

        SlowQueryTemplate template = templateOpt.get();

        // 构建 Map（因为字段超过 10 个，不能用 Map.of）
        Map<String, Object> result = new HashMap<>();
        result.put("id", template.getId());
        result.put("fingerprint", template.getSqlFingerprint());
        result.put("dbName", template.getDbName() != null ? template.getDbName() : "");
        result.put("tableName", template.getTableName() != null ? template.getTableName() : "");
        result.put("sqlTemplate", template.getSqlTemplate() != null ? template.getSqlTemplate() : "");
        result.put("sqlFingerprint", template.getSqlFingerprint());
        result.put("avgQueryTime", template.getAvgQueryTime() != null ? template.getAvgQueryTime() : 0.0);
        result.put("maxQueryTime", template.getMaxQueryTime() != null ? template.getMaxQueryTime() : 0.0);
        result.put("lockTime", 0.0);
        result.put("rowsExamined", 0);
        result.put("rowsSent", 0);
        result.put("occurrenceCount", template.getOccurrenceCount() != null ? template.getOccurrenceCount() : 0L);
        result.put("severityLevel", template.getSeverityLevel() != null ? template.getSeverityLevel().getDisplayName() : "🟢 正常");
        result.put("analysisStatus", template.getStatus() != null ? template.getStatus().name() : "PENDING");
        result.put("lastSeenTime", template.getLastSeenTime() != null
                ? template.getLastSeenTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                : "");
        result.put("aiAnalysisReport", template.getAiAnalysisReport() != null ? template.getAiAnalysisReport() : "暂无分析报告");

        return result;
    }

    /**
     * 转换实体为 DTO
     */
    private Map<String, Object> convertToDto(SlowQueryTemplate template) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", template.getId());
        dto.put("fingerprint", template.getSqlFingerprint());
        dto.put("dbName", template.getDbName() != null ? template.getDbName() : "");
        dto.put("tableName", template.getTableName() != null ? template.getTableName() : "");
        dto.put("sqlTemplate", template.getSqlTemplate() != null ? template.getSqlTemplate() : "");
        dto.put("avgQueryTime", template.getAvgQueryTime() != null ? template.getAvgQueryTime() : 0.0);
        dto.put("maxQueryTime", template.getMaxQueryTime() != null ? template.getMaxQueryTime() : 0.0);
        dto.put("occurrenceCount", template.getOccurrenceCount() != null ? template.getOccurrenceCount() : 0L);
        dto.put("severityLevel", template.getSeverityLevel() != null ? template.getSeverityLevel().getDisplayName() : "🟢 正常");
        dto.put("analysisStatus", template.getStatus() != null ? template.getStatus().name() : "PENDING");
        dto.put("lastSeenTime", template.getLastSeenTime() != null
                ? template.getLastSeenTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                : "");
        return dto;
    }

    /**
     * 重新分析慢查询
     * 将状态重置为 PENDING，触发新的 AI 分析流程
     *
     * @param id 模板 ID
     */
    public void reanalyze(Long id) {
        log.info("重新分析慢查询: id={}", id);

        Optional<SlowQueryTemplate> templateOpt = templateRepository.findById(id);
        if (templateOpt.isEmpty()) {
            throw new IllegalArgumentException("慢查询模板不存在: " + id);
        }

        SlowQueryTemplate template = templateOpt.get();

        // 重置状态为 PENDING
        template.setStatus(SlowQueryTemplate.AnalysisStatus.PENDING);

        // 清空旧的分析报告（可选）
        template.setAiAnalysisReport(null);

        // ✅ 更新最后发现时间为当前时间（确保会被 PendingTaskRetryService 处理）
        template.setLastSeenTime(java.time.LocalDateTime.now());

        // 保存
        templateRepository.save(template);

        log.info("慢查询已重新提交分析: id={}, fingerprint={}", id, template.getSqlFingerprint());

        // ✅ 立即触发异步分析（不等待定时任务）
        log.info("🚀 立即触发 AI 分析: id={}, fingerprint={}", id, template.getSqlFingerprint());
        analysisService.generateReportAndNotify(template);
    }

    /**
     * 获取慢查询趋势数据（按小时统计）
     *
     * @param date 日期（yyyy-MM-dd）
     * @param dbName 数据库名（可选）
     * @return 趋势数据
     */
    public Map<String, Object> getTrend(String date, String dbName) {
        log.info("查询慢查询趋势: date={}, dbName={}", date, dbName);

        // 解析日期
        LocalDateTime startDate;
        LocalDateTime endDate;

        try {
            startDate = LocalDate.parse(date).atStartOfDay();
            endDate = startDate.plusDays(1);
        } catch (Exception e) {
            log.error("日期格式错误: {}", date);
            return Map.of(
                    "hours", new int[0],
                    "counts", new int[0],
                    "date", date
            );
        }

        // 初始化24小时数据
        int[] hours = new int[24];
        int[] counts = new int[24];
        for (int i = 0; i < 24; i++) {
            hours[i] = i;
            counts[i] = 0;
        }

        // 查询数据库获取按小时统计的慢查询数量
        try {
            List<Object[]> hourlyStats = sampleRepository.countByHourRange(startDate, endDate);
            log.info("查询到 {} 条小时统计数据", hourlyStats.size());

            // 填充统计数据
            for (Object[] stat : hourlyStats) {
                int hour = ((Number) stat[0]).intValue();
                long count = ((Number) stat[1]).longValue();
                if (hour >= 0 && hour < 24) {
                    counts[hour] = (int) count;
                    log.debug("小时 {}: {} 条慢查询", hour, count);
                }
            }
        } catch (Exception e) {
            log.error("查询慢查询趋势失败: date={}", date, e);
        }

        return Map.of(
                "hours", hours,
                "counts", counts,
                "date", date
        );
    }

    /**
     * 获取 Top N 慢查询
     *
     * @param limit 数量限制
     * @return Top 慢查询列表
     */
    public Map<String, Object> getTopSlow(int limit) {
        log.info("查询 Top 慢查询: limit={}", limit);

        // 按最大耗时排序查询
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "maxQueryTime"));
        Page<SlowQueryTemplate> result = templateRepository.findAll(pageable);

        var records = result.getContent().stream()
                .map(template -> Map.of(
                        "id", template.getId(),
                        "fingerprint", template.getSqlFingerprint(),
                        "dbName", template.getDbName() != null ? template.getDbName() : "",
                        "tableName", template.getTableName() != null ? template.getTableName() : "",
                        "sqlTemplate", template.getSqlTemplate() != null ? template.getSqlTemplate() : "",
                        "maxQueryTime", template.getMaxQueryTime() != null ? template.getMaxQueryTime() : 0.0,
                        "avgQueryTime", template.getAvgQueryTime() != null ? template.getAvgQueryTime() : 0.0,
                        "occurrenceCount", template.getOccurrenceCount() != null ? template.getOccurrenceCount() : 0L,
                        "severityLevel", template.getSeverityLevel() != null ? template.getSeverityLevel().getDisplayName() : "🟢 正常"
                ))
                .toList();

        return Map.of(
                "records", records,
                "total", result.getTotalElements()
        );
    }
}
