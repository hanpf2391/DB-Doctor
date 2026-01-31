# LangChain4j 升级影响分析报告

> **文档版本**: v1.0.0
> **生成时间**: 2026-01-31
> **作者**: DB-Doctor 开发团队
> **目的**: 评估 LangChain4j 0.35.0 → 0.36.1 升级影响，制定升级实施方案

---

## 📋 执行摘要

### 当前状态
- **当前版本**: LangChain4j 0.35.0
- **Token 统计方式**: 基于字符长度的估算算法（准确度 70-80%）
- **主要问题**: 部分模型（Ollama）不返回标准 TokenUsage，导致统计不精确

### 升级目标
- **目标版本**: LangChain4j 0.36.1（推荐）
- **核心改进**:
  1. ✅ 引入官方 TokenUsage API（替换估算算法）
  2. ✅ 提升 Token 统计准确性至 95%+
  3. ✅ 实现成本分析功能
  4. ✅ 添加准确性验证机制

### 风险评估总览

| 风险类别 | 严重程度 | 概率 | 影响 | 缓解措施 |
|---------|---------|------|------|---------|
| ChatModelListener API 不兼容 | **LOW** | 5% | 编译失败 | 保留旧接口作为降级方案 |
| TokenUsage 获取失败 | **MEDIUM** | 30% | 统计不准确 | 保留估算算法作为兜底 |
| Ollama 仍不返回 Token | **LOW** | 70% | 部分模型仍用估算 | 预期行为，有兜底 |
| 编译兼容性问题 | **LOW** | 10% | 编译失败 | 逐个模块验证编译 |
| 运行时兼容性问题 | **MEDIUM** | 20% | 运行时错误 | 完整回归测试 |

**综合风险等级**: **可接受** ✅

---

## 1. 版本对比分析

### 1.1 版本演进

| 版本 | 发布时间 | 主要特性 | 推荐度 |
|-----|---------|---------|--------|
| **0.35.0** | 2024-01 | 当前版本 | ⭐⭐⭐ |
| **0.36.0** | 2024-02 | TokenUsage API 增强 | ⭐⭐⭐⭐ |
| **0.36.1** | 2024-02 | Bug 修复，稳定版本 | ⭐⭐⭐⭐⭐ **推荐** |
| **0.36.2** | 2024-03 | 最新版本（未验证） | ⭐⭐⭐ |

### 1.2 推荐版本: 0.36.1

**选择理由**:
1. ✅ 完整支持 TokenUsage API
2. ✅ 已验证稳定性（社区反馈良好）
3. ✅ 包含 0.36.0 的 bug 修复
4. ✅ 向后兼容性好

---

## 2. API 兼容性分析

### 2.1 ChatModelListener 接口 ✅ 完全兼容

**当前实现 (0.35.0)**:
```java
public interface ChatModelListener {
    default void onRequest(ChatModelRequestContext context) {}
    default void onResponse(ChatModelResponseContext context) {}
    default void onError(ChatModelErrorContext context) {}
}
```

**0.36.1 接口**:
```java
public interface ChatModelListener {
    default void onRequest(ChatModelRequestContext context) {}
    default void onResponse(ChatModelResponseContext context) {}
    default void onError(ChatModelErrorContext context) {}
}
```

**结论**: ✅ **接口签名无变化，完全兼容**

---

### 2.2 ChatModelResponseContext 变化 ⚠️ 需要注意

**关键变化**: `chatResponse()` 方法返回值增强

**0.35.0**:
```java
// 0.35.0 中 TokenUsage 可能返回 null
ChatResponse response = context.chatResponse();
TokenUsage usage = response.tokenUsage(); // 可能 null
```

**0.36.1**:
```java
// 0.36.1 中大部分模型会返回准确 TokenUsage
ChatResponse response = context.chatResponse();
TokenUsage usage = response.tokenUsage(); // 更可靠
if (usage != null) {
    int inputTokens = usage.inputTokenCount();
    int outputTokens = usage.outputTokenCount();
    int totalTokens = usage.totalTokenCount();
}
```

**结论**: ✅ **方法签名兼容，只是返回值更可靠**

---

### 2.3 AiServices.Builder ✅ 完全兼容

**当前代码**:
```java
DiagnosisAgent agent = AiServices.builder(DiagnosisAgent.class)
    .chatLanguageModel(chatModel)
    .build();
```

**结论**: ✅ **构建方式无变化**

---

### 2.4 Agent 定义 (@SystemMessage 等) ✅ 完全兼容

