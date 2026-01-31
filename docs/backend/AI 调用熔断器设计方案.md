# AI 调用熔断器设计方案

## 📋 文档信息

- **文档名称**: AI 调用熔断器设计方案
- **创建日期**: 2026-01-27
- **版本**: v1.0
- **状态**: 待实施
- **优先级**: 🔵 低（未来优化）

---

## 🎯 问题背景

### 当前问题

DB-Doctor 在调用 AI API（OpenAI/GPT）时存在以下风险：

| 风险类型 | 描述 | 影响 |
|---------|------|------|
| **API 超时** | AI API 响应时间过长（>30秒） | 线程池阻塞，任务堆积 |
| **API 宕机** | AI 服务不可用（大规模故障） | 所有分析任务失败，应用崩溃 |
| **API 限流** | 触发速率限制（429 Too Many Requests） | 大量失败，浪费资源 |
| **网络抖动** | 网络不稳定导致间歇性失败 | 用户体验差，难以排查 |

### 实际故障场景

#### 场景 1: AI API 超时

```
时间线：
10:00:00 - 捕获到 100 条慢查询
10:00:01 - 提交到线程池（4 个线程）
10:00:02 - 线程 1 调用 AI API...
10:00:05 - 线程 2 调用 AI API...
10:00:32 - ⚠️ 线程 1 超时（30秒），失败
10:00:35 - ⚠️ 线程 2 超时，失败
...

结果：
- 4 个线程被占用 40 秒
- 剩余 96 个任务在队列中等待
- 新的慢查询无法及时处理
- 用户体验极差
```

#### 场景 2: AI API 宕机

```
时间线：
09:00:00 - OpenAI API 宕机
09:00:01 - DB-Doctor 开始调用 AI API
09:00:05 - 第一次请求超时
09:00:10 - 所有请求超时
09:00:15 - 线程池占满，任务堆积
09:01:00 - 队列中已有 1000+ 个任务
09:05:00 - 内存溢出（OOM），应用崩溃

影响：
- ❌ 所有慢查询都分析失败
- ❌ 数据库存储大量 ERROR 记录
- ❌ 应用崩溃，需要重启
- ❌ 重启后继续堆积，死循环
```

---

## 💡 解决方案：熔断器模式

### 什么是熔断器？

熔断器（Circuit Breaker）是一种**保护机制**，类似于家里的电闸：

```
正常情况：
电流（请求）→ 电闸（熔断器）→ 电器（AI 服务）
✅ 正常工作

过载情况：
电流过大 → 电闸自动跳闸 → 保护电路
⚡ 切断电源，防止烧坏电路
```

### 熔断器状态

```
┌─────────────────────────────────────────────────────────┐
│              熔断器状态转换                              │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  CLOSED（关闭）→ 正常工作                               │
│  ├─ 失败率 < 50%（可配置）                             │
│  ├- 所有请求正常调用 AI                                 │
│  └─ 滑动窗口统计失败率                                  │
│          ↓ 失败率 >= �50%                                 │
│  OPEN（打开）→ 熔断保护                                 │
│  ├─ 快速失败，不调用 AI                                 │
│  ├─ 直接调用降级方法（基础分析）                        │
│  └─ 等待 60 秒后尝试恢复                               │
│          ↓ 60 秒后                                       │
│  HALF_OPEN（半开）→ 尝试恢复                            │
│  ├─ 允许 1 个请求通过                                   │
│  ├─ 如果成功 → CLOSED                                  │
│  └─ 如果失败 → OPEN                                    │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

### 熔断器的保护效果

| 场景 | 无熔断器 | 有熔断器 |
|------|---------|---------|
| **AI 超时 30 秒** | 线程阻塞 30 秒 | 快速失败，使用降级（< 1 秒） |
| **AI 宕机 1 小时** | 应用崩溃，0 成功率 | 降级运行，基础分析可用 |
| **AI 限流** | 大量 429 错误 | 熔断后跳过 AI，减少调用 |

---

## 🏗️ 技术实现方案

### 方案选择：Resilience4j

#### 为什么选择 Resilience4j？

| 特性 | Resilience4j | Hystrix（已停止维护） |
|------|-------------|----------------------|
| **维护状态** | ✅ 活跃维护 | ❌ 已停止 |
| **Spring Boot 3** | ✅ 原生支持 | ❌ 不兼容 |
| **性能** | ✅ 轻量级 | ⚠️ 较重 |
| **功能** | ✅ 丰富 | ✅ 丰富 |
| **文档** | ✅ 完善 | ⚠️ 一般 |

### 依赖配置

```xml
<!-- pom.xml -->
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
    <version>2.1.0</version>
