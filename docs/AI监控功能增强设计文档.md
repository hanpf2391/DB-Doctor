# AI 监控功能增强设计文档

## 📋 文档信息

| 项目 | 内容 |
|------|------|
| **文档名称** | AI 监控功能增强设计文档 (AI Monitoring Enhancement Design) |
| **文档版本** | v1.0.0 |
| **创建日期** | 2026-01-31 |
| **适用版本** | DB-Doctor v2.3.1+ |
| **作者** | DB-Doctor Team |
| **状态** | 🟡 待实施 |
| **优先级** | 🔴 高（修复关键缺陷 + 新增重要功能） |

---

## 一、问题分析

### 1.1 当前问题

#### 问题 1：Token 统计为 0

**现象**：
```java
// 当前代码
invocationLog.setInputTokens(0);     // ❌ 硬编码为 0
invocationLog.setOutputTokens(0);   // ❌ 硬编码为 0
invocationLog.setTotalTokens(0);    // ❌ 硬编码为 0
```

**根本原因**：

1. **LangChain4j 0.35.0 API 限制**：`ChatModelResponseContext` 没有直接提供 `chatResponse()` 方法
2. **Context 对象结构变化**：新版 API 中，`ChatModelResponseContext` 的方法签名发生变更

**当前代码实现**：
```java
// ❌ 错误尝试（LangChain4j 0.35.0 不支持）
@Override
public void onResponse(ChatModelResponseContext context) {
    // context.chatResponse() 方法不存在！
    ChatResponse response = context.chatResponse();
    if (response.tokenUsage() != null) {
        log.setInputTokens(response.tokenUsage().inputTokenCount());
        log.setOutputTokens(response.tokenUsage().outputTokenCount());
        log.setTotalTokens(response.tokenUsage().totalTokenCount());
    }
}
```

**影响**：
- 监控大盘显示 Token 消耗为 0
- 无法进行成本分析
- 无法优化 Prompt 长度

---

#### 问题 2：缺少分析维度监控

**现象**：

当前只有全局统计（所有调用的聚合），缺少**单次慢查询分析**的详细监控。

**用户需求**：

用户希望能够查看**每个 SQL 指纹（traceId）** 的一次完整分析过程：

```
SQL 指纹: abc123... (SELECT * FROM users WHERE email = ?)
├─ 步骤 1：主治医生诊断
│   ├─ 调用时间：2026-01-31 10:30:05
│   ├─ 耗时：3.5s
│   ├─ Token 消耗：1,250 (输入: 800, 输出: 450)
│   └─ 状态：成功
│
├─ 步骤 2：推理专家深度推理
│   ├─ 调用时间：2026-01-31 10:30:10
│   ├─ 耗时：8.2s
│   ├─ Token 消耗：2,100 (输入: 1,500, 输出: 600)
│   └─ 状态：成功
│
└─ 步骤 3：编码专家生成优化方案
    ├─ 调用时间：2026-01-31 10:30:20
    ├─ 耗时：5.1s
    ├─ Token 消耗：1,800 (输入: 1,200, 输出: 600)
    └─ 状态：成功

总计：3 次调用，16.8s，5,150 Tokens
```

**当前缺失**：
- 没有"单次分析详情"页面
- 无法看到某次分析的完整调用链
- 无法对比不同 SQL 的分析成本

---

### 1.2 架构局限性

#### 当前监控架构

```
┌─────────────────────────────────────────────────────────────┐
│                    当前监控架构（v2.3.0）                     │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  MultiAgentCoordinator                                      │
│  ├─ performDiagnosis()                                      │
│  │  └─ diagnosisAgent.analyzeSlowLog()                      │
│  │       └─ AiMonitoringListener (记录到数据库)             │
│  │                                                           │
│  ├─ performDeepReasoning()                                  │
│  │  └─ reasoningAgent.performDeepReasoning()                │
│  │       └─ AiMonitoringListener (记录到数据库)             │
│  │                                                           │
│  └─ generateOptimizationCode()                              │
│     └─ codingAgent.generateOptimizationCode()               │
│          └─ AiMonitoringListener (记录到数据库)             │
│                                                             │
│  └─> 问题：只有全局统计，缺少单次分析视图                    │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

#### 期望监控架构

```
┌─────────────────────────────────────────────────────────────┐
│                    增强后监控架构（v2.3.1）                   │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. 全局监控大盘                                             │
│     ├─ 总调用次数、Token 消耗、平均耗时、成功率               │
│     ├─ Agent 分布饼图                                       │
│     └─ 24 小时趋势图                                        │
│                                                             │
│  2. 单次分析详情页              ← 🆕 新增                    │
│     ├─ 按 traceId 聚合所有 AI 调用                          │
│     ├─ 展示分析流程（DIAGNOSIS → REASONING → CODING）       │
│     ├─ 每个 Agent 的 Token 消耗、耗时                        │
│     └─ 类似"调用链追踪"的概念                                │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 二、解决方案设计

### 2.1 方案 1：修复 Token 统计

