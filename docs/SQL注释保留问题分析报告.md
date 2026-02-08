# SQL 注释保留问题分析报告

## 1. 问题描述

### 1.1 现象
用户执行包含 SQL 注释的慢查询语句后，在系统各处（前端、数据库、后端）显示的 SQL 都保留了原始注释内容。

**测试 SQL 示例**：
```sql
-- 限制一下避免结果集过大，但查询本身依然会慢
SELECT c.customer_name, ca.activity_type, ca.activity_date, e.first_name, e.last_name, d.department_name
FROM enterprise_crm_system.customer_activities ca
JOIN enterprise_crm_system.customers c ON ca.customer_id = c.customer_id
JOIN enterprise_core_hr.employees e ON ca.assigned_employee_id = e.employee_id
JOIN enterprise_core_hr.departments d ON e.department_id = d.department_id
WHERE ca.activity_date >= DATE_SUB(NOW(), INTERVAL 1 YEAR)
  AND ca.activity_details LIKE '%customer feedback%' -- 故意对TEXT字段模糊查询
  AND d.department_name = 'Sales' -- 对无索引字段进行等值查询
ORDER BY ca.activity_date DESC
LIMIT 2000
```

**显示结果**：
- 前端页面：显示带注释的 SQL
- 数据库 `slow_query_template` 表的 `sql_template` 字段：包含注释
- 数据库 `slow_query_sample` 表的 `original_sql` 字段：包含注释

### 1.2 影响范围
- 所有包含 SQL 注释的慢查询都会保留注释
- 影响 SQL 指纹的生成（虽然计算指纹时会移除注释）
- 影响前端展示的可读性
- 影响 AI 分析的输入质量

---

## 2. 根因分析

### 2.1 代码调用链

```
SlowLogTableMonitor.pollSlowLog()
  ↓
SlowQueryLog.sqlText = sqlContent (原始 SQL，包含注释)
  ↓
AnalysisService.processSlowQuery()
  ↓
SqlFingerprintUtil.cleanSql(rawSql)  ← ⚠️ 问题点：只清理空格，不移除注释
  ↓
存储到 slow_query_template.sql_template (包含注释)
存储到 slow_query_sample.original_sql (包含注释)
```

### 2.2 问题定位

#### 位置 1：`SqlFingerprintUtil.cleanSql()` 方法

**文件**：`src/main/java/com/dbdoctor/common/util/SqlFingerprintUtil.java:231-243`

**当前实现**：
```java
public static String cleanSql(String rawSql) {
    if (rawSql == null || rawSql.isBlank()) {
        return "";
    }

    // 去除前后空格
    String cleaned = rawSql.trim();

    // 去除多余空格（多个连续空格替换为一个）
    cleaned = cleaned.replaceAll("\\s+", " ");

    return cleaned;
}
```

**问题**：
- ✅ 清理了前后空格
- ✅ 压缩了多余空格
- ❌ **没有移除 SQL 注释**（单行注释 `--`、多行注释 `/* */`、MySQL 注释 `#`）

#### 位置 2：`SqlFingerprintUtil.extractTemplate()` 方法

**文件**：`src/main/java/com/dbdoctor/common/util/SqlFingerprintUtil.java:99-128`

**当前实现**：
```java
public static String extractTemplate(String rawSql) {
    if (rawSql == null || rawSql.isBlank()) {
        return "";
    }

    try {
        // 先标准化
        String normalized = normalizeWhitespace(rawSql.trim());  // ← 只标准化空格

        // 解析 SQL 语句
        List<SQLStatement> statements = SQLUtils.parseStatements(normalized, JdbcConstants.MYSQL);

        if (statements.isEmpty()) {
            return cleanSql(rawSql);  // ← 降级调用 cleanSql()，没有移除注释
        }

        // 格式化 SQL（统一格式）
        String formatted = SQLUtils.toSQLString(statements.get(0), JdbcConstants.MYSQL);

        // 参数化：使用正则表达式替换常量值
        String parameterized = parameterizeSql(formatted);

        return parameterized.replaceAll("\\s+", " ").trim();

    } catch (Exception e) {
        log.error("提取 SQL 模板失败: {}", rawSql, e);
        // 降级：使用简单参数化
        return parameterizeSql(cleanSql(rawSql));  // ← 降级调用 cleanSql()，没有移除注释
    }
}
```

