# AI 监控功能需求设计文档 (AI Observability)

## 文档信息

| 项目 | 内容 |
|------|------|
| 功能名称 | AI 可观测性监控系统 (AI Observability Monitor) |
| 文档版本 | v1.0.0 |
| 创建日期 | 2026-01-31 |
| 适用版本 | DB-Doctor v2.3.0+ |
| 作者 | DB-Doctor Team |

---

## 一、功能概述

### 1.1 背景

DB-Doctor 采用多 Agent 架构（主治医生、推理专家、编码专家），一次慢查询诊断可能触发多次 AI 调用。如果没有完善的监控体系，一旦出现性能问题或调用失败，将无法快速定位问题根源。

### 1.2 目标

实现全方位的 AI 调用监控，让系统具备：
- **可度量**：精确统计 Token 消耗、调用耗时、成功率
- **可追踪**：关联 SQL 指纹，实现全链路追踪
- **可优化**：识别瓶颈 Agent，优化 Prompt 和模型选择
- **可运维**：实时监控 AI 健康状态，快速定位故障

### 1.3 核心价值

| 价值点 | 说明 |
|--------|------|
| 成本控制 | 精确统计 Token 消耗，换算为费用（云端模型） |
| 性能优化 | 识别耗时最长的 Agent，针对性优化 |
| 稳定性保障 | 实时监控成功率，及时熔断异常 Agent |
| 调试效率 | 完整记录每次调用的 Prompt 和 Response，便于问题排查 |

---

## 二、监控指标体系设计

### 2.1 核心指标分类

```
┌─────────────────────────────────────────────────────────────┐
│                    AI 监控指标体系                           │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐         │
│  │  基础指标    │  │  质量指标    │  │  业务指标    │         │
│  ├─────────────┤  ├─────────────┤  ├─────────────┤         │
│  │ • 调用次数   │  │ • 成功率     │  │ • Agent分布  │         │
│  │ • Token消耗  │  │ • 错误类型   │  │ • 模型分布   │         │
│  │ • 调用耗时   │  │ • 超时率     │  │ • SQL关联度  │         │
│  │ • 时间分布   │  │ • 重试次数   │  │ • 成本换算   │         │
│  └─────────────┘  └─────────────┘  └─────────────┘         │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 指标详细定义

#### 2.2.1 基础指标

| 指标名称 | 说明 | 计算方式 |
|---------|------|---------|
| 总调用次数 | 统计时间段内的 AI 调用总次数 | `COUNT(*)` |
| 总 Token 消耗 | 输入 + 输出 Token 总数 | `SUM(input_tokens + output_tokens)` |
| 平均耗时 | 单次调用的平均响应时间 | `AVG(duration_ms)` |
| P95/P99 耗时 | 95%/99% 分位数的响应时间 | `PERCENTILE(duration_ms, 0.95)` |

#### 2.2.2 质量指标

| 指标名称 | 说明 | 计算方式 |
|---------|------|---------|
| 成功率 | 成功调用占比 | `SUCCESS 次数 / 总次数 * 100%` |
| 失败率分布 | 各类错误占比 | 按错误类型 GROUP BY |
| 超时率 | 超时调用占比 | `超时次数 / 总次数 * 100%` |

#### 2.2.3 业务指标

| 指标名称 | 说明 | 计算方式 |
|---------|------|---------|
| Agent Token 分布 | 各 Agent 的 Token 消耗占比 | 按 `agent_name` 分组统计 |
| 输入输出比 | 输入 Token / 输出 Token | `SUM(input_tokens) / SUM(output_tokens)` |
| 成本换算 | 根据 Token 单价计算费用 | `总 Token * 单价`（云端模型） |
| SQL 关联度 | 平均每个 SQL 触发的 AI 调用次数 | `调用次数 / DISTINCT(trace_id)` |

### 2.3 分维度监控

#### 2.3.1 按 Agent 角色监控

```
DBAgent（主治医生）
  ├─ Token 消耗：45%
  ├─ 平均耗时：3.2s
  ├─ 成功率：99.5%
  └─ 主要功能：初步诊断、证据收集

ReasoningAgent（推理专家）
  ├─ Token 消耗：30%
  ├─ 平均耗时：8.5s
  ├─ 成功率：98.2%
  └─ 主要功能：深度推理、根因分析

CodingAgent（编码专家）
  ├─ Token 消耗：25%
  ├─ 平均耗时：5.1s
  ├─ 成功率：97.8%
  └─ 主要功能：生成优化代码
```

#### 2.3.2 按模型监控

| 模型名称 | 调用次数 | Token 消耗 | 平均耗时 | 成功率 |
|---------|---------|-----------|---------|--------|
| qwen2.5:7b | 1,205 | 1.2M | 3.5s | 99.5% |
| deepseek-r1 | 356 | 0.8M | 12.3s | 98.2% |
| gpt-4 | 120 | 0.5M | 8.7s | 99.9% |

#### 2.3.3 按时间维度监控

- **实时监控**：最近 5 分钟的调用情况
- **小时维度**：24 小时趋势分析
- **日维度**：最近 30 天的历史数据
- **月维度**：长期趋势和成本分析

---

## 三、数据库设计

### 3.1 新增表：ai_invocation_log

```sql
CREATE TABLE `ai_invocation_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `trace_id` VARCHAR(64) NOT NULL COMMENT 'SQL指纹（关联慢查询模板）',
  `agent_name` VARCHAR(20) NOT NULL COMMENT 'Agent角色（DIAGNOSIS/REASONING/CODING）',
  `model_name` VARCHAR(50) NOT NULL COMMENT '模型名称（qwen2.5:7b/deepseek-r1）',
  `provider` VARCHAR(20) NOT NULL COMMENT '供应商（ollama/openai/deepseek）',

  -- Token 统计
  `input_tokens` INT NOT NULL DEFAULT 0 COMMENT '输入Token数',
  `output_tokens` INT NOT NULL DEFAULT 0 COMMENT '输出Token数',
  `total_tokens` INT NOT NULL DEFAULT 0 COMMENT '总Token数',

  -- 性能指标
  `duration_ms` BIGINT NOT NULL COMMENT '耗时（毫秒）',
  `start_time` DATETIME NOT NULL COMMENT '开始时间',
  `end_time` DATETIME NOT NULL COMMENT '结束时间',

  -- 状态和错误
  `status` VARCHAR(10) NOT NULL COMMENT '状态（SUCCESS/FAILED/TIMEOUT）',
  `error_category` VARCHAR(30) COMMENT '错误分类（TIMEOUT/API_ERROR/RATE_LIMIT）',
  `error_message` TEXT COMMENT '错误信息（失败时记录）',

  -- 详细内容（可选，用于调试）
  `prompt_text` MEDIUMTEXT COMMENT '提示词（根据配置决定是否存储）',
  `response_text` MEDIUMTEXT COMMENT '响应内容（根据配置决定是否存储）',

  -- 元数据
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `tags` JSON COMMENT '扩展标签（如 {"env":"prod","version":"v2.3.0"}）',

  PRIMARY KEY (`id`),
  KEY `idx_trace_id` (`trace_id`),
  KEY `idx_agent_name` (`agent_name`),
  KEY `idx_model_name` (`model_name`),
  KEY `idx_status` (`status`),
  KEY `idx_created_time` (`created_time`),
  KEY `idx_start_time` (`start_time`),
  KEY `idx_trace_agent` (`trace_id`, `agent_name`)  -- 组合索引，用于查询某SQL的所有AI调用
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI调用日志表';
```

