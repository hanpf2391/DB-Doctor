# DB-Doctor 数据表结构说明

## 概述

DB-Doctor 使用 **Template + Sample 双表架构** 来存储慢查询数据，这是一种经典的 1:N 关系设计：

- **SLOW_QUERY_TEMPLATE（模板表）**：一个 SQL 指纹对应 **1条** 记录
- **SLOW_QUERY_SAMPLE（样本表）**：一个 SQL 指纹对应 **N条** 记录

---

## 核心设计理念

### 为什么要分成两张表？

#### ❌ 单表设计的问题（旧架构）

```
SLOW_QUERY_HISTORY（单表）
- SQL1 (SELECT * FROM users WHERE id = 1)  → 第1次出现
- SQL1 (SELECT * FROM users WHERE id = 2)  → 第2次出现
- SQL1 (SELECT * FROM users WHERE id = 3)  → 第3次出现
```

**问题**：
1. ❌ 大量重复存储（SQL 模板、AI 分析报告等字段重复存储）
2. ❌ 去重困难（无法快速判断某个 SQL 是否首次出现）
3. ❌ 统计低效（计算出现次数需要扫描全表）
4. ❌ 存储浪费（同样的 AI 报告存储了 3 次）

#### ✅ 双表设计的优势（新架构）

```
SLOW_QUERY_TEMPLATE（模板表 - 1条）
├─ 指纹: MD5(...)
├─ SQL模板: SELECT * FROM users WHERE id = ?
├─ 首次发现: 2024-01-22 10:00:00
├─ 最后发现: 2024-01-22 15:30:00
├─ 出现次数: 3次（自动统计）
└─ AI分析报告: 1份

SLOW_QUERY_SAMPLE（样本表 - 3条）
├─ 样本1: id=1, 耗时=2.5s, 时间=10:00:00
├─ 样本2: id=2, 耗时=3.1s, 时间=12:00:00
└─ 样本3: id=3, 耗时=2.8s, 时间=15:30:00
```

**优势**：
1. ✅ 去重高效（通过 SQL 指纹快速判断是否为新 SQL）
2. ✅ 节省存储（SQL 模板、AI 报告只存储 1 次）
3. ✅ 统计快速（直接从 Template 表读取预聚合的统计信息）
4. ✅ 灵活分析（保留完整的历史执行数据）

---

## 表结构详解

### 1️⃣ SLOW_QUERY_TEMPLATE（模板表）

**核心职责**：存储 **SQL 结构** 和 **分析结果**

**关系性质**：1:1（一个 SQL 指纹对应一条记录）

#### 关键字段

**基本信息字段**

| 字段名 | 类型 | 说明 | 示例值 |
|--------|------|------|--------|
| `sql_fingerprint` | VARCHAR(64) | **SQL 指纹（MD5）** | `a3f5e8c9d2b1...` |
| `sql_template` | TEXT | **参数化后的 SQL 模板** | `SELECT * FROM users WHERE id = ?` |
| `db_name` | VARCHAR(255) | 数据库名 | `my_app_db` |
| `table_name` | VARCHAR(255) | 表名 | `users` |
| `first_seen_time` | DATETIME | **首次发现时间** | `2024-01-22 10:00:00` |
| `last_seen_time` | DATETIME | **最后发现时间** | `2024-01-22 15:30:00` |
| `status` | VARCHAR(20) | 分析状态 | `PENDING` / `SUCCESS` / `ERROR` |
| `ai_analysis_report` | CLOB | **AI 分析报告（Markdown）** | `# 慢查询分析报告\n...` |

**统计字段（V2.2.0 新增）**

| 字段名 | 类型 | 说明 | 示例值 |
|--------|------|------|--------|
| `occurrence_count` | BIGINT | **出现次数**（预聚合） | `150` |
| `avg_query_time` | DOUBLE | **平均查询耗时（秒）** | `3.523` |
| `max_query_time` | DOUBLE | **最大查询耗时（秒）** | `8.456` |
| `avg_lock_time` | DOUBLE | **平均锁等待时间（秒）** | `0.012` |
| `max_lock_time` | DOUBLE | **最大锁等待时间（秒）** | `0.150` |
| `avg_rows_sent` | DOUBLE | **平均返回行数** | `125.5` |
| `max_rows_sent` | BIGINT | **最大返回行数** | `5000` |
| `avg_rows_examined` | DOUBLE | **平均扫描行数** | `250000.8` |
| `max_rows_examined` | BIGINT | **最大扫描行数** | `1000000` |

**通知字段**

| 字段名 | 类型 | 说明 | 示例值 |
|--------|------|------|--------|
| `last_notified_time` | DATETIME | 最后通知时间 | `2024-01-22 14:00:00` |
| `last_notified_avg_time` | DOUBLE | 最后通知时的平均耗时 | `3.5` |

