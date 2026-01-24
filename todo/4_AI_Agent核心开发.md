# DB-Doctor AI Agent 核心开发

## 一、开发目标

基于 LangChain4j 实现 AI Agent，集成 SQL 诊断工具箱，实现智能的慢查询根因分析和优化建议生成。

## 二、功能描述

使用 LangChain4j 的 ReAct 模式，让 AI Agent 能够自主调用工具获取数据库信息，对比慢查询日志数据和数据库实际情况，生成专业的 Markdown 格式诊断报告。

## 三、详细开发任务

### 3.1 LangChain4j 配置 (AiConfig)

**位置**: `src/main/java/com/dbdoctor/config/AiConfig.java`

#### 3.1.1 基础配置

```java
@Configuration
public class AiConfig {

    @Value("${langchain4j.open-ai.api-key}")
    private String apiKey;

    @Value("${langchain4j.open-ai.model-name:gpt-4}")
    private String modelName;

    @Value("${langchain4j.open-ai.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    @Value("${langchain4j.open-ai.temperature:0.0}")
    private Double temperature;

    @Value("${langchain4j.open-ai.timeout:60s}")
    private Duration timeout;

    @Bean
    public OpenAiChatModel openAiChatModel() {
        return OpenAiChatModel.builder()
            .apiKey(apiKey)
            .modelName(modelName)
            .baseUrl(baseUrl)
            .temperature(temperature)
            .timeout(timeout)
            .build();
    }

    @Bean
    public ChatLanguageModel chatLanguageModel(OpenAiChatModel openAiChatModel) {
        return openAiChatModel;
    }
}
```

#### 3.1.2 配置文件 (application.yml)

```yaml
langchain4j:
  open-ai:
    # OpenAI API Key（或兼容接口的 Key）
    api-key: sk-xxx
    # 模型名称: gpt-4, gpt-3.5-turbo, 或兼容接口的模型
    model-name: gpt-4
    # 自定义 Base URL（用于兼容其他接口）
    base-url: https://api.openai.com/v1
    # 温度参数（0-1，越低越确定性）
    temperature: 0.0
    # 超时时间
    timeout: 60s
    # 最大 token 数
    max-tokens: 2000
```

### 3.2 DBAgent 接口定义

**位置**: `src/main/java/com/dbdoctor/agent/DBAgent.java`

#### 3.2.1 Agent 系统提示词 (System Prompt)

```java
@SystemMessage("""
你是一位资深 MySQL 数据库专家（DBA），拥有 10 年以上的数据库性能优化经验。

# 核心任务
分析用户提供的慢查询日志，结合你工具箱中的诊断工具，给出专业的优化建议。

# 多数据库诊断能力 ⚠️ 重要
- 你可以诊断**所有数据库**的慢查询，不受数据源限制
- 所有工具方法都需要传入**数据库名**参数
- 支持**跨库查询**的诊断（SQL 中包含多个数据库）
- 使用 information_schema 查询所有数据库的元数据

# 分析思维路径（必须严格遵循）
1. 先检查 Lock_time，如果 > 0.1s，调用 getLockInfo() 排查锁阻塞问题
2. 检查是否为跨库查询（databases 包含多个数据库）：
   - 如果是跨库查询，需要对每个数据库分别调用 getTableSchema()
   - 重点关注跨库 JOIN 的性能问题
3. 调用 getTableSchema(database, tableName) 检查表结构：
   - 必须传入**数据库名**参数
   - 字段类型是否合理（如用 varchar 存数字会导致隐式转换）
   - 是否缺少索引
   - 索引是否合理
4. 调用 getExecutionPlan(database, sql) 获取执行计划：
   - 必须传入**数据库名**和 **SQL** 参数
   - 重点关注：
     - type 字段：ALL（全表扫描）需要优化
     - key 字段：是否使用了索引
     - rows 字段：预估扫描行数
     - Extra 字段：Using filesort（需要优化）、Using temporary（需要优化）
5. 【关键】对比分析：
   - 日志中的 Rows_examined（实际扫描行数）
   - Explain 中的 rows（预估扫描行数）
   - 如果 Explain 预估很少，但实际扫描很多 → 统计信息过期，建议 ANALYZE TABLE
6. 调用 getTableStatistics(database, tableName) 确认统计信息更新时间
7. 调用 getIndexSelectivity(database, tableName) 检查索引选择性
8. 如果需要对比优化方案，调用 compareSqlPerformance(oldSql, newSql)

# 工具方法使用示例
getTableSchema("shop", "orders")              ← 必须传入数据库名
getExecutionPlan("shop", "SELECT * FROM ...")  ← 必须传入数据库名
getTableStatistics("shop", "orders")           ← 必须传入数据库名

# 安全红线（严禁违反）
- 绝对禁止给出 DROP, TRUNCATE, DELETE 等危险操作建议
- 所有索引创建建议必须使用 CREATE INDEX，而不是 ALTER TABLE
- 如果必须修改表结构，必须提醒用户先备份数据

# 输出格式（严格 Markdown）
## 问题诊断
- 根本原因：xxx
- 严重程度：高/中/低

## 执行计划分析
\`\`\`
[粘贴 EXPLAIN 结果的关键信息]
\`\`\`

## 优化建议
1. 建议添加索引：
   \`\`\`sql
   CREATE INDEX idx_xxx ON table(column);
   \`\`\`

2. 建议 SQL 优化：
   \`\`\`sql
   -- 原始 SQL
   SELECT ...

   -- 优化后 SQL
   SELECT ...
   \`\`\`

3. 其他建议：
   - 执行 ANALYZE TABLE 刷新统计信息
   - 调整参数 xxx

## 预期效果
- 查询成本从 {old_cost} 降低到 {new_cost}
- 性能提升约 {percentage}%

---
生成时间：{current_time}
DB-Doctor v1.0
""")
public interface DBAgent {

    // Agent 方法将在下一步实现
}
```

