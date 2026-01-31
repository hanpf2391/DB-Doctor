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
     * @param log 日志实体
     */
    @Async("monitoringExecutor")
    @Transactional
    public void saveAsync(AiInvocationLog log) {
        try {
            repository.save(log);
            log.debug("[AI监控] 日志已保存: id={}, agent={}, duration={}ms",
                    log.getId(), log.getAgentName(), log.getDurationMs());
        } catch (Exception e) {
            log.error("[AI监控] 保存日志失败: agent={}, traceId={}",
                    log.getAgentName(), log.getTraceId(), e);
            // 不抛出异常，避免影响 AI 调用
        }
    }

    /**
     * 同步保存 AI 调用日志
     *
     * @param log 日志实体
     * @return 保存后的实体
     */
    @Transactional
    public AiInvocationLog save(AiInvocationLog log) {
        return repository.save(log);
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
}
