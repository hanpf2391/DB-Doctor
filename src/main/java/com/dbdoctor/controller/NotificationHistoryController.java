package com.dbdoctor.controller;

import com.dbdoctor.entity.NotificationHistory;
import com.dbdoctor.service.NotificationHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 通知历史控制器
 *
 * 核心职责：
 * - 提供通知历史查询API
 * - 支持分页查询
 * - 支持按批次、指纹、时间范围查询
 * - 提供统计信息API
 *
 * @author DB-Doctor
 * @version 3.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationHistoryController {

    private final NotificationHistoryService historyService;

    /**
     * 查询批次列表（聚合展示，类似邮件）
     *
     * GET /api/notifications/batches?page=0&size=20&startDate=2026-02-01&endDate=2026-02-08&status=SENT
     *
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @param startDate 开始日期（可选）
     * @param endDate 结束日期（可选）
     * @param status 通知状态（可选）：SENT/FAILED
     * @return 批次列表（聚合统计）
     */
    @GetMapping("/batches")
    public Map<String, Object> queryBatchList(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) String status) {

        log.info("[通知历史] 查询批次列表: page={}, size={}, startDate={}, endDate={}, status={}",
                page, size, startDate, endDate, status);

        try {
            // 转换日期时间为 LocalDateTime
            LocalDateTime startTime = startDate != null ? startDate.atStartOfDay() : null;
            LocalDateTime endTime = endDate != null ? endDate.atTime(23, 59, 59) : null;

            // 转换状态
            NotificationHistory.NotificationStatus notificationStatus = null;
            if (status != null && !status.isEmpty()) {
                try {
                    notificationStatus = NotificationHistory.NotificationStatus.valueOf(status);
                } catch (IllegalArgumentException e) {
                    log.warn("[通知历史] 无效的状态: {}", status);
                }
            }

            // 创建分页参数
            Pageable pageable = PageRequest.of(page, size, Sort.by("sentTime").descending());

            // 查询批次列表
            var batchPage = historyService.queryBatchList(startTime, endTime, notificationStatus, pageable);

            return Map.of(
                    "code", "SUCCESS",
                    "message", "查询成功",
                    "data", Map.of(
                            "total", batchPage.getTotalElements(),
                            "page", page,
                            "size", size,
                            "totalPages", batchPage.getTotalPages(),
                            "list", batchPage.getContent()
                    )
            );
        } catch (Exception e) {
            log.error("[通知历史] 查询失败", e);
            return Map.of(
                    "code", "ERROR",
                    "message", "查询失败: " + e.getMessage()
            );
        }
    }

    /**
     * 查询通知历史列表（分页）
     *
     * GET /api/notifications/history?page=0&size=20&startDate=2026-02-01&endDate=2026-02-08&status=SENT
     *
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @param startDate 开始日期（可选）
     * @param endDate 结束日期（可选）
     * @param status 通知状态（可选）：SENT/FAILED
     * @return 分页结果
     */
    @GetMapping("/history")
    public Map<String, Object> queryHistoryList(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) String status) {

        log.info("[通知历史] 查询历史列表: page={}, size={}, startDate={}, endDate={}, status={}",
                page, size, startDate, endDate, status);

        try {
            // 转换日期时间为 LocalDateTime
            LocalDateTime startTime = startDate != null ? startDate.atStartOfDay() : null;
            LocalDateTime endTime = endDate != null ? endDate.atTime(23, 59, 59) : null;

            // 转换状态
            NotificationHistory.NotificationStatus notificationStatus = null;
            if (status != null && !status.isEmpty()) {
                try {
                    notificationStatus = NotificationHistory.NotificationStatus.valueOf(status);
                } catch (IllegalArgumentException e) {
                    log.warn("[通知历史] 无效的状态: {}", status);
                }
            }

            // 创建分页参数
            Pageable pageable = PageRequest.of(page, size, Sort.by("sentTime").descending());

            // 查询
            Page<NotificationHistory> historyPage = historyService.queryHistoryList(
                    startTime, endTime, notificationStatus, pageable);

            return Map.of(
                    "code", "SUCCESS",
                    "message", "查询成功",
                    "data", Map.of(
                            "total", historyPage.getTotalElements(),
                            "page", page,
                            "size", size,
                            "totalPages", historyPage.getTotalPages(),
                            "list", historyPage.getContent()
                    )
            );
        } catch (Exception e) {
            log.error("[通知历史] 查询失败", e);
            return Map.of(
                    "code", "ERROR",
                    "message", "查询失败: " + e.getMessage()
            );
        }
    }

    /**
     * 查询通知详情（按批次ID）
     *
     * GET /api/notifications/history/{batchId}
     *
     * @param batchId 批次ID
     * @return 通知详情
     */
    @GetMapping("/history/batch/{batchId}")
    public Map<String, Object> queryByBatchId(@PathVariable String batchId) {
        log.info("[通知历史] 查询批次详情: batchId={}", batchId);

        try {
            List<NotificationHistory> historyList = historyService.queryByBatchId(batchId);

            if (historyList.isEmpty()) {
                return Map.of(
                        "code", "NOT_FOUND",
                        "message", "批次不存在: " + batchId
                );
            }

            return Map.of(
                    "code", "SUCCESS",
                    "message", "查询成功",
                    "data", Map.of(
                            "batchId", batchId,
                            "sentTime", historyList.get(0).getSentTime(),
                            "status", historyList.get(0).getNotificationStatus(),
                            "totalCount", historyList.size(),
                            "records", historyList
                    )
            );
        } catch (Exception e) {
            log.error("[通知历史] 查询失败: batchId={}", batchId, e);
            return Map.of(
                    "code", "ERROR",
                    "message", "查询失败: " + e.getMessage()
            );
        }
    }

    /**
     * 查询时间线（按SQL指纹）
     *
     * GET /api/notifications/timeline?fingerprint=abc123&page=0&size=20
     *
     * @param fingerprint SQL指纹
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @return 时间线数据
     */
    @GetMapping("/timeline")
    public Map<String, Object> queryTimeline(
            @RequestParam String fingerprint,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {

        log.info("[通知历史] 查询时间线: fingerprint={}, page={}, size={}", fingerprint, page, size);

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("analyzedTime").descending());
            Page<NotificationHistory> timelinePage = historyService.queryTimeline(fingerprint, pageable);

            return Map.of(
                    "code", "SUCCESS",
                    "message", "查询成功",
                    "data", Map.of(
                            "total", timelinePage.getTotalElements(),
                            "page", page,
                            "size", size,
                            "totalPages", timelinePage.getTotalPages(),
                            "timeline", timelinePage.getContent()
                    )
            );
        } catch (Exception e) {
            log.error("[通知历史] 查询失败: fingerprint={}", fingerprint, e);
            return Map.of(
                    "code", "ERROR",
                    "message", "查询失败: " + e.getMessage()
            );
        }
    }

    /**
     * 统计信息
     *
     * GET /api/notifications/statistics?startDate=2026-02-01&endDate=2026-02-08
     *
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 统计信息
     */
    @GetMapping("/statistics")
    public Map<String, Object> getStatistics(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {

        log.info("[通知历史] 查询统计信息: startDate={}, endDate={}", startDate, endDate);

        try {
            LocalDateTime startTime = startDate.atStartOfDay();
            LocalDateTime endTime = endDate.atTime(23, 59, 59);

            Map<String, Object> statistics = historyService.getStatistics(startTime, endTime);

            return Map.of(
                    "code", "SUCCESS",
                    "message", "查询成功",
                    "data", statistics
            );
        } catch (Exception e) {
            log.error("[通知历史] 查询失败", e);
            return Map.of(
                    "code", "ERROR",
                    "message", "查询失败: " + e.getMessage()
            );
        }
    }

    /**
     * 查询批次中的所有指纹（去重）
     *
     * GET /api/notifications/history/{batchId}/fingerprints
     *
     * @param batchId 批次ID
     * @return SQL指纹列表
     */
    @GetMapping("/history/batch/{batchId}/fingerprints")
    public Map<String, Object> getFingerprintsByBatchId(@PathVariable String batchId) {
        log.info("[通知历史] 查询批次指纹列表: batchId={}", batchId);

        try {
            List<String> fingerprints = historyService.getFingerprintsByBatchId(batchId);

            return Map.of(
                    "code", "SUCCESS",
                    "message", "查询成功",
                    "data", Map.of(
                            "batchId", batchId,
                            "totalCount", fingerprints.size(),
                            "fingerprints", fingerprints
                    )
            );
        } catch (Exception e) {
            log.error("[通知历史] 查询失败: batchId={}", batchId, e);
            return Map.of(
                    "code", "ERROR",
                    "message", "查询失败: " + e.getMessage()
            );
        }
    }
}
