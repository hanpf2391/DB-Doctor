# 🏥 DB-Doctor

<div align="center">

**一款非侵入式的 MySQL 慢查询智能诊疗系统**

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green.svg)](https://spring.io/projects/spring-boot)
[![LangChain4j](https://img.shields.io/badge/LangChain4j-0.29.1-blue.svg)](https://docs.langchain4j.dev)
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
db-doctor:
  target-db:
    url: jdbc:mysql://localhost:3306/information_schema
    username: your_mysql_username
    password: your_mysql_password
```

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

### 4. 多渠道通知

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
mysql -u root -p test_db < src/main/resources/test-slow-queries.sql
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
│   └── test-slow-queries.sql     # 慢查询测试语句
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

- **作者**：DB-Doctor Team
- **邮箱**：support@dbdoctor.com
- **官网**：https://dbdoctor.com

---

## ⭐ Star History

如果这个项目对你有帮助，请给我们一个 Star ⭐

---

<div align="center">

**Made with ❤️ by DB-Doctor Team**

</div>
