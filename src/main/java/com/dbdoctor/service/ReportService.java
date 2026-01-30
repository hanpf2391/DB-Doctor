package com.dbdoctor.service;

import com.dbdoctor.common.enums.SeverityLevel;
import com.dbdoctor.entity.SlowQueryTemplate;
import com.dbdoctor.repository.SlowQueryTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
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

        // 转换严重程度字符串为枚举
        SeverityLevel severity = null;
        if (severityLevel != null && !severityLevel.isEmpty()) {
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

        return Map.of(
                "id", template.getId(),
                "fingerprint", template.getSqlFingerprint(),
                "dbName", template.getDbName() != null ? template.getDbName() : "",
                "tableName", template.getTableName() != null ? template.getTableName() : "",
                "sqlTemplate", template.getSqlTemplate() != null ? template.getSqlTemplate() : "",
                "reportMarkdown", template.getAiAnalysisReport() != null ? template.getAiAnalysisReport() : "暂无分析报告"
        );
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
}
