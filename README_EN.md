<div align="center">

<img src="frontend/public/logo.png" alt="DB-Doctor Logo" width="120" height="120" />

**English | [简体中文](README.md)**

# DB-Doctor

### **MySQL Database "Autopilot" Diagnosis Platform Powered by DeepSeek/LLM**

**Say goodbye to slow queries, let DB-Doctor give your database an intelligent doctor!**

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green.svg)](https://spring.io/projects/spring-boot)
[![LangChain4j](https://img.shields.io/badge/LangChain4j-0.36.1-blue.svg)](https://docs.langchain4j.dev)
[![Vue 3](https://img.shields.io/badge/Vue-3.x-brightgreen.svg)](https://vuejs.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

**[Quick Start](#-quick-start)** •
**[Features](#-core-features)** •
**[Architecture](#-system-architecture)** •
**[Online Demo](#-online-demo)** •
**[Contributing](#-contributing-guide)**

</div>

---

## Project Overview

**DB-Doctor** is a non-intrusive MySQL slow query intelligent diagnosis system based on **multi-AI Agent collaboration**. It monitors MySQL slow query logs in real-time and uses large language models (DeepSeek/Ollama/Qwen/etc.) to analyze root causes and push optimization recommendations.

---

## Feature Demo

<p align="center">
  <img src="demo.gif" width="800" alt="DB-Doctor Feature Demo" />
</p>

**Demo Content**:
1. Automatic detection and capture of slow queries
2. AI Agent intelligent root cause analysis
3. Generate optimization recommendations and execution plans
4. AI Token cost monitoring

---

### Why Choose DB-Doctor?

| Feature | DB-Doctor | Traditional Solutions |
|---------|-----------|----------------------|
| **Data Security** | Supports Ollama local deployment, data never leaves your domain | Requires uploading to cloud |
| **Business Intrusion** | Read-only access, zero business intrusion | Requires modifying business code or importing SDK |
| **Diagnosis Efficiency** | AI automatic analysis, second-level response | Manual troubleshooting, time-consuming |
| **Accuracy** | Multi-Agent collaboration, deep reasoning | Experience-based rules, high false positive rate |
| **Deployment Cost** | Docker one-click deployment, 5 minutes to get started | Complex configuration, high learning curve |

---

## Core Features

### **Multi-AI Agent Collaboration Mechanism**
- **Attending Physician Agent**: Preliminary diagnosis, collect evidence (table structure, execution plan, index information)
- **Reasoning Expert Agent**: Deep reasoning, find root causes (escalate analysis for complex issues)
- **Coding Expert Agent**: Generate optimization code (index creation, SQL rewriting)

### **Data Never Leaves Your Domain**
- Supports **Ollama + DeepSeek R1** local deployment
- Supports Qwen, OpenAI, and other cloud models
- Automatic sensitive data masking (SQL parameters, passwords, etc.)

### **Non-Intrusive Design**
- Read-only access to MySQL, zero business intrusion
- No need to modify business code or import JAR packages
- Standalone deployment, zero impact on existing systems

### **Intelligent Deduplication & Aggregation**
- Uses Druid SQL fingerprint calculation to automatically identify duplicate SQL
- Template + Sample dual-table architecture to avoid notification spam
- Incremental statistics to track performance trends

### **AI Monitoring & Cost Analysis**
- Complete Token statistics (input/output/total cost)
- Multi-model cost analysis (DeepSeek, GPT-4, Qwen, etc.)
- Single analysis tracking (traceId correlates complete analysis chain)
- Error classification and circuit breaker protection

### **Multi-Channel Notifications**
- Email notifications (SMTP)
- DingTalk bot (Webhook + signature verification)
- Feishu bot (Webhook)
- Enterprise WeChat (Webhook)

---

## System Architecture

```
DB-Doctor System Architecture
├────────────────┐    ┌────────────────┐    ┌────────────────┐
│   Vue 3 UI     │    │  Spring Boot   │    │   AI Agents    │
│                │◄──►│     Backend     │◄──►│  (LangChain4j) │
│  - Dashboard   │    │                │    │                │
│  - Report List │    │  - REST API    │    │  - Attending   │
│  - AI Monitor  │    │  - Scheduled    │    │    Physician   │
│  - Config Ctr  │    │  - Async Proc   │    │  - Reasoning   │
└────────────────┘    └────────────────┘    │  - Coding       │
                                │             └────────────────┘
                    ┌───────────┴───────────┐
                    │                       │
            ┌───────▼────────┐      ┌───────▼────────┐
            │  H2 Database   │      │  MySQL Target   │
            │  (Internal)     │      │  (Read-Only)    │
            │                │      │                │
            │  - Template    │      │  - slow_log    │
            │  - Sample      │      │  - information │
            │  - AI Logs     │      │    _schema     │
            │  - System Cfg  │      │                │
            └────────────────┘      └────────────────┘
```

### Core Business Flow

```
Slow Query Processing Complete Flow
1. Data Collection → 2. Processing → 3. AI Analysis → 4. Notify → 5. Batch Send

Poll mysql.slow_log → SQL Fingerprint → Multi-Agent → Smart Strategy → Batch Send
(Every 5 sec)      → Deduplication → Collaboration → Scheduled Delivery
```

---

## Quick Start

### Method 1: Docker Deployment (Recommended)

**Ready to use, 5 minutes to get started!**

```bash
# 1. Pull image
docker pull hanpf23/db-doctor:0.1.0

# 2. Run container
docker run -d \
  --name db-doctor \
  -p 8080:8080 \
  -v $(pwd)/data:/app/data \
  -v $(pwd)/logs:/app/logs \
  hanpf23/db-doctor:0.1.0

# 3. Access Web UI
open http://localhost:8080
```

**Or use docker-compose:**

```bash
# 1. Download docker-compose.yml
wget https://raw.githubusercontent.com/hanpf2391/DB-Doctor/main/docker-compose.yml

# 2. Start
docker-compose up -d

# 3. View logs
docker-compose logs -f
```

**Advantages**:
- No need to install Java, Maven, or other environments
- One-click startup, automatic configuration
- Isolated operation, no impact on existing systems
- Data persistence, no data loss on restart

---

### Method 2: Download Release Package

**Suitable for production deployment, no compilation required!**

#### For Windows Users

```cmd
# 1. Download release package
Visit: https://github.com/hanpf2391/DB-Doctor/releases

# 2. Extract and enter directory
cd DB-Doctor-v0.1.0

# 3. Double-click to run startup script
scripts\start.bat
```

#### For Linux/Mac Users

```bash
# 1. Download release package
wget https://github.com/hanpf2391/DB-Doctor/releases/download/v0.1.0/db-doctor.jar

# 2. Run
java -jar db-doctor.jar

# Or use startup script
chmod +x scripts/start.sh
./scripts/start.sh
```

**Advantages**:
- No need to install Maven, Node.js
- Ready to use, includes frontend and backend
- Single JAR file, simple deployment
- Suitable for production environments

---

### Method 3: Local Development

#### Prerequisites

- **JDK 17+**
- **Maven 3.6+**
- **MySQL 5.7+ / 8.0+**
- **Node.js 16+** (for frontend development)

#### 1. Clone the Project

```bash
git clone https://github.com/hanpf2391/DB-Doctor.git
cd DB-Doctor
```

#### 2. Configure MySQL Slow Query Log

**Method 1: Automatic Check (Recommended)**

DB-Doctor will automatically check MySQL configuration on startup and provide fix suggestions.

**Method 2: Manual Configuration**

```sql
-- Enable slow query log
SET GLOBAL slow_query_log = 'ON';
SET GLOBAL log_output = 'TABLE';
SET GLOBAL long_query_time = 2.0;

-- Verify configuration
SHOW VARIABLES LIKE 'slow_query%';
SHOW VARIABLES LIKE 'long_query_time';
```

#### 3. Start Backend

```bash
# Method 1: Maven run
mvn spring-boot:run

# Method 2: Package and run
mvn clean package -DskipTests
java -jar target/db-doctor-3.0.0.jar
```

#### 4. Start Frontend (Development Mode)

```bash
cd frontend

# Install dependencies
npm install
# or use pnpm
pnpm install

# Start dev server
npm run dev
```

Access: http://localhost:5173

#### 5. Access Web UI for Configuration

After successful startup, visit: http://localhost:8080

**First-time setup requires Web UI configuration**:
1. Click "Configuration Center" → "Data Source Configuration"
2. Fill in MySQL database connection information (host, port, username, password)
3. Click "Configure AI" → Select AI model (DeepSeek/Ollama/Qwen/etc.)
4. Fill in API Key or local model address
5. Click "Test Connection" to verify configuration
6. Click "Reload Configuration" to apply

**Supported hot-reload configurations**:
- Database connection information (supports dynamic switching without restart)
- AI model configuration (supports DeepSeek, Ollama, Qwen, OpenAI, etc.)
- Notification channel configuration (Email, DingTalk, Feishu, Enterprise WeChat)

---

## Feature Details

### 1. Multi-AI Agent Collaboration Mechanism

DB-Doctor uses **ReAct (Reasoning + Acting)** pattern with multiple agents working together:

| Agent | Role | Responsibility | Trigger Condition |
|-------|------|----------------|-------------------|
| **Attending Physician** | Primary Diagnosis | Collect evidence (table structure, execution plan, index info) | All slow queries |
| **Reasoning Expert** | Advanced Diagnosis | Deep reasoning, find root causes | Complex issues (high freq/severe/lock wait/full table scan) |
| **Coding Expert** | Optimization Implementation | Generate optimization code (index creation, SQL rewriting) | Issues needing upgrade |

**Escalation Trigger Conditions**:
- High-frequency SQL: >100 occurrences in 24 hours
- Severe slow query: Average time >3 seconds
- Lock wait issue: Average lock wait >0.1 seconds
- Suspected full table scan: Scan/Return ratio >1000

### 2. SQL Fingerprint Deduplication

Uses **Druid SQL Parser** to calculate SQL fingerprint (MD5), automatically identifying parameterized SQL patterns:

```java
// Example: These SQLs will be identified as the same fingerprint
SELECT * FROM users WHERE id = 1;
SELECT * FROM users WHERE id = 2;
SELECT * FROM users WHERE id = 100;

// SQL Fingerprint: md5("SELECT * FROM users WHERE id = ?")
// SQL Template: SELECT * FROM users WHERE id = ?
```

### 3. Template + Sample Dual-Table Architecture

- **Template Table**: Stores SQL templates and aggregated statistics (avg time, max time, occurrence count, etc.)
- **Sample Table**: Stores specific SQL for each capture (preserves complete history)

**Advantages**:
- Avoid duplicate analysis
- Reduce storage space
- Track performance trends

### 4. Dynamic Data Source Hot Reload

After modifying database configuration, no need to restart the application. Click "Reload Config" to take effect:

```java
// Config hot reload process
1. User modifies config in Web UI
2. Save to H2 database system_config table
3. DynamicDataSourceManager detects config change
4. Close old data source, initialize new one
5. Atomic reference update (thread-safe)
```

### 5. AI Monitoring & Cost Analysis

- **Token Statistics**: Input/Output/Total token counts
- **Cost Analysis**: Classify and analyze costs by model and agent
- **Error Classification**: BLOCKING, TRANSIENT, PERMANENT
- **Circuit Breaker Protection**: Auto-circuit after N consecutive failures to avoid wasting tokens

---

## Usage Example

### Slow Query Notification Example

```
[Slow Query Alert] Database Performance Anomaly

SQL Statement:
SELECT * FROM users WHERE username LIKE '%1234%'

Performance Metrics:
  - Query Time: 3.5 seconds
  - Lock Wait Time: 0.01 seconds
  - Rows Examined: 10,000
  - Rows Sent: 5

AI Analysis:
  1. Root Cause: Leading wildcard query (LIKE '%...%') causes full table scan
  2. Optimization Suggestions:
     - Avoid leading wildcards, use LIKE '1234%' instead
     - Consider adding full-text index
     - Use Elasticsearch or other search engines

Statistics:
  - Occurrences: 1st time
  - Avg Query Time: 3.5 seconds
  - Max Query Time: 3.5 seconds
```

---

## Tech Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| **Backend Framework** | Spring Boot | 3.2.2 |
| **Java Version** | OpenJDK | 17 |
| **AI Framework** | LangChain4j | 0.36.1 |
| **Database** | H2 (Internal) + MySQL (Target) | - |
| **Frontend Framework** | Vue 3 + Element Plus | 3.x |
| **SQL Parser** | Druid | 1.2.20 |
| **Utility Libraries** | Hutool, FastJSON2 | 5.8.27, 2.0.47 |

---

## Test Database

The project provides complete performance test database scripts:

### 1. Initialize Test Database

```bash
mysql -u root -p < src/main/resources/test-db-setup.sql
```

**Includes**:
- 7 tables (users, products, orders, order_items, categories, user_behavior_logs, payments)
- 370,000+ test records
- Covers various field types
- Auto-configures slow query threshold (0.5 seconds)

### 2. Execute Slow Query Tests

```bash
mysql -u root -p test_db < src/main/resources/靶数据库.sql
```

**Includes 20 test scenarios**:
- Full table scan
- Fuzzy query (LIKE %...%)
- Deep pagination
- Complex JOIN
- Subquery
- Aggregation query
- Sorting query
- Non-indexed field query
- And more...

---

## Roadmap

### Current Release

- **v0.1.0** - First official release (February 2026)
  - Docker deployment support
  - Release package download
  - Multi-AI Agent collaboration
  - SQL fingerprint deduplication
  - Multi-channel notifications (Email, DingTalk, Feishu, Enterprise WeChat)
  - Config hot reload (Dynamic data source + AI model switching)

### In Development

- **v0.2.0** - Report export (PDF/Word)
  - [ ] Generate PDF format diagnosis report
  - [ ] Generate Word format optimization plan
  - [ ] Support batch export

### Planned

- **v0.3.0** - Custom notification rules
  - [ ] Intelligent notification based on severity
  - [ ] Time-based notification strategy
  - [ ] Multi-channel notification orchestration

- **v1.0.0** - Multi-tenant support
  - [ ] Monitor multiple MySQL instances
  - [ ] Tenant isolation and permission management
  - [ ] Cross-instance performance analysis

- **v4.1.0** - PostgreSQL support
- **v4.2.0** - Distributed deployment (message queue decoupling)

---

## Contributing Guide

Issues and Pull Requests are welcome!

### Contribution Process

1. Fork this project
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'feat: Add some feature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Submit a Pull Request

### Code Standards

- Follow Alibaba Java Coding Guidelines
- No hardcoding, all parameters from config files
- Use Slf4j logging framework, no System.out.println
- Exceptions must be logged and re-thrown
- All public classes and methods must have JavaDoc

---

## Acknowledgments

Thanks to the following open source projects and tech communities:

- [DeepSeek](https://www.deepseek.com/) - Open source large language model
- [Ollama](https://ollama.com/) - Local model runtime tool
- [LangChain4j](https://docs.langchain4j.dev/) - Java AI framework
- [Spring Boot](https://spring.io/projects/spring-boot) - Java development framework
- [Vue 3](https://vuejs.org/) - Progressive frontend framework
- [Element Plus](https://element-plus.org/) - Vue 3 component library
- [Druid](https://github.com/alibaba/druid) - SQL parser

---

## Contact

- **Author**: hanpf
- **Email**: 2391303768@qq.com
- **GitHub**: https://github.com/hanpf2391/DB-Doctor
- **Gitee**: https://gitee.com/hanpf2391/DB-Doctor

---

## License

This project is open sourced under [MIT License](LICENSE).

---

## Internationalization

- [中文文档](README.md)
- [English Documentation](README_EN.md)

---

<div align="center">

## Downloads & Deployment

### Docker Hub

- **Image URL**: [hanpf23/db-doctor](https://hub.docker.com/r/hanpf23/db-doctor)
- **Quick Start**:
  ```bash
  docker pull hanpf23/db-doctor:0.1.0
  docker run -d -p 8080:8080 hanpf23/db-doctor:0.1.0
  ```

### GitHub Release

- **Release Page**: [DB-Doctor Releases](https://github.com/hanpf2391/DB-Doctor/releases)
- **Downloads**:
  - [db-doctor.jar](https://github.com/hanpf2391/DB-Doctor/releases/download/v0.1.0/db-doctor.jar) (Recommended for production)
  - [db-doctor.jar.sha256](https://github.com/hanpf2391/DB-Doctor/releases/download/v0.1.0/db-doctor.jar.sha256) (Checksum file)

**Made with love by hanpf**

**Give every database an intelligent doctor**

---

## Star History

If this project helps you, please give us a Star

[![Star History Chart](https://api.star-history.com/svg?repos=hanpf2391/DB-Doctor&type=Date)](https://star-history.com/#hanpf2391/DB-Doctor&Date)

</div>