#### 核心特点

✅ **SQL 指纹唯一**：通过 MD5 哈希值判断 SQL 是否重复
✅ **存储 SQL 模板**：使用 `?` 作为参数占位符（Druid 参数化）
✅ **智能去重**：新 SQL 创建记录，老 SQL 只更新 `last_seen_time`
✅ **分析结果缓存**：AI 报告只生成一次，避免重复调用
✅ **统计字段预聚合**：存储统计信息，查询时无需 JOIN 样本表，性能大幅提升

#### 统计字段的更新机制（V2.2.0）

**增量更新算法**：

当新的慢查询样本被捕获时，统计字段会实时增量更新，无需重新计算：

```java
// 平均值更新公式：新平均值 = (旧平均值 × 旧数量 + 新值) / (旧数量 + 1)
new_avg = (old_avg × old_count + new_value) / (old_count + 1)

// 最大值更新：如果新值 > 旧最大值，则更新
if (new_value > old_max) {
    new_max = new_value;
}

// 出现次数：直接 +1
new_count = old_count + 1;
```

**性能优势**：

| 操作 | 实时统计（旧方案） | 预聚合（新方案） | 性能提升 |
|------|-------------------|-----------------|---------|
| 查询慢查询排行 | `COUNT + AVG + GROUP BY` | `ORDER BY avg_query_time` | **100x+** |
| 获取统计信息 | JOIN Sample 表 | 直接读取 Template | **10x+** |
| 写入新样本 | 仅插入 Sample | 插入 Sample + 更新 Template | 略微增加 |
| 数据一致性 | 实时计算，始终准确 | 增量更新，最终一致 | 相同 |

**一致性保证**：

- ✅ 读写分离：读取统计字段不影响写入性能
- ✅ 最终一致：统计字段异步更新，短时间内可能略有偏差（可接受）
- ✅ 自动修复：定期从 Sample 表重新计算统计字段，修正累积误差

---

### 2️⃣ SLOW_QUERY_SAMPLE（样本表）

**核心职责**：存储 **每次执行的历史数据**

**关系性质**：1:N（一个 SQL 指纹对应 N 条记录）

#### 关键字段

| 字段名 | 类型 | 说明 | 示例值 |
|--------|------|------|--------|
| `id` | BIGINT | **主键**（自增） | `1`, `2`, `3` |
| `sql_fingerprint` | VARCHAR(64) | **SQL 指纹（关联 Template）** | `a3f5e8c9d2b1...` |
| `original_sql` | TEXT | **原始 SQL（脱敏后）** | `SELECT * FROM users WHERE id = '138****5678'` |
| `user_host` | VARCHAR(255) | 执行用户@主机 | `root@localhost` |
| `query_time` | DOUBLE | **查询耗时（秒）** | `2.5` |
| `lock_time` | DOUBLE | **锁等待时间（秒）** | `0.001` |
| `rows_sent` | BIGINT | 返回行数 | `100` |
| `rows_examined` | BIGINT | 扫描行数 | `50000` |
| `captured_at` | DATETIME | **慢查询发生时间** | `2024-01-22 10:00:00` |
| `created_at` | DATETIME | 记录创建时间 | `2024-01-22 10:00:05` |

#### 核心特点

✅ **完整历史**：每次捕获慢查询都新增一条记录
✅ **原始 SQL 保留**：存储脱敏后的真实 SQL（包含真实值）
✅ **性能数据**：保留每次执行的详细性能指标
✅ **实时统计**：用于计算平均耗时、最大耗时等统计信息

---

## 数据流转示例

### 场景：慢查询被捕获 3 次

#### 📅 第 1 次捕获（10:00）

```sql
-- 从 mysql.slow_log 读取到
SELECT * FROM customers WHERE phone = '13812345678';

-- 处理流程：
1. 计算 SQL 指纹：MD5("SELECT * FROM customers WHERE phone = ?")
2. 查询 TEMPLATE 表：无记录 → 新面孔
3. 创建 TEMPLATE 记录：
   - sql_template = "SELECT * FROM customers WHERE phone = ?"
   - first_seen_time = NOW()
4. 创建 SAMPLE 记录：
   - original_sql = "SELECT * FROM customers WHERE phone = '138****5678'"
   - query_time = 3.5
   - captured_at = "10:00:00"
5. 异步生成 AI 报告并存储到 TEMPLATE 表
```

#### 📅 第 2 次捕获（12:00）

