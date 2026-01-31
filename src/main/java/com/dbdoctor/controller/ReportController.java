package com.dbdoctor.controller;

import com.dbdoctor.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 慢查询报表控制器
 *
 * @author DB-Doctor
 * @version 2.2.0
 */
@Slf4j
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /**
     * 获取慢查询报表列表（分页）
     *
     * @param page 页码（默认 1）
     * @param size 每页数量（默认 20）
     * @param dbName 数据库名筛选（可选）
     * @param severity 严重程度筛选（可选）
     * @return 报表列表
     */
    @GetMapping
    public Map<String, Object> getReports(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String dbName,
            @RequestParam(required = false) String severity
    ) {
        log.info("查询报表列表: page={}, size={}, dbName={}, severity={}", page, size, dbName, severity);

        Map<String, Object> result = reportService.getReports(page, size, dbName, severity);

        return Map.of(
                "code", 200,
                "message", "success",
                "data", result
        );
    }

    /**
     * 获取报告详情
     *
     * @param id 模板 ID
     * @return 报告详情
     */
    @GetMapping("/{id}")
    public Map<String, Object> getReportDetail(@PathVariable Long id) {
        log.info("查询报告详情: id={}", id);

        Map<String, Object> detail = reportService.getReportDetail(id);

        if (detail == null) {
            return Map.of(
                    "code", 404,
                    "message", "报告不存在",
                    "data", null
            );
        }

        return Map.of(
                "code", 200,
                "message", "success",
                "data", detail
        );
    }

    /**
     * 重新分析慢查询
     *
     * @param id 模板 ID
     * @return 操作结果
     */
    @PostMapping("/{id}/reanalyze")
    public Map<String, Object> reanalyze(@PathVariable Long id) {
        log.info("重新分析慢查询: id={}", id);

        try {
            reportService.reanalyze(id);
            return Map.of(
                    "code", 200,
                    "message", "已提交重新分析",
                    "data", Map.of("id", id)
            );
        } catch (Exception e) {
            log.error("重新分析失败: id={}", id, e);
            return Map.of(
                    "code", 500,
                    "message", "重新分析失败: " + e.getMessage(),
                    "data", null
            );
        }
    }

    /**
     * 获取慢查询趋势数据（按小时统计）
     *
     * @param date 日期（yyyy-MM-dd）
     * @param dbName 数据库名（可选）
     * @return 趋势数据
     */
    @GetMapping("/trend")
    public Map<String, Object> getTrend(
            @RequestParam String date,
            @RequestParam(required = false) String dbName
    ) {
        log.info("查询慢查询趋势: date={}, dbName={}", date, dbName);

        Map<String, Object> trend = reportService.getTrend(date, dbName);

        return Map.of(
                "code", 200,
                "message", "success",
                "data", trend
        );
    }

    /**
     * 获取 Top N 慢查询
     *
     * @param limit 数量限制
     * @return Top 慢查询列表
     */
    @GetMapping("/top")
    public Map<String, Object> getTopSlow(
            @RequestParam(defaultValue = "5") Integer limit
    ) {
        log.info("查询 Top 慢查询: limit={}", limit);

        Map<String, Object> topSlow = reportService.getTopSlow(limit);

        return Map.of(
                "code", 200,
                "message", "success",
                "data", topSlow
        );
    }
}