**问题**：
- ❌ 入口没有调用 `removeSqlComments()`
- ❌ 降级逻辑中调用的 `cleanSql()` 也没有移除注释

#### 位置 3：`SqlFingerprintUtil.calculateFingerprint()` 方法（正确实现）

**文件**：`src/main/java/com/dbdoctor/common/util/SqlFingerprintUtil.java:49-91`

**当前实现**：
```java
public static String calculateFingerprint(String rawSql) {
    if (rawSql == null || rawSql.isBlank()) {
        return "";
    }

    try {
        // 1. 移除 SQL 注释  ← ✅ 正确：先移除注释
        String normalized = removeSqlComments(rawSql);

        // 2. 压缩空白字符
        normalized = normalizeWhitespace(normalized);

        // 3. 标准化：转大写 + 移除反引号
        normalized = normalized
                .toUpperCase()
                .replaceAll("`", "")
                .trim();

        // 4. 解析 SQL 语句并格式化
        List<SQLStatement> statements = SQLUtils.parseStatements(normalized, JdbcConstants.MYSQL);

        String sqlTemplate;
        if (statements.isEmpty()) {
            // 降级：直接参数化
            sqlTemplate = parameterizeSql(normalized);
        } else {
            // 格式化后再参数化
            String formatted = SQLUtils.toSQLString(statements.get(0), JdbcConstants.MYSQL);
            sqlTemplate = parameterizeSql(formatted);
        }

        // 5. 二次标准化（可能会引入多余空格）
        sqlTemplate = normalizeWhitespace(sqlTemplate);

        // 6. 计算 MD5 作为指纹
        return calculateMD5(sqlTemplate);

    } catch (Exception e) {
        log.error("计算 SQL 指纹失败: {}", rawSql, e);
        // 降级：直接对原始 SQL 计算 MD5
        return calculateMD5(rawSql);
    }
}
```

**对比**：
- ✅ `calculateFingerprint()` 在第一步就调用了 `removeSqlComments()`
- ❌ `extractTemplate()` 和 `cleanSql()` 没有调用 `removeSqlComments()`

---

## 3. 为什么指纹计算正确但存储的 SQL 包含注释？

### 3.1 处理流程对比

| 阶段 | 使用方法 | 是否移除注释 | 存储位置 |
|------|---------|------------|---------|
| 指纹计算 | `calculateFingerprint()` | ✅ 是 | `sql_fingerprint` (MD5) |
| 模板提取 | `extractTemplate()` | ❌ 否 | `sql_template` 字段 |
| SQL 清理 | `cleanSql()` | ❌ 否 | `original_sql` 字段 |

### 3.2 代码证据

**AnalysisService.java:71** (处理慢查询入口)
```java
@Transactional
public void processSlowQuery(SlowQueryLog slowLog) {
    String rawSql = slowLog.getSqlText();
    String dbName = slowLog.getDbName();

    // 1. 数据清洗
    String cleanedSql = SqlFingerprintUtil.cleanSql(rawSql);  // ← 调用 cleanSql()

    // 2. 计算 SQL 指纹（内部会调用 removeSqlComments）
    String fingerprint = SqlFingerprintUtil.calculateFingerprint(cleanedSql);

    // 3. 提取 SQL 模板（不会调用 removeSqlComments）
    String sqlTemplate = SqlFingerprintUtil.extractTemplate(cleanedSql);

    // 4. 存储到数据库（sql_template 包含注释）
    SlowQueryTemplate template = SlowQueryTemplate.builder()
            .sqlFingerprint(fingerprint)
            .sqlTemplate(sqlTemplate)  // ← 包含注释
            // ...
            .build();
}
```

**关键发现**：
1. `cleanedSql = SqlFingerprintUtil.cleanSql(rawSql)` - 这一步没有移除注释
2. `calculateFingerprint(cleanedSql)` - 虽然内部会移除注释，但不影响 cleanedSql 变量本身
3. `extractTemplate(cleanedSql)` - 使用包含注释的 cleanedSql 生成模板
4. 存储 `sqlTemplate` - 包含注释的模板被存入数据库

---

## 4. 现有工具方法分析

### 4.1 `removeSqlComments()` 方法（已存在，未使用）

**文件**：`src/main/java/com/dbdoctor/common/util/SqlFingerprintUtil.java:175-197`

**实现**：
```java
/**
 * 移除 SQL 注释
 *
 * @param sql 原始 SQL
 * @return 移除注释后的 SQL
 */
