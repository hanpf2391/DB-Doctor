<div align="center">

<img src="frontend/public/logo.png" alt="DB-Doctor Logo" width="120" height="120" />

# 🏥 DB-Doctor

### **基于 DeepSeek/LLM 的 MySQL 数据库"自动驾驶"诊疗平台**

**告别慢查询，DB-Doctor 让您的数据库拥有智能医生！**

![][github-stars-shield]
![][github-forks-shield]
![][github-issues-shield]
![][github-license-shield]

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green.svg)](https://spring.io/projects/spring-boot)
[![LangChain4j](https://img.shields.io/badge/LangChain4j-0.36.1-blue.svg)](https://docs.langchain4j.dev)
[![Vue 3](https://img.shields.io/badge/Vue-3.x-brightgreen.svg)](https://vuejs.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

[![Star History Chart](https://api.star-history.com/svg?repos=hanpf2391/DB-Doctor&type=Date)](https://star-history.com/#hanpf2391/DB-Doctor&Date)

**[快速开始](#-快速开始)** •
**[功能特性](#-核心特性)** •
**[架构设计](#-系统架构)** •
**[在线演示](#-在线演示)** •
**[贡献指南](#-贡献指南)**

</div>

---

## 📖 项目简介

**DB-Doctor** 是一款基于**多 AI Agent 协作**的非侵入式 MySQL 慢查询智能诊疗系统。通过实时监听 MySQL 慢查询日志，利用大语言模型（DeepSeek/Ollama/通义千问等）分析根因并推送优化建议。

> **💡 AI 声明**：本项目在开发过程中得到了 Claude Code、Gemini、gml等 AI 工具的辅助。作者持续学习优化中，欢迎提 Issue 和 PR！

---

## 💎 核心卖点

### Why DB-Doctor?

- 🔒 **数据不出域**：支持 Ollama + 本地 DeepSeek R1 模型，绝对安全，企业级应用首选
- 🚀 **非侵入性**：无需改业务代码，无需引入 Jar 包，独立部署，对现有系统零影响
- 🤖 **AI 加持**：DeepSeek R1 / 通义千问 / GPT-4 多模型支持，高精准诊断
- 🧠 **多 Agent 协作**：主治医生 + 推理专家 + 编码专家，深度推理解决复杂问题
- ⚡ **开箱即用**：Docker 一键部署，5分钟上手
- 🎯 **解放 DBA**：从繁琐的慢查询诊断中解脱，提升 10 倍效率

---

### 🎯 为什么选择 DB-Doctor？

| 特性 | DB-Doctor | 传统方案 |
|------|-----------|---------|
| **数据安全** | ✅ 支持 Ollama 本地化部署，数据不出域 | ❌ 需上传到云端 |
| **业务侵入** | ✅ 只读访问，零业务侵入 | ⚠️ 需要修改业务代码或引入 SDK |
| **诊断效率** | ✅ AI 自动分析，秒级响应 | ❌ 人工排查，耗时长 |
| **精准度** | ✅ 多 Agent 协作，深度推理 | ⚠️ 经验规则，误报率高 |
| **部署成本** | ✅ Docker 一键部署，5分钟上手 | ❌ 复杂配置，学习成本高 |

---

## ✨ 核心特性

### 🤖 **多 AI Agent 协作机制**
- **主治医生 Agent**：初步诊断，收集证据（表结构、执行计划、索引信息）
- **推理专家 Agent**：深度推理，找到根因（复杂问题升级分析）
- **编码专家 Agent**：生成优化代码（索引创建、SQL 重写）

### 🔒 **数据不出域**
- ✅ 支持 **Ollama + DeepSeek R1** 本地化部署
- ✅ 支持通义千问、OpenAI 等云端模型
- ✅ 敏感数据自动脱敏（SQL 参数、密码等）

### 🚀 **非侵入式设计**
- ✅ 只读访问 MySQL，零业务侵入
- ✅ 无需修改业务代码，无需引入 Jar 包
- ✅ 独立部署，对现有系统零影响

### 🧠 **智能去重与聚合**
- ✅ 使用 Druid SQL 指纹计算，自动识别重复 SQL
- ✅ Template + Sample 双表架构，避免通知轰炸
- ✅ 增量统计，追踪性能趋势

### 📊 **AI 监控与成本分析**
- ✅ 完整的 Token 统计（输入/输出/总成本）
- ✅ 多模型成本分析（DeepSeek、GPT-4、通义千问等）
- ✅ 单次分析追踪（traceId 关联完整分析链路）
- ✅ 错误分类与熔断保护

### 📢 **多渠道通知**
- ✅ 邮件通知（SMTP）
- ✅ 钉钉机器人（Webhook + 签名验证）
- ✅ 飞书机器人（Webhook）
- ✅ 企业微信（Webhook）

---

## 📌 项目状态

**当前版本**：**v3.1.0**（2025年2月更新）

**最新动态**：

- [x] **v3.1.0** - 通知功能完善（邮件、钉钉、飞书、企业微信）
- [x] **v3.0.0** - 动态数据源 + 配置热加载功能
- [x] **v2.0.0** - SQL 指纹去重 + Template/Sample 双表架构
- [x] **v1.0.0** - 基础慢查询监控 + AI 分析功能

**即将发布**：

- [ ] **v3.2.0** - 报告导出（PDF/Word）- 开发中
- [ ] **v3.3.0** - 自定义通知规则 - 计划中
- [ ] **v4.0.0** - 多租户支持（监控多个 MySQL 实例）- 规划中

项目处于**活跃开发**阶段，代码持续更新中。更多迭代进度请阅读 [CHANGELOG.md](frontend/CHANGELOG.md)

---

## 🎬 演示视频

<!-- GIF 演示占位符 -->
<!--
<p align="center">
  <img src="docs/demo.gif" width="800" alt="DB-Doctor 演示" />
</p>

**演示说明**：
1. 发现慢查询（或模拟一个）
2. DB-Doctor AI Agent 思考分析过程
3. 给出索引建议/优化方案
4. （可选）效果对比
-->

<!-- B站视频占位符 -->
### 📺 完整演示视频
[![B站视频封面](docs/video-cover.png)](https://www.bilibili.com/video/XXXXX)

**点击观看**：[DB-Doctor 完整演示](https://www.bilibili.com/video/XXXXX)（录制中...）

---

## 🏗️ 系统架构

```
┌─────────────────────────────────────────────────────────────────────┐
│                         DB-Doctor 系统架构                            │
├─────────────────────────────────────────────────────────────────────┤
│  ┌────────────────┐    ┌────────────────┐    ┌────────────────┐   │
│  │   Vue 3 前端    │    │  Spring Boot   │    │   AI Agents    │   │
│  │                │◄──►│     后端        │◄──►│  (LangChain4j) │   │
│  │  - Dashboard   │    │                │    │                │   │
│  │  - 报告列表     │    │  - REST API    │    │  - 主治医生     │   │
│  │  - AI 监控     │    │  - 定时任务     │    │  - 推理专家     │   │
│  │  - 配置中心     │    │  - 异步处理     │    │  - 编码专家     │   │
│  └────────────────┘    └────────────────┘    └────────────────┘   │
│                                │                                   │
└────────────────────────────────┼───────────────────────────────────┘
                                 │
                    ┌────────────┴────────────┐
                    │                         │
            ┌───────▼────────┐       ┌────────▼────────┐
            │  H2 数据库      │       │  MySQL 目标库   │
            │  (内部数据)     │       │  (只读访问)     │
            │                │       │                │
            │  - Template 表 │       │  - slow_log    │
            │  - Sample 表    │       │  - information │
            │  - AI 调用日志  │       │    _schema     │
            │  - 系统配置     │       │                │
            └────────────────┘       └────────────────┘
```

### 核心业务流程

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        慢查询处理完整流程                                 │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  1. 数据采集 → 2. 数据处理 → 3. AI 分析 → 4. 通知判断 → 5. 批量通知    │
│                                                                         │
│  轮询 mysql.slow_log  →  SQL 指纹去重  →  多 Agent 协作  →  智能策略  │
│  (每 5 秒检查)       →  Template+Sample →  主治/推理/编码  →  定时发送  │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 🚀 快速开始

### 🐳 方式 1：Docker 部署（推荐）⚡️

**开箱即用，5 分钟上手！**

<!-- Docker 镜像占位符 -->
```bash
# 1. 拉取镜像（镜像构建中...）
docker pull ghcr.io/hanpf2391/db-doctor:latest

# 2. 运行容器
docker run -d \
  --name db-doctor \
  -p 8080:8080 \
  -e LANGCHAIN4J_OPEN_AI_API_KEY=your-api-key \
  -e SPRING_DATASOURCE_PASSWORD=your-mysql-password \
  ghcr.io/hanpf2391/db-doctor:latest

# 3. 访问 Web 界面
open http://localhost:8080
```

**优势**：
- ✅ 无需安装 Java、Maven 等环境
- ✅ 一键启动，自动配置
- ✅ 隔离运行，不影响现有系统

### 方式 2：本地开发运行

#### 环境要求

- **JDK 17+**
- **Maven 3.6+**
- **MySQL 5.7+ / 8.0+**
- **Node.js 16+** (前端开发)
- **LLM API Key**（DeepSeek/Ollama/通义千问/OpenAI）

#### 1. 克隆项目

```bash
git clone https://github.com/hanpf2391/DB-Doctor.git
cd DB-Doctor
```

#### 2. 配置数据库

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

#### 3. 配置 AI 模型

**选项 A：使用 DeepSeek（推荐，性价比高）**

```yaml
langchain4j:
  open-ai:
    chat-model:
      api-key: sk-your-deepseek-api-key
      base-url: https://api.deepseek.com/v1
      model-name: deepseek-chat
      temperature: 0.0
```

**选项 B：使用 Ollama 本地模型（数据不出域）**

```yaml
langchain4j:
  open-ai:
    chat-model:
      base-url: http://localhost:11434/v1
      model-name: deepseek-r1:7b
      temperature: 0.0
```

先安装 Ollama 并下载 DeepSeek R1 模型：

```bash
# 安装 Ollama（Mac/Linux）
curl -fsSL https://ollama.com/install.sh | sh

# Windows 下载安装包
# https://ollama.com/download

# 下载 DeepSeek R1 模型（7B 版本约 4GB）
ollama pull deepseek-r1:7b

# 验证安装
ollama run deepseek-r1:7b
```

**选项 C：使用阿里云通义千问**

```yaml
langchain4j:
  open-ai:
    chat-model:
      api-key: sk-your-qwen-api-key
      base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
      model-name: qwen-plus
```

#### 4. 配置慢查询日志

**方式 1：自动检查（推荐）**

DB-Doctor 启动时会自动检查 MySQL 配置，并给出修复建议。

**方式 2：手动配置**

```sql
-- 开启慢查询日志
SET GLOBAL slow_query_log = 'ON';
SET GLOBAL log_output = 'TABLE';
SET GLOBAL long_query_time = 2.0;

-- 验证配置
SHOW VARIABLES LIKE 'slow_query%';
SHOW VARIABLES LIKE 'long_query_time';
```

#### 5. 启动后端

```bash
# 方式 1：Maven 运行
mvn spring-boot:run

# 方式 2：打包后运行
mvn clean package -DskipTests
java -jar target/db-doctor-3.0.0.jar
```

#### 6. 启动前端（开发模式）

```bash
cd frontend

# 安装依赖
npm install
# 或使用 pnpm
pnpm install

# 启动开发服务器
npm run dev
```

访问：http://localhost:5173

#### 7. 访问 Web 界面

启动成功后，访问：http://localhost:8080

<!-- 截图展示占位符 -->
<!--
<p align="center">
  <img src="docs/screenshots/dashboard.png" width="800" alt="仪表盘" />
  <img src="docs/screenshots/report-list.png" width="800" alt="报告列表" />
  <img src="docs/screenshots/ai-monitor.png" width="800" alt="AI 监控" />
</p>
-->

---

## 📚 功能详解

### 1. 多 AI Agent 协作机制

DB-Doctor 采用 **ReAct (Reasoning + Acting)** 模式，多个 Agent 协同工作：

| Agent | 角色 | 职责 | 触发条件 |
|-------|------|------|---------|
| **主治医生** | 初级诊断 | 收集证据（表结构、执行计划、索引信息） | 所有慢查询 |
| **推理专家** | 高级诊断 | 深度推理，找到根因 | 复杂问题（高频/严重/锁等待/全表扫描） |
| **编码专家** | 优化实施 | 生成优化代码（索引创建、SQL 重写） | 需要升级的问题 |

**升级触发条件**：
- 高频 SQL：24 小时内出现 > 100 次
- 严重慢查询：平均耗时 > 3 秒
- 锁等待问题：平均锁等待 > 0.1 秒
- 疑似全表扫描：扫描/返回 > 1000

### 2. SQL 指纹去重

使用 **Druid SQL 解析器**计算 SQL 指纹（MD5），自动识别参数化 SQL 模板：

```java
// 示例：以下 SQL 会被识别为同一个指纹
SELECT * FROM users WHERE id = 1;
SELECT * FROM users WHERE id = 2;
SELECT * FROM users WHERE id = 100;

// SQL 指纹：md5("SELECT * FROM users WHERE id = ?")
// SQL 模板：SELECT * FROM users WHERE id = ?
```

### 3. Template + Sample 双表架构

- **Template 表**：存储 SQL 模板和聚合统计（平均耗时、最大耗时、出现次数等）
- **Sample 表**：存储每次捕获的具体 SQL（保留完整历史）

**优势**：
- ✅ 避免重复分析
- ✅ 减少存储空间
- ✅ 追踪性能趋势

### 4. 动态数据源热加载

修改数据库配置后，无需重启应用，点击"重载配置"即可生效：

```java
// 配置热加载流程
1. 用户在 Web 界面修改配置
2. 保存到 H2 数据库 system_config 表
3. DynamicDataSourceManager 检测到配置变更
4. 关闭旧数据源，初始化新数据源
5. 原子引用更新（线程安全）
```

### 5. AI 监控与成本分析

- **Token 统计**：输入/输出/总 Token 数量
- **成本分析**：按模型和 Agent 分类统计成本
- **错误分类**：BLOCKING（阻塞）、TRANSIENT（瞬态）、PERMANENT（永久）
- **熔断保护**：连续失败 N 次后自动熔断，避免浪费 Token

---

## 📊 使用示例

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

## 🛠️ 技术栈

| 组件 | 技术选型 | 版本 |
|------|---------|------|
| **后端框架** | Spring Boot | 3.2.2 |
| **Java 版本** | OpenJDK | 17 |
| **AI 框架** | LangChain4j | 0.36.1 |
| **数据库** | H2（内部） + MySQL（目标） | - |
| **前端框架** | Vue 3 + Element Plus | 3.x |
| **SQL 解析** | Druid | 1.2.20 |
| **工具库** | Hutool, FastJSON2 | 5.8.27, 2.0.47 |

---

## 📄 配置说明

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
    poll-interval-ms: 5000           # 轮询间隔（毫秒）
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

## 🧪 测试数据库

项目提供了完整的性能测试数据库脚本：

### 1. 初始化测试数据库

```bash
mysql -u root -p < src/main/resources/test-db-setup.sql
```

**包含内容**：
- ✅ 7 张表（users、products、orders、order_items、categories、user_behavior_logs、payments）
- ✅ 37万+ 条测试数据
- ✅ 覆盖各种字段类型
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

## 📈 发展路线

### 已完成 ✅

- **v1.0.0** - 基础慢查询监控 + AI 分析功能
- **v2.0.0** - 动态环境感知 + SQL 指纹去重
- **v3.0.0** - 动态数据源 + 配置热加载
- **v3.1.0** - 通知功能完善（邮件、钉钉、飞书、企业微信）

### 开发中 🚧

- **v3.2.0** - 报告导出（PDF/Word）
  - [ ] 生成 PDF 格式诊断报告
  - [ ] 生成 Word 格式优化方案
  - [ ] 支持批量导出

### 计划中 📋

- **v3.3.0** - 自定义通知规则
  - [ ] 基于严重程度的智能通知
  - [ ] 基于时间段的通知策略
  - [ ] 多渠道通知编排

- **v4.0.0** - 多租户支持
  - [ ] 监控多个 MySQL 实例
  - [ ] 租户隔离与权限管理
  - [ ] 跨实例性能分析

- **v4.1.0** - PostgreSQL 支持
- **v4.2.0** - 分布式部署（消息队列解耦）

---

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

### 贡献流程

1. Fork 本项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'feat: 添加某功能'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 提交 Pull Request

### 代码规范

- ✅ 遵循阿里巴巴 Java 开发规范
- ✅ 禁止硬编码，所有参数从配置文件读取
- ✅ 使用 Slf4j 日志框架，禁止 System.out.println
- ✅ 异常必须记录日志并重新抛出
- ✅ 所有 public 类和方法必须添加 JavaDoc

---

## 🙏 鸣谢

感谢以下开源项目和技术社区：

- [DeepSeek](https://www.deepseek.com/) - 开源大语言模型
- [Ollama](https://ollama.com/) - 本地模型运行工具
- [LangChain4j](https://docs.langchain4j.dev/) - Java AI 框架
- [Spring Boot](https://spring.io/projects/spring-boot) - Java 开发框架
- [Vue 3](https://vuejs.org/) - 渐进式前端框架
- [Element Plus](https://element-plus.org/) - Vue 3 组件库
- [Druid](https://github.com/alibaba/druid) - SQL 解析器

---

## 📮 联系方式

<!-- 微信群二维码占位符 -->
<!--
<div align="center">
  <img src="docs/wechat-qr-code.png" width="200" alt="微信交流群" />
  <p>扫码加入 DB-Doctor 技术交流群</p>
</div>
-->

- **作者**：hanpf
- **邮箱**：2391303768@qq.com
- **GitHub**：https://github.com/hanpf2391/DB-Doctor
- **Gitee**：https://gitee.com/hanpf2391/DB-Doctor

---

## 📄 许可证

本项目基于 [MIT License](LICENSE) 开源。

---

## ⭐ Star History

[![Star History Chart](https://api.star-history.com/svg?repos=hanpf2391/DB-Doctor&type=Date)](https://star-history.com/#hanpf2391/DB-Doctor&Date)

如果这个项目对你有帮助，请给我们一个 Star ⭐

---

## 🌍 国际化

- 🇨🇳 [中文文档](README.md)
- 🇺🇸 [English Documentation](README_EN.md) （翻译中...）

---

<div align="center">

**Made with ❤️ by hanpf**

**让每一个数据库都拥有智能医生**

</div>

<!-- LINK GROUP -->

[github-stars-shield]: https://img.shields.io/github/stars/hanpf2391/DB-Doctor?color=ffcb47&labelColor=black&style=flat-square
[github-forks-shield]: https://img.shields.io/github/forks/hanpf2391/DB-Doctor?color=8ae8ff&labelColor=black&style=flat-square
[github-issues-shield]: https://img.shields.io/github/issues/hanpf2391/DB-Doctor?color=ff80eb&labelColor=black&style=flat-square
[github-license-shield]: https://img.shields.io/github/license/hanpf2391/DB-Doctor?color=c4f042&labelColor=black&style=flat-square