#### 技术方案对比

| 方案 | 描述 | 优点 | 缺点 | 推荐度 |
|------|------|------|------|--------|
| **方案 A** | 升级 LangChain4j 到 0.36.0+ | API 完善，支持完整 Token 统计 | 可能引入不兼容变更 | ⭐⭐⭐⭐ |
| **方案 B** | 使用反射获取 Token 信息 | 不需要升级版本 | 不稳定，可能在未来版本失效 | ⭐⭐ |
| **方案 C** | 手动估算 Token 数量 | 实现简单 | 不准确，误差大 | ⭐ |
| **方案 D** | 扩展 ChatModelRequestContext | 侵入性小 | 需要修改多处代码 | ⭐⭐⭐ |

#### 推荐：方案 A + 方案 D 组合

**策略**：
1. **短期（v2.3.1）**：使用方案 D（扩展 Context）快速修复
2. **长期（v2.4.0）**：升级到 LangChain4j 0.36.0+，使用官方 API

---

#### 实现方案 D：扩展 AiContextHolder 传递 Prompt

**核心思想**：在调用 AI 前记录 Prompt，在响应时通过 Prompt 长度估算 Token 数。

**步骤 1：扩展 AiContextHolder**

```java
// 文件：src/main/java/com/dbdoctor/monitoring/AiContextHolder.java

package com.dbdoctor.monitoring;

import java.util.HashMap;
import java.util.Map;

/**
 * AI 监控上下文持有者（ThreadLocal）- 增强版
 *
 * <p>v2.3.1 新增功能：</p>
 * <ul>
 *   <li>支持传递 Prompt（用于 Token 估算）</li>
 *   <li>支持传递 Response（用于 Token 统计）</li>
 *   <li>支持传递自定义元数据</li>
 * </ul>
 *
 * @author DB-Doctor
 * @version 2.3.1
 * @since 2.3.0
 */
public class AiContextHolder {

    private static final ThreadLocal<Map<String, String>> CONTEXT =
        ThreadLocal.withInitial(HashMap::new);

    // 预定义的键名常量
    public static final String KEY_AGENT_NAME = "agentName";
    public static final String KEY_TRACE_ID = "traceId";
    public static final String KEY_PROMPT = "prompt";           // 🆕
    public static final String KEY_RESPONSE = "response";       // 🆕
    public static final String KEY_MODEL_NAME = "modelName";     // 🆕

    /**
     * 设置元数据
     */
    public static void set(String key, String value) {
        if (value != null) {
            CONTEXT.get().put(key, value);
        }
    }

    /**
     * 获取元数据
     */
    public static String get(String key) {
        return CONTEXT.get().get(key);
    }

    /**
     * 获取元数据（带默认值）
     */
    public static String get(String key, String defaultValue) {
        String value = CONTEXT.get().get(key);
        return value != null ? value : defaultValue;
    }

    /**
     * 检查上下文是否已设置
     */
    public static boolean isSet() {
        return !CONTEXT.get().isEmpty();
    }

    /**
     * 清理上下文
     *
     * <p><strong>重要：</strong>每次 AI 调用完成后必须调用此方法，避免内存泄漏</p>
     */
    public static void clear() {
        CONTEXT.remove();
    }

    /**
     * 获取所有元数据
     */
    public static Map<String, String> getAll() {
        return new HashMap<>(CONTEXT.get());
    }

    /**
     * 批量设置元数据
     */
    public static void setAll(Map<String, String> metadata) {
        if (metadata != null) {
            CONTEXT.get().putAll(metadata);
        }
    }

    /**
     * 获取上下文摘要（用于调试）
     */
    public static String getSummary() {
        return String.format("AiContextHolder{context=%s}", CONTEXT.get());
    }

    // ===== 便捷方法（推荐使用） =====

    /**
     * 设置 Agent 名称
     */
    public static void setAgentName(String agentName) {
        set(KEY_AGENT_NAME, agentName);
    }

    /**
     * 设置 SQL 指纹
     */
    public static void setTraceId(String traceId) {
        set(KEY_TRACE_ID, traceId);
    }

    /**
     * 设置 Prompt（用于 Token 估算）- 🆕
     */
    public static void setPrompt(String prompt) {
        set(KEY_PROMPT, prompt);
    }

    /**
     * 获取 Prompt - 🆕
     */
    public static String getPrompt() {
        return get(KEY_PROMPT, "");
    }

    /**
     * 设置 Response（用于 Token 统计）- 🆕
     */
    public static void setResponse(String response) {
        set(KEY_RESPONSE, response);
    }

    /**
     * 获取 Response - 🆕
     */
    public static String getResponse() {
        return get(KEY_RESPONSE, "");
    }

    /**
     * 设置模型名称 - 🆕
     */
    public static void setModelName(String modelName) {
        set(KEY_MODEL_NAME, modelName);
    }

    /**
     * 获取模型名称 - 🆕
     */
    public static String getModelName() {
        return get(KEY_MODEL_NAME, "unknown");
    }
}
```

