# DB-Doctor 项目开发规范

## 项目概述

DB-Doctor 是一款非侵入式的 MySQL 慢查询智能诊疗系统，通过实时监听慢查询日志，利用 AI Agent 分析根因并推送优化建议。

- **技术栈**: Java 17 + Spring Boot 3.x + LangChain4j
- **开发模式**: 敏捷开发，按功能模块迭代
- **代码规范**: 遵循阿里巴巴 Java 开发规范

## ⚠️ 最重要的开发原则

> **#1 原则：尽量不要写死代码**
>
> - **禁止硬编码**：版本号、路径、端口号、时间参数、线程数等所有可变内容必须从配置文件读取
> - **禁止使用 System.out.println**：统一使用 Slf4j 日志框架
> - **配置优于代码**：所有参数都应该可在 application.yml 中配置
> - **默认值明确**：使用 `@Value("${key:defaultValue}")` 提供合理默认值
>
> **违反此原则的代码将被拒绝！**

---

## 开发流程

### 1. 任务领取与规划

- 开发前先查看 `todo/` 目录下的详细开发文档
- 使用 TodoWrite 工具创建任务列表，跟踪开发进度
- 按文档顺序开发：1 → 2 → 3 → 4 → 5 → 6 → 7

### 2. 开发步骤

```
1. 阅读需求文档 (todo/N_功能开发.md)
2. 创建/更新任务列表
3. 编写代码前检查：
   ✓ 是否存在硬编码的值？
   ✓ 是否使用了 System.out.println？
   ✓ 所有参数是否都能在配置文件中修改？
4. 编写代码（使用 @Value 或 @ConfigurationProperties）
5. 本地编译验证 (mvn clean compile)
6. 代码审查：检查是否有硬编码
7. 更新任务状态
8. 提交代码
```

### 3. 硬编码检查清单（开发前必看）

在编写代码前，确保以下内容**不在代码中硬编码**：

- ✗ 版本号：`"1.0.0"` → ✓ `@Value("${db-doctor.version}")`
- ✗ 项目名：`"DB-Doctor"` → ✓ `@Value("${db-doctor.name}")`
- ✗ 日志路径：`"/var/log/mysql/slow.log"` → ✓ `@Value("${db-doctor.log.slow-log-path}")`
- ✗ 端口号：`8080` → ✓ `@Value("${server.port}")`
- ✗ 超时时间：`1000` → ✓ `@Value("${db-doctor.timeout:1000}")`
- ✗ 线程数：`8` → ✓ `@Value("${db-doctor.thread-pool.core-size:8}")`
- ✗ 重试次数：`3` → ✓ `@Value("${db-doctor.retry.max-times:3}")`
- ✗ 输出语句：`System.out.println()` → ✓ `log.info()`

### 3. 验收标准

每个功能模块开发完成后，必须满足：
- ✓ 代码编译通过 (`mvn clean compile`)
- ✓ 单元测试通过 (`mvn test`)
- ✓ 符合验收标准（见各功能文档）

---

## 代码规范

### 配置化开发规范 ⚠️ **最重要原则：尽量不要写死代码**

> **核心原则**：代码中不应该出现任何硬编码的值、参数、路径、版本号等，所有可变内容都应该从配置文件读取或定义为常量。

#### 硬编码的危害

```java
// ❌ 问题代码示例
public class LogMonitorService {
    // 问题1：硬编码版本号
    private String version = "1.0.0";

    // 问题2：硬编码路径
    private String logPath = "/var/log/mysql/slow.log";

    // 问题3：硬编码时间参数
    private Integer checkInterval = 1000;

    // 问题4：硬编码线程数
    private Integer threadPoolSize = 8;

    // 问题5：硬编码端口号
    private String apiUrl = "http://localhost:8080/api";

    public void start() {
        // 问题6：使用 System.out 而不是日志框架
        System.out.println("DB-Doctor v1.0.0 启动中...");

        for (int i = 0; i < 3; i++) {  // 问题7：硬编码循环次数
            // ...
        }
    }
}
```

#### 正确的配置化开发

```java
// ✓ 正确：从配置文件读取
@Slf4j
@Service
public class LogMonitorService {

    @Value("${db-doctor.version}")
    private String version;

    @Value("${db-doctor.log.slow-log-path}")
    private String logPath;

    @Value("${db-doctor.log.check-interval:1000}")
    private Integer checkInterval;

    @Value("${db-doctor.thread-pool.log-parser.core-size:8}")
    private Integer threadPoolSize;

    @Value("${db-doctor.api.url:http://localhost:8080/api}")
    private String apiUrl;

    @Value("${db-doctor.retry.max-times:3}")
    private Integer maxRetryTimes;

    public void start() {
        log.info("{} 启动中，版本: {}", getAppName(), version);

        for (int i = 0; i < maxRetryTimes; i++) {
            // ...
        }
    }

    @Value("${db-doctor.name:DB-Doctor}")
    private String appName;
}
```