private static String removeSqlComments(String sql) {
    if (sql == null || sql.isBlank()) {
        return sql;
    }

    String result = sql;

    try {
        // 移除单行注释（-- comment）
        result = result.replaceAll("--[^\\n]*", "");

        // 移除多行注释（/* comment */）
        result = result.replaceAll("/\\*.*?\\*/", "");

        // 移除 MySQL 注释（# comment）
        result = result.replaceAll("#[^\\n]*", "");

    } catch (Exception e) {
        log.debug("移除 SQL 注释失败: {}", sql, e);
    }

    return result;
}
```

**问题**：
- ✅ 实现正确，支持三种注释格式
- ❌ 权限是 `private`，只能在 `SqlFingerprintUtil` 类内部使用
- ❌ 只有 `calculateFingerprint()` 方法调用了它

### 4.2 `StringUtil.cleanSql()` 方法（重复实现，未使用）

**文件**：`src/main/java/com/dbdoctor/common/util/StringUtil.java:105-120`

**实现**：
```java
/**
 * 清理 SQL 语句（移除注释和多余空格）
 *
 * @param sql SQL 语句
 * @return 清理后的 SQL
 */
public String cleanSql(String sql) {
    if (isEmpty(sql)) {
        return "";
    }

    // 移除单行注释
    String cleaned = sql.replaceAll("--.*", "");

    // 移除多行注释
    cleaned = cleaned.replaceAll("/\\*.*?\\*/", "");

    // 移除多余空格和换行
    cleaned = cleaned.replaceAll("\\s+", " ").trim();

    return cleaned;
}
```

**对比**：
- ✅ 公开方法（`public`），可被外部调用
- ✅ 实现了移除注释逻辑
- ❌ 与 `SqlFingerprintUtil.cleanSql()` 功能重复
- ❌ 代码中没有被使用

---

## 5. 问题影响分析

### 5.1 功能影响

| 影响点 | 严重程度 | 说明 |
|-------|---------|------|
| SQL 指纹生成 | ✅ 无影响 | `calculateFingerprint()` 内部已移除注释 |
| 前端展示 | ⚠️ 中等影响 | 显示包含注释的 SQL，影响可读性 |
| 数据存储 | ⚠️ 中等影响 | 存储冗余的注释内容，浪费空间 |
| AI 分析 | ⚠️ 潜在影响 | AI 需要额外过滤注释，可能干扰分析 |
| SQL 模板 | ❌ 严重影响 | 参数化后的模板仍包含注释，失去模板意义 |

### 5.2 数据质量影响

**示例对比**：

原始 SQL（带注释）：
```sql
-- 限制一下避免结果集过大
SELECT * FROM users WHERE id = 123 -- 查询用户
```

期望的 SQL 模板（无注释）：
```sql
SELECT * FROM users WHERE id = ?
```

实际的 SQL 模板（包含注释）：
```sql
-- 限制一下避免结果集过大 SELECT * FROM users WHERE id = ? -- 查询用户
```

### 5.3 性能影响

| 影响点 | 评估 |
|-------|------|
| 存储空间 | 轻微增加（注释通常占 10-30% 额外空间） |
| 传输带宽 | 轻微增加（前端请求时传输更多数据） |
| 指纹计算 | 无影响（已移除注释） |
| 数据库查询 | 无影响（按指纹查询，不涉及 SQL 内容） |

---

## 6. 解决方案

### 6.1 方案对比

| 方案 | 修改范围 | 兼容性 | 推荐度 |
|------|---------|-------|-------|
| 方案 1：修改 `cleanSql()` 方法 | 最小（1 个方法） | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| 方案 2：新增独立方法 | 中等（新增 1 个方法） | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| 方案 3：重构工具类结构 | 大（多个方法） | ⭐⭐⭐ | ⭐⭐ |
| 方案 4：数据库迁移脚本 | 无代码修改 | ⭐⭐ | ⭐⭐⭐ |

### 6.2 推荐方案：修改 `cleanSql()` 方法

**修改文件**：`src/main/java/com/dbdoctor/common/util/SqlFingerprintUtil.java`

**修改内容**：
```java
/**
 * 清理 SQL（去除注释和多余空格）
 *
 * @param rawSql 原始 SQL
 * @return 清理后的 SQL
 */