**步骤 2：创建 Token 估算工具类**

```java
// 文件：src/main/java/com/dbdoctor/monitoring/TokenEstimator.java

package com.dbdoctor.monitoring;

import lombok.extern.slf4j.Slf4j;

/**
 * Token 估算工具类
 *
 * <p>用于在 LangChain4j API 不支持 Token 统计时的备用方案</p>
 *
 * <p>估算规则：</p>
 * <ul>
 *   <li>英文：约 4 字符 / Token</li>
 *   <li>中文：约 1.5 字符 / Token</li>
 *   <li>代码（SQL）：约 3 字符 / Token</li>
 *   <li>混合内容：加权平均</li>
 * </ul>
 *
 * @author DB-Doctor
 * @version 2.3.1
 * @since 2.3.1
 */
@Slf4j
public class TokenEstimator {

    /**
     * 英文 Token 估算系数（字符数 / Token 数）
     */
    private static final double ENGLISH_RATIO = 4.0;

    /**
     * 中文 Token 估算系数（字符数 / Token 数）
     */
    private static final double CHINESE_RATIO = 1.5;

    /**
     * 代码（SQL）Token 估算系数（字符数 / Token 数）
     */
    private static final double CODE_RATIO = 3.0;

    /**
     * 估算输入 Token 数
     *
     * @param text 输入文本（Prompt）
     * @return 估算的 Token 数
     */
    public static int estimateInputTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        int length = text.length();

        // 统计中文字符数
        int chineseChars = countChineseCharacters(text);
        // 统计英文字符数（字母 + 空格）
        int englishChars = countEnglishCharacters(text);
        // 其余视为代码/特殊字符
        int otherChars = length - chineseChars - englishChars;

        // 估算 Token 数
        double chineseTokens = chineseChars / CHINESE_RATIO;
        double englishTokens = englishChars / ENGLISH_RATIO;
        double otherTokens = otherChars / CODE_RATIO;

        return (int) Math.ceil(chineseTokens + englishTokens + otherTokens);
    }

    /**
     * 估算输出 Token 数
     *
     * @param text 输出文本（Response）
     * @return 估算的 Token 数
     */
    public static int estimateOutputTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        // 使用与输入相同的估算逻辑
        return estimateInputTokens(text);
    }

    /**
     * 估算总 Token 数
     *
     * @param inputText 输入文本
     * @param outputText 输出文本
     * @return 总 Token 数
     */
    public static int estimateTotalTokens(String inputText, String outputText) {
        return estimateInputTokens(inputText) + estimateOutputTokens(outputText);
    }

    /**
     * 统计中文字符数
     *
     * @param text 文本
     * @return 中文字符数
     */
    private static int countChineseCharacters(String text) {
        int count = 0;
        for (char c : text.toCharArray()) {
            if (isChineseCharacter(c)) {
                count++;
            }
        }
        return count;
    }

    /**
     * 统计英文字符数（字母 + 空格）
     *
     * @param text 文本
     * @return 英文字符数
     */
    private static int countEnglishCharacters(String text) {
        int count = 0;
        for (char c : text.toCharArray()) {
            if (isEnglishCharacter(c) || c == ' ') {
                count++;
            }
        }
        return count;
    }

    /**
     * 判断是否为中文字符
     *
     * @param c 字符
     * @return 如果是中文返回 true
     */
    private static boolean isChineseCharacter(char c) {
        return c >= 0x4E00 && c <= 0x9FA5;
    }

    /**
     * 判断是否为英文字符
     *
     * @param c 字符
     * @return 如果是英文返回 true
     */
    private static boolean isEnglishCharacter(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    /**
     * 计算估算准确度（用于日志）
     *
     * @param estimated 估算值
     * @param actual 实际值（如果有）
     * @return 准确度百分比
     */
    public static double calculateAccuracy(int estimated, Integer actual) {
        if (actual == null || actual == 0) {
            return 0.0;
        }

        double error = Math.abs(estimated - actual) * 100.0 / actual;
        return 100.0 - error;
    }
}
```

**步骤 3：修改 AiMonitoringListener 使用估算**