### 3.2 字段说明

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | BIGINT | ✓ | 主键，自增 |
| trace_id | VARCHAR(64) | ✓ | 关联 `slow_query_template.sql_fingerprint` |
| agent_name | VARCHAR(20) | ✓ | 枚举值：`DIAGNOSIS`、`REASONING`、`CODING` |
| model_name | VARCHAR(50) | ✓ | 模型名称，如 `qwen2.5:7b` |
| provider | VARCHAR(20) | ✓ | 供应商：`ollama`、`openai`、`deepseek` |
| input_tokens | INT | ✓ | 输入 Token 数 |
| output_tokens | INT | ✓ | 输出 Token 数 |
| total_tokens | INT | ✓ | 总 Token 数 |
| duration_ms | BIGINT | ✓ | 调用耗时（毫秒） |
| start_time | DATETIME | ✓ | 调用开始时间 |
| end_time | DATETIME | ✓ | 调用结束时间 |
| status | VARCHAR(10) | ✓ | 枚举值：`SUCCESS`、`FAILED`、`TIMEOUT` |
| error_category | VARCHAR(30) | ✗ | 错误分类 |
| error_message | TEXT | ✗ | 错误详情 |
| prompt_text | MEDIUMTEXT | ✗ | 提示词（可选存储） |
| response_text | MEDIUMTEXT | ✗ | 响应内容（可选存储） |
| created_time | DATETIME | ✓ | 记录创建时间 |
| tags | JSON | ✗ | 扩展标签 |

### 3.3 索引设计

| 索引名 | 字段 | 用途 |
|--------|------|------|
| PRIMARY | id | 主键 |
| idx_trace_id | trace_id | 查询某 SQL 的所有 AI 调用 |
| idx_agent_name | agent_name | 按 Agent 统计 |
| idx_model_name | model_name | 按模型统计 |
| idx_status | status | 按状态统计（成功率） |
| idx_created_time | created_time | 时间范围查询 |
| idx_start_time | start_time | 趋势图查询 |
| idx_trace_agent | trace_id, agent_name | 组合查询 |

---

## 四、后端设计

### 4.1 整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                       AI 监控架构                            │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────────┐    ┌──────────────────────────────────┐  │
│  │ ChatModel    │───▶│ AiMonitoringListener             │  │
│  │ (LangChain4j)│    │ - 监听所有 AI 调用               │  │
│  └──────────────┘    │ - 提取 Token、耗时等指标         │  │
│                      │ - 异步写入数据库                  │  │
│                      └──────────────────────────────────┘  │
│                                       │                     │
│                                       ▼                     │
│                      ┌──────────────────────────────────┐  │
│                      │ AiInvocationLogService           │  │
│                      │ - 统计聚合                        │  │
│                      │ - 指标计算                        │  │
│                      │ - 缓存管理                        │  │
│                      └──────────────────────────────────┘  │
│                                       │                     │
│                                       ▼                     │
│                      ┌──────────────────────────────────┐  │
│                      │ AiMonitorController              │  │
│                      │ - 提供监控 API                   │  │
│                      │ - 实时数据推送（WebSocket）      │  │
│                      └──────────────────────────────────┘  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 4.2 核心类设计

#### 4.2.1 Entity: AiInvocationLog

```java
package com.dbdoctor.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * AI 调用日志实体
 *
 * @author DB-Doctor
 * @version 2.3.0
 */
@Data
@Entity
@Table(name = "ai_invocation_log")
public class AiInvocationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * SQL 指纹（关联 slow_query_template.sql_fingerprint）
     */
    @Column(name = "trace_id", nullable = false, length = 64)
    private String traceId;

    /**
     * Agent 角色（DIAGNOSIS/REASONING/CODING）
     */
    @Column(name = "agent_name", nullable = false, length = 20)
    private String agentName;

    /**
     * 模型名称（qwen2.5:7b/deepseek-r1）
     */
    @Column(name = "model_name", nullable = false, length = 50)
    private String modelName;

    /**
     * 供应商（ollama/openai/deepseek）
     */
    @Column(name = "provider", nullable = false, length = 20)
    private String provider;

    /**
     * 输入 Token 数
     */
    @Column(name = "input_tokens", nullable = false)
    private Integer inputTokens;

    /**
     * 输出 Token 数
     */
    @Column(name = "output_tokens", nullable = false)
    private Integer outputTokens;

    /**
     * 总 Token 数
     */
    @Column(name = "total_tokens", nullable = false)
    private Integer totalTokens;

    /**
     * 耗时（毫秒）
     */
    @Column(name = "duration_ms", nullable = false)
    private Long durationMs;

    /**
     * 开始时间
     */
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    /**
     * 状态（SUCCESS/FAILED/TIMEOUT）
     */
    @Column(name = "status", nullable = false, length = 10)
    private String status;

    /**
     * 错误分类
     */
    @Column(name = "error_category", length = 30)
    private String errorCategory;

    /**
     * 错误信息
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * 提示词（可选存储）
     */
    @Column(name = "prompt_text", columnDefinition = "MEDIUMTEXT")
    private String promptText;

    /**
     * 响应内容（可选存储）
     */
    @Column(name = "response_text", columnDefinition = "MEDIUMTEXT")
    private String responseText;

    /**
     * 创建时间
     */
    @Column(name = "created_time", nullable = false, updatable = false)
    private LocalDateTime createdTime;

    /**
     * 扩展标签（JSON）
     */
    @Column(name = "tags", columnDefinition = "JSON")
    private String tags;
}
```

#### 4.2.2 Enum: AgentName

```java
package com.dbdoctor.common.enums;

import lombok.Getter;

/**
 * Agent 角色枚举
 *
 * @author DB-Doctor
 * @version 2.3.0
 */
@Getter
public enum AgentName {

    /**
     * 主治医生
     */
    DIAGNOSIS("DIAGNOSIS", "主治医生", "DBAgent"),

    /**
     * 推理专家
     */
    REASONING("REASONING", "推理专家", "ReasoningAgent"),

    /**
     * 编码专家
     */
    CODING("CODING", "编码专家", "CodingAgent");

    private final String code;
    private final String displayName;
    private final String className;

    AgentName(String code, String displayName, String className) {
        this.code = code;
        this.displayName = displayName;
        this.className = className;
    }

    public static AgentName fromClassName(String className) {
        for (AgentName agent : values()) {
            if (agent.className.equals(className)) {
                return agent;
            }
        }
        throw new IllegalArgumentException("Unknown Agent class: " + className);
    }
}
```

#### 4.2.3 Enum: InvocationStatus

```java
package com.dbdoctor.common.enums;

import lombok.Getter;

/**
 * AI 调用状态枚举
 *
 * @author DB-Doctor
 * @version 2.3.0
 */
@Getter
public enum InvocationStatus {

    /**
     * 调用成功
     */
    SUCCESS("SUCCESS", "成功"),

    /**
     * 调用失败
     */
    FAILED("FAILED", "失败"),

    /**
     * 调用超时
     */
    TIMEOUT("TIMEOUT", "超时");

    private final String code;
    private final String displayName;

    InvocationStatus(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }
}
```

#### 4.2.4 Enum: ErrorCategory

```java
package com.dbdoctor.common.enums;

import lombok.Getter;

/**
 * 错误分类枚举
 *
 * @author DB-Doctor
 * @version 2.3.0
 */
@Getter
public enum ErrorCategory {

    /**
     * 超时
     */
    TIMEOUT("TIMEOUT", "超时"),

    /**
     * API 错误
     */
    API_ERROR("API_ERROR", "API错误"),

    /**
     * 速率限制
     */
    RATE_LIMIT("RATE_LIMIT", "速率限制"),

    /**
     * 网络错误
     */
    NETWORK_ERROR("NETWORK_ERROR", "网络错误"),

    /**
     * 配置错误
     */
    CONFIG_ERROR("CONFIG_ERROR", "配置错误"),

    /**
     * 未知错误
     */
    UNKNOWN("UNKNOWN", "未知错误");

    private final String code;
    private final String displayName;

    ErrorCategory(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }
}
```

#### 4.2.5 Repository: AiInvocationLogRepository