</dependency>

<!-- 如果需要 AOP 支持（推荐） -->
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-circuitbreaker</artifactId>
    <version>2.1.0</version>
</dependency>

<!-- Spring Boot AOP -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

### 配置文件

```yaml
# application.yml
resilience4j:
  circuitbreaker:
    instances:
      aiAnalysis:
        # 熔断器配置
        registerHealthIndicator: true  # 注册健康检查
        slidingWindowSize: 10           # 滑动窗口大小：10 次调用
        minimumNumberOfCalls: 5         # 最小调用次数：5 次后才开始统计失败率
        permittedNumberOfCallsInHalfOpenState: 3  # 半开状态允许的调用数
        automaticTransitionFromOpenToHalfOpenEnabled: true  # 自动从 OPEN 转到 HALF_OPEN
        waitDurationInOpenState: 60s    # OPEN 状态等待 60 秒后转到 HALF_OPEN
        failureRateThreshold: 50        # 失败率阈值：50%

        # 异常配置
        recordExceptions:
          - java.util.concurrent.TimeoutException
          - org.springframework.web.client.HttpServerErrorException
          - org.springframework.web.client.HttpClientErrorException
        ignoreExceptions:
          - java.lang.IllegalArgumentException  # 忽略参数异常（业务错误）
```

### 代码实现

#### 1. 创建 AI 分析服务

```java
package com.dbdoctor.service;

import com.dbdoctor.agent.DBAgent;
import com.dbdoctor.model.SlowQueryLog;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

/**
 * AI 分析服务（带熔断器）
 *
 * @author DB-Doctor
 * @version 2.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiAnalysisService {

    private final DBAgent dbAgent;

    /**
     * 使用 AI 分析慢查询
     *
     * 熔断器：aiAnalysis
     * - 失败率 >= 50% 时熔断
     * - 熔断后调用降级方法（基础分析）
     *
     * @param log 慢查询日志
     * @return AI 分析报告
     */
    @CircuitBreaker(
        name = "aiAnalysis",
        fallbackMethod = "fallbackAnalysis"  // 降级方法
    )
    @TimeLimiter(
        name = "aiAnalysis",
        fallbackMethod = "fallbackAnalysis",
        duration = 30  // 超时时间：30 秒
    )
    public String analyzeWithAI(SlowQueryLog log) {
        log.info("调用 AI 分析: db={}, queryTime={}s",
            log.getDbName(), log.getQueryTime());

        try {
            // 调用 AI API
            String report = dbAgent.analyzeSlowLog(
                log.getDbName(),
                log.getLogTime(),
                log.getQueryTime(),
                log.getLockTime(),
                log.getRowsExamined(),
                log.getRowsSent(),
                log.getSqlText()
            );

            log.info("✅ AI 分析成功");
            return report;

        } catch (Exception e) {
            log.error("❌ AI 分析失败", e);
            throw e;  // 抛出异常，触发熔断器
        }
    }

    /**
     * 降级方法：基础分析（不依赖 AI）
     *
     * @param log 慢查询日志
     * @param e 异常
     * @return 基础分析报告
     */
    private String fallbackAnalysis(SlowQueryLog log, Exception e) {
        log.warn("⚠️ AI 分析失败，使用基础分析: {}", e.getMessage());

        // 判断异常类型
        if (e instanceof TimeoutException) {
            log.warn("AI 调用超时（>30秒），使用基础分析");
        } else if (e.getCause() instanceof org.springframework.web.client.HttpServerErrorException) {
            log.warn("AI 服务异常（5xx），使用基础分析");
        } else {
            log.warn("AI 调用失败，使用基础分析");
        }

        // 生成基础分析报告
        return generateBasicAnalysis(log);
    }

    /**
     * 基础分析（基于 SQL 规则）
     *
     * @param log 慢查询日志
     * @return 基础分析报告
     */
    private String generateBasicAnalysis(SlowQueryLog log) {
        StringBuilder report = new StringBuilder();

        report.append("# 慢查询基础分析（自动生成）\n\n");
        report.append("**说明**：AI 服务暂时不可用，以下是基于规则的基础分析。\n\n");

        // 1. 基础信息
        report.append("## 📊 统计信息\n\n");
        report.append(String.format("- **执行时间**: %.3f 秒\n", log.getQueryTime()));
        report.append(String.format("- **锁等待时间**: %.3f 秒\n", log.getLockTime()));
        report.append(String.format("- **扫描行数**: %d 行\n", log.getRowsExamined()));
        report.append(String.format("- **返回行数**: %d 行\n", log.getRowsSent()));

        // 2. 问题诊断（基于规则）
        report.append("\n## 🔍 问题诊断\n\n");

        // 规则 1：扫描行数过多
        if (log.getRowsExamined() != null && log.getRowsExamined() > 100000) {
            report.append("### ⚠️ 全表扫描风险\n\n");
            report.append(String.format("扫描行数过多（%d 行），可能存在全表扫描。\n", log.getRowsExamined()));
            report.append("**建议**：检查是否缺少索引，考虑添加合适的索引。\n\n");
        }

        // 规则 2：锁等待时间过长
        if (log.getLockTime() != null && log.getLockTime() > 1.0) {
            report.append("### ⚠️ 锁等待问题\n\n");
            report.append(String.format("锁等待时间过长（%.3f 秒），可能存在锁竞争。\n", log.getLockTime()));
            report.append("**建议**：检查是否有长时间事务，优化事务隔离级别。\n\n");
        }

        // 规则 3：返回行数过多
        if (log.getRowsSent() != null && log.getRowsSent() > 10000) {
            report.append("### ⚠️ 大结果集问题\n\n");
            report.append(String.format("返回行数过多（%d 行），可能导致网络传输和内存压力。\n", log.getRowsSent()));
            report.append("**建议**：考虑使用分页查询，限制返回行数。\n\n");
        }

        // 3. SQL 语句
        report.append("## 📝 SQL 语句\n\n```sql\n");
        report.append(log.getSqlText());
        report.append("\n```\n");

        // 4. 优化建议
        report.append("## 💡 优化建议\n\n");
        report.append("1. 使用 EXPLAIN 分析执行计划\n");
        report.append("2. 检查是否使用了合适的索引\n");
        report.append("3. 考虑使用分页查询减少返回行数\n");
        report.append("4. 优化 WHERE 条件，避免全表扫描\n");

        report.append("\n---\n");
        report.append("*本报告由 DB-Doctor 自动生成（基础分析模式）*\n");

        return report.toString();
    }
}
```

#### 2. 修改 AnalysisService 使用熔断器

```java
// AnalysisService.java