```java
// 文件：src/main/java/com/dbdoctor/monitoring/AiMonitoringListener.java（修改版）

package com.dbdoctor.monitoring;

import com.dbdoctor.common.enums.AiErrorCategory;
import com.dbdoctor.common.enums.InvocationStatus;
import com.dbdoctor.entity.AiInvocationLog;
import com.dbdoctor.service.AiInvocationLogService;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI 监控监听器 - v2.3.1 增强版
 *
 * <p>核心改进：</p>
 * <ul>
 *   <li>使用 AiContextHolder 获取 Prompt 和 Response</li>
 *   <li>使用 TokenEstimator 估算 Token 数</li>
 *   <li>改进 Token 统计准确性</li>
 * </ul>
 *
 * @author DB-Doctor
 * @version 2.3.1
 * @since 2.3.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiMonitoringListener implements ChatModelListener {

    private final AiInvocationLogService logService;

    private final ConcurrentHashMap<String, LocalDateTime> requestStartTimes = new ConcurrentHashMap<>();

    @Override
    public void onRequest(ChatModelRequestContext context) {
        try {
            String requestId = String.valueOf(context.hashCode());
            requestStartTimes.put(requestId, LocalDateTime.now());

            // 🆕 从 Context 获取模型名称
            String modelName = AiContextHolder.getModelName();
            AiContextHolder.setModelName(modelName);

            log.debug("[AI监控] 请求开始: requestId={}, model={}", requestId, modelName);
        } catch (Exception e) {
            log.error("[AI监控] onRequest 处理失败", e);
        }
    }

    @Override
    public void onResponse(ChatModelResponseContext context) {
        try {
            String requestId = String.valueOf(context.hashCode());
            LocalDateTime startTime = requestStartTimes.remove(requestId);

            if (startTime == null) {
                log.warn("[AI监控] 无法找到请求开始时间: requestId={}", requestId);
                startTime = LocalDateTime.now();
            }

            LocalDateTime endTime = LocalDateTime.now();
            long durationMs = java.time.Duration.between(startTime, endTime).toMillis();

            // 从 ThreadLocal 获取元数据
            String agentName = AiContextHolder.get(AiContextHolder.KEY_AGENT_NAME, "UNKNOWN");
            String traceId = AiContextHolder.get(AiContextHolder.KEY_TRACE_ID, "UNKNOWN");
            String modelName = AiContextHolder.getModelName();

            // 🆕 获取 Prompt 和 Response
            String prompt = AiContextHolder.getPrompt();
            String response = AiContextHolder.getResponse();

            // 构建日志实体
            AiInvocationLog invocationLog = new AiInvocationLog();

            // 基本信息
            invocationLog.setTraceId(traceId);
            invocationLog.setAgentName(agentName);
            invocationLog.setModelName(modelName);
            invocationLog.setProvider(extractProvider(modelName));

            // 时间信息
            invocationLog.setStartTime(startTime);
            invocationLog.setEndTime(endTime);
            invocationLog.setDurationMs(durationMs);
            invocationLog.setCreatedTime(LocalDateTime.now());

            // 状态信息
            invocationLog.setStatus(InvocationStatus.SUCCESS.getCode());

            // 🆕 Token 统计（使用估算）
            int inputTokens = TokenEstimator.estimateInputTokens(prompt);
            int outputTokens = TokenEstimator.estimateOutputTokens(response);
            int totalTokens = inputTokens + outputTokens;

            invocationLog.setInputTokens(inputTokens);
            invocationLog.setOutputTokens(outputTokens);
            invocationLog.setTotalTokens(totalTokens);

            // 🆕 可选：存储 Prompt 和 Response（根据配置决定）
            // invocationLog.setPromptText(prompt);
            // invocationLog.setResponseText(response);

            // 异步保存
            logService.saveAsync(invocationLog);

            log.debug("[AI监控] 请求成功: agent={}, tokens={}, duration={}ms",
                    agentName, totalTokens, durationMs);

        } catch (Exception e) {
            log.error("[AI监控] onResponse 处理失败", e);
        }
    }

    @Override
    public void onError(ChatModelErrorContext context) {
        try {
            // ... (保持原有逻辑)

            // 🆕 即使失败也尝试估算 Token（基于 Prompt）
            String prompt = AiContextHolder.getPrompt();
            int inputTokens = TokenEstimator.estimateInputTokens(prompt);
            invocationLog.setInputTokens(inputTokens);
            invocationLog.setOutputTokens(0);
            invocationLog.setTotalTokens(inputTokens);

            logService.saveAsync(invocationLog);

        } catch (Exception e) {
            log.error("[AI监控] onError 处理失败", e);
        }
    }

    /**
     * 从模型名称提取供应商
     */
    private String extractProvider(String modelName) {
        if (modelName == null) {
            return "unknown";
        }
        String lower = modelName.toLowerCase();
        if (lower.contains("qwen") || lower.contains("deepseek") || lower.contains("ollama")) {
            return "ollama";
        } else if (lower.contains("gpt") || lower.contains("openai")) {
            return "openai";
        } else if (lower.contains("deepseek") && !lower.contains("ollama")) {
            return "deepseek";
        }
        return "unknown";
    }
}
```

**步骤 4：修改 MultiAgentCoordinator 传递 Prompt 和 Response**