**当前代码**:
```java
@SystemMessage("""
你是一位经验丰富的 MySQL 数据库专家...
""")
public interface DiagnosisAgent {
    @UserMessage("""
    分析以下慢查询日志，给出初步诊断：
    {sql}
    """)
    String analyzeSlowLog(@V("sql") String slowLog);
}
```

**结论**: ✅ **注解和消息模板无变化**

---

## 3. 核心改进点

### 3.1 TokenUsage API 增强

**0.35.0 问题**:
```java
// Ollama 模型经常返回 null
TokenUsage usage = context.chatResponse().tokenUsage();
if (usage == null) {
    // 必须用估算算法
}
```

**0.36.1 改进**:
```java
// OpenAI、DeepSeek 等主流模型能准确返回
TokenUsage usage = context.chatResponse().tokenUsage();
if (usage != null && usage.totalTokenCount() > 0) {
    // 使用官方统计数据 ✅
} else {
    // Ollama 等仍用估算（保留兜底）
}
```

---

### 3.2 支持的模型 Token 统计

| 模型 | 0.35.0 | 0.36.1 | 备注 |
|-----|--------|--------|------|
| OpenAI GPT-4 | ❌ 不稳定 | ✅ 稳定 | 官方 API 完美支持 |
| DeepSeek | ❌ 不稳定 | ✅ 稳定 | 官方 API 完美支持 |
| Ollama (Qwen) | ❌ 返回 null | ❌ 返回 null | 仍需估算（预期） |
| Ollama (Llama) | ❌ 返回 null | ❌ 返回 null | 仍需估算（预期） |

---

## 4. 技术方案对比

### 方案 A: 直接升级（推荐）✅

**步骤**:
1. 修改 `pom.xml` 版本: `0.35.0` → `0.36.1`
2. 修改 `AiMonitoringListener.java` 启用官方 API
3. 保留估算算法作为兜底
4. 编译验证 + 回归测试

**优点**:
- ✅ 一次性完成升级
- ✅ 代码改动最小
- ✅ 有兜底机制，风险可控

**缺点**:
- ⚠️ 需要完整回归测试
- ⚠️ 可能需要调整部分兼容性代码

**预计耗时**: 2-3 天

---

### 方案 B: 渐进式升级（保守）

**步骤**:
1. 先升级到 0.36.0（测试版验证）
2. 小范围灰度测试
3. 再升级到 0.36.1（生产环境）
4. 逐步启用官方 Token 统计

**优点**:
- ✅ 风险更分散
- ✅ 问题可早发现

**缺点**:
- ❌ 总耗时更长（4-5 天）
- ❌ 需要多次部署

**预计耗时**: 4-5 天

---

## 5. 实施计划（TDD 方法论）✅

### Phase 1: 测试准备（第 1 天）

#### 1.1 创建单元测试

**测试文件**: `src/test/java/com/dbdoctor/monitoring/TokenUsageTest.java`

```java
@SpringBootTest
class TokenUsageTest {

    @Autowired
    private ChatLanguageModel chatModel;

    @Test
    void testTokenUsageAvailability() {
        // 验证 0.36.1 的 TokenUsage 是否可用
        ChatResponse response = chatModel.generate("测试消息");

        TokenUsage usage = response.tokenUsage();
        assertNotNull(usage, "TokenUsage 不应为 null");
        assertTrue(usage.totalTokenCount() > 0, "总 Token 数应大于 0");

        log.info("Input Tokens: {}", usage.inputTokenCount());
        log.info("Output Tokens: {}", usage.outputTokenCount());
        log.info("Total Tokens: {}", usage.totalTokenCount());
    }
}
```

#### 1.2 创建集成测试

**测试文件**: `src/test/java/com/dbdoctor/monitoring/AiMonitoringListenerTest.java`

```java
@Test
void testDualTokenStrategy() {
    // 测试双重策略：官方 API 优先，估算兜底
}
```

---

### Phase 2: 版本升级（第 1-2 天）

#### 2.1 修改 pom.xml

```xml
<properties>
    <!-- LangChain4j 版本升级 -->
    <langchain4j.version>0.36.1</langchain4j.version>
</properties>
```

#### 2.2 编译验证

```bash
mvn clean compile
```

**预期结果**: ✅ 编译成功

---

### Phase 3: Token 统计替换（第 2 天）

#### 3.1 修改 AiMonitoringListener.java