@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final AiAnalysisService aiAnalysisService;  // 注入 AI 分析服务

    /**
     * 生成分析报告
     */
    public void generateAnalysisReport(String fingerprint) {
        SlowQueryHistory history = historyRepo.findBySqlFingerprint(fingerprint);

        try {
            // ✅ 使用带熔断器的 AI 分析
            SlowQueryLog log = convertToSlowQueryLog(history);
            String report = aiAnalysisService.analyzeWithAI(log);

            history.setAiAnalysisReport(report);
            history.setStatus(SlowQueryHistory.AnalysisStatus.SUCCESS);
            historyRepo.save(history);

        } catch (Exception e) {
            log.error("分析失败: fingerprint={}", fingerprint, e);
            history.setStatus(SlowQueryHistory.AnalysisStatus.ERROR);
            historyRepo.save(history);
        }
    }

    private SlowQueryLog convertToSlowQueryLog(SlowQueryHistory history) {
        return SlowQueryLog.builder()
            .dbName(history.getDbName())
            .logTime(history.getLastSeenTime())
            .queryTime(history.getAvgQueryTime())
            .lockTime(history.getAvgLockTime())
            .rowsExamined(history.getMaxRowsExamined())
            .rowsSent(history.getMaxRowsSent())
            .sqlText(history.getExampleSql())
            .build();
    }
}
```

---

## 📊 配置参数说明

### 熔断器核心参数

| 参数 | 默认值 | 说明 | 推荐值 |
|------|--------|------|--------|
| **slidingWindowSize** | 10 | 滑动窗口大小（调用次数） | 10-50 |
| **minimumNumberOfCalls** | 5 | 最小调用次数（开始统计） | 5-10 |
| **failureRateThreshold** | 50 | 失败率阈值（%） | 50-70 |
| **waitDurationInOpenState** | 60s | 熔断等待时间 | 30s-120s |
| **permittedNumberOfCallsInHalfOpenState** | 3 | 半开状态允许调用数 | 3-10 |

### 超时配置

```yaml
resilience4j:
  timelimiter:
    instances:
      aiAnalysis:
        timeout-duration: 30s  # AI 调用超时时间
        cancelRunningFuture: true  # 超时后取消正在执行的任务