```java
// 文件：src/main/java/com/dbdoctor/service/MultiAgentCoordinator.java（修改版）

// 在 performDiagnosis 方法中：

private String performDiagnosis(AnalysisContext context) {
    log.info("调用主治医生 Agent 进行初步诊断");

    String formattedPrompt = promptUtil.formatUserPrompt(
            context.getDbName(),
            formatTime(context.getTemplateStats().getFirstSeenTime()),
            context.getTemplateStats().getAvgQueryTime(),
            context.getTemplateStats().getAvgLockTime(),
            context.getTemplateStats().getMaxRowsExamined(),
            context.getTemplateStats().getMaxRowsSent(),
            context.getSampleSql()
    );

    try {
        // 🆕 设置监控元数据（包括 Prompt）
        AiContextHolder.setAgentName(AgentName.DIAGNOSIS.getCode());
        AiContextHolder.setTraceId(context.getSqlFingerprint());
        AiContextHolder.setPrompt(formattedPrompt);  // ← 传递 Prompt

        String result = diagnosisAgent.analyzeSlowLog(formattedPrompt);

        // 🆕 记录 Response
        AiContextHolder.setResponse(result);

        return result;
    } catch (Exception e) {
        log.error("主治医生诊断失败", e);
        throw new RuntimeException("主治医生诊断失败: " + e.getMessage(), e);
    } finally {
        // 清理监控元数据
        AiContextHolder.clear();
    }
}
```

---

### 2.2 方案 2：新增分析维度监控

#### 数据模型调整

**无需修改表结构**：现有的 `ai_invocation_log` 表已经包含 `trace_id` 字段，可以直接使用。

#### 新增 DTO：AnalysisTraceDetail

```java
// 文件：src/main/java/com/dbdoctor/model/AnalysisTraceDetail.java

package com.dbdoctor.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 单次分析详情 DTO
 *
 * <p>表示一个 SQL 指纹（traceId）的完整分析过程</p>
 *
 * @author DB-Doctor
 * @version 2.3.1
 * @since 2.3.1
 */
@Data
public class AnalysisTraceDetail {

    /**
     * SQL 指纹
     */
    private String traceId;

    /**
     * SQL 示例（用于展示）
     */
    private String sampleSql;

    /**
     * 数据库名称
     */
    private String dbName;

    /**
     * 分析开始时间
     */
    private LocalDateTime startTime;

    /**
     * 分析结束时间
     */
    private LocalDateTime endTime;

    /**
     * 总耗时（毫秒）
     */
    private Long totalDurationMs;

    /**
     * 总 Token 消耗
     */
    private Integer totalTokens;

    /**
     * 总调用次数
     */
    private Integer totalCalls;

    /**
     * 成功率
     */
    private Double successRate;

    /**
     * AI 调用详情列表（按时间顺序）
     */
    private List<AiInvocationDetail> invocations;

    /**
     * 状态：SUCCESS（全部成功）/ PARTIAL_FAILURE（部分失败）/ FAILED（全部失败）
     */
    private String status;

    /**
     * 获取格式化的耗时描述
     */
    public String getDurationDescription() {
        if (totalDurationMs == null) {
            return "N/A";
        }
        if (totalDurationMs < 1000) {
            return totalDurationMs + "ms";
        } else if (totalDurationMs < 60000) {
            return String.format("%.1fs", totalDurationMs / 1000.0);
        } else {
            long minutes = totalDurationMs / 60000;
            long seconds = (totalDurationMs % 60000) / 1000;
            return String.format("%dm%ds", minutes, seconds);
        }
    }

    /**
     * 判断是否全部成功
     */
    public boolean isAllSuccess() {
        return "SUCCESS".equals(this.status);
    }
}
```

#### 新增 Service 方法

```java
// 文件：src/main/java/com/dbdoctor/service/AiInvocationLogService.java（新增方法）

/**
 * 获取单次分析详情（按 traceId 聚合）
 *
 * @param traceId SQL 指纹
 * @return 分析详情
 */
public AnalysisTraceDetail getAnalysisTraceDetail(String traceId) {
    List<AiInvocationLog> logs = repository.findByTraceIdOrderByStartTimeAsc(traceId);

    if (logs.isEmpty()) {
        return null;
    }

    AnalysisTraceDetail detail = new AnalysisTraceDetail();
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
    List<AiInvocationDetail> details = logs.stream()
            .map(this::toDetail)
            .toList();
    detail.setInvocations(details);

    return detail;
}

/**
 * 获取所有分析记录的分页列表
 *
 * @param startTime 开始时间
 * @param endTime   结束时间
 * @param page      页码（从 0 开始）
 * @param size      每页大小
 * @return 分页结果
 */
public Page<AnalysisTraceSummary> listAnalysisTraces(
        LocalDateTime startTime,
        LocalDateTime endTime,
        int page,
        int size) {

    // 查询所有不重复的 traceId
    List<String> traceIds = repository.findDistinctTraceIdsByStartTimeBetween(startTime, endTime);

    // 分页
    int start = page * size;
    int end = Math.min(start + size, traceIds.size());
    List<String> pageTraceIds = traceIds.subList(start, end);

    // 构建摘要列表
    List<AnalysisTraceSummary> summaries = new ArrayList<>();
    for (String traceId : pageTraceIds) {
        AnalysisTraceDetail detail = getAnalysisTraceDetail(traceId);
        AnalysisTraceSummary summary = new AnalysisTraceSummary();
        summary.setTraceId(traceId);
        summary.setStartTime(detail.getStartTime());
        summary.setTotalCalls(detail.getTotalCalls());
        summary.setTotalDurationMs(detail.getTotalDurationMs());
        summary.setTotalTokens(detail.getTotalTokens());
        summary.setStatus(detail.getStatus());
        summaries.add(summary);
    }

    return new PageImpl<>(summaries, PageRequest.of(page, size), traceIds.size());
}
```