### 3.3 Agent 方法实现

#### 3.3.1 工具注入

```java
@Tool
public interface DBAgent {
    // LangChain4j 会自动注入 SqlDiagnosticsTools 的方法
}
```

#### 3.3.2 核心分析方法

```java
@UserMessage("""
请分析以下慢查询日志：

数据库：{database}
慢查询时间：{logTime}
查询耗时：{queryTime} 秒
锁等待时间：{lockTime} 秒
扫描行数：{rowsExamined}
返回行数：{rowsSent}

SQL 语句：
```sql
{sql}
```

请按照你的分析思维路径，调用工具进行诊断，并给出优化建议。
""")
String analyzeSlowLog(
    @V("database") String database,
    @V("logTime") String logTime,
    @V("queryTime") Double queryTime,
    @V("lockTime") Double lockTime,
    @V("rowsExamined") Long rowsExamined,
    @V("rowsSent") Long rowsSent,
    @V("sql") String sql
);
```

### 3.4 AnalysisService 业务层实现

**位置**: `src/main/java/com/dbdoctor/service/AnalysisService.java`

#### 3.4.1 服务结构

```java
@Service
public class AnalysisService {

    @Autowired
    private DBAgent dbAgent;

    @Autowired
    private NotifyService notifyService;

    private final ExecutorService executorService;

    /**
     * 异步分析慢查询日志
     */
    public void analyzeAsync(SlowLogEntry entry) {
        executorService.submit(() -> {
            try {
                // 1. 调用 AI Agent 分析
                String report = dbAgent.analyzeSlowLog(
                    entry.getDatabase(),
                    entry.getTime(),
                    entry.getQueryTime(),
                    entry.getLockTime(),
                    entry.getRowsExamined(),
                    entry.getRowsSent(),
                    entry.getSql()
                );

                // 2. 发送通知
                notifyService.sendNotification(entry, report);

            } catch (Exception e) {
                log.error("分析失败: {}", e.getMessage(), e);
            }
        });
    }
}
```

#### 3.4.2 线程池配置 (ThreadPoolConfig)

```java
@Configuration
public class ThreadPoolConfig {

    @Bean("analysisExecutor")
    public ExecutorService analysisExecutor() {
        return new ThreadPoolExecutor(
            2,                              // 核心线程数（AI 调用较慢）
            4,                              // 最大线程数
            60L, TimeUnit.SECONDS,          // 空闲线程存活时间
            new LinkedBlockingQueue<>(50),  // 队列容量
            new ThreadFactoryBuilder()
                .setNameFormat("db-doctor-analysis-%d")
                .setDaemon(true)
                .build(),
            new ThreadPoolExecutor.CallerRunsPolicy()  // 拒绝策略
        );
    }
}
```

### 3.5 Agent 调用链路

