package com.dbdoctor.controller;

import com.dbdoctor.entity.AlertRule;
import com.dbdoctor.service.AlertRuleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 告警规则 API 控制器
 *
 * <p>提供告警规则的 CRUD REST API</p>
 *
 * @author DB-Doctor
 * @version 3.2.0
 * @since 3.2.0
 */
@Slf4j
@RestController
@RequestMapping("/api/alert-rules")
@RequiredArgsConstructor
public class AlertRuleController {

    private final AlertRuleService alertRuleService;

    /**
     * 查询所有告警规则（分页）
     *
     * GET /api/alert-rules?page=0&size=20
     *
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @return 告警规则列表
     */
    @GetMapping
    public Map<String, Object> getAlertRules(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("[告警规则API] 查询告警规则列表: page={}, size={}", page, size);

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<AlertRule> rulePage = alertRuleService.findAll(pageable);

            return Map.of(
                    "code", "SUCCESS",
                    "message", "查询成功",
                    "data", Map.of(
                            "total", rulePage.getTotalElements(),
                            "page", page,
                            "size", size,
                            "list", rulePage.getContent()
                    )
            );
        } catch (Exception e) {
            log.error("[告警规则API] 查询告警规则列表失败", e);
            return Map.of(
                    "code", "ERROR",
                    "message", "查询失败: " + e.getMessage()
            );
        }
    }

    /**
     * 查询所有启用的告警规则
     *
     * GET /api/alert-rules/enabled
     *
     * @return 启用的告警规则列表
     */
    @GetMapping("/enabled")
    public Map<String, Object> getEnabledAlertRules() {
        log.info("[告警规则API] 查询启用的告警规则");

        try {
            List<AlertRule> rules = alertRuleService.findAllEnabled();

            return Map.of(
                    "code", "SUCCESS",
                    "message", "查询成功",
                    "data", rules
            );
        } catch (Exception e) {
            log.error("[告警规则API] 查询启用的告警规则失败", e);
            return Map.of(
                    "code", "ERROR",
                    "message", "查询失败: " + e.getMessage()
            );
        }
    }

    /**
     * 根据ID查询告警规则
     *
     * GET /api/alert-rules/{id}
     *
     * @param id 规则ID
     * @return 告警规则
     */
    @GetMapping("/{id}")
    public Map<String, Object> getAlertRule(@PathVariable Long id) {
        log.info("[告警规则API] 查询告警规则: id={}", id);

        try {
            return alertRuleService.findById(id)
                    .map(rule -> Map.<String, Object>of(
                            "code", "SUCCESS",
                            "message", "查询成功",
                            "data", rule
                    ))
                    .orElse(Map.<String, Object>of(
                            "code", "NOT_FOUND",
                            "message", "告警规则不存在"
                    ));
        } catch (Exception e) {
            log.error("[告警规则API] 查询告警规则失败: id={}", id, e);
            return Map.of(
                    "code", "ERROR",
                    "message", "查询失败: " + e.getMessage()
            );
        }
    }

    /**
     * 创建告警规则
     *
     * POST /api/alert-rules
     *
     * @param rule 告警规则
     * @return 创建的告警规则
     */
    @PostMapping
    public Map<String, Object> createAlertRule(@RequestBody AlertRule rule) {
        log.info("[告警规则API] 创建告警规则: name={}, displayName={}",
                rule.getName(), rule.getDisplayName());

        try {
            AlertRule created = alertRuleService.create(rule, "admin");

            return Map.of(
                    "code", "SUCCESS",
                    "message", "创建成功",
                    "data", created
            );
        } catch (IllegalArgumentException e) {
            log.warn("[告警规则API] 创建告警规则失败: {}", e.getMessage());
            return Map.of(
                    "code", "INVALID_PARAMS",
                    "message", e.getMessage()
            );
        } catch (Exception e) {
            log.error("[告警规则API] 创建告警规则失败", e);
            return Map.of(
                    "code", "ERROR",
                    "message", "创建失败: " + e.getMessage()
            );
        }
    }

    /**
     * 更新告警规则
     *
     * PUT /api/alert-rules/{id}
     *
     * @param id 规则ID
     * @param rule 更新的告警规则
     * @return 更新后的告警规则
     */
    @PutMapping("/{id}")
    public Map<String, Object> updateAlertRule(
            @PathVariable Long id,
            @RequestBody AlertRule rule
    ) {
        log.info("[告警规则API] 更新告警规则: id={}, displayName={}",
                id, rule.getDisplayName());

        try {
            AlertRule updated = alertRuleService.update(id, rule, "admin");

            return Map.of(
                    "code", "SUCCESS",
                    "message", "更新成功",
                    "data", updated
            );
        } catch (IllegalArgumentException e) {
            log.warn("[告警规则API] 更新告警规则失败: {}", e.getMessage());
            return Map.of(
                    "code", "INVALID_PARAMS",
                    "message", e.getMessage()
            );
        } catch (Exception e) {
            log.error("[告警规则API] 更新告警规则失败: id={}", id, e);
            return Map.of(
                    "code", "ERROR",
                    "message", "更新失败: " + e.getMessage()
            );
        }
    }

    /**
     * 删除告警规则
     *
     * DELETE /api/alert-rules/{id}
     *
     * @param id 规则ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public Map<String, Object> deleteAlertRule(@PathVariable Long id) {
        log.info("[告警规则API] 删除告警规则: id={}", id);

        try {
            alertRuleService.delete(id);

            return Map.of(
                    "code", "SUCCESS",
                    "message", "删除成功"
            );
        } catch (IllegalArgumentException e) {
            log.warn("[告警规则API] 删除告警规则失败: {}", e.getMessage());
            return Map.of(
                    "code", "INVALID_PARAMS",
                    "message", e.getMessage()
            );
        } catch (Exception e) {
            log.error("[告警规则API] 删除告警规则失败: id={}", id, e);
            return Map.of(
                    "code", "ERROR",
                    "message", "删除失败: " + e.getMessage()
            );
        }
    }

    /**
     * 启用/禁用告警规则
     *
     * PUT /api/alert-rules/{id}/toggle
     *
     * @param id 规则ID
     * @param requestBody 请求体（包含 enabled 字段）
     * @return 更新后的告警规则
     */
    @PutMapping("/{id}/toggle")
    public Map<String, Object> toggleAlertRule(
            @PathVariable Long id,
            @RequestBody Map<String, Object> requestBody
    ) {
        boolean enabled = (boolean) requestBody.getOrDefault("enabled", true);

        log.info("[告警规则API] 切换告警规则状态: id={}, enabled={}", id, enabled);

        try {
            AlertRule rule = alertRuleService.toggleEnabled(id, enabled, "admin");

            return Map.of(
                    "code", "SUCCESS",
                    "message", enabled ? "启用成功" : "禁用成功",
                    "data", rule
            );
        } catch (IllegalArgumentException e) {
            log.warn("[告警规则API] 切换告警规则状态失败: {}", e.getMessage());
            return Map.of(
                    "code", "INVALID_PARAMS",
                    "message", e.getMessage()
            );
        } catch (Exception e) {
            log.error("[告警规则API] 切换告警规则状态失败: id={}", id, e);
            return Map.of(
                    "code", "ERROR",
                    "message", "操作失败: " + e.getMessage()
            );
        }
    }

    /**
     * 根据指标名称查询告警规则
     *
     * GET /api/alert-rules/metric/{metricName}
     *
     * @param metricName 指标名称
     * @return 告警规则列表
     */
    @GetMapping("/metric/{metricName}")
    public Map<String, Object> getAlertRulesByMetric(@PathVariable String metricName) {
        log.info("[告警规则API] 根据指标名称查询: metricName={}", metricName);

        try {
            List<AlertRule> rules = alertRuleService.findByMetricName(metricName);

            return Map.of(
                    "code", "SUCCESS",
                    "message", "查询成功",
                    "data", rules
            );
        } catch (Exception e) {
            log.error("[告警规则API] 根据指标名称查询失败: metricName={}", metricName, e);
            return Map.of(
                    "code", "ERROR",
                    "message", "查询失败: " + e.getMessage()
            );
        }
    }
}