#### 新增 DTO：AnalysisTraceSummary

```java
// 文件：src/main/java/com/dbdoctor/model/AnalysisTraceSummary.java

package com.dbdoctor.model;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 单次分析摘要 DTO
 *
 * <p>用于列表展示，不包含详细信息</p>
 *
 * @author DB-Doctor
 * @version 2.3.1
 * @since 2.3.1
 */
@Data
public class AnalysisTraceSummary {

    /**
     * SQL 指纹
     */
    private String traceId;

    /**
     * 分析开始时间
     */
    private LocalDateTime startTime;

    /**
     * 总调用次数
     */
    private Integer totalCalls;

    /**
     * 总耗时（毫秒）
     */
    private Long totalDurationMs;

    /**
     * 总 Token 消耗
     */
    private Integer totalTokens;

    /**
     * 状态
     */
    private String status;
}
```

#### 新增 Repository 方法

```java
// 文件：src/main/java/com/dbdoctor/repository/AiInvocationLogRepository.java（新增方法）

/**
 * 查询指定时间内的所有不重复 traceId
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
```

#### 新增 Controller 接口

```java
// 文件：src/main/java/com/dbdoctor/controller/AiMonitorController.java（新增方法）

/**
 * 获取单次分析详情（按 traceId 聚合）
 *
 * @param traceId SQL 指纹
 * @return 分析详情
 */
@GetMapping("/analysis-trace/{traceId}")
public Result<AnalysisTraceDetail> getAnalysisTraceDetail(@PathVariable String traceId) {
    log.info("[AI监控] 查询单次分析详情: traceId={}", traceId);

    try {
        AnalysisTraceDetail detail = logService.getAnalysisTraceDetail(traceId);

        if (detail == null) {
            return Result.error(404, "未找到该 SQL 的分析记录");
        }

        log.info("[AI监控] 查询成功: traceId={}, calls={}, tokens={}, duration={}ms",
                traceId, detail.getTotalCalls(), detail.getTotalTokens(), detail.getTotalDurationMs());

        return Result.success(detail);
    } catch (Exception e) {
        log.error("[AI监控] 查询失败: traceId={}", traceId, e);
        return Result.error("查询失败: " + e.getMessage());
    }
}

/**
 * 获取分析记录列表（分页）
 *
 * @param startTime 开始时间（可选，默认最近24小时）
 * @param endTime   结束时间（可选，默认当前时间）
 * @param page      页码（从 0 开始，默认 0）
 * @param size      每页大小（默认 20）
 * @return 分页结果
 */
@GetMapping("/analysis-traces")
public Result<Page<AnalysisTraceSummary>> listAnalysisTraces(
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,

        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,

        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {

    log.info("[AI监控] 查询分析记录列表: startTime={}, endTime={}, page={}, size={}",
            startTime, endTime, page, size);

    // 默认最近 24 小时
    if (startTime == null) {
        startTime = LocalDateTime.now().minusHours(24);
    }
    if (endTime == null) {
        endTime = LocalDateTime.now();
    }

    try {
        Page<AnalysisTraceSummary> result = logService.listAnalysisTraces(
                startTime, endTime, page, size);

        log.info("[AI监控] 查询成功: total={}, page={}", result.getTotalElements(), page);

        return Result.success(result);
    } catch (Exception e) {
        log.error("[AI监控] 查询失败", e);
        return Result.error("查询失败: " + e.getMessage());
    }
}
```

---

## 三、前端页面设计

### 3.1 全局监控大盘（调整版）

**文件**：`frontend/src/views/AiMonitor/index.vue`

**主要调整**：
- 修复 Token 显示（从后端获取正确的 Token 数据）
- 添加"查看分析详情"入口