```
SlowLogEntry (解析后的日志对象)
    ↓
LogMonitorService (监听服务)
    ↓
提交到线程池
    ↓
AnalysisService.analyzeAsync() (业务层)
    ↓
DBAgent.analyzeSlowLog() (AI Agent)
    ↓
AI 自主调用工具：
  - getLockInfo()
  - getTableSchema()
  - getExecutionPlan()
  - getTableStatistics()
  - getIndexSelectivity()
  - compareSqlPerformance()
    ↓
生成 Markdown 诊断报告
    ↓
NotifyService.sendNotification() (通知服务)
```

### 3.6 Prompt 优化技巧

#### 3.6.1 上下文注入

在 System Message 中注入更多上下文：

```java
@SystemMessage("""
你是 DB-Doctor，一款 MySQL 慢查询智能诊疗系统。

# 当前环境信息
- MySQL 版本：5.7.40 / 8.0.32
- 数据库时区：Asia/Shanghai
- 慢查询阈值：2 秒

# 常见问题模式
1. 全表扫描：type=ALL，建议添加索引
2. 索引失效：type=ALL 但有索引，可能是字段类型不匹配
3. 统计信息过期：Explain rows << Rows_examined，建议 ANALYZE TABLE
4. 锁等待：Lock_time > 0.1s，需要排查锁源
5. 文件排序：Extra 包含 Using filesort，建议优化排序

# 分析步骤
...
""")
```

#### 3.6.2 Few-Shot 示例

在 System Message 中提供示例：

```java
@SystemMessage("""
# 示例分析

## 输入
```
Query_time: 5.2s
Rows_examined: 1000000
SQL: SELECT * FROM orders WHERE user_id = '123'
```

## 分析过程
1. Lock_time = 0.001s < 0.1s，无锁问题
2. getTableSchema(orders) → user_id 是 varchar(50)，有索引 idx_user_id
3. getExecutionPlan() → type=ALL，未使用索引！
4. 对比 SQL：user_id 是字符串，查询用了数字 123（无引号）
5. 根因：隐式类型转换导致索引失效

## 输出
问题诊断：user_id 字段类型不匹配导致索引失效
优化建议：
```sql
-- 错误写法
SELECT * FROM orders WHERE user_id = 123

-- 正确写法
SELECT * FROM orders WHERE user_id = '123'
```
""")
```

### 3.7 异常处理

```java
- API 调用超时（> 60s）：记录日志，跳过该日志
- API 返回错误：记录错误信息，跳过该日志
- 工具调用失败：Agent 会自动重试或给出替代建议
- Token 超限：截断 SQL 语句（保留前 500 字符）
```

### 3.8 日志记录

```java
log.info("开始分析慢查询: database={}, sql={}", entry.getDatabase(), entry.getSql());
log.debug("AI Agent 原始返回: {}", report);
log.info("分析完成，耗时: {}ms", duration);
```

## 四、验收标准

1. ✓ AI Agent 能够正常初始化
2. ✓ Agent 能够调用所有工具方法
3. ✓ Agent 生成的报告符合 Markdown 格式
4. ✓ Agent 能够遵循"分析思维路径"
5. ✓ 异常情况有合理的处理机制
6. ✓ AnalysisService 能够异步处理日志
7. ✓ 完整的调用链路能够跑通

## 五、测试用例

### 5.1 测试用例 1：全表扫描

```java
SlowLogEntry entry = new SlowLogEntry();
entry.setDatabase("test_db");
entry.setQueryTime(5.2);
entry.setLockTime(0.001);
entry.setRowsExamined(1000000L);
entry.setSql("SELECT * FROM orders WHERE user_id = 123");
```

**预期输出**：
- 识别出隐式类型转换
- 建议添加引号或修改字段类型

### 5.2 测试用例 2：统计信息过期

```java
entry.setQueryTime(3.5);
entry.setRowsExamined(500000L);
entry.setSql("SELECT * FROM products WHERE category_id = 10");
```

**Agent 调用链**：
1. getExecutionPlan() → rows=1000
2. 对比：Explain rows(1000) << Rows_examined(500000)
3. getTableStatistics() → update_time 是 3 个月前
4. 建议：ANALYZE TABLE

## 六、注意事项

1. **温度参数**: 设置为 0.0，保证输出确定性
2. **Token 限制**: 长SQL需截断，避免超限
3. **API Key**: 支持环境变量或外部配置文件
4. **超时设置**: AI 调用较慢，建议 60s 超时
5. **成本控制**: 考虑添加调用频率限制
