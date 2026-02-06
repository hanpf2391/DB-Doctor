package com.dbdoctor.controller;

import com.dbdoctor.entity.AlertHistory;
import com.dbdoctor.service.AlertHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 告警 API 控制器
 *
 * <p>提供告警管理相关的 REST API</p>
 *
 * @author DB-Doctor
 * @version 3.2.0
 * @since 3.2.0
 */
@Slf4j
@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertHistoryService alertHistoryService;

    /**
     * 查询告警列表（分页）
     *
     * GET /api/alerts?page=0&size=20&severity=CRITICAL&status=FIRING
     *
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @param severity 严重程度（可选）
     * @param status 状态（可选）
     * @return 告警列表
     */
    @GetMapping
    public Map<String, Object> getAlerts(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) String severity,
        @RequestParam(required = false) String status
    ) {
        log.info("[告警API] 查询告警列表: page={}, size={}, severity={}, status={}",
                page, size, severity, status);

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "triggeredAt"));
            Page<AlertHistory> alertPage;

            if (severity != null && !severity.isEmpty()) {
                alertPage = alertHistoryService.findBySeverity(severity, pageable);
            } else if (status != null && !status.isEmpty()) {
                alertPage = alertHistoryService.findByStatus(status, pageable);
            } else {
                // 如果没有筛选条件，返回所有告警（按触发时间倒序）
                // 注意：这里需要添加一个查询所有的方法到 AlertHistoryService
                // 暂时返回空列表
                alertPage = Page.empty(pageable);
            }

            return Map.of(
                "code", "SUCCESS",
                "message", "查询成功",
                "data", Map.of(
                    "total", alertPage.getTotalElements(),
                    "page", page,
                    "size", size,
                    "list", alertPage.getContent()
                )
            );
        } catch (Exception e) {
            log.error("[告警API] 查询告警列表失败", e);
            return Map.of(
                "code", "ERROR",
                "message", "查询失败: " + e.getMessage()
            );
        }
    }

    /**
     * 查询告警详情
     *
     * GET /api/alerts/{id}
     *
     * @param id 告警ID
     * @return 告警详情
     */
    @GetMapping("/{id}")
    public Map<String, Object> getAlertDetail(@PathVariable Long id) {
        log.info("[告警API] 查询告警详情: id={}", id);

        try {
            AlertHistory alert = alertHistoryService.findById(id);

            if (alert == null) {
                return Map.of(
                    "code", "NOT_FOUND",
                    "message", "告警不存在"
                );
            }

            return Map.of(
                "code", "SUCCESS",
                "message", "查询成功",
                "data", alert
            );
        } catch (Exception e) {
            log.error("[告警API] 查询告警详情失败: id={}", id, e);
            return Map.of(
                "code", "ERROR",
                "message", "查询失败: " + e.getMessage()
            );
        }
    }

    /**
     * 标记告警已解决
     *
     * POST /api/alerts/{id}/resolve
     *
     * @param id 告警ID
     * @param requestBody 请求体（包含 resolvedBy）
     * @return 操作结果
     */
    @PostMapping("/{id}/resolve")
    public Map<String, Object> resolveAlert(
        @PathVariable Long id,
        @RequestBody Map<String, String> requestBody
    ) {
        String resolvedBy = requestBody.getOrDefault("resolvedBy", "system");

        log.info("[告警API] 标记告警已解决: id={}, resolvedBy={}", id, resolvedBy);

        try {
            boolean success = alertHistoryService.markAsResolved(id, resolvedBy);

            if (success) {
                return Map.of(
                    "code", "SUCCESS",
                    "message", "标记成功"
                );
            } else {
                return Map.of(
                    "code", "FAILED",
                    "message", "标记失败：告警不存在或已经解决"
                );
            }
        } catch (Exception e) {
            log.error("[告警API] 标记告警解决失败: id={}", id, e);
            return Map.of(
                "code", "ERROR",
                "message", "操作失败: " + e.getMessage()
            );
        }
    }

    /**
     * 获取告警统计信息
     *
     * GET /api/alerts/stats
     *
     * @return 统计信息
     */
    @GetMapping("/stats")
    public Map<String, Object> getAlertStats() {
        log.info("[告警API] 查询告警统计信息");

        try {
            Map<String, Long> statistics = alertHistoryService.getStatistics();

            return Map.of(
                "code", "SUCCESS",
                "message", "查询成功",
                "data", statistics
            );
        } catch (Exception e) {
            log.error("[告警API] 查询告警统计信息失败", e);
            return Map.of(
                "code", "ERROR",
                "message", "查询失败: " + e.getMessage()
            );
        }
    }

    /**
     * 获取告警趋势数据
     *
     * GET /api/alerts/trend?hours=24
     *
     * @param hours 时间范围（小时）
     * @return 趋势数据
     */
    @GetMapping("/trend")
    public Map<String, Object> getAlertTrend(@RequestParam(defaultValue = "24") int hours) {
        log.info("[告警API] 查询告警趋势数据: hours={}", hours);

        try {
            LocalDateTime endTime = LocalDateTime.now();
            LocalDateTime startTime = endTime.minusHours(hours);

            var trend = alertHistoryService.getAlertTrend(startTime, endTime);

            return Map.of(
                "code", "SUCCESS",
                "message", "查询成功",
                "data", Map.of(
                    "startTime", startTime,
                    "endTime", endTime,
                    "trend", trend
                )
            );
        } catch (Exception e) {
            log.error("[告警API] 查询告警趋势数据失败", e);
            return Map.of(
                "code", "ERROR",
                "message", "查询失败: " + e.getMessage()
            );
        }
    }
}