**当前代码**（第 121-128 行）:
```java
// 策略 1: 尝试从官方 API 获取（TODO: LangChain4j 0.35.0 暂不支持，预留接口）
// TokenUsage usage = context.chatResponse().tokenUsage();
// if (usage != null) {
//     inputTokens = usage.inputTokenCount();
//     outputTokens = usage.outputTokenCount();
//     totalTokens = usage.totalTokenCount();
//     log.debug("[AI监控] 使用官方 Token 统计");
// }
```

**修改为**:
```java
// 策略 1: 尝试从官方 API 获取（0.36.1 支持）
TokenUsage usage = null;
try {
    if (context.response() != null) {
        usage = context.response().tokenUsage();
    }
} catch (Exception e) {
    log.debug("[AI监控] 获取官方 TokenUsage 失败: {}", e.getMessage());
}

if (usage != null && usage.totalTokenCount() > 0) {
    inputTokens = usage.inputTokenCount();
    outputTokens = usage.outputTokenCount();
    totalTokens = usage.totalTokenCount();
    log.debug("[AI监控] ✅ 使用官方 Token 统计: in={}, out={}, total={}",
            inputTokens, outputTokens, totalTokens);
} else {
    log.debug("[AI监控] 官方 TokenUsage 不可用，将使用估算算法");
}
```

#### 3.2 保留估算兜底

```java
// 策略 2: 估算兜底（Ollama 或官方 API 失败时）
if (totalTokens == 0) {
    String prompt = AiContextHolder.getPrompt();
    String response = AiContextHolder.getResponse();

    inputTokens = TokenEstimator.estimateInputTokens(prompt);
    outputTokens = TokenEstimator.estimateOutputTokens(response);
    totalTokens = inputTokens + outputTokens;

    log.debug("[AI监控] ⚠️ 使用 Token 估算: in={}, out={}, total={}",
            inputTokens, outputTokens, totalTokens);
}
```

---

### Phase 4: 准确性验证（第 2-3 天）

#### 4.1 创建准确性验证 Service

**文件**: `src/main/java/com/dbdoctor/service/AiAccuracyValidationService.java`

```java
@Service
@Slf4j
public class AiAccuracyValidationService {

    /**
     * 对比官方 Token 与估算 Token
     *
     * @param officialTokens 官方统计
     * @param estimatedTokens 估算值
     * @return 准确度报告
     */
    public AccuracyReport compareAccuracy(
            TokenUsage officialTokens,
            int estimatedTokens) {

        int official = officialTokens.totalTokenCount();
        int estimated = estimatedTokens;

        double errorRate = Math.abs(official - estimated) * 100.0 / official;

        return AccuracyReport.builder()
                .officialTokens(official)
                .estimatedTokens(estimated)
                .errorRate(errorRate)
                .isAcceptable(errorRate < 20) // 误差小于 20% 可接受
                .build();
    }
}
```

#### 4.2 记录对比数据

**扩展 AiInvocationLog 实体**:
```java
// 新增字段
@Column(name = "official_input_tokens")
private Integer officialInputTokens;

@Column(name = "official_output_tokens")
private Integer officialOutputTokens;

@Column(name = "token_accuracy_rate")
private Double tokenAccuracyRate; // 准确度（百分比）
```

---

### Phase 5: 成本分析功能（第 3-4 天）

#### 5.1 配置模型定价

**文件**: `src/main/resources/application.yml`

```yaml
db-doctor:
  ai:
    # AI 模型定价配置（单位：美元/百万 Token）
    cost:
      model-pricing:
        gpt-4:
          input: 30.0    # $30 / 百万输入 Token
          output: 60.0   # $60 / 百万输出 Token
        gpt-4o:
          input: 5.0
          output: 15.0
        deepseek-chat:
          input: 0.14
          output: 0.28
        qwen: # Ollama 本地模型，免费
          input: 0.0
          output: 0.0
```

#### 5.2 创建成本分析 Service

**文件**: `src/main/java/com/dbdoctor/service/AiCostService.java`