```vue
<template>
  <div class="ai-monitor-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2>AI 监控中心</h2>
      <p>实时监控 AI 调用情况、Token 消耗和性能指标</p>
    </div>

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
            <div class="card-header">
              <span class="card-title">Agent Token 分布</span>
              <el-tag size="small" type="info">Token 消耗占比</el-tag>
            </div>
          </template>
          <v-chart
            :option="agentPieOption"
            style="height: 300px"
            autoresize
          />
        </el-card>
      </el-col>

      <!-- Agent 调用次数分布（饼图） -->
      <el-col :span="12">
        <el-card class="chart-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span class="card-title">Agent 调用次数分布</span>
              <el-tag size="small" type="info">工作量占比</el-tag>
            </div>
          </template>
          <v-chart
            :option="agentCallPieOption"
            style="height: 300px"
            autoresize
          />
        </el-card>
      </el-col>
    </el-row>

    <!-- 耗时趋势图（折线图） -->
    <el-row :gutter="20" class="charts-row">
      <el-col :span="24">
        <el-card class="chart-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span class="card-title">24 小时调用趋势</span>
              <div class="header-actions">
                <el-button size="small" @click="refreshData" :loading="loading">
                  <el-icon><Refresh /></el-icon>
                  刷新
                </el-button>
              </div>
            </div>
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
        <el-button type="primary" @click="goToAnalysisTraces">
          <el-icon><View /></el-icon>
          查看分析详情  <!-- 🆕 新增入口 -->
        </el-button>
        <el-button type="success" @click="refreshData">
          <el-icon><Refresh /></el-icon>
          刷新数据
        </el-button>
        <el-button type="info" @click="exportData">
          <el-icon><Download /></el-icon>
          导出数据
        </el-button>
      </el-space>
    </el-card>
  </div>
</template>

<script setup lang="ts">
// ...（保持原有代码）

/**
 * 跳转到分析详情列表 - 🆕
 */
function goToAnalysisTraces() {
  router.push('/ai-monitor/analysis-traces')
}

// ...（其他方法保持不变）
</script>
```

---

### 3.2 分析详情列表页（新增）

**文件**：`frontend/src/views/AiMonitor/AnalysisTraceList.vue`

这个页面将展示所有 SQL 指纹的分析记录，点击可查看详细调用链。

---

### 3.3 路由配置（新增）

**文件**：`frontend/src/router/index.ts`（新增路由）

```typescript
{
  path: 'analysis-traces',  // 🆕 新增路由
  name: 'AnalysisTraces',
  component: () => import('@/views/AiMonitor/AnalysisTraceList.vue'),
  meta: { title: '分析详情' }
}
```

---

## 四、实施步骤

### Phase 1：修复 Token 统计（优先级：🔴 高）

**任务清单**：

| 步骤 | 任务 | 文件路径 | 预估时间 | 风险 |
|------|------|---------|---------|------|
| 1.1 | 扩展 `AiContextHolder` | `src/main/java/com/dbdoctor/monitoring/AiContextHolder.java` | 30min | 低 |
| 1.2 | 创建 `TokenEstimator` | `src/main/java/com/dbdoctor/monitoring/TokenEstimator.java` | 1h | 低 |
| 1.3 | 修改 `AiMonitoringListener` | `src/main/java/com/dbdoctor/monitoring/AiMonitoringListener.java` | 1h | 中 |
| 1.4 | 修改 `MultiAgentCoordinator` | `src/main/java/com/dbdoctor/service/MultiAgentCoordinator.java` | 30min | 低 |
| 1.5 | 单元测试 | `src/test/java/com/dbdoctor/monitoring/TokenEstimatorTest.java` | 30min | 低 |
| 1.6 | 集成测试 | `src/test/java/com/dbdoctor/monitoring/AiMonitoringListenerTest.java` | 1h | 中 |
| 1.7 | 验证修复 | 手动测试，查看监控大盘 | 30min | 低 |

**验收标准**：
- ✅ 监控大盘显示 Token 消耗 > 0
- ✅ Token 估算误差 < 30%（相对实际值）
- ✅ 不影响 AI 调用性能

---

### Phase 2：新增后端 API（优先级：🟡 中）

**任务清单**：

| 步骤 | 任务 | 文件路径 | 预估时间 | 风险 |
|------|------|---------|---------|------|
| 2.1 | 创建 `AnalysisTraceDetail` DTO | `src/main/java/com/dbdoctor/model/AnalysisTraceDetail.java` | 30min | 低 |
| 2.2 | 创建 `AnalysisTraceSummary` DTO | `src/main/java/com/dbdoctor/model/AnalysisTraceSummary.java` | 15min | 低 |
| 2.3 | 新增 Repository 方法 | `src/main/java/com/dbdoctor/repository/AiInvocationLogRepository.java` | 30min | 低 |
| 2.4 | 新增 Service 方法 | `src/main/java/com/dbdoctor/service/AiInvocationLogService.java` | 1h | 中 |
| 2.5 | 新增 Controller 接口 | `src/main/java/com/dbdoctor/controller/AiMonitorController.java` | 30min | 低 |
| 2.6 | API 测试 | Postman/Apifox | 30min | 低 |

**验收标准**：
- ✅ `/api/ai-monitor/analysis-trace/{traceId}` 返回完整分析详情
- ✅ `/api/ai-monitor/analysis-traces` 分页查询正常
- ✅ 聚合统计准确（总耗时、总 Token）

---

### Phase 3：前端页面开发（优先级：🟡 中）

**任务清单**：

| 步骤 | 任务 | 文件路径 | 预估时间 | 风险 |
|------|------|---------|---------|------|
| 3.1 | 创建类型定义 | `frontend/src/views/AiMonitor/types.ts` | 15min | 低 |
| 3.2 | 新增 API 封装 | `frontend/src/api/ai-monitor.ts` | 30min | 低 |
| 3.3 | 创建分析详情列表页 | `frontend/src/views/AiMonitor/AnalysisTraceList.vue` | 2h | 中 |
| 3.4 | 调整监控大盘 | `frontend/src/views/AiMonitor/index.vue` | 30min | 低 |
| 3.5 | 新增路由配置 | `frontend/src/router/index.ts` | 15min | 低 |
| 3.6 | 前端测试 | 手动测试 | 30min | 低 |

