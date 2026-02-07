package com.dbdoctor.controller;

import com.dbdoctor.dto.AlertSettingsDTO;
import com.dbdoctor.service.AlertSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 告警设置 API 控制器
 *
 * <p>提供告警参数的查询和更新 REST API</p>
 *
 * @author DB-Doctor
 * @version 3.2.0
 * @since 3.2.0
 */
@Slf4j
@RestController
@RequestMapping("/api/alert-settings")
@RequiredArgsConstructor
public class AlertSettingsController {

    private final AlertSettingsService alertSettingsService;

    /**
     * 查询告警设置
     *
     * GET /api/alert-settings
     *
     * @return 告警设置
     */
    @GetMapping
    public Map<String, Object> getAlertSettings() {
        log.info("[告警设置API] 查询告警设置");
        try {
            AlertSettingsDTO settings = alertSettingsService.getAlertSettings();
            return Map.of(
                    "code", "SUCCESS",
                    "message", "查询成功",
                    "data", settings
            );
        } catch (Exception e) {
            log.error("[告警设置API] 查询失败", e);
            return Map.of(
                    "code", "ERROR",
                    "message", "查询失败: " + e.getMessage()
            );
        }
    }

    /**
     * 更新告警设置
     *
     * POST /api/alert-settings
     *
     * @param settings 告警设置
     * @return 操作结果
     */
    @PostMapping
    public Map<String, Object> updateAlertSettings(@Validated @RequestBody AlertSettingsDTO settings) {
        log.info("[告警设置API] 更新告警设置: {}", settings);
        try {
            alertSettingsService.updateAlertSettings(settings);
            return Map.of(
                    "code", "SUCCESS",
                    "message", "保存成功"
            );
        } catch (Exception e) {
            log.error("[告警设置API] 更新失败", e);
            return Map.of(
                    "code", "ERROR",
                    "message", "保存失败: " + e.getMessage()
            );
        }
    }

    /**
     * 重置为默认值
     *
     * POST /api/alert-settings/reset
     *
     * @return 重置后的告警设置
     */
    @PostMapping("/reset")
    public Map<String, Object> resetAlertSettings() {
        log.info("[告警设置API] 重置告警设置");
        try {
            AlertSettingsDTO settings = alertSettingsService.resetAlertSettings();
            return Map.of(
                    "code", "SUCCESS",
                    "message", "已重置为默认值",
                    "data", settings
            );
        } catch (Exception e) {
            log.error("[告警设置API] 重置失败", e);
            return Map.of(
                    "code", "ERROR",
                    "message", "重置失败: " + e.getMessage()
            );
        }
    }
}