#### 配置文件示例（application.yml）

```yaml
db-doctor:
  # 项目基本信息
  name: DB-Doctor
  version: 1.0.0

  # 日志配置
  log:
    slow-log-path: /var/log/mysql/slow.log
    check-interval: 1000

  # 线程池配置
  thread-pool:
    log-parser:
      core-size: 8

  # API 配置
  api:
    url: http://localhost:8080/api

  # 重试配置
  retry:
    max-times: 3
```

#### 常见硬编码问题检查清单

开发前检查以下内容是否存在硬编码：

- [ ] 版本号、项目名称
- [ ] 文件路径（日志路径、配置路径）
- [ ] 端口号、URL
- [ ] 时间参数（超时时间、间隔时间）
- [ ] 线程池大小、队列容量
- [ ] 重试次数、回退时间
- [ ] 魔法数字（循环次数、数组大小）
- [ ] 错误消息文本（应使用国际化或配置文件）
- [ ] SQL 语句中的参数（应使用参数化查询）

#### 原则：禁止硬编码，所有参数从配置文件读取

#### 使用 ConfigurationProperties 管理复杂配置

```java
// ✓ 推荐：使用 @ConfigurationProperties
@Data
@Component
@ConfigurationProperties(prefix = "db-doctor")
public class DbDoctorProperties {

    private String name;
    private String version;
    private LogConfig log = new LogConfig();

    @Data
    public static class LogConfig {
        private Integer checkInterval;
        private String slowLogPath;
    }
}

// 使用配置类
@Service
public class LogMonitorService {

    @Autowired
    private DbDoctorProperties properties;

    public void start() {
        log.info("启动检查，间隔: {}ms", properties.getLog().getCheckInterval());
    }
}
```

#### 日志规范：禁止使用 System.out.println

```java
// ✗ 错误：使用 System.out
System.out.println("应用启动");
System.err.println("错误信息");

// ✓ 正确：使用 Slf4j
@Slf4j
@Service
public class MyService {
    public void doSomething() {
        log.info("应用启动");          // 普通信息
        log.debug("调试信息: {}", data); // 调试信息
        log.warn("警告信息");          // 警告
        log.error("错误信息", e);      // 错误（带异常）
    }
}
```

#### 配置文件中的魔法值

```yaml
# ✗ 错误：代码中硬编码
private static final int MAX_RETRY = 3;

# ✓ 正确：配置文件中定义
db-doctor:
  retry:
    max-times: 3
    backoff: 1000
```

#### 配置管理规范：禁止使用环境变量 ⚠️

**原则**：
- ✗ **禁止**使用 `${环境变量名:默认值}` 方式读取环境变量
- ✗ **禁止**使用 `System.getenv()` 读取环境变量
- ✓ **推荐**使用 `application-local.yml` 管理本地敏感配置
- ✓ **推荐**使用 `application.yml` 管理通用配置
- ✓ 本地配置文件必须加入 `.gitignore`，防止泄露隐私

**为什么不使用环境变量？**
1. 不利于本地开发调试（需要配置多个环境变量）
2. 配置分散，不易管理
3. Windows/Linux/Mac 环境变量设置方式不统一
4. 敏感信息容易泄露（环境变量可被其他进程读取）

**正确的配置管理方式**：

```yaml
# ✗ 错误：使用环境变量
langchain4j:
  open-ai:
    api-key: ${OPENAI_API_KEY:sk-xxx}
    base-url: ${OPENAI_BASE_URL:https://api.openai.com/v1}

spring:
  datasource:
    password: ${DB_PASSWORD:your_password}

# ✓ 正确：使用本地配置文件
# application.yml - 通用配置（所有环境共享，可提交到 Git）
langchain4j:
  open-ai:
    model-name: gpt-4
    temperature: 0.0
    timeout: 60s

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/information_schema
    username: db_doctor
    # 密码在 local 配置中

# application-local.yml - 本地配置（包含敏感信息，不提交到 Git）
langchain4j:
  open-ai:
    api-key: sk-your-actual-api-key-here
    base-url: https://api.openai.com/v1

spring:
  datasource:
    password: your_actual_password_here

db-doctor:
  notify:
    email:
      to:
        - your-email@example.com
```

