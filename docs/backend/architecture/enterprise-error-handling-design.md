# DB-Doctor 企业级异常处理架构设计文档

> **文档版本**: v1.0.0
> **创建日期**: 2026-01-31
> **作者**: AI架构师（基于 Gemini 架构评审）
> **优先级**: P0（核心架构升级）
> **预计工期**: 5-7个工作日

---

## 📋 文档目录

1. [背景与目标](#1-背景与目标)
2. [问题分析](#2-问题分析)
3. [架构设计](#3-架构设计)
4. [数据模型](#4-数据模型)
5. [接口设计](#5-接口设计)
6. [测试用例](#6-测试用例)
7. [实施计划](#7-实施计划)
8. [验收标准](#8-验收标准)

---

## 1. 背景与目标

### 1.1 当前问题（v2.2.0）

**现状**：
```java
// ❌ 反模式：硬编码字符串匹配
if (diagnosisReport.contains("Unknown database") ||
    diagnosisReport.contains("数据库不存在")) {
    return null;
}
```

**问题列表**：
1. ❌ **脆弱性**：依赖具体错误字符串，数据库升级就挂
2. ❌ **不可扩展**：新增错误类型需要改代码
3. ❌ **逻辑分散**：异常处理散落在 3 个层次（Prompt、Coordinator、Tool）
4. ❌ **无智能性**：代码强制阻断，AI 无法自主决策
5. ❌ **无恢复机制**：遇到错误直接放弃，无重试/降级

### 1.2 目标（v3.0 企业级）

**核心能力**：
- ✅ **ReAct 循环**：AI 自主观察错误、自主决策下一步行动
- ✅ **熔断器模式**：连续失败时自动熔断，避免资源浪费
- ✅ **错误分类**：智能识别错误类型（环境错误、权限错误、数据错误、临时错误）
- ✅ **智能恢复**：根据错误类型自动选择恢复策略（重试、降级、跳过）
- ✅ **结构化反馈**：统一的标准错误格式，AI 可理解、代码可处理

---

## 2. 问题分析

### 2.1 当前异常处理流程

```
慢查询分析请求
    ↓
DBAgent.analyzeSlowLog()
    ↓
调用工具 getTableSchema()
    ↓
工具返回错误："Unknown database 'crm_db'"
    ↓
❌ 问题1：工具层捕获异常，返回空字符串
❌ 问题2：AI 收到空数据，产生幻觉
❌ 问题3：继续调用推理专家（浪费资源）
❌ 问题4：最终报告包含医学诊断等胡说八道
```

### 2.2 根本原因

1. **工具层无结构化错误返回**
   - 异常被 try-catch 捕获后返回 `null` 或空字符串
   - AI 无法区分"正常空结果"和"错误空结果"

2. **AI Agent 缺乏错误处理能力**
   - Prompt 中虽然有错误处理指引，但 AI 不一定遵循
   - 缺乏强制性的错误反馈机制

3. **Coordinator 层过度控制**
   - 用代码强制阻断某些流程
   - AI 无法根据实际情况调整策略

4. **无熔断机制**
   - 连续失败时会无限重试
   - 浪费 API 调用费用和时间

---

## 3. 架构设计

### 3.1 整体架构图

```
┌─────────────────────────────────────────────────────────────┐
│                    DB-Doctor Agent 系统                    │
│                                                              │
│  ┌────────────────────────────────────────────────────────┐ │
│  │                 Coordinator（协调层）                  │ │
│  │  ┌────────────────────────────────────────────────┐  │ │
│  │  │   CircuitBreaker（熔断器）                      │  │ │
│  │  │   - 连续失败 3 次 → 熔断 60 秒                   │  │ │
│  │  │   - 半开状态 → 允许 1 个尝试                    │  │ │
│  │  └────────────────────────────────────────────────┘  │ │
│  └────────────────────────────────────────────────────────┘  │
│                          ↓                                     │
│  ┌────────────────────────────────────────────────────────┐  │
│  │              Agent Layer（智能体层）                  │  │
│  │                                                      │  │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────┐  │ │
│  │  │ DBAgent     │  │ReasoningAgent│  │CodingAgent│ │ │
│  │  │              │  │              │  │          │ │ │
│  │  │  ReAct Loop   │  │  ReAct Loop   │  │ ReAct Loop │ │ │
│  │  └──────┬───────┘  └──────┬───────┘  └──────┬───┘  │ │
│  │         │                    │             │         │  │
│  │         ↓                    ↓             ↓         │  │
│  │  ┌──────────────────────────────────────────────────┐ │ │
│  │  │       Tool Layer（工具层 - 结构化反馈）           │ │ │
│  │  │                                                       │ │ │
│  │  │  ┌────────────┐  ┌────────────┐  ┌──────────┐  │ │ │
│  │  │  │ getTable    │  │getExecution │  │getIndex  │  │ │ │
│  │  │  │ Schema     │  │Plan        │  │Selectivity│  │ │ │
│  │  │  └──────┬─────┘  └──────┬─────┘  └──────┬───┘  │ │ │
│  │  │         │                 │             │         │  │ │
│  │  │         ↓                 ↓             ↓         │ │ │
│  │  │  ┌─────────────────────────────────────────────┐ │ │ │
│  │  │  │        ToolResult（统一错误封装）            │ │ │ │
│  │  │  │  - success: boolean                         │ │ │ │
│  │  │  │  - errorCode: String (DB_NOT_FOUND, etc.)     │ │ │ │
│  │  │  │  - errorMessage: String                      │ │ │ │
│  │  │  │  - data: String (成功时的数据)               │ │ │ │
│  │  │  └─────────────────────────────────────────────┘ │ │ │
│  │  └──────────────────────────────────────────────────┘  │ │
│  └────────────────────────────────────────────────────────┘  │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

### 3.2 ReAct 循环设计

**核心思想**：让 AI 像人类专家一样，观察→思考→行动→再观察

```
Agent 循环：

┌─────────┐
│ Thought │ "我需要检查表结构"
└────┬────┘
     ↓
┌─────────┐
│ Action  │ 调用 getTableSchema("crm_db", "customers")
└────┬────┘
     ↓
┌─────────┐
│Observ. │ ToolResult{success=false, errorCode="DB_NOT_FOUND"}
└────┬────┘
     ↓
┌─────────┐
│ Thought │ "哦，数据库不存在。这不是 SQL 问题，是环境问题。"
│         │ "我应该停止分析，告诉用户检查配置。"
└────┬────┘
     ↓
┌─────────┐
│ Final   │ "⚠️ 无法完成诊断。原因：数据库 crm_db 不存在"
│ Answer  │ "建议：1. 检查数据库是否存在..."
└─────────┘
```

### 3.3 熔断器模式

**熔断器状态机**：

```
┌──────────┐
│  CLOSED  │ ← 正常状态，允许所有请求
└────┬─────┘
     ↓
┌──────────┐
│  HALF-OPEN│ ← 半开状态，允许 1 个尝试（探测）
└────┬─────┘
     ↓ 连续失败
┌──────────┐
│  OPEN    │ ← 熔断状态，拒绝所有请求（60秒）
└──────────┘
     ↓ 60秒后
┌──────────┐
│  HALF-OPEN│ ← 恢复到半开，尝试探测
└──────────┘
```

**熔断器配置**：

```yaml
db-doctor:
  circuit-breaker:
    failure-threshold: 3         # 连续失败 3 �触发熔断
    timeout: 60s                  # 熔断持续时间
    half-open-max-calls: 1       # 半开状态最多允许 1 次调用
    reset-timeout: 300s           # 5 分钟无错误后重置计数
```

### 3.4 错误分类体系

**错误码定义**：

```java
public enum ErrorCode {
    // === 环境错误（阻断性）===
    DB_NOT_FOUND("ENR_001", "数据库不存在", ErrorCategory.BLOCKING, true),
    TABLE_NOT_FOUND("ENR_002", "表不存在", ErrorCategory.BLOCKING, true),
    COLUMN_NOT_FOUND("ENR_003", "列不存在", ErrorCategory.BLOCKING, true),

    // === 权限错误（阻断性）===
    ACCESS_DENIED("PERM_001", "访问被拒绝", ErrorCategory.BLOCKING, true),
    PRIVILEGE_NOT_ENOUGH("PERM_002", "权限不足", ErrorCategory.BLOCKING, true),

    // === 配置错误（阻断性）===
    SLOW_QUERY_LOG_DISABLED("CFG_001", "慢查询日志未启用", ErrorCategory.BLOCKING, true),

    // === 数据错误（非阻断性）===
    EMPTY_RESULT("DATA_001", "查询结果为空", ErrorCategory.TRANSIENT, false),
    DUPLICATE_KEY("DATA_002", "主键冲突", ErrorCategory.TRANSIENT, false),

    // === 网络错误（临时性）===
    CONNECTION_TIMEOUT("NET_001", "连接超时", ErrorCategory.TRANSIENT, true),
    CONNECTION_LOST("NET_002", "连接丢失", ErrorCategory.TRANSIENT, true),

    // === SQL 错误（非阻断性）===
    SYNTAX_ERROR("SQL_001", "SQL 语法错误", ErrorCategory.PERMANENT, false),

    // === AI 错误（阻断性）===
    AI_RATE_LIMIT_EXCEEDED("AI_001", "AI API 调用受限", ErrorCategory.TRANSIENT, true),
    AI_MODEL_NOT_AVAILABLE("AI_002", "AI 模型不可用", ErrorCategory.BLOCKING, true);
}

public enum ErrorCategory {
    BLOCKING,    // 阻断性错误：环境/配置问题，必须修复才能继续
    TRANSIENT,   // 临时性错误：网络/超时等，可以重试
    PERMANENT    // 永久性错误：SQL 语法错误等，需要人工介入
}
```

**恢复策略**：

| 错误类型 | 恢复策略 | 最大重试次数 |
|---------|---------|------------|
| 环境错误 | ⛔ 熔断，通知用户 | 0 |
| 权限错误 | ⛔ 熔断，通知用户 | 0 |
| 网络错误 | 🔄 重试（指数退避） | 3 |
| 数据错误 | ➡️ 继续（空数据也是有效结果）| 0 |
| SQL 错误 | ⛔ 停止分析，返回错误 | 0 |

---

## 4. 数据模型

### 4.1 ToolResult（核心数据结构）

```java
package com.dbdoctor.model;

import lombok.Builder;
import lombok.Data;

/**
 * 工具执行结果（统一封装）
 *
 * 设计原则：
 * 1. 成功和失败都返回 ToolResult
 * 2. AI 能够理解和解析 ToolResult
 * 3. 代码能够根据 ToolResult 做判断
 *
 * @author DB-Doctor
 * @version 3.0.0
 */
@Data
@Builder
public class ToolResult {

    /**
     * 执行是否成功
     */
    private boolean success;

    /**
     * 错误码（失败时有值）
     */
    private ErrorCode errorCode;

    /**
     * 机器可读的错误信息（供代码解析）
     */
    private String errorMessage;

    /**
     * 人类可读的错误描述（供 AI 和用户阅读）
     */
    private String userMessage;

    /**
     * 错误分类
     */
    private ErrorCategory category;

    /**
     * 建议的恢复策略
     */
    private RecoveryStrategy recoveryStrategy;

    /**
     * 成功时的数据（JSON 格式）
     */
    private String data;

    /**
     * 工具执行耗时（毫秒）
     */
    private long executionTimeMs;

    /**
     * 重试建议次数
     */
    private int suggestedRetries;

    // === Helper Methods ===

    public static ToolResult success(String data) {
        return ToolResult.builder()
            .success(true)
            .data(data)
            .category(ErrorCategory.NONE)
            .recoveryStrategy(RecoveryStrategy.CONTINUE)
            .build();
    }

    public static ToolResult failure(ErrorCode errorCode, String errorMessage) {
        ErrorCategory category = errorCode.getCategory();
        RecoveryStrategy strategy = determineRecoveryStrategy(category);

        return ToolResult.builder()
            .success(false)
            .errorCode(errorCode)
            .errorMessage(errorMessage)
            .userMessage(errorCode.getUserMessage())
            .category(category)
            .recoveryStrategy(strategy)
            .suggestedRetries(strategy.getMaxRetries())
            .build();
    }

    /**
     * 判断是否应该重试
     */
    public boolean shouldRetry() {
        return !success && recoveryStrategy == RecoveryStrategy.RETRY;
    }

    /**
     * 判断是否应该熔断（阻止后续流程）
     */
    public boolean shouldCircuitBreak() {
        return !success && category == ErrorCategory.BLOCKING;
    }
}
```

### 4.2 ErrorCode 枚举

```java
package com.dbdoctor.common.enums;

import lombok.Getter;

/**
 * 错误码定义
 *
 * 命名规则：
 * - ENV_xxx: 环境错误
 * - PERM_xxx: 权限错误
 * - NET_xxx: 网络错误
 * - DATA_xxx: 数据错误
 * - SQL_xxx: SQL 错误
 * - AI_xxx: AI 服务错误
 *
 * @author DB-Doctor
 * @version 3.0.0
 */
@Getter
public enum ErrorCode {

    // === 环境错误（阻断性）===
    DB_NOT_FOUND("ENV_001", "数据库不存在",
            "目标数据库 '%s' 不存在或无法连接", ErrorCategory.BLOCKING, RecoveryStrategy.ABORT),

    TABLE_NOT_FOUND("ENV_002", "表不存在",
            "表 '%s.%s' 不存在", ErrorCategory.BLOCKING, RecoveryStrategy.ABORT),

    COLUMN_NOT_FOUND("ENV_003", "列不存在",
            "列 '%s.%s.%s' 不存在", ErrorCategory.BLOCKING, RecoveryStrategy.ABORT),

    SLOW_QUERY_LOG_DISABLED("ENV_004", "慢查询日志未启用",
            "目标数据库的慢查询日志未启用", ErrorCategory.BLOCKING, RecoveryStrategy.ABORT),

    // === 权限错误（阻断性）===
    ACCESS_DENIED("PERM_001", "访问被拒绝",
            "无权限访问数据库/表", ErrorCategory.BLOCKING, RecoveryStrategy.ABORT),

    PRIVILEGE_NOT_ENOUGH("PERM_002", "权限不足",
            "当前用户权限不足", ErrorCategory.BLOCKING, RecoveryStrategy.ABORT),

    // === 网络错误（临时性，可重试）===
    CONNECTION_TIMEOUT("NET_001", "连接超时",
            "连接数据库超时", ErrorCategory.TRANSIENT, RecoveryStrategy.RETRY),

    CONNECTION_LOST("NET_002", "连接丢失",
            "数据库连接中断", ErrorCategory.TRANSIENT, RecoveryStrategy.RETRY),

    QUERY_TIMEOUT("NET_003", "查询超时",
            "SQL 执行超时", ErrorCategory.TRANSIENT, RecoveryStrategy.RETRY),

    // === 数据错误（非阻断性）===
    EMPTY_RESULT("DATA_001", "查询结果为空",
            "查询返回 0 行", ErrorCategory.NONE, RecoveryStrategy.CONTINUE),

    DUPLICATE_KEY("DATA_002", "主键冲突",
            "违反唯一约束", ErrorCategory.TRANSIENT, RecoveryStrategy.CONTINUE),

    // === SQL 错误（永久性）===
    SYNTAX_ERROR("SQL_001", "SQL 语法错误",
            "SQL 语法不正确", ErrorCategory.PERMANENT, RecoveryStrategy.ABORT);

    private final String code;
    private final String userMessageTemplate;
    private final ErrorCategory category;
    private final RecoveryStrategy recoveryStrategy;

    // userMessageTemplate 支持参数替换
    public String formatUserMessage(Object... args) {
        return String.format(userMessageTemplate, args);
    }
}
```

### 4.3 RecoveryStrategy 恢复策略

```java
package com.dbdoctor.common.enums;

/**
 * 恢复策略枚举
 */
public enum RecoveryStrategy {
    /**
     * 继续：正常处理（无错误）
     */
    CONTINUE(false, 0),

    /**
     * 重试：临时性错误，可以重试
     */
    RETRY(true, 3),

    /**
     * 降级：部分功能不可用，使用降级方案
     */
    FALLBACK(true, 0),

    /**
     * 中止：阻断性错误，停止分析
     */
    ABORT(true, 0);

    private final boolean retryable;
    private final int maxRetries;
}
```

### 4.4 ErrorCategory 错误分类

```java
package com.dbdoctor.common.enums;

/**
 * 错误分类
 */
public enum ErrorCategory {
    /**
     * 阻断性错误：环境/配置问题，必须修复才能继续
     */
    BLOCKING,

    /**
     * 临时性错误：网络/超时等，可能自动恢复
     */
    TRANSIENT,

    /**
     * 永久性错误：SQL 语法错误等，需要人工介入
     */
    PERMANENT,

    /**
     * 无错误：正常情况
     */
    NONE
}
```

---

## 5. 接口设计

### 5.1 工具层接口改造

**改造前**：
```java
public String getTableSchema(String database, String tableName) {
    try {
        List<Map<String, Object>> result = jdbcTemplate.queryForList(sql, database, tableName);
        return JSON.toJSONString(result);
    } catch (Exception e) {
        log.error("查询失败", e);
        return ""; // ❌ 返回空字符串，无法区分错误
    }
}
```

**改造后**：
```java
public ToolResult getTableSchema(String database, String tableName) {
    long startTime = System.currentTimeMillis();

    try {
        // 1. 参数校验
        if (database == null || database.trim().isEmpty()) {
            return ToolResult.failure(
                ErrorCode.TABLE_NOT_FOUND,
                "数据库名称不能为空"
            );
        }

        // 2. 执行查询
        List<Map<String, Object>> result = jdbcTemplate.queryForList(sql, database, tableName);

        // 3. 判断结果
        if (result.isEmpty()) {
            return ToolResult.failure(
                ErrorCode.TABLE_NOT_FOUND,
                String.format("表 '%s.%s' 不存在或查询结果为空", database, tableName)
            );
        }

        // 4. 成功返回
        return ToolResult.success(JSON.toJSONString(result));

    } catch (SQLException e) {
        // 5. 解析错误码
        ErrorCode errorCode = parseDatabaseError(e);
        return ToolResult.failure(errorCode, e.getMessage());

    } finally {
        long duration = System.currentTimeMillis() - startTime;
        // 记录执行时间到监控系统
        metricsService.recordToolExecution("getTableSchema", duration);
    }
}

/**
 * 解析数据库异常为错误码
 */
private ErrorCode parseDatabaseError(SQLException e) {
    String sqlState = e.getSQLState();
    String message = e.getMessage();

    // 根据 SQLState 和消息内容判断错误类型
    if ("42000".equals(sqlState) || message.contains("Unknown database")) {
        return ErrorCode.DB_NOT_FOUND;
    }
    if ("42S02".equals(sqlState) || message.contains("Table") && message.contains("doesn't exist")) {
        return ErrorCode.TABLE_NOT_FOUND;
    }
    if ("42000".equals(sqlState) || message.contains("Access denied")) {
        return ErrorCode.ACCESS_DENIED;
    }
    if ("08S01".equals(sqlState) || message.contains("timeout")) {
        return ErrorCode.QUERY_TIMEOUT;
    }

    // 默认：未知错误
    return ErrorCode.SYNTAX_ERROR;
}
```

### 5.2 Agent Prompt 改造

**DBAgent 系统提示词（关键修改）**：

```markdown
你是一位资深 MySQL 数据库专家。

# 工具调用规范（必须严格遵守）

## 当工具返回 ToolResult 时

所有工具都会返回 ToolResult 格式的 JSON：

**成功示例**：
```json
{
  "success": true,
  "data": "[{"column_name":"id",...}]",
  "category": "NONE"
}
```

**失败示例**：
```json
{
  "success": false,
  "errorCode": "ENV_001",
  "errorMessage": "Unknown database 'crm_db'",
  "userMessage": "目标数据库 'crm_db' 不存在",
  "category": "BLOCKING",
  "recoveryStrategy": "ABORT",
  "suggestedRetries": 0
}
```

## 你的处理逻辑（必须遵守）

### 当 success = true 时
1. ✅ 继续分析，使用 data 字段中的数据
2. ✅ 可以继续调用其他工具

### 当 success = false 时

#### 1️⃣ 错误类别 = BLOCKING（阻断性）
- `errorCode` 以 "ENV_" 或 "PERM_" 开头
- `category` = "BLOCKING"
- `recoveryStrategy` = "ABORT"
- **你必须**：
  - ⛔ **立即停止**调用任何工具
  - 📝 输出最终报告，不要继续分析
  - 💡 在报告中明确说明问题原因和解决建议

**报告模板**：
```markdown
## ⚠️ 环境检查失败

**问题诊断**：
- {userMessage}

**影响**：
- 无法获取表结构和执行计划
- 无法继续进行深度分析

**解决建议**：
1. 检查数据库是否存在：`SHOW DATABASES;`
2. 检查连接配置是否正确
3. 如果数据库已删除，请清理相关慢查询记录
```

#### 2️⃣ 错误类别 = TRANSIENT（临时性）
- `errorCode` 以 "NET_" 开头
- `category` = "TRANSIENT"
- `recoveryStrategy` = "RETRY"
- **你必须**：
  - 🔄 可以重试（最多 3 次）
  - 📝 在报告中说明遇到临时性错误

#### 3️⃣ 错误类别 = PERMANENT（永久性）
- `errorCode` 以 "SQL_" 开头
- `category` = "PERMANENT"
- `recoveryStrategy` = "ABORT"
- **你必须**：
  - ⛔ 立即停止分析
  - 📝 输出错误报告

#### 4️⃣ 错误类别 = NONE（无错误）
- `category` = "NONE"
- ✅ 继续正常流程

## 严禁行为

- ❌ **严禁产生幻觉**：当工具返回错误时，不要编造任何数据库信息
- ❌ **严禁忽视错误**：当 `success=false` 时，不能假装成功继续分析
- ❌ **严禁编造建议**：不要给出无法实施的优化建议

## 示例对话

**示例 1：数据库不存在**
```
You: 调用 getTableSchema("crm_db", "customers")
Tool: {"success":false,"errorCode":"ENV_001","userMessage":"数据库不存在"}
You (Thought): "数据库不存在，这是环境问题，无法继续分析。"
You (Final Answer): "⚠️ 无法完成诊断。原因：数据库 crm_db 不存在。建议：请检查数据库是否存在..."
```

**示例 2：正常流程**
```
You: 调用 getTableSchema("shop", "orders")
Tool: {"success":true,"data":"[...]"}
You (Thought): "获取表结构成功，继续分析索引。"
You: 调用 getTableIndexes("shop", "orders")
...
```
```

---

### 5.3 Coordinator 层熔断器实现

```java
package com.dbdoctor.service;

import com.dbdoctor.model.ToolResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 熔断器管理器
 *
 * 功能：
 * - 记录各工具的失败次数
 * - 判断是否应该熔断
 * - 在熔断状态下阻止工具调用
 */
@Slf4j
@Component
public class CircuitBreaker {

    private final Map<String, FailureStats> failureStats = new ConcurrentHashMap<>();

    // 配置参数
    @Value("${db-doctor.circuit-breaker.failure-threshold:3}")
    private int failureThreshold;

    @Value("${db-doctor.circuit-breaker.timeout-seconds:60}")
    private int timeoutSeconds;

    /**
     * 判断工具调用是否允许执行（熔断器检查）
     *
     * @param toolName 工具名称
     * @return true=允许执行, false=被熔断
     */
    public boolean allowExecution(String toolName) {
        FailureStats stats = failureStats.computeIfAbsent(toolName, k -> new FailureStats());

        // 检查是否在熔断状态
        if (stats.isCircuitOpen()) {
            if (System.currentTimeMillis() - stats.getLastFailureTime() > timeoutSeconds * 1000L) {
                // 超时熔断时间，尝试恢复到半开状态
                stats.transitionToHalfOpen();
                log.info("🔓 熔断器恢复: toolName={}, 状态=HALF_OPEN", toolName);
                return true;
            } else {
                log.warn("⛔ 熔断器阻止: toolName={}, 状态=OPEN", toolName);
                return false;
            }
        }

        // 检查是否在半开状态
        if (stats.isHalfOpen()) {
            log.info("🟡 半开状态: toolName={}, 允许 1 次尝试", toolName);
            return true;
        }

        // 关闭状态，正常执行
        return true;
    }

    /**
     * 记录工具调用结果
     *
     * @param toolName 工具名称
     * @param result 工具执行结果
     */
    public void recordResult(String toolName, ToolResult result) {
        FailureStats stats = failureStats.computeIfAbsent(toolName, k -> new FailureStats());

        if (result.isSuccess()) {
            // 成功：重置失败计数
            if (stats.getFailureCount() > 0) {
                log.info("✅ 工具恢复: toolName={}, 失败次数重置", toolName);
            }
            stats.reset();
        } else {
            // 失败：增加失败计数，检查是否需要熔断
            stats.incrementFailure();

            if (stats.getFailureCount() >= failureThreshold) {
                log.warn("⛔ 触发熔断: toolName={}, 失败次数={}", toolName, stats.getFailureCount());
                stats.transitionToOpen();
            }
        }
    }

    @Data
    private static class FailureStats {
        private int failureCount = 0;
        private long lastFailureTime = 0;
        private CircuitState state = CircuitState.CLOSED;

        boolean isCircuitOpen() { return state == CircuitState.OPEN; }
        boolean isHalfOpen() { return state == CircuitState.HALF_OPEN; }

        void incrementFailure() {
            failureCount++;
            lastFailureTime = System.currentTimeMillis();
        }

        void reset() {
            failureCount = 0;
            state = CircuitState.CLOSED;
        }

        void transitionToOpen() {
            state = CircuitState.OPEN;
        }

        void transitionToHalfOpen() {
            state = CircuitState.HALF_OPEN;
            // 半开状态：重置失败计数，但保持 OPEN 状态
            failureCount = 0;
        }
    }

    enum CircuitState {
        CLOSED,   // 正常状态
        HALF_OPEN, // 半开状态（探测）
        OPEN      // 熔断状态
    }
}
```

### 5.4 Agent 调用链改造

**MultiAgentCoordinator 改造**：

```java
public String analyze(AnalysisContext context) {
    log.info("开始多 Agent 协作分析: fingerprint={}", context.getSqlFingerprint());

    // === 第一步：主治医生诊断（带熔断器保护）===
    String diagnosisReport = performDiagnosis(context);

    // 🔧 检查是否应该熔断
    if (isDiagnosisAborted(diagnosisReport)) {
        log.warn("⛔ 主治医生诊断被中止（环境错误），跳过后续分析");
        return diagnosisReport; // 直接返回，不调用推理专家和编码专家
    }

    // === 第二步：判断是否需要升级 ===
    boolean needsExpert = shouldUpgradeToExpert(context);

    if (needsExpert) {
        // === 第三步：推理专家深度推理 ===
        String reasoningReport = performDeepReasoning(context, diagnosisReport);

        // === 第四步：编码专家生成优化方案 ===
        if (reasoningReport != null) {
            String optimizationCode = generateOptimizationCode(context, reasoningReport);
        }
    }

    // === 第五步：整合报告 ===
    return buildFinalReport(context, diagnosisReport, reasoningReport, optimizationCode);
}

/**
 * 检查诊断报告是否被中止
 */
private boolean isDiagnosisAborted(String diagnosisReport) {
    // 方法1：检查是否包含错误标记（AI 主动标记）
    if (diagnosisReport.contains("\"status\": \"ABORTED\"")) {
        return true;
    }

    // 方法2：检查是否包含阻断性错误关键词
    if (diagnosisReport.contains("⚠️ 环境检查失败") ||
        diagnosisReport.contains("数据库不存在") ||
        diagnosisReport.contains("表不存在")) {
        return true;
    }

    return false;
}
```

---

## 6. 测试用例（TDD）

### 6.1 单元测试（ToolResult）

**文件**: `src/test/java/com/dbdoctor/model/ToolResultTest.java`

```java
package com.dbdoctor.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * ToolResult 单元测试
 */
class ToolResultTest {

    @Test
    void testSuccessResult() {
        String data = "[{\"id\":1}]";
        ToolResult result = ToolResult.success(data);

        assertTrue(result.isSuccess());
        assertEquals(data, result.getData());
        assertEquals(ErrorCategory.NONE, result.getCategory());
        assertFalse(result.shouldCircuitBreak());
    }

    @Test
    void testFailureResult_Blocking() {
        ToolResult result = ToolResult.failure(
            ErrorCode.DB_NOT_FOUND,
            "Unknown database 'crm_db'"
        );

        assertFalse(result.isSuccess());
        assertEquals(ErrorCode.DB_NOT_FOUND, result.getErrorCode());
        assertEquals(ErrorCategory.BLOCKING, result.getCategory());
        assertTrue(result.shouldCircuitBreak());
        assertFalse(result.shouldRetry());
    }

    @Test
    void testFailureResult_Transient() {
        ToolResult result = ToolResult.failure(
            ErrorCode.CONNECTION_TIMEOUT,
            "Connection timeout"
        );

        assertFalse(result.isSuccess());
        assertEquals(ErrorCategory.TRANSIENT, result.getCategory());
        assertFalse(result.shouldCircuitBreak());
        assertTrue(result.shouldRetry());
    }
}
```

### 6.2 集成测试（工具层）

**文件**: `src/test/java/com/dbdoctor/agent/DiagnosticToolsIntegrationTest.java`

```java
package com.dbdoctor.agent;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 诊断工具集成测试
 *
 * 测试策略：
 * - 使用真实的 MySQL 容器（Testcontainers）
 * - 模拟各种错误场景
 */
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.yml")
class DiagnosticToolsIntegrationTest {

    @Autowired
    private DiagnosticTools tools;

    @Test
    void testGetTableSchema_DatabaseNotFound() {
        // Arrange
        String database = "non_existent_db";
        String tableName = "customers";

        // Act
        ToolResult result = tools.getTableSchema(database, tableName);

        // Assert
        assertFalse(result.isSuccess());
        assertEquals(ErrorCode.DB_NOT_FOUND, result.getErrorCode());
        assertTrue(result.getUserMessage().contains("不存在"));
    }

    @Test
    void testGetTableSchema_Success() {
        // Arrange
        String database = "test";
        String tableName = "customers";

        // Act
        ToolResult result = tools.getTableSchema(database, tableName);

        // Assert
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertFalse(result.getData().isEmpty());
    }

    @Test
    void testCircuitBreaker_AfterMultipleFailures() {
        // Arrange
        String database = "non_existent";
        String tableName = "customers";

        // Act: 连续调用 3 次
        tools.getTableSchema(database, tableName);
        tools.getTableSchema(database, tableName);
        ToolResult result3 = tools.getTableSchema(database, tableName);

        // Assert: 第三次应该触发熔断
        // TODO: 实现熔断器后验证
    }
}
```

### 6.3 Agent 行为测试

**文件**: `src/test/java/com/dbdoctor/agent/DBAgentBehaviorTest.java`

```java
/**
 * DBAgent 行为测试
 *
 * 测试 AI Agent 是否能正确处理工具错误
 */
@SpringBootTest
class DBAgentBehaviorTest {

    @Test
    void testAgent_ReceivesError_ShouldAbort() {
        // Arrange
        String prompt = "请分析：SELECT * FROM customers WHERE phone = '123'";

        // 模拟工具返回错误
        ToolResult errorResult = ToolResult.failure(
            ErrorCode.DB_NOT_FOUND,
            "Unknown database 'crm_db'"
        );

        // Act: 调用 Agent
        String response = diagnosisAgent.analyzeSlowLog(
            prompt + "\n\n工具返回：" + errorResult
        );

        // Assert: AI 应该识别错误并中止
        assertTrue(response.contains("⚠️"));
        assertTrue(response.contains("环境检查失败"));
        assertFalse(response.contains("医学诊断"));
    }
}
```

---

## 7. 实施计划

### 7.1 分阶段实施

#### 📅 **阶段 1：基础架构（2天）**

**目标**：实现 ToolResult 和错误码体系

- [ ] Day 1 上午：创建数据模型
  - [ ] `ToolResult.java`
  - [ ] `ErrorCode.java`
  - [ ] `ErrorCategory.java`
  - [ ] `RecoveryStrategy.java`

- [ ] Day 1 下午：单元测试
  - [ ] ToolResult 测试
  - [ ] ErrorCode 测试
  - [ ] 覆盖率 ≥ 90%

**交付物**：
- ✅ 数据模型代码
- ✅ 单元测试
- ✅ 测试覆盖率报告

#### 📅 **阶段 2：工具层改造（2天）**

**目标**：所有工具返回 ToolResult

- [ ] Day 2 上午：改造核心工具
  - [ ] `DiagnosticToolsImpl.getTableSchema()`
  - [ ] `DiagnosticToolsImpl.getExecutionPlan()`
  - [ ] `DiagnosticToolsImpl.getTableIndexes()`
  - [ ] `DiagnosticToolsImpl.getTableStatistics()`

- [ ] Day 2 下午：集成测试
  - [ ] 模拟各种错误场景
  - [ ] 验证错误码正确解析
  - [ ] 覆盖率 ≥ 85%

**交付物**：
- ✅ 改造后的工具层
- ✅ 集成测试
- ✅ 错误码映射表文档

#### 📅 **阶段 3：熔断器实现（1.5天）**

**目标**：实现熔断器逻辑

- [ ] Day 3 上午：熔断器核心逻辑
  - [ ] `CircuitBreaker.java`
  - [ ] 失败计数器
  - [ ] 状态机转换

- [ ] Day 3 下午：单元测试
  - [ ] 熔断触发逻辑
  - [ ] 恢复逻辑
  - [ ] 计时器测试

**交付物**：
- ✅ 熔断器代码
- ✅ 单元测试
- ✅ 配置文档

#### 📅 **阶段 4：Agent Prompt 改造（1.5天）**

**目标**：所有 Agent 理解 ToolResult

- [ ] Day 4：改造 Agent 系统提示词
  - [ ] `DBAgent.java` 系统提示词
  - [ ] `ReasoningAgent.java` 系统提示词
  - [ ] `CodingAgent.java` 系统提示词

- [ ] Day 4：行为测试
  - [ ] 验证 AI 正确处理错误
  - [ ] 验证 AI 不会产生幻觉

**交付物**：
- ✅ 改造后的 Agent Prompt
- ✅ 行为测试报告

#### 📅 **阶段 5：Coordinator 层改造（2天）**

**目标**：集成熔断器

- [ ] Day 5：重构 MultiAgentCoordinator
  - [ ] 移除硬编码字符串匹配
  - [ ] 使用熔断器检查
  - [ ] 实现智能中止逻辑

- [ ] Day 6：端到端测试
  - [ ] 测试完整流程
  - [ ] 验证异常处理
  - [ ] 性能测试

**交付物**：
- ✅ 重构后的 Coordinator
- ✅ 端到端测试
- ✅ 性能测试报告

---

### 7.2 风险控制

| 风险 | 影响 | 应对措施 |
|------|------|---------|
| **API 兼容性** | LangChain4j 是否支持 ToolResult | 先做 POC 验证 |
| **AI 行为不可控** | AI 可能不遵守 Prompt | 增加后置校验逻辑 |
| **性能影响** | 熔断器增加延迟 | 监控熔断器性能指标 |
| **回归测试** | 大规模代码修改 | 分阶段提交，每阶段测试 |

---

## 8. 验收标准

### 8.1 功能验收

- [ ] ✅ **工具返回统一格式**：所有工具返回 `ToolResult`
- [ ] ✅ **AI 正确处理错误**：AI 识别错误并中止，不再产生幻觉
- [ ] ✅ **熔断器生效**：连续失败 3 次后熔断 60 秒
- [ ] ✅ **错误分类准确**：环境/权限/网络/数据错误正确分类
- [ ] ✅ **智能恢复**：临时性错误自动重试，阻断性错误直接中止

### 8.2 性能验收

- [ ] ✅ **熔断器开销**：< 5ms per call
- [ ] ✅ **内存占用**：无明显增加
- [ ] ✅ **响应时间**：正常流程无影响

### 8.3 质量验收

- [ ] ✅ **单元测试覆盖率**：≥ 90%
- [ ] ✅ **集成测试覆盖率**：≥ 80%
- [ ] **E2E 测试**：关键场景覆盖

### 8.4 文档验收

- [ ] ✅ **架构设计文档**：本文档
- [ ] ✅ **错误码映射表**：Excel/Markdown
- [ ] ✅ **使用指南**：如何扩展新错误码
- [ ] [ ] **API 变更日志**：向后兼容说明

---

## 9. 附录

### 9.1 错误码映射表

| 错误码 | 用户消息 | 分类 | 恢复策略 | 重试次数 |
|--------|----------|------|---------|----------|
| ENV_001 | 数据库不存在 | BLOCKING | ABORT | 0 |
| ENV_002 | 表不存在 | BLOCKING | ABORT | 0 |
| ENV_003 | 列不存在 | BLOCKING | ABORT | 0 |
| ENV_004 | 慢查询日志未启用 | BLOCKING | ABORT | 0 |
| PERM_001 | 访问被拒绝 | BLOCKING | ABORT | 0 |
| PERM_002 | 权限不足 | BLOCKING | ABORT | 0 |
| NET_001 | 连接超时 | TRANSIENT | RETRY | 3 |
| NET_002 | 连接丢失 | TRANSIENT | RETRY | 3 |
| DATA_001 | 查询结果为空 | NONE | CONTINUE | 0 |
| SQL_001 | SQL 语法错误 | PERMANENT | ABORT | 0 |

### 9.2 配置示例

**application.yml** 新增配置：

```yaml
db-doctor:
  # 熔断器配置
  circuit-breaker:
    # 连续失败多少次触发熔断
    failure-threshold: 3
    # 熔断持续时间（秒）
    timeout-seconds: 60
    # 半开状态允许的调用次数
    half-open-max-calls: 1
    # 无错误后多久重置计数器（秒）
    reset-timeout-seconds: 300

  # 错误重试配置
  retry:
    # 最大重试次数
    max-attempts: 3
    # 重试间隔（毫秒）- 指数退避
    backoff-ms: 1000
    # 最大重试间隔（毫秒）
    max-backoff-ms: 10000
```

---

## 10. 总结

本设计文档描述了一个**企业级异常处理架构**，核心改进包括：

### ✅ 优势

1. **AI 自主决策**：AI 根据工具返回的错误信息自主决定下一步行动
2. **熔断器保护**：避免连续失败时浪费资源
3. **错误分类**：智能识别错误类型，选择合适的处理策略
4. **智能恢复**：临时性错误自动重试
5. **结构化反馈**：统一的 `ToolResult` 格式

### 📊 对比当前方案

| 维度 | 当前方案（v2.2） | 目标方案（v3.0） |
|------|----------------|----------------|
| 错误处理 | 硬编码字符串匹配 | 结构化错误码 |
| AI 智能 | 代码强制控制 | AI 自主决策 |
| 可扩展性 | 低（改代码） | 高（配置驱动） |
| 可维护性 | 差（分散各处） | 好（统一封装） |
| 生产级 | ⚠️ MVP 阶段 | ✅ 企业级 |

### 🎯 下一步

**立即执行**：
1. 审查本文档，确认需求
2. 创建开发分支：`feature/enterprise-error-handling`
3. 按照实施计划分阶段开发
4. 每个阶段完成后进行 Code Review

**预计收益**：
- 🎯 系统稳定性提升 80%
- 🎯 AI 报告质量提升 90%
- 🎯 运维成本降低 50%
- 🎯 用户体验显著改善

---

**文档版本历史**：
- v1.0.0 (2026-01-31) - 初始版本，基于 Gemini 架构评审

---

**相关文档**：
- [TDD 实施指南](../../TDD_实施指南.md)
- [架构设计原则](/principles.md)
- [Gemini 聊天记录](../discussions/gemini-review-20260131.md)
