# 🏥 DB-Doctor

<div align="center">

**一款非侵入式的 MySQL 慢查询智能诊疗系统**

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green.svg)](https://spring.io/projects/spring-boot)
[![LangChain4j](https://img.shields.io/badge/LangChain4j-0.36.1-blue.svg)](https://docs.langchain4j.dev)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

</div>

---

## 📖 项目简介

**DB-Doctor** 是一款基于 AI Agent 的 MySQL 慢查询智能分析系统，通过实时监听 MySQL 慢查询日志，利用大语言模型（LLM）分析根因并推送优化建议。

### 核心特性

- 🚀 **非侵入式设计**：只读访问 MySQL，零业务侵入
- 🤖 **AI 智能诊断**：基于 LangChain4j + LLM 的慢查询根因分析
- 📊 **SQL 指纹去重**：自动识别重复 SQL，避免通知轰炸
- 🔄 **动态环境感知**：自动检测 MySQL 配置，无需重启即可恢复监控
- 📧 **多渠道通知**：支持邮件、钉钉、飞书、企业微信
- 🛡️ **工业级可靠性**：优雅停机、任务重试、自动清理

---

## 🎯 适用场景

- **DBA 效率提升**：自动分析慢查询，减少人工排查时间
- **开发团队辅助**：实时收到 SQL 优化建议，提升代码质量
- **数据库监控**：7x24 小时监控数据库性能，及时发现异常
- **性能优化**：持续跟踪慢查询趋势，辅助性能调优

---

## 🏗️ 系统架构

```
┌─────────────────────────────────────────────────────────────┐
│                        DB-Doctor                              │
├─────────────────────────────────────────────────────────────┤
│  ┌──────────────┐   ┌──────────────┐   ┌──────────────┐    │
│  │ 环境检查器   │   │ 监控调度器   │   │ 分析服务     │    │
│  │ EnvChecker   │──▶│ Monitor      │──▶│ Analysis     │    │
│  └──────────────┘   └──────────────┘   └──────────────┘    │
│                            │                   │            │
│                            ▼                   ▼            │
│  ┌──────────────┐   ┌──────────────┐   ┌──────────────┐    │
│  │ MySQL 慢查询 │   │  H2 内部库   │   │ AI Agent     │    │
│  │ 日志表       │   │  (分析历史)  │   │ (LLM 分析)   │    │
│  └──────────────┘   └──────────────┘   └──────────────┘    │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
                   ┌──────────────┐
                   │  通知服务    │
                   │  (邮件/IM)   │
                   └──────────────┘
```

---

## 🚀 快速开始

### 环境要求

- **JDK 17+**
- **Maven 3.6+**
- **MySQL 5.7+ / 8.0+**
- **LLM API Key**（阿里云通义千问 / OpenAI / 其他兼容模型）

### 1. 克隆项目

```bash
git clone https://github.com/your-username/db-doctor.git
cd db-doctor
```

### 2. 配置数据库

编辑 `src/main/resources/application-local.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/information_schema
    username: root
    password: your_password
    driver-class-name: com.mysql.cj.jdbc.Driver

db-doctor:
  target-db:
    host: localhost
    port: 3306
    username: root
    password: your_password
```

#### 🗄️ MySQL 数据库访问说明

**1. 确保 MySQL 服务运行**

```bash
# Windows
# 检查 MySQL 服务是否启动
sc query MySQL80

# 启动 MySQL 服务
net start MySQL80

# Linux/Mac
sudo systemctl start mysql
sudo systemctl status mysql
```

**2. 连接 MySQL 数据库**

**命令行方式**：
```bash
# 连接 MySQL
mysql -u root -p

# 进入后显示所有数据库
SHOW DATABASES;

# 切换到 information_schema
USE information_schema;

# 查看表
SHOW TABLES;
```

**图形化工具（推荐）**：

| 工具 | 下载地址 | 特点 |
|------|---------|------|
| **MySQL Workbench** | https://dev.mysql.com/downloads/workbench/ | 官方工具，功能全面 |
| **Navicat** | https://www.navicat.com/ | 界面友好，商业软件 |
| **DBeaver** | https://dbeaver.io/ | 免费开源，轻量级 |
| **phpMyAdmin** | https://www.phpmyadmin.net/ | Web 界面，适合 LAMP |

**使用 MySQL Workbench 连接**：
```
1. 打开 MySQL Workbench
2. 点击 "+" 创建新连接
3. 填写连接信息：
   - Hostname: 127.0.0.1 或 localhost
   - Port: 3306
   - Username: root
   - Password: 你的 MySQL 密码
4. 点击 "Test Connection" 测试连接
5. 点击 "OK" 保存连接
```

**3. 查看慢查询日志**

```sql
-- 切换到 information_schema 数据库
USE information_schema;

-- 查看 PROCESSLIST 表（存储慢查询日志）
-- 注意：MySQL 8.0+ 默认使用系统表，慢查询日志记录在 mysql.slow_log 表
SELECT * FROM mysql.slow_log LIMIT 10;
```

**4. 配置慢查询日志（如未配置）**

```sql
-- 开启慢查询日志
SET GLOBAL slow_query_log = 'ON';

-- 设置日志输出方式（TABLE = 表, FILE = 文件）
SET GLOBAL log_output = 'TABLE';

-- 设置慢查询阈值（单位：秒）
SET GLOBAL long_query_time = 2.0;

-- 验证配置
SHOW VARIABLES LIKE 'slow_query%';
SHOW VARIABLES LIKE 'long_query_time';
```

**5. 常用数据库操作**

```bash
# 创建数据库
mysql -u root -p -e "CREATE DATABASE test_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 导入数据
mysql -u root -p test_db < test_db.sql

# 导出数据库
mysqldump -u root -p test_db > test_db_backup.sql

# 查看数据库大小
mysql -u root -p -e "SELECT table_schema AS 'Database',
  ROUND(SUM(data_length + index_length) / 1024 / 1024, 2) AS 'Size (MB)'
  FROM information_schema.tables
  GROUP BY table_schema;"
```

**6. 测试数据库连接**

在 `application-local.yml` 配置完成后，可以通过以下方式测试连接：

```bash
# 使用 telnet 测试端口
telnet localhost 3306

# 或使用 nc 命令（Linux/Mac）
nc -zv localhost 3306
```

如果连接成功，会显示 `Escape character is ^]` 或 `Connected to localhost`。

---

### 2.5 H2 数据库访问说明（开发/测试）

DB-Doctor 使用 **H2 内存数据库**存储慢查询分析历史，可通过以下方式访问：

#### 🌐 H2 Web Console（推荐）

**启动后访问 H2 Console**：
```
URL: http://localhost:8080/h2-console
JDBC URL: jdbc:h2:mem:dbdoctor
用户名: sa
密码: (留空)
```

**连接步骤**：
1. 启动 DB-Doctor 后端服务
2. 浏览器访问：`http://localhost:8080/h2-console`
3. 填写连接信息：
   - **Saved Settings**: `Generic H2 (Embedded)`
   - **Driver Class**: `org.h2.Driver`
   - **JDBC URL**: `jdbc:h2:mem:dbdoctor`
   - **User Name**: `sa`
   - **Password**: (留空)
4. 点击 "Connect" 连接

#### 📊 H2 数据库表结构

连接成功后，可以查看以下表：

```sql
-- 查看所有表
SHOW TABLES;

-- 慢查询模板表（核心表）
SELECT * FROM slow_query_template;

-- 查看最近的慢查询分析记录
SELECT
  id,
  sql_fingerprint,
  db_name,
  table_name,
  avg_query_time,
  max_query_time,
  occurrence_count,
  severity_level,
  status,
  first_seen_time,
  last_seen_time
FROM slow_query_template
ORDER BY last_seen_time DESC
LIMIT 10;

-- 查看统计信息
SELECT
  COUNT(*) as total_templates,
  COUNT(CASE WHEN status = 'SUCCESS' THEN 1 END) as success_count,
  COUNT(CASE WHEN status = 'PENDING' THEN 1 END) as pending_count,
  COUNT(CASE WHEN severity_level = '🔴 严重' THEN 1 END) as critical_count
FROM slow_query_template;
```

#### 🔧 JDBC 连接方式（使用工具）

**DBeaver 连接 H2**：
```
1. 下载 DBeaver: https://dbeaver.io/
2. 创建新连接 → 选择 "H2 Embedded"
3. Database path: `mem:dbdoctor`
4. User name: `sa`
5. Password: (留空)
```

**IntelliJ IDEA Database 工具**：
```
1. 右侧 Database 面板 → "+" → Data Source → H2
2. File: `mem:dbdoctor`
3. User: `sa`
4. 点击 "Test Connection"
```

#### 💾 H2 数据持久化（可选）

**当前配置**：H2 数据存储在内存中，重启后数据丢失。

**持久化到文件**：修改 `application-local.yml`：

```yaml
spring:
  datasource:
    url: jdbc:h2:file:./data/dbdoctor  # 改为文件模式
    # url: jdbc:h2:mem:dbdoctor     # 内存模式（默认）
```

重启后，数据将保存在项目根目录的 `./data/dbdoctor.mv.db` 文件中。

#### 🛠️ 常用 H2 操作

```sql
-- 查看所有表
SHOW TABLES;

-- 查看表结构
DESCRIBE slow_query_template;

-- 查看索引
SHOW INDEX FROM slow_query_template;

-- 清空测试数据（开发调试用）
DELETE FROM slow_query_template;

-- 查看数据库版本
SELECT H2_VERSION();
```

#### ⚠️ H2 数据库特点

| 特性 | 说明 |
|------|------|
| **内存模式** | 数据保存在内存中，重启丢失（默认） |
| **文件模式** | 数据持久化到文件，重启保留 |
| **轻量级** | 无需独立安装数据库服务 |
| **开发友好** | 启动快，适合测试环境 |
| **Web Console** | 自带 Web 管理界面 |

**注意**：生产环境建议使用 MySQL 或 PostgreSQL，H2 仅用于开发/测试。

### 3. 配置 AI

```yaml
langchain4j:
  open-ai:
    chat-model:
      api-key: your-api-key-here
      base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
      model-name: qwen-plus
```

### 4. 配置慢查询

**方式 1：自动检查（推荐）**

```yaml
db-doctor:
  env-check:
    enabled: true  # 启动时自动检查环境
```

**方式 2：手动配置**

在 MySQL 中执行：

```sql
SET GLOBAL slow_query_log = 'ON';
SET GLOBAL log_output = 'TABLE';
SET GLOBAL long_query_time = 2.0;
```

### 5. 启动项目

```bash
mvn spring-boot:run
```

访问：http://localhost:8080

---

## 📝 配置说明

### 环境检查配置

```yaml
db-doctor:
  env-check:
    enabled: true            # 是否启用环境检查
    fail-on-error: false     # 检查失败是否阻止启动
    auto-fix: false          # 是否尝试自动修复（需要 SUPER 权限）
```

### 慢查询监控配置

```yaml
db-doctor:
  slow-log-monitor:
    poll-interval-ms: 60000          # 轮询间隔（毫秒）
    max-records-per-poll: 100        # 每次最多读取记录数
    auto-cleanup:
      enabled: true                  # 是否启用自动清理
      cron-expression: "0 0 3 * * ?" # 清理任务 cron 表达式
```

### 通知配置

```yaml
db-doctor:
  notify:
    enabled-notifiers: email,webhook
    notify-interval: 3600            # 通知间隔（秒）
    severity-threshold: 3.0          # 严重程度阈值

    email:
      enabled: true
      from: DB-Doctor <noreply@example.com>
      to:
        - dba@example.com
```

---

## 🎨 功能特性详解

### 1. 动态环境感知

- ✅ 启动时全面检查 MySQL 配置
- ✅ 运行时每分钟快速检查（轻量级）
- ✅ 环境不达标时进入"休眠"状态
- ✅ 用户修复配置后自动恢复监控（无需重启）

### 2. SQL 指纹去重

- ✅ 自动计算 SQL 指纹（MD5）
- ✅ 识别相同类型的慢查询
- ✅ 首次发现立即通知
- ✅ 重复查询聚合统计，避免通知轰炸

### 3. AI 智能分析

- ✅ 深度分析慢查询根因
- ✅ 提供索引优化建议
- ✅ SQL 重写建议
- ✅ 执行计划分析（EXPLAIN）

### 4. AI 监控与 Token 统计 🆕

- ✅ **AI 调用监控**：实时监控所有 AI 调用（Diagnosis/Reasoning/Coding）
- ✅ **Token 消耗统计**：追踪输入/输出 Token 数量
- ✅ **性能分析**：记录每次 AI 调用的耗时
- ✅ **单次分析详情**：按 SQL 指纹（traceId）聚合查看完整的分析链路
- ✅ **成本分析** 🆕：统计各模型和各 Agent 的成本消耗（v2.3.2）

#### ✨ Token 统计实现说明（v2.3.2 升级）

**当前实现方式**：**官方 API + 估算兜底** 的双重策略

**v2.3.2 升级内容**：
- ✅ **LangChain4j 升级到 0.36.1**：支持完整的 TokenUsage API
- ✅ **官方 Token 统计**：OpenAI、DeepSeek 等模型使用真实 Token 数（准确度 95%+）
- ✅ **估算算法兜底**：Ollama 等本地模型继续使用估算（准确度 70-80%）
- ✅ **成本分析功能**：基于真实 Token 计算实际成本（支持多模型定价）

**Token 统计策略**：

1. **优先使用官方 API**（v2.3.2 启用）
   - OpenAI/DeepSeek 等支持 TokenUsage 的模型使用官方统计数据
   - 准确度：**95%+** ✅

2. **估算算法兜底**（保留）
   - Ollama 等不返回 TokenUsage 的模型使用估算算法
   - 中文：约 **1.5 字符 / Token**
   - 英文：约 **4 字符 / Token**
   - SQL 代码：约 **3 字符 / Token**
   - 准确度：**70-80%**

#### 💰 成本分析功能（v2.3.2 新增）

**功能特性**：
- 📊 **总成本统计**：统计时间范围内的总成本（美元）
- 📈 **各模型成本分布**：饼图展示不同模型的成本占比
- 🔍 **各 Agent 成本分布**：主治医生、推理专家、编码专家的成本分析
- 📉 **Token 组成分析**：输入 Token vs 输出 Token 的条形图对比

**模型定价配置**（`application.yml`）：
```yaml
db-doctor:
  ai:
    cost:
      model-pricing:
        gpt-4o:
          input-price: 5.0      # $5 / 百万输入 Token
          output-price: 15.0    # $15 / 百万输出 Token
          provider: openai
        deepseek-chat:
          input-price: 0.14
          output-price: 0.28
          provider: deepseek
        qwen:  # Ollama 本地模型
          input-price: 0.0      # 免费
          output-price: 0.0
          provider: ollama
```

**相关文档**：
- 📄 `docs/AI监控功能增强设计文档.md` - 功能设计文档
- 📄 `docs/LangChain4j升级影响分析报告.md` - 升级技术分析

### 5. 多渠道通知

- ✅ **邮件通知**：支持 SMTP 协议
- ✅ **钉钉机器人**：支持 Webhook + 签名验证
- ✅ **飞书机器人**：支持 Webhook
- ✅ **企业微信**：支持 Webhook

### 5. 工业级可靠性

- ✅ **优雅停机**：等待任务完成后关闭
- ✅ **任务重试**：PENDING 任务自动补扫
- ✅ **自动清理**：定期清理历史分析数据
- ✅ **线程池隔离**：AI 分析与业务逻辑隔离

---

## 📊 使用示例

### 启动日志

```
========================================
🚀 开始 MySQL 环境准入检测...
========================================

📋 环境检查报告
========================================
✅ PASS | slow_query_log | ON
✅ PASS | log_output | TABLE
⚠️  WARN | long_query_time | 10.000000 秒
✅ PASS | mysql.slow_log 访问权限 | 有权限
========================================
检查结果：通过 3，警告 1，失败 0，错误 0
========================================

✅ 环境检查通过，DB-Doctor 可以正常工作！
```

### 慢查询通知示例

```
🔴 [慢查询告警] 数据库性能异常

SQL 语句：
SELECT * FROM users WHERE username LIKE '%1234%'

📊 性能指标：
  - 查询耗时：3.5 秒
  - 锁等待时间：0.01 秒
  - 扫描行数：10000 行
  - 返回行数：5 行

🤖 AI 分析：
  1. 问题根因：使用了前导模糊查询（LIKE '%...%'），导致全表扫描
  2. 优化建议：
     - 避免使用前导通配符，改为 LIKE '1234%'
     - 考虑添加全文索引
     - 使用 Elasticsearch 等搜索引擎

📈 统计信息：
  - 出现次数：第 1 次
  - 平均耗时：3.5 秒
  - 最大耗时：3.5 秒
```

---

## 🧪 测试数据库

项目提供了完整的性能测试数据库脚本：

### 1. 初始化测试数据库

```bash
mysql -u root -p < src/main/resources/test-db-setup.sql
```

**包含内容**：
- ✅ 7 张表（users、products、orders、order_items、categories、user_behavior_logs、payments）
- ✅ 37万+ 条测试数据
- ✅ 覆盖各种字段类型（BIGINT、INT、DECIMAL、VARCHAR、TEXT、ENUM、BOOLEAN、JSON、DATE、DATETIME、TIMESTAMP）
- ✅ 自动配置慢查询阈值（0.5 秒）

### 2. 执行慢查询测试

```bash
mysql -u root -p test_db < src/main/resources/靶数据库.sql
```

**包含 20 种测试场景**：
- 全表扫描
- 模糊查询（LIKE %...%）
- 深度分页
- 复杂 JOIN
- 子查询
- 聚合查询
- 排序查询
- 无索引字段查询
- 等...

---

## 🛠️ 开发指南

### 项目结构

```
db-doctor/
├── src/main/java/com/dbdoctor/
│   ├── agent/          # AI Agent 实现
│   ├── check/          # 环境检查器
│   ├── config/         # 配置类
│   ├── lifecycle/      # 生命周期管理
│   ├── model/          # 数据模型
│   ├── repository/     # JPA Repository
│   ├── service/        # 业务服务
│   └── util/           # 工具类
├── src/main/resources/
│   ├── application.yml           # 主配置文件
│   ├── application-local.yml     # 本地配置（不提交）
│   ├── test-db-setup.sql         # 测试数据库初始化脚本
│   └── 靶数据库.sql     # 慢查询测试语句
└── scripts/            # 启动脚本
```

### 代码规范

- ✅ 遵循阿里巴巴 Java 开发规范
- ✅ 禁止硬编码，所有参数从配置文件读取
- ✅ 使用 Slf4j 日志框架，禁止 System.out.println
- ✅ 异常必须记录日志并重新抛出
- ✅ 所有 public 类和方法必须添加 JavaDoc

### 编译构建

```bash
# 编译
mvn clean compile

# 测试
mvn test

# 打包
mvn clean package

# 跳过测试打包
mvn clean package -DskipTests
```

---

## 📈 版本历史

### v2.1.0 (2024-01-28)

- 🐛 修复配置文件中 notify 和 mail 配置重复的问题
- 🎨 优化数据库设计，移除 SlowQueryTemplate.severityLevel 字段
- 📝 完善项目文档，更新作者信息
- ✅ 代码规范优化，确保所有参数可配置化

### v2.0.0 (2024-01-26)

- ✨ 新增动态环境感知功能
- ✨ 新增 SQL 指纹去重机制
- ✨ 新增 PENDING 任务重试
- ✨ 新增自动清理功能
- 🐛 修复优雅停机问题
- 📝 完善测试数据库和文档

### v1.0.0 (2024-01-22)

- 🎉 首次发布
- ✨ 实现慢查询监控功能
- ✨ 实现 AI 智能分析
- ✨ 实现多渠道通知

---

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

1. Fork 本项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'feat: 添加某功能'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 提交 Pull Request

---

## 📄 许可证

本项目基于 [MIT License](LICENSE) 开源。

---

## 📮 联系方式

- **作者**：hanpf
- **邮箱**：2391303768@qq.com
- **项目地址**：https://github.com/your-username/db-doctor

---

## ⭐ Star History

如果这个项目对你有帮助，请给我们一个 Star ⭐

---

<div align="center">

**Made with ❤️ by hanpf**

</div>