```java
package com.dbdoctor.repository;

import com.dbdoctor.entity.AiInvocationLog;
import com.dbdoctor.common.enums.AgentName;
import com.dbdoctor.common.enums.InvocationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * AI 调用日志 Repository
 *
 * @author DB-Doctor
 * @version 2.3.0
 */
@Repository
public interface AiInvocationLogRepository extends JpaRepository<AiInvocationLog, Long> {

    /**
     * 根据 trace_id 查询所有相关的 AI 调用
     *
     * @param traceId SQL 指纹
     * @return AI 调用列表
     */
    List<AiInvocationLog> findByTraceIdOrderByStartTimeAsc(String traceId);

    /**
     * 根据 Agent 角色查询
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
     * 统计各 Agent 的调用次数
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return Map<AgentName, Count>
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
     * @return Map<AgentName, TotalTokens>
     */
    @Query("SELECT a.agentName, SUM(a.totalTokens) FROM AiInvocationLog a " +
           "WHERE a.startTime BETWEEN :startTime AND :endTime " +
           "GROUP BY a.agentName")
    List<Object[]> sumTokensByAgentName(@Param("startTime") LocalDateTime startTime,
                                        @Param("endTime") LocalDateTime endTime);

    /**
     * 统计成功率
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 成功调用次数 / 总调用次数
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
     * 计算平均耗时
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
     * 按小时统计调用次数（用于趋势图）
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return Map<HOUR, COUNT>
     */
    @Query("SELECT HOUR(a.startTime), COUNT(a) FROM AiInvocationLog a " +
           "WHERE a.startTime BETWEEN :startTime AND :endTime " +
           "GROUP BY HOUR(a.startTime) " +
           "ORDER BY HOUR(a.startTime)")
    List<Object[]> countByHour(@Param("startTime") LocalDateTime startTime,
                                @Param("endTime") LocalDateTime endTime);

    /**
     * 分页查询调用日志
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
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
}
```

#### 4.2.6 DTO: AiMonitorStats

```java
package com.dbdoctor.model;

import lombok.Data;
import java.util.Map;

/**
 * AI 监控统计数据
 *
 * @author DB-Doctor
 * @version 2.3.0
 */
@Data
public class AiMonitorStats {

    /**
     * 总调用次数
     */
    private Long totalCalls;

    /**
     * 总 Token 消耗
     */
    private Long totalTokens;

    /**
     * 平均耗时（毫秒）
     */
    private Double avgDuration;

    /**
     * 成功率（百分比）
     */
    private Double successRate;

    /**
     * 各 Agent 的 Token 分布
     */
    private Map<String, Long> agentTokenDistribution;

    /**
     * 各 Agent 的调用次数
     */
    private Map<String, Long> agentCallDistribution;

    /**
     * 按小时的调用次数（用于趋势图）
     */
    private Map<Integer, Long> hourlyCallCount;

    /**
     * 统计时间范围
     */
    private String timeRange;
}
```

#### 4.2.7 DTO: AiInvocationDetail

```java
package com.dbdoctor.model;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * AI 调用详情
 *
 * @author DB-Doctor
 * @version 2.3.0
 */
@Data
public class AiInvocationDetail {

    /**
     * 主键 ID
     */
    private Long id;

    /**
     * SQL 指纹
     */
    private String traceId;

    /**
     * Agent 角色名称（中文）
     */
    private String agentDisplayName;

    /**
     * 模型名称
     */
    private String modelName;

    /**
     * 供应商
     */
    private String provider;

    /**
     * 输入 Token 数
     */
    private Integer inputTokens;

    /**
     * 输出 Token 数
     */
    private Integer outputTokens;

    /**
     * 总 Token 数
     */
    private Integer totalTokens;

    /**
     * 耗时（毫秒）
     */
    private Long durationMs;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 状态（中文）
     */
    private String statusDisplayName;

    /**
     * 错误分类
     */
    private String errorCategory;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 提示词（可选）
     */
    private String promptText;

    /**
     * 响应内容（可选）
     */
    private String responseText;
}
```

#### 4.2.8 Service: AiInvocationLogService

```java
package com.dbdoctor.service;

import com.dbdoctor.entity.AiInvocationLog;
import com.dbdoctor.repository.AiInvocationLogRepository;
import com.dbdoctor.model.AiMonitorStats;
import com.dbdoctor.model.AiInvocationDetail;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * AI 调用日志服务
 *
 * @author DB-Doctor
 * @version 2.3.0
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
            log.debug("AI 调用日志已保存: id={}, agent={}, duration={}ms",
                    log.getId(), log.getAgentName(), log.getDurationMs());
        } catch (Exception e) {
            log.error("保存 AI 调用日志失败", e);
            // 不抛出异常，避免影响 AI 调用
        }
    }

    /**
     * 获取监控统计数据
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 统计数据
     */
    public AiMonitorStats getStats(LocalDateTime startTime, LocalDateTime endTime) {
        AiMonitorStats stats = new AiMonitorStats();

        // 基础统计
        stats.setTotalCalls(repository.countTotal(startTime, endTime));
        stats.setAvgDuration(repository.avgDuration(startTime, endTime));

        // 成功率
        long successCount = repository.countSuccess(startTime, endTime);
        long totalCount = stats.getTotalCalls();
        stats.setSuccessRate(totalCount > 0 ? (successCount * 100.0 / totalCount) : 0.0);

        // Agent 分布统计
        Map<String, Long> agentTokenDist = new HashMap<>();
        Map<String, Long> agentCallDist = new HashMap<>();

        repository.sumTokensByAgentName(startTime, endTime).forEach(row -> {
            agentTokenDist.put((String) row[0], (Long) row[1]);
        });

        repository.countByAgentName(startTime, endTime).forEach(row -> {
            agentCallDist.put((String) row[0], (Long) row[1]);
        });

        stats.setAgentTokenDistribution(agentTokenDist);
        stats.setAgentCallDistribution(agentCallDist);

        // 按小时统计
        Map<Integer, Long> hourlyCount = new LinkedHashMap<>();
        repository.countByHour(startTime, endTime).forEach(row -> {
            hourlyCount.put((Integer) row[0], (Long) row[1]);
        });
        stats.setHourlyCallCount(hourlyCount);

        // 时间范围描述
        stats.setTimeRange(startTime + " ~ " + endTime);

        return stats;
    }

    /**
     * 根据 SQL 指纹查询所有相关的 AI 调用
     *
     * @param traceId SQL 指纹
     * @return 调用详情列表
     */
    public List<AiInvocationDetail> getByTraceId(String traceId) {
        return repository.findByTraceIdOrderByStartTimeAsc(traceId).stream()
                .map(this::toDetail)
                .toList();
    }

    /**
     * 分页查询调用日志
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param agentName Agent 角色（可选）
     * @param status    状态（可选）
     * @return 调用详情列表
     */
    public List<AiInvocationDetail> query(
            LocalDateTime startTime,
            LocalDateTime endTime,
            String agentName,
            String status) {
        return repository.findByConditions(startTime, endTime, agentName, status)
                .stream()
                .map(this::toDetail)
                .toList();
    }

    /**
     * 转换为详情对象
     */
    private AiInvocationDetail toDetail(AiInvocationLog log) {
        AiInvocationDetail detail = new AiInvocationDetail();
        detail.setId(log.getId());
        detail.setTraceId(log.getTraceId());
        detail.setAgentDisplayName(getAgentDisplayName(log.getAgentName()));
        detail.setModelName(log.getModelName());
        detail.setProvider(log.getProvider());
        detail.setInputTokens(log.getInputTokens());
        detail.setOutputTokens(log.getOutputTokens());
        detail.setTotalTokens(log.getTotalTokens());
        detail.setDurationMs(log.getDurationMs());
        detail.setStartTime(log.getStartTime());
        detail.setStatusDisplayName(getStatusDisplayName(log.getStatus()));
        detail.setErrorCategory(log.getErrorCategory());
        detail.setErrorMessage(log.getErrorMessage());
        detail.setPromptText(log.getPromptText());
        detail.setResponseText(log.getResponseText());
        return detail;
    }

    private String getAgentDisplayName(String agentCode) {
        return switch (agentCode) {
            case "DIAGNOSIS" -> "主治医生";
            case "REASONING" -> "推理专家";
            case "CODING" -> "编码专家";
            default -> agentCode;
        };
    }

    private String getStatusDisplayName(String statusCode) {
        return switch (statusCode) {
            case "SUCCESS" -> "成功";
            case "FAILED" -> "失败";
            case "TIMEOUT" -> "超时";
            default -> statusCode;
        };
    }
}
```