**验收标准**：
- ✅ 分析详情列表正常显示
- ✅ 点击"查看详情"弹窗正常
- ✅ 调用链追踪时间线正确显示
- ✅ Token 统计显示正确

---

### Phase 4：测试和优化（优先级：🟢 低）

**任务清单**：

| 步骤 | 任务 | 预估时间 | 风险 |
|------|------|---------|------|
| 4.1 | 压力测试 | 2h | 中 |
| 4.2 | 性能优化 | 1h | 中 |
| 4.3 | 文档更新 | 1h | 低 |
| 4.4 | 代码审查 | 1h | 低 |

**验收标准**：
- ✅ 监控系统对 AI 调用性能影响 < 5%
- ✅ 所有测试用例通过
- ✅ 文档完整

---

## 五、技术方案对比

### 5.1 Token 统计方案对比

| 方案 | 优点 | 缺点 | 准确度 | 实施难度 | 推荐度 |
|------|------|------|--------|---------|--------|
| **方案 A：升级 LangChain4j** | 官方 API，准确可靠 | 可能引入不兼容变更 | ⭐⭐⭐⭐⭐ | 🔴 高 | ⭐⭐⭐⭐ |
| **方案 B：使用反射** | 不需要升级 | 不稳定，可能失效 | ⭐⭐⭐ | 🟡 中 | ⭐⭐ |
| **方案 C：手动估算** | 实现简单 | 不准确 | ⭐⭐ | 🟢 低 | ⭐ |
| **方案 D：扩展 Context（推荐）** | 侵入性小，兼容性好 | 依赖 Prompt 质量 | ⭐⭐⭐⭐ | 🟢 低 | ⭐⭐⭐⭐⭐ |

**推荐方案**：**方案 D（扩展 Context）**

**理由**：
1. ✅ 实施简单，不需要升级依赖
2. ✅ 兼容性好，不影响现有功能
3. ✅ 准确度可接受（误差约 20-30%）
4. ✅ 为未来升级留有空间

---

### 5.2 分析维度监控方案对比

| 方案 | 优点 | 缺点 | 实施难度 | 推荐度 |
|------|------|------|---------|--------|
| **方案 A：新增 analysis_trace 表** | 数据隔离，查询性能好 | 需要维护两张表 | 🔴 高 | ⭐⭐⭐ |
| **方案 B：使用现有 ai_invocation_log 表（推荐）** | 无需修改表结构，代码简单 | 聚合查询性能略低 | 🟢 低 | ⭐⭐⭐⭐⭐ |

**推荐方案**：**方案 B（使用现有表）**

**理由**：
1. ✅ 无需修改数据库表结构
2. ✅ 实施简单，只需新增聚合查询
3. ✅ 数据一致性好（单数据源）
4. ✅ 查询性能可接受（可添加索引优化）

---

## 六、成功标准

### 6.1 功能验收标准

| 标准 | 验收方法 |
|------|---------|
| **Token 统计不为 0** | 监控大盘显示 Token 消耗 > 0 |
| **Token 估算准确度** | 误差 < 30%（相对实际值） |
| **分析详情页可用** | 能查看单次分析的完整调用链 |
| **聚合统计准确** | 总耗时、总 Token 计算正确 |
| **性能影响可控** | 监控系统对 AI 调用性能影响 < 5% |

---

### 6.2 代码质量标准

| 标准 | 要求 |
|------|------|
| **测试覆盖率** | ≥ 80% |
| **代码规范** | 遵循阿里巴巴 Java 开发规范 |
| **无硬编码** | 所有参数从配置文件读取 |
| **日志规范** | 使用 Slf4j，不使用 System.out.println |

---

## 七、附录

### 7.1 相关文档

| 文档名称 | 路径 |
|---------|------|
| AI 监控功能需求设计文档 | `docs/AI监控功能需求设计文档.md` |
| AI 调用熔断器设计方案 | `docs/backend/AI 调用熔断器设计方案.md` |

---

### 7.2 版本历史

| 版本 | 日期 | 变更说明 | 作者 |
|------|------|---------|------|
| v1.0.0 | 2026-01-31 | 初始版本 | DB-Doctor Team |

---

### 7.3 术语表

| 术语 | 说明 |
|------|------|
| **traceId** | SQL 指纹，用于关联慢查询和 AI 调用 |
| **Token** | AI 模型的计费单位，约 3-4 个英文单词或 1-2 个中文字符 |
| **分析链路** | 一个 SQL 从开始分析到结束的完整过程（DIAGNOSIS → REASONING → CODING） |
| **Token 估算** | 在无法获取真实 Token 数时，基于文本长度估算的 Token 数 |

---

**文档结束**