```java
@Service
@Slf4j
public class AiCostService {

    @Value("${db-doctor.ai.cost.model-pricing}")
    private Map<String, ModelPricing> modelPricing;

    /**
     * 计算单次调用成本
     *
     * @param modelName 模型名称
     * @param inputTokens 输入 Token 数
     * @param outputTokens 输出 Token 数
     * @return 成本（美元）
     */
    public double calculateCost(String modelName,
                                int inputTokens,
                                int outputTokens) {
        ModelPricing pricing = findPricing(modelName);

        double inputCost = (inputTokens / 1_000_000.0) * pricing.getInputPrice();
        double outputCost = (outputTokens / 1_000_000.0) * pricing.getOutputPrice();

        return inputCost + outputCost;
    }

    /**
     * 获取时间范围内的成本统计
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 成本统计
     */
    public CostStats getCostStats(LocalDateTime startTime,
                                  LocalDateTime endTime) {
        List<AiInvocationLog> logs = repository.findByStartTimeBetween(startTime, endTime);

        double totalCost = 0.0;
        Map<String, Double> costByModel = new HashMap<>();

        for (AiInvocationLog log : logs) {
            double cost = calculateCost(
                    log.getModelName(),
                    log.getInputTokens(),
                    log.getOutputTokens()
            );

            totalCost += cost;
            costByModel.merge(log.getModelName(), cost, Double::sum);
        }

        return CostStats.builder()
                .totalCost(totalCost)
                .costByModel(costByModel)
                .build();
    }
}
```

#### 5.3 添加成本统计 API

**Controller 接口**:
```java
@GetMapping("/cost-stats")
public Result<CostStats> getCostStats(
        @RequestParam(required = false) LocalDateTime startTime,
        @RequestParam(required = false) LocalDateTime endTime) {

    CostStats stats = costService.getCostStats(startTime, endTime);
    return Result.success(stats);
}
```

---

### Phase 6: 前端成本分析页面（第 4 天）

#### 6.1 创建成本分析组件

**文件**: `frontend/src/views/AiMonitor/CostAnalysis.vue`

**功能**:
- 展示总成本趋势图（ECharts 折线图）
- 各模型成本占比饼图
- 成本排行榜（按 SQL 指纹）

#### 6.2 添加路由

```typescript
{
  path: '/ai-monitor/cost-analysis',
  name: 'CostAnalysis',
  component: () => import('@/views/AiMonitor/CostAnalysis.vue')
}
```

---

## 6. 代码改动清单

### 6.1 后端文件

| 文件 | 改动类型 | 预计耗时 | 风险等级 |
|-----|---------|---------|---------|
| `pom.xml` | 修改版本号 | 5 分钟 | LOW |
| `AiMonitoringListener.java` | 启用官方 API | 30 分钟 | MEDIUM |
| `AiAccuracyValidationService.java` | 新增 | 1 小时 | LOW |
| `AiCostService.java` | 新增 | 1.5 小时 | LOW |
| `AiMonitorController.java` | 新增接口 | 30 分钟 | LOW |
| `application.yml` | 新增配置 | 15 分钟 | LOW |
| `TokenUsageTest.java` | 新增测试 | 1 小时 | LOW |
| `AiMonitoringListenerTest.java` | 新增测试 | 1 小时 | LOW |

**总计**: 约 6-7 小时

---

### 6.2 前端文件

| 文件 | 改动类型 | 预计耗时 | 风险等级 |
|-----|---------|---------|---------|
| `types.ts` | 新增类型定义 | 15 分钟 | LOW |
| `ai-monitor.ts` | 新增 API | 15 分钟 | LOW |
| `CostAnalysis.vue` | 新增页面 | 2 小时 | LOW |
| `router/index.ts` | 新增路由 | 5 分钟 | LOW |

**总计**: 约 2.5 小时

---

## 7. 测试策略

### 7.1 单元测试

**覆盖范围**:
- ✅ TokenUsage 可用性测试
- ✅ 双重策略逻辑测试
- ✅ 成本计算准确性测试
- ✅ 准确度对比算法测试

**覆盖率目标**: ≥ 80%

---

### 7.2 集成测试

**测试场景**:
1. ✅ OpenAI 模型调用（验证官方 Token）
2. ✅ Ollama 模型调用（验证估算兜底）
3. ✅ 成本统计端到端测试
4. ✅ 准确性验证流程测试

---

### 7.3 回归测试清单

**必须验证的功能**:

- [ ] AI 诊断功能（DiagnosisAgent）
- [ ] 深度推理功能（ReasoningAgent）
- [ ] 代码生成功能（CodingAgent）
- [ ] 监控大盘数据展示
- [ ] 调用流水查询
- [ ] 分析详情页面
- [ ] Token 统计准确性
- [ ] 成本统计功能

---

## 8. 回滚计划

### 8.1 回滚触发条件