#### 4.2.9 Core: AiMonitoringListener

```java
package com.dbdoctor.monitoring;

import com.dbdoctor.common.enums.AgentName;
import com.dbdoctor.common.enums.ErrorCategory;
import com.dbdoctor.common.enums.InvocationStatus;
import com.dbdoctor.entity.AiInvocationLog;
import com.dbdoctor.service.AiInvocationLogService;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequest;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponse;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI 监控监听器
 *
 * 核心职责：
 * - 监听所有 AI 调用（LangChain4j ChatModelListener）
 * - 提取 Token、耗时等指标
 * - 异步写入数据库
 *
 * @author DB-Doctor
 * @version 2.3.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiMonitoringListener implements ChatModelListener {

    private final AiInvocationLogService logService;

    /**
     * 存储请求开始时间（thread-safe）
     * Key: requestId, Value: startTime
     */
    private final ConcurrentHashMap<String, LocalDateTime> requestStartTimes = new ConcurrentHashMap<>();

    /**
     * 存储 Agent 名称（需要在调用时通过 attributes 传递）
     */
    private static final String ATTR_AGENT_NAME = "agentName";
    private static final String ATTR_TRACE_ID = "traceId";

    @Override
    public void onRequest(ChatModelRequestContext context) {
        ChatModelRequest request = context.chatModelRequest();
        String requestId = generateRequestId(request);

        // 记录开始时间
        requestStartTimes.put(requestId, LocalDateTime.now());

        log.debug("[AI监控] 请求开始: model={}, requestId={}",
                request.model(), requestId);
    }

    @Override
    public void onResponse(ChatModelResponseContext context) {
        ChatModelRequest request = context.chatModelRequest();
        ChatResponse response = context.chatResponse();
        String requestId = generateRequestId(request);

        // 获取开始时间
        LocalDateTime startTime = requestStartTimes.remove(requestId);
        if (startTime == null) {
            log.warn("[AI监控] 无法找到请求开始时间: requestId={}", requestId);
            return;
        }

        LocalDateTime endTime = LocalDateTime.now();
        long durationMs = java.time.Duration.between(startTime, endTime).toMillis();

        // 从 attributes 获取元数据
        String agentName = (String) context.attributes().get(ATTR_AGENT_NAME);
        String traceId = (String) context.attributes().get(ATTR_TRACE_ID);

        // 构建日志实体
        AiInvocationLog log = new AiInvocationLog();
        log.setTraceId(traceId != null ? traceId : "UNKNOWN");
        log.setAgentName(agentName != null ? agentName : "UNKNOWN");
        log.setModelName(request.model());
        log.setProvider(extractProvider(request.model()));
        log.setStartTime(startTime);
        log.setEndTime(endTime);
        log.setDurationMs(durationMs);
        log.setStatus(InvocationStatus.SUCCESS.getCode());
        log.setCreatedTime(LocalDateTime.now());

        // Token 统计
        if (response.tokenUsage() != null) {
            log.setInputTokens(response.tokenUsage().inputTokenCount());
            log.setOutputTokens(response.tokenUsage().outputTokenCount());
            log.setTotalTokens(response.tokenUsage().totalTokenCount());
        } else {
            log.setInputTokens(0);
            log.setOutputTokens(0);
            log.setTotalTokens(0);
            log.warn("[AI监控] Token 统计信息缺失: model={}", request.model());
        }

        // 可选：存储提示词和响应（根据配置决定）
        // log.setPromptText(request.chatMessages().toString());
        // log.setResponseText(response.text());

        // 异步保存
        logService.saveAsync(log);

        log.debug("[AI监控] 请求成功: model={}, agent={}, tokens={}, duration={}ms",
                request.model(), agentName, log.getTotalTokens(), durationMs);
    }

    @Override
    public void onError(ChatModelErrorContext context) {
        ChatModelRequest request = context.chatModelRequest();
        Throwable error = context.error();
        String requestId = generateRequestId(request);

        // 获取开始时间
        LocalDateTime startTime = requestStartTimes.remove(requestId);
        if (startTime == null) {
            log.warn("[AI监控] 无法找到请求开始时间: requestId={}", requestId);
            return;
        }

        LocalDateTime endTime = LocalDateTime.now();
        long durationMs = java.time.Duration.between(startTime, endTime).toMillis();

        // 从 attributes 获取元数据
        String agentName = (String) context.attributes().get(ATTR_AGENT_NAME);
        String traceId = (String) context.attributes().get(ATTR_TRACE_ID);

        // 构建日志实体
        AiInvocationLog log = new AiInvocationLog();
        log.setTraceId(traceId != null ? traceId : "UNKNOWN");
        log.setAgentName(agentName != null ? agentName : "UNKNOWN");
        log.setModelName(request.model());
        log.setProvider(extractProvider(request.model()));
        log.setStartTime(startTime);
        log.setEndTime(endTime);
        log.setDurationMs(durationMs);
        log.setStatus(determineStatus(error));
        log.setErrorCategory(categorizeError(error));
        log.setErrorMessage(error.getMessage());
        log.setCreatedTime(LocalDateTime.now());

        // Token 统计（失败时可能没有）
        log.setInputTokens(0);
        log.setOutputTokens(0);
        log.setTotalTokens(0);

        // 异步保存
        logService.saveAsync(log);

        log.error("[AI监控] 请求失败: model={}, agent={}, error={}, duration={}ms",
                request.model(), agentName, error.getMessage(), durationMs);
    }

    /**
     * 生成请求 ID
     */
    private String generateRequestId(ChatModelRequest request) {
        return request.model() + "_" + request.hashCode() + "_" + System.currentTimeMillis();
    }

    /**
     * 从模型名称提取供应商
     */
    private String extractProvider(String modelName) {
        if (modelName.contains("qwen") || modelName.contains("deepseek")) {
            return "ollama";
        } else if (modelName.contains("gpt")) {
            return "openai";
        }
        return "unknown";
    }

    /**
     * 根据异常确定状态
     */
    private String determineStatus(Throwable error) {
        if (error instanceof java.util.concurrent.TimeoutException) {
            return InvocationStatus.TIMEOUT.getCode();
        }
        return InvocationStatus.FAILED.getCode();
    }

    /**
     * 错误分类
     */
    private String categorizeError(Throwable error) {
        String message = error.getMessage();
        if (message == null) {
            return ErrorCategory.UNKNOWN.getCode();
        }

        if (message.contains("timeout")) {
            return ErrorCategory.TIMEOUT.getCode();
        } else if (message.contains("rate limit")) {
            return ErrorCategory.RATE_LIMIT.getCode();
        } else if (message.contains("connection") || message.contains("network")) {
            return ErrorCategory.NETWORK_ERROR.getCode();
        } else if (message.contains("401") || message.contains("403")) {
            return ErrorCategory.CONFIG_ERROR.getCode();
        } else if (message.contains("500") || message.contains("502")) {
            return ErrorCategory.API_ERROR.getCode();
        }

        return ErrorCategory.UNKNOWN.getCode();
    }
}
```

#### 4.2.10 Controller: AiMonitorController