public static String cleanSql(String rawSql) {
    if (rawSql == null || rawSql.isBlank()) {
        return "";
    }

    // 去除前后空格
    String cleaned = rawSql.trim();

    // ✅ 新增：移除 SQL 注释
    cleaned = removeSqlComments(cleaned);

    // 去除多余空格（多个连续空格替换为一个）
    cleaned = cleaned.replaceAll("\\s+", " ");

    return cleaned;
}
```

**修改点**：
1. 调用现有的 `removeSqlComments()` 方法（代码复用）
2. 保持方法签名不变（向后兼容）
3. 权限改为 `public`（已是 public，无需修改）

### 6.3 数据迁移方案（可选）

**目的**：清理历史数据中已存在的注释

**SQL 脚本**：
```sql
-- 清理 slow_query_template 表中的注释
UPDATE slow_query_template
SET sql_template = REGEXP_REPLACE(sql_template, '--[^\n]*', '')
WHERE sql_template REGEXP '--[^\n]*';

UPDATE slow_query_template
SET sql_template = REGEXP_REPLACE(sql_template, '/\\*.*?\\*/', '')
WHERE sql_template REGEXP '/\\*.*?\\*/';

UPDATE slow_query_template
SET sql_template = REGEXP_REPLACE(sql_template, '#[^\n]*', '')
WHERE sql_template REGEXP '#[^\n]*';

-- 清理 slow_query_sample 表中的注释
UPDATE slow_query_sample
SET original_sql = REGEXP_REPLACE(original_sql, '--[^\n]*', '')
WHERE original_sql REGEXP '--[^\n]*';

UPDATE slow_query_sample
SET original_sql = REGEXP_REPLACE(original_sql, '/\\*.*?\\*/', '')
WHERE original_sql REGEXP '/\\*.*?\\*/';