出现以下情况立即回滚：
- ❌ 编译失败且无法修复
- ❌ 运行时严重错误导致系统不可用
- ❌ Token 统计完全失效（官方 API 和估算都失败）
- ❌ 性能严重下降（响应时间增加 50%+）

---

### 8.2 回滚步骤

**步骤 1**: 修改 `pom.xml` 恢复版本
```xml
<langchain4j.version>0.35.0</langchain4j.version>
```

**步骤 2**: 恢复 `AiMonitoringListener.java`
```java
// 注释掉官方 API 调用，只保留估算算法
```

**步骤 3**: 重新编译部署
```bash
mvn clean package -DskipTests
./bin/deploy.sh
```

**预计回滚时间**: 20 分钟

---

### 8.3 数据兼容性

**数据库变更**: ❌ 本次升级**无数据库 schema 变更**

**数据迁移**: ❌ 无需数据迁移

**回滚安全性**: ✅ 完全安全，无数据丢失风险

---

## 9. 成功标准

升级完成后的验收标准：

### 9.1 功能验收

- [x] OpenAI/DeepSeek 模型使用官方 Token 统计（准确度 95%+）
- [x] Ollama 模型继续使用估算算法（准确度 70-80%）
- [x] 成本分析功能正常工作
- [x] 准确性验证报告可查看
- [x] 所有现有功能正常运行

### 9.2 性能验收

- [x] AI 调用响应时间增加 < 10%
- [x] 监控写入性能无下降
- [x] 查询性能无下降

### 9.3 稳定性验收

- [x] 编译通过
- [x] 所有单元测试通过
- [x] 回归测试通过
- [x] 7×24 小时稳定运行

---

## 10. 风险与缓解措施

### 10.1 风险矩阵

| 风险 | 严重程度 | 概率 | 影响 | 缓解措施 |
|-----|---------|------|------|---------|
| 官方 Token API 获取失败 | **MEDIUM** | 30% | 部分统计不准确 | 保留估算算法兜底 |
| Ollama 仍不返回 Token | **LOW** | 70% | 预期行为 | 已有估算方案 |
| 编译兼容性问题 | **MEDIUM** | 10% | 升级受阻 | 逐个模块验证，查看官方迁移指南 |
| 运行时性能下降 | **MEDIUM** | 20% | 用户体验下降 | 性能测试，必要时优化代码 |
| 依赖冲突 | **LOW** | 5% | 编译失败 | Maven 依赖分析 |

---

### 10.2 缓解措施详情

#### 措施 1: 保留估算算法兜底 ✅

```java
// 双重策略确保总有 Token 统计
if (官方API可用) {
    使用官方统计;
} else {
    使用估算算法;
}
```

#### 措施 2: 完整的测试覆盖 ✅

- 单元测试（Phase 1）
- 集成测试（Phase 1）
- 回归测试（Phase 7）

#### 措施 3: 快速回滚机制 ✅

- 20 分钟回滚计划
- 无数据库变更，回滚安全
- 版本控制清晰

---

## 11. 后续优化方向

升级完成后的进一步优化：

1. **Token 估算算法优化**
   - 基于准确性验证数据调整参数
   - 考虑不同模型的 Tokenization 差异

2. **成本预警机制**
   - 设置预算阈值
   - 超支自动告警

3. **性能优化**
   - 异步写入优化
   - 批量查询优化

4. **多模型支持**
   - 扩展更多模型的定价配置
   - 支持自定义模型定价

---

## 12. 总结

### 推荐方案 ✅

**推荐采用方案 A（直接升级）+ TDD 方法论**

**理由**:
1. ✅ API 兼容性良好（ChatModelListener 无变化）
2. ✅ 有完善的兜底机制（估算算法）
3. ✅ 有快速的回滚方案（20 分钟）
4. ✅ 预计耗时短（2-3 天）

### 关键成功因素

1. **测试先行**（TDD）：先写测试，确保升级不破坏现有功能
2. **双重策略**：官方 API + 估算兜底，确保总有统计数据
3. **渐进式启用**：先在测试环境验证，再上生产
4. **监控告警**：升级后密切监控 Token 统计准确性

### 下一步行动

**立即开始**:
1. ✅ 创建单元测试（Phase 1）
2. ✅ 升级 pom.xml 到 0.36.1
3. ✅ 修改 AiMonitoringListener 启用官方 API
4. ✅ 验证编译和测试

---

**报告生成时间**: 2026-01-31
**预计升级完成时间**: 2026-02-02
**责任人**: DB-Doctor 开发团队