```java
package com.dbdoctor.controller;

import com.dbdoctor.common.Result;
import com.dbdoctor.model.AiMonitorStats;
import com.dbdoctor.model.AiInvocationDetail;
import com.dbdoctor.service.AiInvocationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 监控 API
 *
 * @author DB-Doctor
 * @version 2.3.0
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

        // 默认最近 24 小时
        if (startTime == null) {
            startTime = LocalDateTime.now().minusHours(24);
        }
        if (endTime == null) {
            endTime = LocalDateTime.now();
        }

        AiMonitorStats stats = logService.getStats(startTime, endTime);
        return Result.success(stats);
    }

    /**
     * 根据 SQL 指纹查询所有相关的 AI 调用
     *
     * @param traceId SQL 指纹
     * @return 调用详情列表
     */
    @GetMapping("/by-trace/{traceId}")
    public Result<List<AiInvocationDetail>> getByTraceId(@PathVariable String traceId) {
        List<AiInvocationDetail> details = logService.getByTraceId(traceId);
        return Result.success(details);
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

        List<AiInvocationDetail> details = logService.query(startTime, endTime, agentName, status);
        return Result.success(details);
    }
}
```

#### 4.2.11 Config: MonitoringConfig（新增线程池）

```java
package com.dbdoctor.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 监控线程池配置
 *
 * @author DB-Doctor
 * @version 2.3.0
 */
@Slf4j
@Configuration
@EnableAsync
public class MonitoringConfig {

    /**
     * 监控日志异步写入线程池
     *
     * 核心配置：
     * - 核心线程数：2
     * - 最大线程数：5
     * - 队列容量：1000
     * - 拒绝策略：CallerRunsPolicy（主线程执行，保证不丢数据）
     */
    @Bean("monitoringExecutor")
    public Executor monitoringExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(1000);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("ai-monitor-");

        // 拒绝策略：由调用线程执行（保证不丢数据）
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        executor.initialize();

        log.info("✅ 监控线程池已初始化: corePoolSize={}, maxPoolSize={}, queueCapacity={}",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());

        return executor;
    }
}
```

#### 4.2.12 AiConfig 修改：注入监听器

```java
// 在 AiConfig.java 的 createModel 方法中添加监听器注入

private ChatLanguageModel createModel(AiProperties.AgentConfig config) {
    String provider = config.getProvider();
    String baseUrl = config.getBaseUrl();
    String apiKey = config.getApiKey();
    String modelName = config.getModelName();
    Double temperature = config.getTemperature();
    Long timeoutSeconds = config.getTimeoutSeconds();

    log.debug("创建模型: provider={}, baseUrl={}, model={}, temperature={}",
            provider, baseUrl, modelName, temperature);

    // 分支 A：本地 Ollama 模型
    if ("ollama".equalsIgnoreCase(provider)) {
        log.info("使用 OllamaChatModel（原生客户端，完美支持工具调用）");
        return dev.langchain4j.model.ollama.OllamaChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(modelName)
                .temperature(temperature)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .logRequests(true)
                .logResponses(true)
                .listeners(List.of(aiMonitoringListener))  // ← 新增：注入监控监听器
                .build();
    }

    // 分支 B：OpenAI 兼容协议
    else if ("openai".equalsIgnoreCase(provider)
            || "deepseek".equalsIgnoreCase(provider)
            || "aliyun".equalsIgnoreCase(provider)
            || "siliconflow".equalsIgnoreCase(provider)) {
        log.info("使用 OpenAiChatModel（OpenAI 兼容协议）");
        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(temperature)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .logRequests(true)
                .logResponses(true)
                .listeners(List.of(aiMonitoringListener))  // ← 新增：注入监控监听器
                .build();
    }

    throw new IllegalArgumentException("❌ 不支持的 AI 供应商: " + provider);
}
```

#### 4.2.13 MultiAgentCoordinator 修改：透传元数据

```java
// 在 MultiAgentCoordinator.java 中，调用 Agent 前设置 attributes

private String performDiagnosis(AnalysisContext context) {
    log.info("调用主治医生 Agent 进行初步诊断");

    // 格式化提示词
    String formattedPrompt = promptUtil.formatUserPrompt(
            context.getDbName(),
            formatTime(context.getTemplateStats().getFirstSeenTime()),
            context.getTemplateStats().getAvgQueryTime(),
            context.getTemplateStats().getAvgLockTime(),
            context.getTemplateStats().getMaxRowsExamined(),
            context.getTemplateStats().getMaxRowsSent(),
            context.getSampleSql()
    );

    // ← 新增：设置监控元数据（通过 ThreadLocal 或 RequestScope）
    AiMonitoringContext.setAgentName(AgentName.DIAGNOSIS.getCode());
    AiMonitoringContext.setTraceId(context.getSqlFingerprint());

    try {
        return diagnosisAgent.analyzeSlowLog(formattedPrompt);
    } finally {
        // ← 新增：清理元数据
        AiMonitoringContext.clear();
    }
}
```

#### 4.2.14 Helper: AiMonitoringContext（用于传递元数据）

```java
package com.dbdoctor.monitoring;

/**
 * AI 监控上下文（ThreadLocal）
 *
 * 用于在 Agent 调用前后传递元数据给监听器
 *
 * @author DB-Doctor
 * @version 2.3.0
 */
public class AiMonitoringContext {

    private static final ThreadLocal<String> AGENT_NAME = new ThreadLocal<>();
    private static final ThreadLocal<String> TRACE_ID = new ThreadLocal<>();

    /**
     * 设置 Agent 名称
     */
    public static void setAgentName(String agentName) {
        AGENT_NAME.set(agentName);
    }

    /**
     * 获取 Agent 名称
     */
    public static String getAgentName() {
        return AGENT_NAME.get();
    }

    /**
     * 设置 SQL 指纹
     */
    public static void setTraceId(String traceId) {
        TRACE_ID.set(traceId);
    }

    /**
     * 获取 SQL 指纹
     */
    public static String getTraceId() {
        return TRACE_ID.get();
    }

    /**
     * 清理上下文
     */
    public static void clear() {
        AGENT_NAME.remove();
        TRACE_ID.remove();
    }
}
```

### 4.3 配置文件设计

#### 4.3.1 application.yml 新增配置

```yaml
db-doctor:
  # AI 监控配置
  monitoring:
    # 是否启用 AI 监控
    enabled: true

    # 是否存储提示词和响应（调试用，会占用大量存储空间）
    save-prompt-response: false

    # 数据保留天数（默认 90 天）
    retention-days: 90

    # 是否启用成本计算（需要配置 token 单价）
    cost-calculation:
      enabled: false
      # 各模型的 Token 单价（元/1K tokens）
      prices:
        qwen2.5:7b: 0.0    # 本地模型，免费
        deepseek-r1: 0.001  # DeepSeek 价格
        gpt-4: 0.03         # GPT-4 价格
```

---

## 五、前端设计

### 5.1 页面结构

```
前端管理系统
└─ src/
   ├─ views/
   │  └─ AiMonitor/              # ← 新增：AI 监控中心
   │     ├─ index.vue            #   主页面（仪表盘）
   │     ├─ InvocationLog.vue    #   调用流水列表
   │     └─ types.ts             #   类型定义
   ├─ api/
   │  └─ ai-monitor.ts           # ← 新增：监控 API
   └─ components/
      └─ AgentDistribution.vue   # ← 新增：Agent 分布饼图
```

### 5.2 类型定义

```typescript
// src/views/AiMonitor/types.ts

/**
 * AI 监控统计数据
 */
export interface AiMonitorStats {
  totalCalls: number
  totalTokens: number
  avgDuration: number
  successRate: number
  agentTokenDistribution: Record<string, number>
  agentCallDistribution: Record<string, number>
  hourlyCallCount: Record<number, number>
  timeRange: string
}

/**
 * AI 调用详情
 */
export interface AiInvocationDetail {
  id: number
  traceId: string
  agentDisplayName: string
  modelName: string
  provider: string
  inputTokens: number
  outputTokens: number
  totalTokens: number
  durationMs: number
  startTime: string
  statusDisplayName: string
  errorCategory?: string
  errorMessage?: string
  promptText?: string
  responseText?: string
}

/**
 * 查询参数
 */
export interface QueryParams {
  startTime?: string
  endTime?: string
  agentName?: string
  status?: string
}
```