UPDATE slow_query_sample
SET original_sql = REGEXP_REPLACE(original_sql, '#[^\n]*', '')
WHERE original_sql REGEXP '#[^\n]*';
```

**注意事项**：
- ⚠️ MySQL 的 `REGEXP_REPLACE` 在 8.0+ 版本可用
- ⚠️ 建议先在测试环境验证
- ⚠️ 备份数据后再执行

---

## 7. 验证计划

### 7.1 单元测试

**测试用例**：
```java
@Test
void testCleanSqlWithComments() {
    // 单行注释
    String sql1 = "-- 这是注释\nSELECT * FROM users";
    assertEquals("SELECT * FROM users", SqlFingerprintUtil.cleanSql(sql1));

    // 多行注释
    String sql2 = "/* 这是注释 */SELECT * FROM users";
    assertEquals("SELECT * FROM users", SqlFingerprintUtil.cleanSql(sql2));

    // MySQL 注释
    String sql3 = "# 这是注释\nSELECT * FROM users";
    assertEquals("SELECT * FROM users", SqlFingerprintUtil.cleanSql(sql3));

    // 混合注释
    String sql4 = "-- 注释1\nSELECT /* 注释2 */ * FROM users WHERE id = ? # 注释3";
    assertEquals("SELECT * FROM users WHERE id = ?", SqlFingerprintUtil.cleanSql(sql4));

    // 用户测试用例
    String sql5 = """
        -- 限制一下避免结果集过大，但查询本身依然会慢
        SELECT c.customer_name, ca.activity_type, ca.activity_date, e.first_name, e.last_name, d.department_name
        FROM enterprise_crm_system.customer_activities ca
        JOIN enterprise_crm_system.customers c ON ca.customer_id = c.customer_id
        WHERE ca.activity_date >= DATE_SUB(NOW(), INTERVAL 1 YEAR)
          AND ca.activity_details LIKE '%customer feedback%' -- 故意对TEXT字段模糊查询
          AND d.department_name = 'Sales' -- 对无索引字段进行等值查询
        ORDER BY ca.activity_date DESC
        LIMIT 2000
        """;
    String expected = "SELECT c.customer_name, ca.activity_type, ca.activity_date, e.first_name, e.last_name, d.department_name FROM enterprise_crm_system.customer_activities ca JOIN enterprise_crm_system.customers c ON ca.customer_id = c.customer_id WHERE ca.activity_date >= DATE_SUB(NOW(), INTERVAL ? YEAR) AND ca.activity_details LIKE ? AND d.department_name = ? ORDER BY ca.activity_date DESC LIMIT ?";
    assertEquals(expected, SqlFingerprintUtil.cleanSql(sql5));
}
```

### 7.2 集成测试

**测试步骤**：
1. 执行包含注释的慢查询 SQL
2. 等待系统自动采集和分析
3. 检查数据库 `slow_query_template.sql_template` 字段（应无注释）
4. 检查数据库 `slow_query_sample.original_sql` 字段（应无注释）
5. 检查前端展示（应无注释）

### 7.3 回归测试

**验证点**：
- ✅ SQL 指纹计算不受影响
- ✅ 现有模板数据不损坏
- ✅ 前端展示正常
- ✅ AI 分析功能正常
- ✅ 通知邮件内容正常

---

## 8. 优先级与排期建议

### 8.1 优先级评估

| 维度 | 评分 | 说明 |
|------|------|------|
| 影响范围 | 3/5 | 所有包含注释的慢查询 |
| 严重程度 | 2/5 | 不影响核心功能，但影响用户体验 |
| 修复难度 | 1/5 | 简单，1 个方法修改 |
| 测试成本 | 2/5 | 需要单元测试 + 集成测试 |

**综合优先级**：P2（中优先级）

### 8.2 排期建议

| 阶段 | 工作内容 | 预计时间 |
|------|---------|---------|
| 开发 | 修改 `cleanSql()` 方法 | 0.5 小时 |
| 单元测试 | 编写测试用例 | 1 小时 |
| 集成测试 | 执行测试 SQL，验证数据库 | 0.5 小时 |
| 前端验证 | 检查前端展示 | 0.5 小时 |
| 数据迁移（可选） | 清理历史数据 | 1 小时 |
| **总计** | | **3.5 小时** |

---

## 9. 风险评估

### 9.1 修改风险

| 风险点 | 可能性 | 影响 | 缓解措施 |
|-------|-------|------|---------|
| 破坏现有功能 | 低 | 高 | 完善单元测试，先在测试环境验证 |
| SQL 解析错误 | 低 | 中 | `removeSqlComments()` 已在 `calculateFingerprint()` 中验证 |
| 性能下降 | 极低 | 低 | 正则替换性能影响可忽略 |
| 历史数据兼容性 | 无 | 无 | 不影响已存储的指纹 |

### 9.2 数据迁移风险（可选）

| 风险点 | 可能性 | 影响 | 缓解措施 |
|-------|-------|------|---------|
| 正则表达式误删 | 低 | 高 | 先在测试环境验证，备份数据 |
| 大表更新锁表 | 中 | 中 | 分批更新，使用低峰期 |
| 存储空间碎片 | 低 | 低 | 执行 `OPTIMIZE TABLE` |

---

## 10. 后续优化建议

### 10.1 代码质量优化

1. **统一 SQL 清理逻辑**
   - 删除 `StringUtil.cleanSql()` 方法（避免重复）
   - 统一使用 `SqlFingerprintUtil.cleanSql()`

2. **增强注释移除逻辑**
   - 支持嵌套注释（目前不支持）
   - 支持字符串中的注释符号（不应被移除）

3. **添加单元测试覆盖**
   - 覆盖率目标：≥ 90%
   - 边界条件测试

### 10.2 功能增强

1. **SQL 格式化**
   - 统一关键字大小写（大写）
   - 统一缩进风格

2. **敏感信息脱敏增强**
   - 检测注释中的敏感信息
   - 自动移除或替换

3. **前端展示优化**
   - 语法高亮
   - 格式化展示
   - 注释与 SQL 分离展示

---

## 11. 附录

### 11.1 相关文件清单

| 文件路径 | 作用 | 是否需要修改 |
|---------|------|------------|
| `src/main/java/com/dbdoctor/common/util/SqlFingerprintUtil.java` | SQL 指纹和清理工具 | ✅ 是 |
| `src/main/java/com/dbdoctor/common/util/StringUtil.java` | 通用字符串工具 | ⚠️ 建议（删除重复方法） |
| `src/main/java/com/dbdoctor/service/AnalysisService.java` | 慢查询分析服务 | ❌ 否 |
| `src/main/java/com/dbdoctor/service/SlowLogTableMonitor.java` | 慢查询监控 | ❌ 否 |

### 11.2 测试 SQL 清单

```sql
-- 测试用例 1：单行注释
-- 这是一个单行注释
SELECT * FROM users WHERE id = 1