```

---

## 🧪 测试方案

### 单元测试

```java
@SpringBootTest
class AiAnalysisServiceTest {

    @Autowired
    private AiAnalysisService aiAnalysisService;

    @Test
    @DisplayName("正常情况：AI 分析成功")
    void testAnalyzeWithAI_Success() {
        SlowQueryLog log = createMockLog();

        String report = aiAnalysisService.analyzeWithAI(log);

        assertNotNull(report);
        assertTrue(report.contains("AI 分析"));
    }

    @Test
    @DisplayName("降级情况：AI 超时，使用基础分析")
    void testAnalyzeWithAI_Timeout() {
        // 模拟超时
        SlowQueryLog log = createMockLog();
        when(dbAgent.analyzeSlowLog(any()))
            .thenAnswer(invocation -> {
                Thread.sleep(35000);  // 超过 30 秒
                return "AI 报告";
            });

        String report = aiAnalysisService.analyzeWithAI(log);

        assertNotNull(report);
        assertTrue(report.contains("基础分析"));
    }

    @Test
    @DisplayName("熔断器：连续失败后熔断")
    void testCircuitBreakerOpens() {
        // 模拟连续失败
        for (int i = 0; i < 10; i++) {
            try {
                aiAnalysisService.analyzeWithAI(createMockLog());
            } catch (Exception e) {
                // 预期失败
            }
        }

        // 第 11 次应该直接返回降级结果（不调用 AI）
        String report = aiAnalysisService.analyzeWithAI(createMockLog());
        assertTrue(report.contains("基础分析"));
    }
}
```

### 集成测试

```java
@SpringBootTest
@TestPropertySource(properties = {
    "resilience4j.circuitbreaker.instances.aiAnalysis.failureRateThreshold=50",
    "resilience4j.circuitbreaker.instances.aiAnalysis.slidingWindowSize=5"
})
class CircuitBreakerIntegrationTest {

    @Test
    @DisplayName("集成测试：熔断器完整流程")
    void testCircuitBreakerFullCycle() {
        // 1. 初始状态：CLOSED
        // 2. 连续失败 3/5 次（失败率 60%）
        // 3. 状态变为：OPEN
        // 4. 等待 60 秒
        // 5. 状态变为：HALF_OPEN
        // 6. 成功调用
        // 7. 状态恢复：CLOSED
    }
}
```

---

## 📈 监控和观察

### 健康检查

```java
@RestController
@RequestMapping("/actuator")
public class HealthController {

    @GetMapping("/health/circuitbreaker")
    public Map<String, Object> getCircuitBreakerStatus() {
        CircuitBreaker circuitBreaker = CircuitBreaker.of("aiAnalysis");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();

        Map<String, Object> status = new HashMap<>();
        status.put("state", circuitBreaker.getState());  // CLOSED/OPEN/HALF_OPEN
        status.put("failureRate", metrics.getFailureRate());
        status.put("numberOfBufferedCalls", metrics.getNumberOfBufferedCalls());
        status.put("numberOfFailedCalls", metrics.getNumberOfFailedCalls());

        return status;
    }
}
```

### 监控指标

```java
@Component
@Slf4j
public class CircuitBreakerMetrics {