### 5.3 API 封装

```typescript
// src/api/ai-monitor.ts

import request from './index'
import type { AiMonitorStats, AiInvocationDetail, QueryParams } from '@/views/AiMonitor/types'

/**
 * 获取监控统计数据
 */
export function getAiMonitorStats(params: QueryParams): Promise<AiMonitorStats> {
  return request({
    url: '/api/ai-monitor/stats',
    method: 'get',
    params
  })
}

/**
 * 根据 SQL 指纹查询所有相关的 AI 调用
 */
export function getAiInvocationByTrace(traceId: string): Promise<AiInvocationDetail[]> {
  return request({
    url: `/api/ai-monitor/by-trace/${traceId}`,
    method: 'get'
  })
}

/**
 * 分页查询调用日志
 */
export function queryAiInvocations(params: QueryParams): Promise<AiInvocationDetail[]> {
  return request({
    url: '/api/ai-monitor/query',
    method: 'get',
    params
  })
}
```

### 5.4 监控大盘页面

```vue
<!-- src/views/AiMonitor/index.vue -->
<template>
  <div class="ai-monitor-page">
    <!-- 顶部统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <el-statistic title="总调用次数" :value="stats.totalCalls">
            <template #suffix>
              <el-icon><DataAnalysis /></el-icon>
            </template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card primary" shadow="hover">
          <el-statistic title="总 Token 消耗" :value="formatTokens(stats.totalTokens)">
            <template #suffix>
              <span class="unit">Tokens</span>
            </template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card success" shadow="hover">
          <el-statistic title="平均耗时" :value="stats.avgDuration" :precision="0">
            <template #suffix>
              <span class="unit">ms</span>
            </template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" :class="getRateClass(stats.successRate)" shadow="hover">
          <el-statistic title="成功率" :value="stats.successRate" :precision="2">
            <template #suffix>
              <span class="unit">%</span>
            </template>
          </el-statistic>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="20" class="charts-row">
      <!-- Agent Token 分布（饼图） -->
      <el-col :span="12">
        <el-card class="chart-card" shadow="never">
          <template #header>
            <span class="card-title">Agent Token 分布</span>
          </template>
          <v-chart
            :option="agentPieOption"
            style="height: 300px"
            autoresize
          />
        </el-card>
      </el-col>

      <!-- 耗时趋势图（折线图） -->
      <el-col :span="12">
        <el-card class="chart-card" shadow="never">
          <template #header>
            <span class="card-title">24 小时调用趋势</span>
          </template>
          <v-chart
            :option="trendLineOption"
            style="height: 300px"
            autoresize
          />
        </el-card>
      </el-col>
    </el-row>

    <!-- 快捷操作 -->
    <el-card class="actions-card" shadow="never">
      <template #header>
        <span class="card-title">快捷操作</span>
      </template>
      <el-space :size="20">
        <el-button type="primary" @click="goToInvocationLog">
          <el-icon><View /></el-icon>
          查看调用流水
        </el-button>
        <el-button type="success" @click="refreshData">
          <el-icon><Refresh /></el-icon>
          刷新数据
        </el-button>
      </el-space>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { DataAnalysis, View, Refresh } from '@element-plus/icons-vue'
import type { EChartsOption } from 'echarts'
import { getAiMonitorStats } from '@/api/ai-monitor'
import type { AiMonitorStats } from './types'

const router = useRouter()

// 数据
const stats = ref<AiMonitorStats>({
  totalCalls: 0,
  totalTokens: 0,
  avgDuration: 0,
  successRate: 0,
  agentTokenDistribution: {},
  agentCallDistribution: {},
  hourlyCallCount: {},
  timeRange: ''
})

// Agent Token 分布饼图
const agentPieOption = computed<EChartsOption>(() => {
  const data = Object.entries(stats.value.agentTokenDistribution).map(([name, value]) => ({
    name: getAgentDisplayName(name),
    value
  }))

  return {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)'
    },
    legend: {
      orient: 'vertical',
      left: 'left'
    },
    series: [{
      type: 'pie',
      radius: '60%',
      data,
      emphasis: {
        itemStyle: {
          shadowBlur: 10,
          shadowOffsetX: 0,
          shadowColor: 'rgba(0, 0, 0, 0.5)'
        }
      }
    }]
  }
})

// 趋势折线图
const trendLineOption = computed<EChartsOption>(() => {
  const hours = Object.keys(stats.value.hourlyCallCount).map(h => `${h}:00`)
  const counts = Object.values(stats.value.hourlyCallCount)

  return {
    tooltip: {
      trigger: 'axis'
    },
    xAxis: {
      type: 'category',
      data: hours,
      boundaryGap: false
    },
    yAxis: {
      type: 'value',
      name: '调用次数'
    },
    series: [{
      name: '调用次数',
      type: 'line',
      data: counts,
      smooth: true,
      areaStyle: {}
    }]
  }
})

/**
 * 加载统计数据
 */
async function loadStats() {
  try {
    const result = await getAiMonitorStats({})
    stats.value = result
  } catch (error: any) {
    ElMessage.error(error.message || '加载统计数据失败')
  }
}

/**
 * 刷新数据
 */
function refreshData() {
  loadStats()
  ElMessage.success('数据已刷新')
}

/**
 * 跳转到调用流水页面
 */
function goToInvocationLog() {
  router.push('/ai-monitor/invocation-log')
}

/**
 * 格式化 Token 数
 */
function formatTokens(tokens: number): string {
  if (tokens >= 1000000) {
    return (tokens / 1000000).toFixed(1) + 'M'
  }
  if (tokens >= 1000) {
    return (tokens / 1000).toFixed(1) + 'K'
  }
  return tokens.toString()
}

/**
 * 获取 Agent 显示名称
 */
function getAgentDisplayName(code: string): string {
  const map: Record<string, string> = {
    'DIAGNOSIS': '主治医生',
    'REASONING': '推理专家',
    'CODING': '编码专家'
  }
  return map[code] || code
}

/**
 * 根据成功率返回样式类
 */
function getRateClass(rate: number): string {
  if (rate >= 99) return 'success'
  if (rate >= 95) return 'warning'
  return 'danger'
}

onMounted(() => {
  loadStats()
})
</script>

<style scoped>
.ai-monitor-page {
  max-width: 1400px;
  margin: 0 auto;
}

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  text-align: center;
  transition: transform 0.2s;
}

.stat-card:hover {
  transform: translateY(-4px);
}

.stat-card.primary :deep(.el-statistic__content) {
  color: #409eff;
}

.stat-card.success :deep(.el-statistic__content) {
  color: #67c23a;
}

.stat-card.warning :deep(.el-statistic__content) {
  color: #e6a23c;
}

.stat-card.danger :deep(.el-statistic__content) {
  color: #f56c6c;
}

.charts-row {
  margin-bottom: 20px;
}

.chart-card {
  height: 400px;
}

.card-title {
  font-weight: bold;
  font-size: 16px;
}

.actions-card {
  margin-bottom: 20px;
}

.unit {
  font-size: 14px;
  color: #909399;
}
</style>
```

### 5.5 调用流水页面

