package com.dbdoctor.repository;

import com.dbdoctor.entity.AiInvocationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 调用日志 Repository
 *
 * <p>提供 AI 调用日志的数据访问操作</p>
 *
 * @author DB-Doctor
 * @version 2.3.0
 * @since 2.3.0
 */
@Repository
public interface AiInvocationLogRepository extends JpaRepository<AiInvocationLog, Long> {

    /**
     * 查询指定时间范围内的所有调用
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return AI 调用列表
     */
    List<AiInvocationLog> findByStartTimeBetween(
            LocalDateTime startTime,
            LocalDateTime endTime
    );

    /**
     * 根据 trace_id 查询所有相关的 AI 调用（按开始时间升序）
     *
     * @param traceId SQL 指纹
     * @return AI 调用列表
     */
    List<AiInvocationLog> findByTraceIdOrderByStartTimeAsc(String traceId);

    /**
     * 根据 Agent 角色查询指定时间范围内的调用
     *
     * @param agentName Agent 角色
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return AI 调用列表
     */
    List<AiInvocationLog> findByAgentNameAndStartTimeBetween(
            String agentName,
            LocalDateTime startTime,
            LocalDateTime endTime
    );

    /**
     * 根据状态查询指定时间范围内的调用
     *
     * @param status    状态
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return AI 调用列表
     */
    List<AiInvocationLog> findByStatusAndStartTimeBetween(
            String status,
            LocalDateTime startTime,
            LocalDateTime endTime
    );

    /**
     * 统计各 Agent 的调用次数
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return Object[] 数组，[0]=agentName, [1]=count
     */
    @Query("SELECT a.agentName, COUNT(a) FROM AiInvocationLog a " +
           "WHERE a.startTime BETWEEN :startTime AND :endTime " +
           "GROUP BY a.agentName")
    List<Object[]> countByAgentName(@Param("startTime") LocalDateTime startTime,
                                     @Param("endTime") LocalDateTime endTime);

    /**
     * 统计各 Agent 的 Token 消耗
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return Object[] 数组，[0]=agentName, [1]=totalTokens
     */
    @Query("SELECT a.agentName, SUM(a.totalTokens) FROM AiInvocationLog a " +
           "WHERE a.startTime BETWEEN :startTime AND :endTime " +
           "GROUP BY a.agentName")
    List<Object[]> sumTokensByAgentName(@Param("startTime") LocalDateTime startTime,
                                        @Param("endTime") LocalDateTime endTime);

    /**
     * 统计成功的调用次数
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 成功调用次数
     */
    @Query("SELECT COUNT(a) FROM AiInvocationLog a " +
           "WHERE a.startTime BETWEEN :startTime AND :endTime " +
           "AND a.status = 'SUCCESS'")
    long countSuccess(@Param("startTime") LocalDateTime startTime,
                      @Param("endTime") LocalDateTime endTime);

    /**
     * 统计总调用次数
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 总调用次数
     */
    @Query("SELECT COUNT(a) FROM AiInvocationLog a " +
           "WHERE a.startTime BETWEEN :startTime AND :endTime")
    long countTotal(@Param("startTime") LocalDateTime startTime,
                    @Param("endTime") LocalDateTime endTime);

    /**
     * 计算平均耗时（仅成功的调用）
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 平均耗时（毫秒）
     */
    @Query("SELECT AVG(a.durationMs) FROM AiInvocationLog a " +
           "WHERE a.startTime BETWEEN :startTime AND :endTime " +
           "AND a.status = 'SUCCESS'")
    Double avgDuration(@Param("startTime") LocalDateTime startTime,
                       @Param("endTime") LocalDateTime endTime);

    /**
     * 计算最大耗时
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 最大耗时（毫秒）
     */
    @Query("SELECT MAX(a.durationMs) FROM AiInvocationLog a " +
           "WHERE a.startTime BETWEEN :startTime AND :endTime " +
           "AND a.status = 'SUCCESS'")
    Long maxDuration(@Param("startTime") LocalDateTime startTime,
                     @Param("endTime") LocalDateTime endTime);