```sql
-- 从 mysql.slow_log 读取到
SELECT * FROM customers WHERE phone = '15987654321';

-- 处理流程：
1. 计算 SQL 指纹：MD5("SELECT * FROM customers WHERE phone = ?") ← 相同
2. 查询 TEMPLATE 表：有记录 → 老面孔
3. 更新 TEMPLATE 记录：
   - last_seen_time = NOW()
4. 新增 SAMPLE 记录：
   - original_sql = "SELECT * FROM customers WHERE phone = '159****4321'"
   - query_time = 4.2
   - captured_at = "12:00:00"
5. 跳过 AI 分析（已有报告）
```

#### 📅 第 3 次捕获（15:30）

```sql
-- 从 mysql.slow_log 读取到
SELECT * FROM customers WHERE phone = '18600001111';

-- 处理流程：
1. 计算 SQL 指纹：相同
2. 查询 TEMPLATE 表：有记录 → 老面孔
3. 更新 TEMPLATE 记录：
   - last_seen_time = NOW()
4. 新增 SAMPLE 记录：
   - original_sql = "SELECT * FROM customers WHERE phone = '186****1111'"
   - query_time = 2.8
   - captured_at = "15:30:00"
```

### 📊 最终表数据

**SLOW_QUERY_TEMPLATE（1条记录）**

| sql_fingerprint | sql_template | first_seen_time | last_seen_time | ai_analysis_report |
|----------------|--------------|-----------------|----------------|-------------------|
| a3f5e8c9... | SELECT * FROM customers WHERE phone = ? | 10:00:00 | 15:30:00 | # 慢查询分析报告... |

**SLOW_QUERY_SAMPLE（3条记录）**

| id | original_sql | query_time | captured_at |
|----|--------------|------------|-------------|
| 1 | ...WHERE phone = '138****5678' | 3.5 | 10:00:00 |
| 2 | ...WHERE phone = '159****4321' | 4.2 | 12:00:00 |
| 3 | ...WHERE phone = '186****1111' | 2.8 | 15:30:00 |

---

## 常见问题

### Q1: 为什么不在 Template 表存储真实 SQL？

**A**: 因为 Template 表的目的是 **去重** 和 **存储分析结果**，真实 SQL 的具体值（如 `id = 1` vs `id = 2`）对于去重没有意义。使用参数化后的模板（`id = ?`）可以正确识别相同的 SQL 结构。

### Q2: Sample 表的数据会一直增长吗？

**A**: 是的，Sample 表会保留所有历史记录。可以通过配置自动清理任务来定期清理旧数据（见 `application.yml` 中的 `auto-cleanup` 配置）。

### Q3: 如何统计某个 SQL 的出现次数？

**A**:
```sql
-- 推荐：直接从 Template 表读取预聚合字段（V2.2.0+）
SELECT
    occurrence_count,
    avg_query_time,
    max_query_time
FROM slow_query_template
WHERE sql_fingerprint = 'a3f5e8c9...';

-- 备选：从 Sample 表实时统计（用于验证）
SELECT COUNT(*) FROM slow_query_sample
WHERE sql_fingerprint = 'a3f5e8c9...';
```

**性能对比**：
- 预聚合字段（推荐）：O(1) 复杂度，直接查询，毫秒级响应
- 实时统计（备选）：O(N) 复杂度，需要扫描样本表，性能较差

### Q4: SQL 指纹是如何计算的？

**A**:
```java
// SqlFingerprintUtil.calculateFingerprint()
1. 移除 SQL 注释（-- comment）
2. 标准化空白字符（多个空格 → 一个）
3. 转大写 + 移除反引号（`table` → TABLE）
4. Druid 参数化（把值替换成 ?）
5. 计算 MD5 哈希值
```

### Q5: 为什么 Sample 表的 SQL 是脱敏的？

**A**: 为了保护敏感数据（手机号、身份证、IP 等），Sample 表存储的是脱敏后的 SQL，例如：
- 原始：`WHERE phone = '13812345678'`
- 脱敏：`WHERE phone = '138****5678'`

脱敏规则见 `SqlMaskingUtil.java`。

---

## 总结

| 对比维度 | SLOW_QUERY_TEMPLATE | SLOW_QUERY_SAMPLE |
|---------|---------------------|-------------------|
| **用途** | 存储 SQL 结构和分析结果 | 存储每次执行的历史数据 |
| **关系** | 1:1（一个指纹一条记录） | 1:N（一个指纹多条记录） |
| **SQL 内容** | 参数化模板（`?`） | 脱敏后的真实 SQL |
| **主要字段** | sql_template, ai_analysis_report, severity_level | original_sql, query_time, rows_examined |
| **数据增长** | 慢（只有新 SQL 才新增） | 快（每次捕获都新增） |
| **历史数据** | ❌ 不保留历史 | ✅ 保留完整历史 |

**核心思想**：
- **Template 表** = SQL 的"身份证"（去重、缓存分析结果）
- **Sample 表** = SQL 的"执行日志"（完整历史、性能分析）