```vue
<!-- src/views/AiMonitor/InvocationLog.vue -->
<template>
  <div class="invocation-log-page">
    <!-- 查询条件 -->
    <el-card class="filter-card" shadow="never">
      <el-form :inline="true" :model="queryForm">
        <el-form-item label="Agent">
          <el-select v-model="queryForm.agentName" placeholder="全部" clearable>
            <el-option label="主治医生" value="DIAGNOSIS" />
            <el-option label="推理专家" value="REASONING" />
            <el-option label="编码专家" value="CODING" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="全部" clearable>
            <el-option label="成功" value="SUCCESS" />
            <el-option label="失败" value="FAILED" />
            <el-option label="超时" value="TIMEOUT" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="dateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 数据表格 -->
    <el-card class="table-card" shadow="never">
      <el-table
        :data="tableData"
        v-loading="loading"
        stripe
        highlight-current-row
      >
        <el-table-column prop="startTime" label="调用时间" width="180" />
        <el-table-column prop="agentDisplayName" label="Agent" width="100" />
        <el-table-column prop="modelName" label="模型" width="150" />
        <el-table-column prop="durationMs" label="耗时" width="100">
          <template #default="{ row }">
            {{ row.durationMs }} ms
          </template>
        </el-table-column>
        <el-table-column prop="totalTokens" label="Token" width="100">
          <template #default="{ row }">
            {{ row.inputTokens }} / {{ row.outputTokens }}
          </template>
        </el-table-column>
        <el-table-column prop="statusDisplayName" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.statusDisplayName)">
              {{ row.statusDisplayName }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="traceId" label="SQL 指纹" show-overflow-tooltip />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleViewDetail(row)">
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 详情弹窗 -->
    <el-dialog
      v-model="detailVisible"
      title="AI 调用详情"
      width="800px"
    >
      <el-descriptions :column="2" border>
        <el-descriptions-item label="调用时间">{{ currentDetail.startTime }}</el-descriptions-item>
        <el-descriptions-item label="Agent">{{ currentDetail.agentDisplayName }}</el-descriptions-item>
        <el-descriptions-item label="模型">{{ currentDetail.modelName }}</el-descriptions-item>
        <el-descriptions-item label="供应商">{{ currentDetail.provider }}</el-descriptions-item>
        <el-descriptions-item label="耗时">{{ currentDetail.durationMs }} ms</el-descriptions-item>
        <el-descriptions-item label="Token">{{ currentDetail.inputTokens }} / {{ currentDetail.outputTokens }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(currentDetail.statusDisplayName)">
            {{ currentDetail.statusDisplayName }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="SQL 指纹" :span="2">{{ currentDetail.traceId }}</el-descriptions-item>
        <el-descriptions-item v-if="currentDetail.errorMessage" label="错误信息" :span="2">
          <el-text type="danger">{{ currentDetail.errorMessage }}</el-text>
        </el-descriptions-item>
      </el-descriptions>

      <!-- 提示词和响应（如果有） -->
      <el-tabs v-if="currentDetail.promptText || currentDetail.responseText" class="detail-tabs">
        <el-tab-pane label="提示词">
          <el-input
            type="textarea"
            :model-value="currentDetail.promptText"
            :rows="10"
            readonly
          />
        </el-tab-pane>
        <el-tab-pane label="响应">
          <el-input
            type="textarea"
            :model-value="currentDetail.responseText"
            :rows="10"
            readonly
          />
        </el-tab-pane>
      </el-tabs>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { queryAiInvocations } from '@/api/ai-monitor'
import type { AiInvocationDetail, QueryParams } from './types'

// 查询表单
const queryForm = ref<QueryParams>({})
const dateRange = ref<string[]>([])
const loading = ref(false)
const tableData = ref<AiInvocationDetail[]>([])

// 详情弹窗
const detailVisible = ref(false)
const currentDetail = ref<AiInvocationDetail>({} as any)

/**
 * 查询数据
 */
async function handleQuery() {
  loading.value = true
  try {
    const params: QueryParams = {
      agentName: queryForm.value.agentName,
      status: queryForm.value.status
    }

    if (dateRange.value && dateRange.value.length === 2) {
      params.startTime = dateRange.value[0]
      params.endTime = dateRange.value[1]
    }

    tableData.value = await queryAiInvocations(params)
  } catch (error: any) {
    ElMessage.error(error.message || '查询失败')
  } finally {
    loading.value = false
  }
}

/**
 * 重置查询
 */
function handleReset() {
  queryForm.value = {}
  dateRange.value = []
  handleQuery()
}

/**
 * 查看详情
 */
function handleViewDetail(row: AiInvocationDetail) {
  currentDetail.value = row
  detailVisible.value = true
}

/**
 * 获取状态标签类型
 */
function getStatusType(status: string): string {
  if (status === '成功') return 'success'
  if (status === '失败') return 'danger'
  if (status === '超时') return 'warning'
  return 'info'
}

onMounted(() => {
  handleQuery()
})
</script>

<style scoped>
.invocation-log-page {
  max-width: 1400px;
  margin: 0 auto;
}

.filter-card {
  margin-bottom: 20px;
}

.table-card {
  margin-bottom: 20px;
}

.detail-tabs {
  margin-top: 20px;
}
</style>
```

### 5.6 路由配置

```typescript
// src/router/index.ts 新增路由

{
  path: '/ai-monitor',
  component: () => import('@/layouts/MainLayout.vue'),
  meta: { title: 'AI 监控', icon: 'Monitor' },
  children: [
    {
      path: '',
      name: 'AiMonitor',
      component: () => import('@/views/AiMonitor/index.vue'),
      meta: { title: '监控大盘' }
    },
    {
      path: 'invocation-log',
      name: 'InvocationLog',
      component: () => import('@/views/AiMonitor/InvocationLog.vue'),
      meta: { title: '调用流水' }
    }
  ]
}
```

---

## 六、数据流设计

### 6.1 完整调用链路

```
┌─────────────────────────────────────────────────────────────┐
│                    AI 调用监控数据流                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. 用户触发慢查询分析                                        │
│     └─> AnalysisService.analyze()                           │
│                                                             │
│  2. MultiAgentCoordinator 协调多 Agent                      │
│     ├─> 设置 ThreadLocal: agentName=DIAGNOSIS              │
│     ├─> 设置 ThreadLocal: traceId=abc123...                │
│     ├─> 调用 diagnosisAgent.analyzeSlowLog()               │
│     │     └─> LangChain4j 发起 HTTP 请求                   │
│     │           └─> 触发 AiMonitoringListener              │
│     │                 ├─> onRequest(): 记录开始时间        │
│     │                 ├─> onResponse(): 提取 Token、耗时   │
│     │                 └─> 异步保存到数据库                  │
│     └─> 清理 ThreadLocal                                   │
│                                                             │
│  3. 数据存储                                                │
│     └─> ai_invocation_log 表                               │
│                                                             │
│  4. 前端查询监控数据                                         │
│     ├─> GET /api/ai-monitor/stats                          │
│     └─> GET /api/ai-monitor/query                          │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 6.2 元数据传递机制

由于 LangChain4j 的 `ChatModelListener` 的 `attributes` 参数在某些情况下可能无法获取自定义属性，我们采用 **ThreadLocal** 方式传递元数据：

```java
// 调用前
AiMonitoringContext.setAgentName(AgentName.DIAGNOSIS.getCode());
AiMonitoringContext.setTraceId(context.getSqlFingerprint());

// 调用 AI
String result = diagnosisAgent.analyzeSlowLog(prompt);

