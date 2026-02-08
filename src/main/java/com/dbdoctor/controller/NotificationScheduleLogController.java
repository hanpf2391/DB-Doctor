package com.dbdoctor.controller;

import com.dbdoctor.dto.NotificationScheduleLogDTO;
import com.dbdoctor.service.NotificationScheduleLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/**
 * 通知调度日志控制器
 *
 * @author DB-Doctor
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/notification-schedule")
@RequiredArgsConstructor
public class NotificationScheduleLogController {

    private final NotificationScheduleLogService logService;

    /**
     * 查询通知调度日志（分页）
     *
     * GET /api/notification-schedule/logs?page=0&size=20&status=SUCCESS&startDate=2026-02-01&endDate=2026-02-08
     */
    @GetMapping("/logs")
    public Map<String, Object> getLogs(
        @RequestParam(defaultValue = "0") Integer page,
        @RequestParam(defaultValue = "20") Integer size,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) LocalDate startDate,
        @RequestParam(required = false) LocalDate endDate
    ) {
        log.info("[通知日志] 查询通知调度日志: page={}, size={}, status={}, startDate={}, endDate={}",
            page, size, status, startDate, endDate);

        try {
            Page<NotificationScheduleLogDTO> logPage = logService.findLogs(
                page, size, status, startDate, endDate
            );

            return Map.of(
                "code", "SUCCESS",
                "message", "查询成功",
                "data", Map.of(
                    "total", logPage.getTotalElements(),
                    "page", page,
                    "size", size,
                    "list", logPage.getContent()
                )
            );
        } catch (Exception e) {
            log.error("[通知日志] 查询失败", e);
            return Map.of(
                "code", "ERROR",
                "message", "查询失败: " + e.getMessage()
            );
        }
    }

    /**
     * 查询日志详情
     *
     * GET /api/notification-schedule/logs/{id}
     */
    @GetMapping("/logs/{id}")
    public Map<String, Object> getLogDetail(@PathVariable Long id) {
        log.info("[通知日志] 查询日志详情: id={}", id);

        try {
            NotificationScheduleLogDTO log = logService.findById(id);

            if (log == null) {
                return Map.of(
                    "code", "ERROR",
                    "message", "日志不存在"
                );
            }

            return Map.of(
                "code", "SUCCESS",
                "message", "查询成功",
                "data", log
            );
        } catch (Exception e) {
            log.error("[通知日志] 查询详情失败", e);
            return Map.of(
                "code", "ERROR",
                "message", "查询失败: " + e.getMessage()
            );
        }
    }
}
