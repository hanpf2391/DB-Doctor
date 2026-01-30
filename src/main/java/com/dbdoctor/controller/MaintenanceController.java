package com.dbdoctor.controller;

import com.dbdoctor.service.MaintenanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 系统维护控制器
 *
 * @author DB-Doctor
 * @version 2.2.0
 */
@Slf4j
@RestController
@RequestMapping("/api/maintenance")
@RequiredArgsConstructor
public class MaintenanceController {

    private final MaintenanceService maintenanceService;

    /**
     * 清理历史数据
     *
     * @param days 清理 N 天前的数据
     * @return 删除结果
     */
    @DeleteMapping("/cleanup")
    public Map<String, Object> cleanupHistory(@RequestParam Integer days) {
        log.info("清理历史数据: days={}", days);

        if (days < 1 || days > 365) {
            return Map.of(
                    "code", 400,
                    "message", "天数范围错误，必须在 1-365 之间",
                    "data", null
            );
        }

        Map<String, Integer> result = maintenanceService.cleanupHistory(days);

        return Map.of(
                "code", 200,
                "message", "已清理 " + result.get("deletedTemplates") + " 条模板, " + result.get("deletedSamples") + " 条样本",
                "data", result
        );
    }

    /**
     * 重置系统（清空所有数据）
     *
     * @return 删除结果
     */
    @PostMapping("/reset")
    public Map<String, Object> resetSystem() {
        log.warn("执行系统重置操作");

        Map<String, Integer> result = maintenanceService.resetSystem();

        return Map.of(
                "code", 200,
                "message", "系统已重置，清空所有数据",
                "data", result
        );
    }
}
