package com.dbdoctor.controller;

import com.dbdoctor.dto.NotificationScheduleConfigDTO;
import com.dbdoctor.service.NotificationScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 定时批量通知配置 API 控制器
 *
 * <p>提供定时通知配置的查询、更新和手动触发 REST API</p>
 *
 * @author DB-Doctor
 * @version 3.2.0
 * @since 3.2.0
 */
@Slf4j
@RestController
@RequestMapping("/api/notification-schedule")
@RequiredArgsConstructor
public class NotificationScheduleController {

    private final NotificationScheduleService scheduleService;

    /**
     * 查询配置
     *
     * GET /api/notification-schedule/config
     *
     * @return 定时通知配置
     */
    @GetMapping("/config")
    public Map<String, Object> getConfig() {
        log.info("[定时通知配置API] 查询配置");
        try {
            return Map.of(
                    "code", "SUCCESS",
                    "message", "查询成功",
                    "data", scheduleService.getConfig()
            );
        } catch (Exception e) {
            log.error("[定时通知配置API] 查询失败", e);
            return Map.of(
                    "code", "ERROR",
                    "message", "查询失败: " + e.getMessage()
            );
        }
    }

    /**
     * 更新配置
     *
     * POST /api/notification-schedule/config
     *
     * @param config 配置 DTO
     * @return 操作结果
     */
    @PostMapping("/config")
    public Map<String, Object> updateConfig(@Validated @RequestBody NotificationScheduleConfigDTO config) {
        log.info("[定时通知配置API] 更新配置: {}", config);
        try {
            return scheduleService.updateConfig(config);
        } catch (IllegalArgumentException e) {
            log.warn("[定时通知配置API] 参数校验失败: {}", e.getMessage());
            return Map.of(
                    "code", "INVALID_PARAMS",
                    "message", e.getMessage()
            );
        } catch (Exception e) {
            log.error("[定时通知配置API] 更新失败", e);
            return Map.of(
                    "code", "ERROR",
                    "message", "更新失败: " + e.getMessage()
            );
        }
    }

    /**
     * 手动触发定时任务
     *
     * POST /api/notification-schedule/trigger
     *
     * @param request 请求体（包含触发原因）
     * @return 执行结果
     */
    @PostMapping("/trigger")
    public Map<String, Object> triggerNow(@RequestBody Map<String, String> request) {
        log.info("[定时通知配置API] 手动触发: {}", request);
        try {
            String reason = request.getOrDefault("reason", "手动触发");
            return scheduleService.triggerNow(reason);
        } catch (Exception e) {
            log.error("[定时通知配置API] 触发失败", e);
            return Map.of(
                    "code", "ERROR",
                    "message", "触发失败: " + e.getMessage()
            );
        }
    }
}
