package com.dbdoctor.controller;

import com.dbdoctor.common.Result;
import com.dbdoctor.common.enums.AgentName;
import com.dbdoctor.common.enums.InvocationStatus;
import com.dbdoctor.entity.AiInvocationLog;
import com.dbdoctor.model.AiInvocationDetail;
import com.dbdoctor.model.AiMonitorStats;
import com.dbdoctor.service.AiInvocationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * AI 监控 API 控制器
 *
 * <p>提供 AI 监控相关的查询接口</p>
 *
 * @author DB-Doctor
 * @version 2.3.0
 * @since 2.3.0
 */
@Slf4j
@RestController
@RequestMapping("/api/ai-monitor")
@RequiredArgsConstructor
public class AiMonitorController {

    private final AiInvocationLogService logService;

    /**
     * 获取监控统计数据
     *
     * @param startTime 开始时间（可选，默认最近24小时）
     * @param endTime   结束时间（可选，默认当前时间）
     * @return 统计数据
     */
    @GetMapping("/stats")
    public Result<AiMonitorStats> getStats(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        log.info("[AI监控] 查询统计数据: startTime={}, endTime={}", startTime, endTime);

        // 默认最近 24 小时
        if (startTime == null) {
            startTime = LocalDateTime.now().minusHours(24);
        }
        if (endTime == null) {
            endTime = LocalDateTime.now();
        }

        try {
            Map<String, Object> statsMap = logService.getStats(startTime, endTime);

            // 转换为 DTO
            AiMonitorStats stats = new AiMonitorStats();
            stats.setTotalCalls((Long) statsMap.get("totalCalls"));
            stats.setSuccessCount((Long) statsMap.get("successCount"));
            stats.setSuccessRate((Double) statsMap.get("successRate"));
            stats.setAvgDuration((Long) statsMap.get("avgDuration"));
            stats.setMaxDuration((Long) statsMap.get("maxDuration"));
            stats.setMinDuration((Long) statsMap.get("minDuration"));
            stats.setTotalTokens((Long) statsMap.get("totalTokens"));
            stats.setInputTokens((Long) statsMap.get("inputTokens"));
            stats.setOutputTokens((Long) statsMap.get("outputTokens"));
            stats.setAgentTokenDistribution((Map<String, Long>) statsMap.get("agentTokenDistribution"));
            stats.setAgentCallDistribution((Map<String, Long>) statsMap.get("agentCallDistribution"));
            stats.setHourlyCallCount((Map<Integer, Long>) statsMap.get("hourlyCallCount"));
            stats.setTimeRange((String) statsMap.get("timeRange"));

            log.info("[AI监控] 查询统计数据成功: totalCalls={}, successRate={}%",
                    stats.getTotalCalls(), stats.getSuccessRate());

            return Result.success(stats);
        } catch (Exception e) {
            log.error("[AI监控] 查询统计数据失败", e);
            return Result.error("查询统计数据失败: " + e.getMessage());
        }
    }