    /**
     * 计算最小耗时
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 最小耗时（毫秒）
     */
    @Query("SELECT MIN(a.durationMs) FROM AiInvocationLog a " +
           "WHERE a.startTime BETWEEN :startTime AND :endTime " +
           "AND a.status = 'SUCCESS'")
    Long minDuration(@Param("startTime") LocalDateTime startTime,
                     @Param("endTime") LocalDateTime endTime);

    /**
     * 按小时统计调用次数（用于趋势图）
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return Object[] 数组，[0]=hour, [1]=count
     */
    @Query("SELECT HOUR(a.startTime), COUNT(a) FROM AiInvocationLog a " +
           "WHERE a.startTime BETWEEN :startTime AND :endTime " +
           "GROUP BY HOUR(a.startTime) " +
           "ORDER BY HOUR(a.startTime)")
    List<Object[]> countByHour(@Param("startTime") LocalDateTime startTime,
                                @Param("endTime") LocalDateTime endTime);

    /**
     * 统计总 Token 消耗
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 总 Token 数
     */
    @Query("SELECT SUM(a.totalTokens) FROM AiInvocationLog a " +
           "WHERE a.startTime BETWEEN :startTime AND :endTime")
    Long sumTotalTokens(@Param("startTime") LocalDateTime startTime,
                        @Param("endTime") LocalDateTime endTime);

    /**
     * 统计输入 Token 总数
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 输入 Token 总数
     */
    @Query("SELECT SUM(a.inputTokens) FROM AiInvocationLog a " +
           "WHERE a.startTime BETWEEN :startTime AND :endTime")
    Long sumInputTokens(@Param("startTime") LocalDateTime startTime,
                        @Param("endTime") LocalDateTime endTime);

    /**
     * 统计输出 Token 总数
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 输出 Token 总数
     */
    @Query("SELECT SUM(a.outputTokens) FROM AiInvocationLog a " +
           "WHERE a.startTime BETWEEN :startTime AND :endTime")
    Long sumOutputTokens(@Param("startTime") LocalDateTime startTime,
                         @Param("endTime") LocalDateTime endTime);

    /**
     * 分页查询调用日志（支持多条件过滤）
     *
     * @param startTime 开始时间（可选）
     * @param endTime   结束时间（可选）
     * @param agentName Agent 角色（可选）
     * @param status    状态（可选）
     * @return 调用日志列表
     */
    @Query("SELECT a FROM AiInvocationLog a WHERE " +
           "(:startTime IS NULL OR a.startTime >= :startTime) AND " +
           "(:endTime IS NULL OR a.startTime <= :endTime) AND " +
           "(:agentName IS NULL OR a.agentName = :agentName) AND " +
           "(:status IS NULL OR a.status = :status) " +
           "ORDER BY a.startTime DESC")
    List<AiInvocationLog> findByConditions(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("agentName") String agentName,
            @Param("status") String status
    );

    /**
     * 删除指定时间之前的数据（用于数据归档）
     *
     * @param beforeTime 删除此时间之前的数据
     * @return 删除的记录数
     */
    @Query("DELETE FROM AiInvocationLog a WHERE a.createdTime < :beforeTime")
    int deleteByCreatedTimeBefore(@Param("beforeTime") LocalDateTime beforeTime);

    /**
     * 统计各错误类型的数量
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return Object[] 数组，[0]=errorCategory, [1]=count
     */
    @Query("SELECT a.errorCategory, COUNT(a) FROM AiInvocationLog a " +
           "WHERE a.startTime BETWEEN :startTime AND :endTime " +
           "AND a.status != 'SUCCESS' " +
           "GROUP BY a.errorCategory")
    List<Object[]> countByErrorCategory(@Param("startTime") LocalDateTime startTime,
                                         @Param("endTime") LocalDateTime endTime);

    /**
     * 查询指定时间内的所有不重复 traceId（按开始时间降序）
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return traceId 列表
     */
    @Query("SELECT DISTINCT a.traceId FROM AiInvocationLog a " +
           "WHERE a.startTime BETWEEN :startTime AND :endTime " +
           "ORDER BY a.startTime DESC")
    List<String> findDistinctTraceIdsByStartTimeBetween(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
}