**配置文件使用方式**：

```bash
# 开发环境：使用 local 配置
java -jar app.jar --spring.profiles.active=local

# 生产环境：使用生产配置（需要单独准备）
java -jar app.jar --spring.profiles.active=prod
```

### Java 代码规范

#### 命名规范

```java
// 类名：大驼峰
public class LogMonitorService { }

// 方法名：小驼峰
public void parseLog(String rawLog) { }

// 常量：全大写+下划线
private static final int MAX_RETRY_TIMES = 3;

// 变量：小驼峰
private String slowLogPath;
```

#### 注释规范

```java
/**
 * 日志监听服务
 * 实现对 MySQL 慢查询日志文件的实时增量读取（类似 Linux tail -f）
 *
 * @author DB-Doctor
 * @version 1.0.0
 * @since 1.0.0
 */
@Service
public class LogMonitorService {

    /**
     * 解析单条慢查询日志
     *
     * @param rawLog 原始日志内容
     * @return 解析后的慢查询对象
     * @throws IllegalArgumentException 日志格式错误时抛出
     */
    public SlowLogEntry parseLog(String rawLog) {
        // TODO: 待实现
    }
}
```

#### 异常处理

```java
// 不要吞掉异常
try {
    // ...
} catch (Exception e) {
    log.error("操作失败", e);  // ✓ 记录日志
    throw new BusinessException("操作失败", e);  // ✓ 重新抛出
}

// ✗ 错误示例
} catch (Exception e) {
    // 什么都不做
}
```

#### 日志规范

```java
// 使用 Slf4j
@Slf4j
@Service
public class LogMonitorService {

    public void parseLog(String rawLog) {
        log.debug("开始解析日志: {}", rawLog);  // 调试信息
        log.info("解析成功，耗时: {}ms", duration);  // 关键信息
        log.warn("日志格式异常: {}", rawLog);  // 警告
        log.error("解析失败", e);  // 错误（带异常堆栈）
    }
}
```

### 配置文件规范

#### application.yml

```yaml
# 1. 分组注释
db-doctor:
  # 日志监听配置
  log:
    slow-log-path: /var/log/mysql/slow.log
    check-interval: 1000

  # AI 配置
  ai:
    enabled: true

# 2. 使用环境变量
langchain4j:
  open-ai:
    api-key: ${OPENAI_API_KEY:sk-xxx}
```

### 依赖管理规范

#### pom.xml

```xml
<!-- 1. 版本号统一在 properties 中定义 -->
<properties>
    <langchain4j.version>0.29.1</langchain4j.version>
</properties>

<!-- 2. 依赖版本使用属性引用 -->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j</artifactId>
    <version>${langchain4j.version}</version>
</dependency>

<!-- 3. 排除不必要的依赖 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <optional>true</optional>
</dependency>
```

---

## Git 提交规范

### 提交信息格式

```
<type>(<scope>): <subject>

<body>

<footer>
```

### Type 类型

- `feat`: 新功能
- `fix`: 修复 bug
- `docs`: 文档修改
- `style`: 代码格式修改（不影响逻辑）
- `refactor`: 重构（既不是新功能也不是修复 bug）
- `test`: 测试相关
- `chore`: 构建过程或辅助工具的变动

### 示例

```bash
# 新功能
git commit -m "feat(log): 实现慢查询日志实时监听功能

- 使用 Apache Commons IO 的 Tailer 实现文件监听
- 实现日志解析状态机
- 添加线程池削峰填谷机制

Closes #1"

# 修复 Bug
git commit -m "fix(parser): 修复多行 SQL 解析错误

修复了 SQL 语句跨越多行时只保留第一行的问题"

# 文档更新
git commit -m "docs: 更新 README 部署说明"

# 重构
git commit -m "refactor(service): 重构 AnalysisService 异步处理逻辑"
```

---

## 分支管理规范

### 分支策略

```
main (生产环境)
  ↑
  merge
  ↑
develop (开发环境)
  ↑
  feature/功能名称 (功能分支)
```

### 分支命名

- `feature/日志监听功能` - 功能开发
- `fix/SQL注入防护` - Bug 修复
- `refactor/线程池优化` - 重构

### 工作流程

```bash
# 1. 从 develop 创建功能分支
git checkout develop
git checkout -b feature/日志监听功能

# 2. 开发并提交
git add .
git commit -m "feat(log): 实现日志监听"

# 3. 推送到远程
git push origin feature/日志监听功能

# 4. 合并到 develop
git checkout develop
git merge feature/日志监听功能

# 5. 删除功能分支
git branch -d feature/日志监听功能
```