    /**
     * 根据 SQL 指纹查询所有相关的 AI 调用
     *
     * @param traceId SQL 指纹
     * @return 调用详情列表
     */
    @GetMapping("/by-trace/{traceId}")
    public Result<List<AiInvocationDetail>> getByTraceId(@PathVariable String traceId) {
        log.info("[AI监控] 根据 SQL 指纹查询 AI 调用: traceId={}", traceId);

        try {
            List<AiInvocationLog> logs = logService.getByTraceId(traceId);
            List<AiInvocationDetail> details = logs.stream()
                    .map(this::convertToDetail)
                    .toList();

            log.info("[AI监控] 查询成功: 找到 {} 条记录", details.size());
            return Result.success(details);
        } catch (Exception e) {
            log.error("[AI监控] 查询失败: traceId={}", traceId, e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 分页查询调用日志
     *
     * @param startTime 开始时间（可选）
     * @param endTime   结束时间（可选）
     * @param agentName Agent 角色（可选）
     * @param status    状态（可选）
     * @return 调用详情列表
     */
    @GetMapping("/query")
    public Result<List<AiInvocationDetail>> query(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,

            @RequestParam(required = false) String agentName,

            @RequestParam(required = false) String status) {

        log.info("[AI监控] 查询调用日志: startTime={}, endTime={}, agentName={}, status={}",
                startTime, endTime, agentName, status);

        try {
            // 验证 Agent 名称
            if (agentName != null && !AgentName.isValidCode(agentName)) {
                return Result.error(400, "无效的 Agent 名称: " + agentName);
            }

            // 验证状态
            if (status != null && !InvocationStatus.isValidCode(status)) {
                return Result.error(400, "无效的状态: " + status);
            }

            List<AiInvocationLog> logs = logService.query(startTime, endTime, agentName, status);
            List<AiInvocationDetail> details = logs.stream()
                    .map(this::convertToDetail)
                    .toList();

            log.info("[AI监控] 查询成功: 找到 {} 条记录", details.size());
            return Result.success(details);
        } catch (Exception e) {
            log.error("[AI监控] 查询失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 获取错误分类统计
     *
     * @param startTime 开始时间（可选，默认最近24小时）
     * @param endTime   结束时间（可选，默认当前时间）
     * @return 错误分类统计
     */
    @GetMapping("/error-stats")
    public Result<Map<String, Long>> getErrorStats(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        log.info("[AI监控] 查询错误分类统计: startTime={}, endTime={}", startTime, endTime);

        // 默认最近 24 小时
        if (startTime == null) {
            startTime = LocalDateTime.now().minusHours(24);
        }
        if (endTime == null) {
            endTime = LocalDateTime.now();
        }

        try {
            Map<String, Long> errorStats = logService.getErrorStats(startTime, endTime);
            log.info("[AI监控] 查询错误分类统计成功: {} 个错误类型", errorStats.size());
            return Result.success(errorStats);
        } catch (Exception e) {
            log.error("[AI监控] 查询错误分类统计失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 获取默认时间范围
     *
     * @return [开始时间, 结束时间]
     */
    @GetMapping("/default-time-range")
    public Result<Map<String, String>> getDefaultTimeRange() {
        LocalDateTime[] range = AiInvocationLogService.getDefaultTimeRange();
        return Result.success(Map.of(
                "startTime", range[0].toString(),
                "endTime", range[1].toString()
        ));
    }

    /**
     * 将 AiInvocationLog 转换为 AiInvocationDetail
     *
     * @param log 日志实体
     * @return 详情 DTO
     */
    private AiInvocationDetail convertToDetail(AiInvocationLog log) {
        AiInvocationDetail detail = new AiInvocationDetail();

        detail.setId(log.getId());
        detail.setTraceId(log.getTraceId());
        detail.setAgentCode(log.getAgentName());
        detail.setAgentDisplayName(getAgentDisplayName(log.getAgentName()));
        detail.setModelName(log.getModelName());
        detail.setProvider(log.getProvider());
        detail.setInputTokens(log.getInputTokens());
        detail.setOutputTokens(log.getOutputTokens());
        detail.setTotalTokens(log.getTotalTokens());
        detail.setDurationMs(log.getDurationMs());
        detail.setDurationDescription(log.getDurationDescription());
        detail.setStartTime(log.getStartTime());
        detail.setEndTime(log.getEndTime());
        detail.setStatusCode(log.getStatus());
        detail.setStatusDisplayName(getStatusDisplayName(log.getStatus()));
        detail.setErrorCategory(log.getErrorCategory());
        detail.setErrorCategoryDisplayName(getErrorCategoryDisplayName(log.getErrorCategory()));
        detail.setErrorMessage(log.getErrorMessage());
        detail.setPromptText(log.getPromptText());
        detail.setResponseText(log.getResponseText());
        detail.setCreatedTime(log.getCreatedTime());

        return detail;
    }

    /**
     * 获取 Agent 显示名称
     */
    private String getAgentDisplayName(String code) {
        return switch (code) {
            case "DIAGNOSIS" -> "主治医生";
            case "REASONING" -> "推理专家";
            case "CODING" -> "编码专家";
            default -> code;
        };
    }

    /**
     * 获取状态显示名称
     */
    private String getStatusDisplayName(String code) {
        return switch (code) {
            case "SUCCESS" -> "成功";
            case "FAILED" -> "失败";
            case "TIMEOUT" -> "超时";
            default -> code;
        };
    }

    /**
     * 获取错误分类显示名称
     */
    private String getErrorCategoryDisplayName(String code) {
        if (code == null) {
            return null;
        }

        return switch (code) {
            case "TIMEOUT" -> "超时";
            case "API_ERROR" -> "API错误";
            case "RATE_LIMIT" -> "速率限制";
            case "NETWORK_ERROR" -> "网络错误";
            case "CONFIG_ERROR" -> "配置错误";
            case "AUTH_ERROR" -> "认证错误";
            case "CONTENT_FILTER" -> "内容过滤";
            case "TOKEN_LIMIT" -> "令牌超限";
            case "UNKNOWN" -> "未知错误";
            default -> code;
        };
    }
}
