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
}