---

## 测试规范

### 单元测试

```java
@SpringBootTest
class LogMonitorServiceTest {

    @Autowired
    private LogMonitorService logMonitorService;

    @Test
    void testParseSingleLog() {
        // Given
        String rawLog = "# Time: 2024-01-22T10:30:00.123456Z\n...";

        // When
        SlowLogEntry entry = logMonitorService.parseLog(rawLog);

        // Then
        assertNotNull(entry);
        assertEquals(3.5, entry.getQueryTime());
    }

    @Test
    void testParseMultiLineSql() {
        // ...
    }
}
```

### 测试覆盖率

- 核心业务逻辑覆盖率 ≥ 80%
- 工具类覆盖率 ≥ 90%

---

## 文档规范

### 代码文档

- 所有 public 类必须添加 JavaDoc
- 所有 public 方法必须添加 JavaDoc
- 复杂逻辑必须添加行内注释

### 功能文档

- 每个功能模块的开发文档放在 `todo/` 目录
- 文档命名：`N_功能名称.md`
- 文档内容包含：开发目标、详细任务、验收标准、测试用例

### README 更新

- 新功能开发完成后，更新 README.md
- 添加使用示例和配置说明

---

## 安全规范

### SQL 注入防护

```java
// ✗ 禁止：字符串拼接
String sql = "SELECT * FROM users WHERE id = " + userId;

// ✓ 正确：使用参数化查询
String sql = "SELECT * FROM users WHERE id = ?";
jdbcTemplate.query(sql, userId);
```

### AI 提示词安全

```java
// 在 System Prompt 中明确安全红线
@SystemMessage("""
安全红线：
- 绝对禁止给出 DROP, TRUNCATE, DELETE 等危险操作建议
- 所有索引创建建议必须使用 CREATE INDEX
- 如果必须修改表结构，必须提醒用户先备份数据
""")
```

### 敏感信息处理

```java
// ✗ 禁止：日志中打印敏感信息
log.info("用户登录: username={}, password={}", username, password);

// ✓ 正确：脱敏处理
log.info("用户登录: username={}", username);
```

---

## 性能优化规范

### 异步处理

```java
// 使用线程池异步处理耗时操作
@Service
public class AnalysisService {

    @Autowired
    @Qualifier("analysisExecutor")
    private ExecutorService executorService;

    public void analyzeAsync(SlowLogEntry entry) {
        executorService.submit(() -> {
            // 耗时分析操作
        });
    }
}
```

### 资源管理

```java
// 使用 try-with-resources
try (Connection conn = dataSource.getConnection()) {
    // ...
}

// 使用 @PreDestroy 销毁资源
@PreDestroy
public void destroy() {
    executorService.shutdown();
}
```

---

## 开发工具配置

### IDEA 配置

1. **代码格式化**
   - 导入 `idea-code-style.xml`（如有）
   - Settings → Editor → Code Style → Java

2. **自动导入**
   - Settings → Editor → General → Auto Import
   - 勾选 "Optimize imports on the fly"

3. **保存时操作**
   - Settings → Tools → Actions on Save
   - 勾选 "Reformat code"
   - 勾选 "Optimize imports"

### Maven 配置

```xml
<!-- settings.xml 配置阿里云镜像 -->
<mirrors>
    <mirror>
        <id>aliyun</id>
        <mirrorOf>central</mirrorOf>
        <url>http://maven.aliyun.com/nexus/content/groups/public</url>
    </mirror>
</mirrors>
```

---

## 常见问题

### Q1: 编译失败怎么办？

```bash
# 清理并重新编译
mvn clean compile -U

# -U: 强制更新依赖
```

### Q2: 如何运行项目？

```bash
# 方式 1: Maven 运行
mvn spring-boot:run

# 方式 2: Java 运行
java -jar target/db-doctor-1.0.0.jar

# 方式 3: 使用启动脚本
./bin/startup.sh
```

### Q3: 如何跳过测试？

```bash
mvn clean package -DskipTests
```

---

## 附录

### 参考文档

- [阿里巴巴 Java 开发手册](https://github.com/alibaba/p3c)
- [Spring Boot 官方文档](https://spring.io/projects/spring-boot)
- [LangChain4j 文档](https://docs.langchain4j.dev/)

### 项目联系人

- 项目负责人: [待填写]
- 技术支持: [待填写]

---

**最后更新时间**: 2024-01-22
**文档版本**: v1.0.0