-- 测试用例 2：多行注释
/* 这是一个
   多行注释 */
SELECT * FROM users WHERE id = 1

-- 测试用例 3：MySQL 注释
# 这是一个 MySQL 注释
SELECT * FROM users WHERE id = 1

-- 测试用例 4：混合注释
-- 注释1
SELECT /* 注释2 */ * FROM users WHERE id = 1 # 注释3

-- 测试用例 5：用户真实场景
-- 限制一下避免结果集过大，但查询本身依然会慢
SELECT c.customer_name, ca.activity_type, ca.activity_date, e.first_name, e.last_name, d.department_name
FROM enterprise_crm_system.customer_activities ca
JOIN enterprise_crm_system.customers c ON ca.customer_id = c.customer_id
JOIN enterprise_core_hr.employees e ON ca.assigned_employee_id = e.employee_id
JOIN enterprise_core_hr.departments d ON e.department_id = d.department_id
WHERE ca.activity_date >= DATE_SUB(NOW(), INTERVAL 1 YEAR)
  AND ca.activity_details LIKE '%customer feedback%' -- 故意对TEXT字段模糊查询
  AND d.department_name = 'Sales' -- 对无索引字段进行等值查询
ORDER BY ca.activity_date DESC
LIMIT 2000
```

### 11.3 参考资料

- [MySQL Slow Query Log](https://dev.mysql.com/doc/refman/8.0/en/slow-query-log.html)
- [Druid SQL Parser](https://github.com/alibaba/druid/wiki/SQL-Parser)
- [Java 正则表达式语法](https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html)

---

## 12. 结论

### 12.1 问题总结
- **根因**：`SqlFingerprintUtil.cleanSql()` 方法未调用 `removeSqlComments()` 移除注释
- **影响**：存储的 SQL 模板和样本包含注释，影响可读性和数据质量
- **解决方案**：在 `cleanSql()` 方法中调用 `removeSqlComments()`（代码复用，改动最小）

### 12.2 行动建议
1. ✅ **立即执行**：修改 `cleanSql()` 方法
2. ✅ **补充测试**：添加单元测试和集成测试
3. ⚠️ **可选执行**：数据迁移脚本清理历史数据
4. 📋 **后续优化**：统一工具类，增强 SQL 格式化功能

### 12.3 预期效果
- ✅ 新采集的慢查询 SQL 不再包含注释
- ✅ 前端展示更加清晰易读
- ✅ 数据库存储空间优化（约 10-30%）
- ✅ AI 分析输入质量提升

---

**文档版本**：v1.0.0
**创建时间**：2026-02-08
**作者**：DB-Doctor 开发团队
**审核状态**：待审核
