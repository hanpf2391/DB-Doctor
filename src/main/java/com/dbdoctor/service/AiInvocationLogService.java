package com.dbdoctor.service;

import com.dbdoctor.entity.AiInvocationLog;
import com.dbdoctor.repository.AiInvocationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 调用日志服务
 *
 * <p>提供 AI 调用日志的保存、查询和统计功能</p>
 *
 * @author DB-Doctor
 * @version 2.3.0
 * @since 2.3.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiInvocationLogService {

    private final AiInvocationLogRepository repository;

    /**
     * 异步保存 AI 调用日志（不阻塞 AI 调用线程）
     *
     * @param invocationLog 日志实体
     */
    @Async("monitoringExecutor")
    @Transactional
    public void saveAsync(AiInvocationLog invocationLog) {
        try {
            repository.save(invocationLog);
            log.debug("[AI监控] 日志已保存: id={}, agent={}, traceId={}, duration={}ms",
                    invocationLog.getId(), invocationLog.getAgentName(), invocationLog.getTraceId(), invocationLog.getDurationMs());
        } catch (Exception e) {
            log.error("[AI监控] 保存日志失败: agent={}, traceId={}",
                    invocationLog.getAgentName(), invocationLog.getTraceId(), e);
            // 不抛出异常，避免影响 AI 调用
        }
    }

    /**
     * 同步保存 AI 调用日志
     *
     * @param invocationLog 日志实体
     * @return 保存后的实体
     */
    @Transactional
    public AiInvocationLog save(AiInvocationLog invocationLog) {
        return repository.save(invocationLog);
    }

    /**
     * 清理指定 trace_id 的所有旧记录
     *
     * <p>在开始新的分析前调用，清理上次分析的调用链路数据</p>
     *
     * @param traceId SQL 指纹
     * @return 删除的记录数
     */
    @Transactional
    public int cleanByTraceId(String traceId) {
        if ("UNKNOWN".equals(traceId)) {
            return 0;
        }

        int deletedCount = repository.deleteByTraceId(traceId);
        if (deletedCount > 0) {
            log.info("[AI监控] 清理旧分析记录: traceId={}, 删除了 {} 条旧记录", traceId, deletedCount);
        }

        return deletedCount;
    }

    /**
     * 根据 SQL 指纹查询所有相关的 AI 调用
     *
     * @param traceId SQL 指纹
     * @return 调用日志列表
     */
    public List<AiInvocationLog> getByTraceId(String traceId) {
        return repository.findByTraceIdOrderByStartTimeAsc(traceId);
    }

    /**
     * 分页查询调用日志
     *
     * @param startTime 开始时间（可选）
     * @param endTime   结束时间（可选）
     * @param agentName Agent 角色（可选）
     * @param status    状态（可选）
     * @return 调用日志列表
     */
    public List<AiInvocationLog> query(
            LocalDateTime startTime,
            LocalDateTime endTime,
            String agentName,
            String status) {
        return repository.findByConditions(startTime, endTime, agentName, status);
    }

    /**
     * 获取基础统计数据
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 统计数据 Map
     */
    public Map<String, Object> getStats(LocalDateTime startTime, LocalDateTime endTime) {
        Map<String, Object> stats = new LinkedHashMap<>();

        // 基础统计
        long totalCalls = repository.countTotal(startTime, endTime);
        stats.put("totalCalls", totalCalls);

        long successCount = repository.countSuccess(startTime, endTime);
        stats.put("successCount", successCount);

        double successRate = totalCalls > 0 ? (successCount * 100.0 / totalCalls) : 0.0;
        stats.put("successRate", Math.round(successRate * 100.0) / 100.0); // 保留两位小数

        Double avgDuration = repository.avgDuration(startTime, endTime);
        stats.put("avgDuration", avgDuration != null ? avgDuration.longValue() : 0);

        Long maxDuration = repository.maxDuration(startTime, endTime);
        stats.put("maxDuration", maxDuration != null ? maxDuration : 0);

        Long minDuration = repository.minDuration(startTime, endTime);
        stats.put("minDuration", minDuration != null ? minDuration : 0);

        // Token 统计
        Long totalTokens = repository.sumTotalTokens(startTime, endTime);
        stats.put("totalTokens", totalTokens != null ? totalTokens : 0);

        Long inputTokens = repository.sumInputTokens(startTime, endTime);
        stats.put("inputTokens", inputTokens != null ? inputTokens : 0);

        Long outputTokens = repository.sumOutputTokens(startTime, endTime);
        stats.put("outputTokens", outputTokens != null ? outputTokens : 0);

        // Agent 分布统计
        Map<String, Long> agentCallDist = new LinkedHashMap<>();
        repository.countByAgentName(startTime, endTime).forEach(row -> {
            agentCallDist.put((String) row[0], (Long) row[1]);
        });
        stats.put("agentCallDistribution", agentCallDist);

        Map<String, Long> agentTokenDist = new LinkedHashMap<>();
        repository.sumTokensByAgentName(startTime, endTime).forEach(row -> {
            agentTokenDist.put((String) row[0], (Long) row[1]);
        });
        stats.put("agentTokenDistribution", agentTokenDist);

        // 按小时统计
        Map<Integer, Long> hourlyCount = new LinkedHashMap<>();
        repository.countByHour(startTime, endTime).forEach(row -> {
            hourlyCount.put((Integer) row[0], (Long) row[1]);
        });
        stats.put("hourlyCallCount", hourlyCount);

        // 时间范围描述
        stats.put("timeRange", startTime + " ~ " + endTime);

        return stats;
    }

    /**
     * 获取错误分类统计
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 错误分类统计 Map
     */
    public Map<String, Long> getErrorStats(LocalDateTime startTime, LocalDateTime endTime) {
        Map<String, Long> errorStats = new LinkedHashMap<>();

        repository.countByErrorCategory(startTime, endTime).forEach(row -> {
            String category = (String) row[0];
            Long count = (Long) row[1];
            if (category != null) {
                errorStats.put(category, count);
            }
        });

        return errorStats;
    }

    /**
     * 删除指定时间之前的数据（用于数据归档）
     *
     * @param beforeTime 删除此时间之前的数据
     * @return 删除的记录数
     */
    @Transactional
    public int deleteByCreatedTimeBefore(LocalDateTime beforeTime) {
        log.info("[AI监控] 删除 {} 之前的监控数据", beforeTime);
        return repository.deleteByCreatedTimeBefore(beforeTime);
    }

    /**
     * 获取默认时间范围（最近 24 小时）
     *
     * @return [开始时间, 结束时间]
     */
    public static LocalDateTime[] getDefaultTimeRange() {
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusHours(24);
        return new LocalDateTime[]{startTime, endTime};
    }

    /**
     * 计算成本（根据 Token 单价）
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param tokenPrices Token 单价映射（模型名 -> 单价/1K tokens）
     * @return 成本统计 Map
     */
    public Map<String, Object> calculateCost(LocalDateTime startTime, LocalDateTime endTime,
                                              Map<String, Double> tokenPrices) {
        Map<String, Object> costStats = new LinkedHashMap<>();

        // 获取所有日志记录
        List<AiInvocationLog> logs = repository.findByConditions(startTime, endTime, null, null);

        // 按模型统计 Token 消耗
        Map<String, Long> modelTokens = new LinkedHashMap<>();
        Map<String, Integer> modelCalls = new LinkedHashMap<>();
        Map<String, Double> modelCosts = new LinkedHashMap<>();

        double totalCost = 0.0;
        long totalTokens = 0;

        for (AiInvocationLog log : logs) {
            String modelName = log.getModelName();
            int tokens = log.getTotalTokens();

            modelTokens.put(modelName, modelTokens.getOrDefault(modelName, 0L) + tokens);
            modelCalls.put(modelName, modelCalls.getOrDefault(modelName, 0) + 1);
            totalTokens += tokens;

            // 计算成本
            double price = tokenPrices.getOrDefault(modelName, 0.0);
            double cost = (tokens / 1000.0) * price;
            modelCosts.put(modelName, modelCosts.getOrDefault(modelName, 0.0) + cost);
            totalCost += cost;
        }

        costStats.put("totalCost", Math.round(totalCost * 10000.0) / 10000.0); // 保留4位小数
        costStats.put("totalTokens", totalTokens);
        costStats.put("modelTokens", modelTokens);
        costStats.put("modelCalls", modelCalls);
        costStats.put("modelCosts", modelCosts);
        costStats.put("timeRange", startTime + " ~ " + endTime);

        return costStats;
    }

    // ===== 🆕 单次分析详情相关方法（v2.3.1） =====

    /**
     * 获取单次分析详情（按 traceId 聚合）- 🆕
     *
     * <p>聚合指定 traceId 的所有 AI 调用记录，返回完整的分析链路</p>
     *
     * @param traceId SQL 指纹
     * @return 分析详情
     */
    public com.dbdoctor.model.AnalysisTraceDetail getAnalysisTraceDetail(String traceId) {
        List<AiInvocationLog> allLogs = repository.findByTraceIdOrderByStartTimeAsc(traceId);

        if (allLogs.isEmpty()) {
            return null;
        }

        // 🔧 只返回最近一次分析的调用记录
        // 策略：找到最后一次成功的编码专家或推理专家调用，只返回该时间前后的记录
        LocalDateTime lastAnalysisTime = null;

        // 从后往前找最后一次成功的推理专家或编码专家调用
        for (int i = allLogs.size() - 1; i >= 0; i--) {
            AiInvocationLog log = allLogs.get(i);
            String agentName = log.getAgentName();
            String status = log.getStatus();

            if (("REASONING".equals(agentName) || "CODING".equals(agentName))
                    && "SUCCESS".equals(status)) {
                lastAnalysisTime = log.getStartTime();
                break;
            }
        }

        // 如果没找到推理专家或编码专家，使用最后一次成功的主治医生调用
        if (lastAnalysisTime == null) {
            for (int i = allLogs.size() - 1; i >= 0; i--) {
                AiInvocationLog log = allLogs.get(i);
                if ("SUCCESS".equals(log.getStatus())) {
                    lastAnalysisTime = log.getStartTime();
                    break;
                }
            }
        }

        // 如果还是没找到，使用最后一条记录的时间
        if (lastAnalysisTime == null && !allLogs.isEmpty()) {
            lastAnalysisTime = allLogs.get(allLogs.size() - 1).getStartTime();
        }

        LocalDateTime finalAnalysisTime = lastAnalysisTime;

        // 只保留该时间前后5分钟内的记录（一次完整的分析通常在几分钟内完成）
        List<AiInvocationLog> logs = allLogs.stream()
                .filter(log -> {
                    LocalDateTime logTime = log.getStartTime();
                    long diffMinutes = Math.abs(java.time.Duration.between(logTime, finalAnalysisTime).toMinutes());
                    return diffMinutes <= 5; // 前后5分钟内
                })
                .toList();

        log.info("[分析跟踪] traceId={}, 总记录数={}, 过滤后记录数={}, 最后分析时间={}",
                traceId, allLogs.size(), logs.size(), finalAnalysisTime);

        if (logs.isEmpty()) {
            return null;
        }

        com.dbdoctor.model.AnalysisTraceDetail detail = new com.dbdoctor.model.AnalysisTraceDetail();
        detail.setTraceId(traceId);

        // 基本信息
        detail.setStartTime(logs.get(0).getStartTime());
        detail.setEndTime(logs.get(logs.size() - 1).getEndTime());
        detail.setTotalCalls(logs.size());

        // 统计信息
        long totalDuration = 0;
        int totalTokens = 0;
        int successCount = 0;

        for (AiInvocationLog log : logs) {
            totalDuration += log.getDurationMs();
            totalTokens += log.getTotalTokens();
            if ("SUCCESS".equals(log.getStatus())) {
                successCount++;
            }
        }

        detail.setTotalDurationMs(totalDuration);
        detail.setTotalTokens(totalTokens);
        detail.setSuccessRate(successCount * 100.0 / logs.size());

        // 状态
        if (successCount == logs.size()) {
            detail.setStatus("SUCCESS");
        } else if (successCount == 0) {
            detail.setStatus("FAILED");
        } else {
            detail.setStatus("PARTIAL_FAILURE");
        }

        // 转换调用详情列表
        List<com.dbdoctor.model.AiInvocationDetail> details = logs.stream()
                .map(this::toDetail)
                .toList();
        detail.setInvocations(details);

        return detail;
    }

    /**
     * 获取所有分析记录的分页列表 - 🆕
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param page      页码（从 0 开始）
     * @param size      每页大小
     * @return 分页结果
     */
    public org.springframework.data.domain.Page<com.dbdoctor.model.AnalysisTraceSummary> listAnalysisTraces(
            LocalDateTime startTime,
            LocalDateTime endTime,
            int page,
            int size) {

        // 查询所有不重复的 traceId
        List<String> traceIds = repository.findDistinctTraceIdsByStartTimeBetween(startTime, endTime);

        // 分页
        int start = page * size;
        int end = Math.min(start + size, traceIds.size());

        if (start >= traceIds.size()) {
            // 页码超出范围，返回空结果
            return org.springframework.data.domain.Page.empty();
        }

        List<String> pageTraceIds = traceIds.subList(start, end);

        // 构建摘要列表
        List<com.dbdoctor.model.AnalysisTraceSummary> summaries = new java.util.ArrayList<>();
        for (String traceId : pageTraceIds) {
            com.dbdoctor.model.AnalysisTraceDetail detail = getAnalysisTraceDetail(traceId);
            com.dbdoctor.model.AnalysisTraceSummary summary = new com.dbdoctor.model.AnalysisTraceSummary();
            summary.setTraceId(traceId);
            summary.setStartTime(detail.getStartTime());
            summary.setTotalCalls(detail.getTotalCalls());
            summary.setTotalDurationMs(detail.getTotalDurationMs());
            summary.setTotalTokens(detail.getTotalTokens());
            summary.setStatus(detail.getStatus());
            summaries.add(summary);
        }

        return new org.springframework.data.domain.PageImpl<>(
                summaries,
                org.springframework.data.domain.PageRequest.of(page, size),
                traceIds.size()
        );
    }

    /**
     * 将 AiInvocationLog 转换为 AiInvocationDetail - 🆕
     *
     * @param log 日志实体
     * @return 详情 DTO
     */
    private com.dbdoctor.model.AiInvocationDetail toDetail(AiInvocationLog log) {
        com.dbdoctor.model.AiInvocationDetail detail = new com.dbdoctor.model.AiInvocationDetail();

        detail.setId(log.getId());
        detail.setTraceId(log.getTraceId());
        detail.setAgentCode(log.getAgentName());
        detail.setAgentDisplayName(getAgentDisplayName(log.getAgentName()));
        detail.setModelName(log.getModelName());
        detail.setProvider(log.getProvider());
        detail.setStartTime(log.getStartTime());
        detail.setEndTime(log.getEndTime());
        detail.setDurationMs(log.getDurationMs());
        detail.setStatusCode(log.getStatus());
        detail.setStatusDisplayName(getStatusDisplayName(log.getStatus()));
        detail.setInputTokens(log.getInputTokens());
        detail.setOutputTokens(log.getOutputTokens());
        detail.setTotalTokens(log.getTotalTokens());
        detail.setErrorCategory(log.getErrorCategory());
        detail.setErrorCategoryDisplayName(getErrorCategoryDisplayName(log.getErrorCategory()));
        detail.setErrorMessage(log.getErrorMessage());

        return detail;
    }

    /**
     * 获取 Agent 显示名称
     */
    private String getAgentDisplayName(String agentCode) {
        if (agentCode == null) return "未知";

        return switch (agentCode) {
            case "DIAGNOSIS" -> "主治医生";
            case "REASONING" -> "推理专家";
            case "CODING" -> "编码专家";
            default -> agentCode;
        };
    }

    /**
     * 获取状态显示名称
     */
    private String getStatusDisplayName(String statusCode) {
        if (statusCode == null) return "未知";

        return switch (statusCode) {
            case "SUCCESS" -> "成功";
            case "FAILED" -> "失败";
            case "TIMEOUT" -> "超时";
            default -> statusCode;
        };
    }

    /**
     * 获取错误分类显示名称
     */
    private String getErrorCategoryDisplayName(String errorCode) {
        if (errorCode == null) return null;

        return switch (errorCode) {
            case "TIMEOUT" -> "超时";
            case "API_ERROR" -> "API 错误";
            case "RATE_LIMIT" -> "频率限制";
            case "NETWORK_ERROR" -> "网络错误";
            case "INVALID_RESPONSE" -> "响应无效";
            case "CONTEXT_TOO_LONG" -> "上下文过长";
            case "INSUFFICIENT_QUOTA" -> "配额不足";
            case "MODEL_UNAVAILABLE" -> "模型不可用";
            case "UNKNOWN_ERROR" -> "未知错误";
            default -> errorCode;
        };
    }
}