    @EventListener
    public void onCircuitBreakerEvent(CircuitBreakerEvent event) {
        log.info("熔断器事件: type={}, state={}",
            event.getEventType(),
            event.getCircuitBreakerState());

        // 发送到监控系统（Prometheus/Grafana）
        // meterRegistry.counter("dbdoctor.circuitbreaker.events",
        //     "type", event.getEventType().name()).increment();
    }
}
```

---

## 🚀 实施步骤

### 阶段 1：准备工作

1. ✅ **添加依赖**
   ```bash
   # 编辑 pom.xml
   # 添加 resilience4j-spring-boot3 依赖
   ```

2. ✅ **添加配置**
   ```yaml
   # application.yml
   # 添加 resilience4j 配置
   ```

3. ✅ **编写测试**
   ```java
   # 创建单元测试
   # 验证熔断器逻辑
   ```

### 阶段 2：代码实现

1. ✅ **创建 AiAnalysisService**
   - 实现带熔断器的分析方法
   - 实现降级方法（基础分析）

2. ✅ **修改 AnalysisService**
   - 使用 AiAnalysisService 而不是直接调用 AI

3. ✅ **添加监控**
   - 健康检查端点
   - 熔断器事件监听

### 阶段 3：测试验证

1. ✅ **单元测试**
   ```bash
   mvn test -Dtest=AiAnalysisServiceTest
   ```

2. ✅ **集成测试**
   ```bash
   mvn test -Dtest=CircuitBreakerIntegrationTest
   ```

3. ✅ **手动测试**
   - 正常情况：AI 调用成功
   - 超时情况：使用降级
   - 熔断情况：快速失败

### 阶段 4：上线部署

1. ✅ **灰度发布**
   - 先在测试环境验证
   - 再在生产环境小范围测试

2. ✅ **监控观察**
   - 观察熔断器状态变化
   - 调整参数（如果需要）

3. ✅ **全量发布**
   - 确认稳定后全量上线

---

## 📝 配置调优建议

### 开发环境

```yaml
resilience4j:
  circuitbreaker:
    instances:
      aiAnalysis:
        slidingWindowSize: 5           # 小窗口，快速测试
        failureRateThreshold: 50       # 50% 失败率即熔断
        waitDurationInOpenState: 10s    # 短等待时间，方便测试
```

### 生产环境

```yaml
resilience4j:
  circuitbreaker:
    instances:
      aiAnalysis:
        slidingWindowSize: 20          # 大窗口，更稳定
        minimumNumberOfCalls: 10        # 最小 10 次调用
        failureRateThreshold: 60       # 60% 失败率才熔断（更宽容）
        waitDurationInOpenState: 60s    # 60 秒等待时间
        permittedNumberOfCallsInHalfOpenState: 5  # 半开状态允 5 次调用
```

---

## ⚠️ 注意事项

### 1. 降级方法不能抛异常

```java
// ❌ 错误：降级方法抛出异常
private String fallbackAnalysis(SlowQueryLog log, Exception e) {
    throw new RuntimeException("降级失败");  // ❌
}

// ✅ 正确：降级方法必须返回值
private String fallbackAnalysis(SlowQueryLog log, Exception e) {
    return generateBasicAnalysis(log);  // ✅
}
```

### 2. 降级方法签名要一致

```java
// ✅ 正确：参数和返回类型要一致
public String analyzeWithAI(SlowQueryLog log) { ... }

private String fallbackAnalysis(SlowQueryLog log, Exception e) { ... }
```

### 3. 异常配置要合理

```yaml
# ✅ 记录需要触发熔断的异常
recordExceptions:
  - java.util.concurrent.TimeoutException
  - org.springframework.web.client.HttpServerErrorException

# ✅ 忽略业务异常（不触发熔断）
ignoreExceptions:
  - java.lang.IllegalArgumentException  # 参数错误
  - com.dbdoctor.exception.BusinessException  # 业务异常
```

---

## 📚 参考资料

### 官方文档

- [Resilience4j 官方文档](https://resilience4j.readme.io/)
- [Spring Boot 3 集成指南](https://resilience4j.readme.io/docs/getting-started)
- [熔断器模式详解](https://resilience4j.readme.io/docs/circuitbreaker)

### 相关文章

- [微服务容错机制：熔断器模式](https://martinfowler.com/bliki/CircuitBreaker.html)
- [Spring Boot 3 使用 Resilience4j](https://reflectoring.io/2023/03/01/resilience4j-spring-boot-3/)

---

## 🎯 总结

### 核心价值

1. ✅ **提高可用性**：AI 故障时降级到基础分析，服务不中断
2. ✅ **防止雪崩**：快速失败，避免线程池耗尽
3. ✅ **自动恢复**：熔断后自动尝试恢复，无需人工干预
4. ✅ **监控友好**：提供熔断器状态监控，便于运维

### 实施建议

- **优先级**：🔵 低（当前数据量不大，可暂缓）
- **实施时机**：当 AI 调用失败率 > 5% 或准备对外提供服务时
- **实施周期**：2-3 天（包括测试）
- **维护成本**：低（配置化，调整参数即可）

---

**文档结束**

*如有疑问，请联系技术团队*