// 调用后清理
AiMonitoringContext.clear();
```

在 `AiMonitoringListener` 中读取：

```java
@Override
public void onResponse(ChatModelResponseContext context) {
    String agentName = AiMonitoringContext.getAgentName();
    String traceId = AiMonitoringContext.getTraceId();

    // 构建日志实体并保存
    // ...
}
```

---

## 七、实施步骤

### 7.1 Phase 1: 数据库和基础实体（优先级：最高）

**任务清单**：
- [ ] 创建数据库表 `ai_invocation_log`
- [ ] 创建枚举类：`AgentName`、`InvocationStatus`、`ErrorCategory`
- [ ] 创建实体类：`AiInvocationLog`
- [ ] 创建 Repository：`AiInvocationLogRepository`
- [ ] 编写单元测试验证 Repository

**验收标准**：
- 表结构创建成功，索引正确
- Repository 基本 CRUD 功能正常

### 7.2 Phase 2: 监控监听器（优先级：最高）

**任务清单**：
- [ ] 实现 `AiMonitoringListener`（实现 `ChatModelListener`）
- [ ] 实现 `AiMonitoringContext`（ThreadLocal 传递元数据）
- [ ] 创建 `AiInvocationLogService`
- [ ] 创建监控线程池配置 `MonitoringConfig`
- [ ] 修改 `AiConfig.createModel()` 注入监听器
- [ ] 修改 `MultiAgentCoordinator` 传递元数据
- [ ] 编写集成测试验证监听器工作正常

**验收标准**：
- 调用 Agent 后，数据库中有对应的记录
- Token、耗时等指标准确

### 7.3 Phase 3: 后端 API（优先级：高）

**任务清单**：
- [ ] 创建 DTO 类：`AiMonitorStats`、`AiInvocationDetail`
- [ ] 创建 `AiMonitorController`
- [ ] 实现 `/api/ai-monitor/stats` 接口
- [ ] 实现 `/api/ai-monitor/by-trace/{traceId}` 接口
- [ ] 实现 `/api/ai-monitor/query` 接口
- [ ] 编写 API 集成测试

**验收标准**：
- 所有 API 接口返回正确数据
- 错误处理完善

### 7.4 Phase 4: 前端监控大盘（优先级：高）

**任务清单**：
- [ ] 创建前端类型定义 `types.ts`
- [ ] 创建前端 API 封装 `ai-monitor.ts`
- [ ] 实现监控大盘页面 `index.vue`
- [ ] 实现调用流水页面 `InvocationLog.vue`
- [ ] 创建 Agent 分布饼图组件 `AgentDistribution.vue`
- [ ] 配置路由
- [ ] 编写前端单元测试

**验收标准**：
- 监控大盘显示正确的统计数据
- 调用流水列表可以正常查询
- 图表渲染正常

### 7.5 Phase 5: 高级功能（优先级：中）

**任务清单**：
- [ ] 实现数据归档功能（定期清理超过保留期的数据）
- [ ] 实现成本计算功能（根据 Token 单价计算费用）
- [ ] 实现 WebSocket 实时推送监控数据
- [ ] 实现告警功能（成功率低于阈值时发送通知）
- [ ] 实现监控数据导出（CSV/Excel）

**验收标准**：
- 数据归档自动执行
- 成本计算准确
- 实时推送正常工作

### 7.6 Phase 6: 测试和优化（优先级：中）

**任务清单**：
- [ ] 压力测试：验证监控系统对 AI 调用性能的影响
- [ ] 性能优化：确保监控不阻塞 AI 调用
- [ ] 日志优化：调整日志级别，避免日志过多
- [ ] 文档完善：编写监控使用文档

**验收标准**：
- 监控系统对 AI 调用性能影响 < 5%
- 所有单元测试通过
- 文档完整

---

## 八、测试策略

### 8.1 单元测试

**AiInvocationLogRepositoryTest**：
```java
@SpringBootTest
class AiInvocationLogRepositoryTest {

    @Autowired
    private AiInvocationLogRepository repository;

    @Test
    void testFindByTraceId() {
        // Given
        AiInvocationLog log = new AiInvocationLog();
        log.setTraceId("test-fingerprint");
        log.setAgentName("DIAGNOSIS");
        // ... 其他字段
        repository.save(log);

        // When
        List<AiInvocationLog> results = repository.findByTraceIdOrderByStartTimeAsc("test-fingerprint");

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getAgentName()).isEqualTo("DIAGNOSIS");
    }
}
```

### 8.2 集成测试

**AiMonitoringListenerIntegrationTest**：
```java
@SpringBootTest
class AiMonitoringListenerIntegrationTest {

    @Autowired
    private DBAgent diagnosisAgent;

    @Autowired
    private AiInvocationLogRepository repository;

    @Test
    void testListenerCapturesInvocation() {
        // Given
        String testPrompt = "分析慢查询：SELECT * FROM users";

        // When
        AiMonitoringContext.setAgentName(AgentName.DIAGNOSIS.getCode());
        AiMonitoringContext.setTraceId("test-fingerprint-123");

        try {
            diagnosisAgent.analyzeSlowLog(testPrompt);
        } finally {
            AiMonitoringContext.clear();
        }

        // Then: 等待异步保存完成
        await().atMost(5, SECONDS).until(() -> {
            List<AiInvocationLog> logs = repository.findByTraceIdOrderByStartTimeAsc("test-fingerprint-123");
            return !logs.isEmpty();
        });

        List<AiInvocationLog> logs = repository.findByTraceIdOrderByStartTimeAsc("test-fingerprint-123");
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getAgentName()).isEqualTo("DIAGNOSIS");
        assertThat(logs.get(0).getStatus()).isEqualTo("SUCCESS");
    }
}
```

### 8.3 压力测试

**监控性能测试**：
```java
@Test
void testMonitoringPerformanceImpact() {
    // 测试目标：监控系统对 AI 调用性能的影响 < 5%

    int iterations = 100;

    // 不启用监控的耗时
    long withoutMonitoring = measureAiCalls(iterations, false);

    // 启用监控的耗时
    long withMonitoring = measureAiCalls(iterations, true);

    // 计算性能影响
    double impact = (withMonitoring - withoutMonitoring) * 100.0 / withoutMonitoring;

    assertThat(impact).isLessThan(5.0); // 影响 < 5%
}
```

---

## 九、运维指南

### 9.1 数据清理策略

```sql
-- 定期清理超过保留期的数据（根据配置的 retention-days）
DELETE FROM ai_invocation_log
WHERE created_time < DATE_SUB(NOW(), INTERVAL 90 DAY);

-- 或使用分区表（推荐）
ALTER TABLE ai_invocation_log
PARTITION BY RANGE (TO_DAYS(created_time)) (
    PARTITION p202401 VALUES LESS THAN (TO_DAYS('2024-02-01')),
    PARTITION p202402 VALUES LESS THAN (TO_DAYS('2024-03-01')),
    -- ...
    PARTITION p_future VALUES LESS THAN MAXVALUE
);

-- 删除旧分区
ALTER TABLE ai_invocation_log DROP PARTITION p202401;
```

### 9.2 监控告警规则

```yaml
# Prometheus 告警规则（示例）
groups:
  - name: ai_monitoring
    rules:
      - alert: HighFailureRate
        expr: ai_invocation_failure_rate > 0.05
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "AI 调用失败率过高"
          description: "最近 5 分钟失败率为 {{ $value }}%"

      - alert: SlowResponseTime
        expr: ai_invocation_avg_duration_ms > 10000
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "AI 响应时间过长"
          description: "平均响应时间为 {{ $value }}ms"
```

### 9.3 常见问题排查

| 问题 | 可能原因 | 排查方法 |
|------|---------|---------|
| 监控数据缺失 | 监听器未注入 | 检查 `AiConfig.createModel()` 是否注入了监听器 |
| Agent 名称为 UNKNOWN | ThreadLocal 未设置 | 检查 `MultiAgentCoordinator` 是否设置了元数据 |
| Token 统计为 0 | 模型未返回 Token 信息 | 检查模型是否支持 Token 统计 |
| 性能下降 | 异步保存队列满了 | 检查线程池配置，增加队列容量 |

---

## 十、附录

### 10.1 术语表

| 术语 | 说明 |
|------|------|
| Agent | AI 智能体（主治医生、推理专家、编码专家） |
| Token | 文本的最小单位，AI 模�型的计费单位 |
| Trace ID | SQL 指纹，用于关联慢查询和 AI 调用 |
| Provider | AI 模型供应商（ollama、openai、deepseek） |

### 10.2 参考资料

- [LangChain4j 官方文档](https://docs.langchain4j.dev/)
- [Spring Boot 异步任务](https://spring.io/guides/gs/async-method/)
- [ECharts 文档](https://echarts.apache.org/zh/index.html)

### 10.3 版本历史

| 版本 | 日期 | 变更说明 |
|------|------|---------|
| v1.0.0 | 2026-01-31 | 初始版本 |

---

**文档结束**
