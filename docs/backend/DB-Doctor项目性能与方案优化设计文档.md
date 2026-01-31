# DB-Doctor 项目性能与方案优化设计文档

> **文档版本**: v1.0.0
> **编写日期**: 2026-01-25
> **目标读者**: DB-Doctor 开发团队
> **文档性质**: 技术架构设计文档

---

## 📋 文档概述

本文档基于 DB-Doctor 项目当前实现，结合 Gemini AI 的专业建议，对项目进行全面的性能评估和可靠性优化设计。旨在将 DB-Doctor 从"原型玩具"进化为"工业级可靠"的慢查询诊疗工具。

**设计原则**：
- ✅ 轻量级、实时感知、零负担监控
- ✅ 不补发历史数据，只关注当下
- ✅ 优雅停机，快速失败（Fail-fast）
- ✅ 数据一致性优先于绝对完整性

---

## 🎯 第一部分：当前项目性能评估

### 1.1 架构现状分析

#### 技术栈
```
前端：无（纯后端监控）
后端：Spring Boot 3.2.2 + Java 17
数据库：H2 (嵌入式) + MySQL (只读监控)
AI 框架：LangChain4j (当前版本已禁用)
异步处理：ThreadPoolTaskExecutor + @Async
```

#### 核心流程
```
MySQL slow_log 表 (每 60 秒轮询)
    ↓
游标读取 (WHERE start_time > lastCheckTime)
    ↓
同步处理：计算指纹 → 去重 → 保存 H2 (状态=PENDING)
    ↓
异步处理：生成报告 → 智能通知 → 更新状态 (PENDING→SUCCESS)
```

### 1.2 性能瓶颈识别

| 瓶颈点 | 位置 | 影响 | 评估 |
|--------|------|------|------|
| AI 调用 | LangChain4j | 5-30 秒/次 | ⚠️ 当前已禁用，影响小 |
| 邮件发送 | SMTP | 1-2 秒/次 | ✅ 异步处理，可接受 |
| 线程池队列 | 内存队列 | 队列满时背压 | ✅ CallerRunsPolicy 保护 |
| 数据库事务 | H2 + MySQL | 每秒数百条 | ✅ 轻量级，无压力 |

**结论**：
> **项目性能瓶颈不在 Java 层，而在 AI 调用和网络 IO。** 采用 @Async 异步处理是极其正确的架构设计，保证了"采集"和"诊疗"互不阻塞。

### 1.3 当前防漏消息机制评估

#### 三层现有保护
1. **数据库持久化** ✅ - 数据先保存 H2 再异步处理
2. **线程池队列** ✅ - 50 容量 + CallerRunsPolicy
3. **游标机制** ✅ - 避免重复读取

#### 潜在风险点
| 风险 | 严重性 | 影响 |
|------|--------|------|
| 游标在内存中 | 🔴 高 | 应用重启后丢失断点 |
| PENDING 状态无重试 | 🔴 高 | AI 失败后永久 PENDING |
| 应用崩溃队列丢失 | 🟡 中 | 内存队列任务丢失 |
| 并发统计误差 | 🟡 中 | 高并发下 occurrenceCount 可能不准 |

---

## 🚀 第二部分：核心优化方案设计

### 2.1 方案选择：轻量级实时监控（不补发历史）

#### 设计哲学
> **"挂了就挂了，从启动时间重新开始"** - 只关注当下，不纠结历史

**优点**：
- ✅ 逻辑简单，无状态压力
- ✅ 避免重启后被历史数据轰炸
- ✅ 极致性能，扫描逻辑极简

**适用场景**：
- 开发者本地监控
- 运维跳板机部署
- 单机/中小规模数据库 (QPS < 5000)

### 2.2 优化架构图

```
┌──────────────────────────────────────────────────────────────┐
│                    优雅停机流程                               │
│  1. 关水龙头（停止扫描新日志）                                 │
│  2. 倒掉桶里的水（清空队列中未开始的任务）                      │
│  3. 等碗里的饭吃完（等待正在分析的 2-4 个线程结束）             │
│  4. 关门走人                                                  │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│                    启动自愈流程                                │
│  1. H2 文件锁 + 端口锁（防止多实例冲突）                       │
│  2. 清空旧账（UPDATE status = 'ABANDONED' WHERE PENDING）     │
│  3. 游标初始化为当前时间                                       │
│  4. 开始实时监控                                              │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│                    PENDING 补扫策略                            │
│  目标：处理程序运行期间 AI 偶发失败的任务                       │
│  频率：每 10 分钟                                             │
│  范围：只处理本次启动后的 PENDING（创建时间 > 启动时间）         │
│  重试：最多 3 次，超过改为 FAILED                              │
└──────────────────────────────────────────────────────────────┘
```

---

## 🔧 第三部分：详细实现方案

### 3.1 优雅停机机制（Graceful Shutdown）

#### 3.1.1 Spring Boot 配置

**文件**: `src/main/resources/application.yml`

```yaml
server:
  # 开启优雅停机
  shutdown: graceful

spring:
  lifecycle:
    # 停机阶段最大等待时间（60 秒）
    timeout-per-shutdown-phase: 60s

db-doctor:
  shutdown:
    # 优雅停机等待时间（应该略小于 spring.lifecycle.timeout）
    await-termination-seconds: 50
    # 是否在停机时清空队列（快速失败策略）
    clear-queue-on-shutdown: true
```

#### 3.1.2 停机管理器

**文件**: `src/main/java/com/dbdoctor/lifecycle/ShutdownManager.java`

```java
package com.dbdoctor.lifecycle;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;

/**
 * 停机管理器
 * 负责在应用关闭时执行优雅停机流程
 *
 * 核心策略：
 * 1. 关水龙头 - 停止接收新任务
 * 2. 倒掉桶里的水 - 清空队列中未开始的任务
 * 3. 等碗里的饭吃完 - 等待正在执行的任务完成
 *
 * @author DB-Doctor
 * @version 2.0.0
 */
@Slf4j
@Component
public class ShutdownManager {

    @Autowired
    private ThreadPoolTaskExecutor analysisExecutor;

    @Value("${db-doctor.shutdown.clear-queue-on-shutdown:true}")
    private boolean clearQueueOnShutdown;

    /**
     * 全局停机标志位
     * 定时任务检测到此标志为 true 时，停止扫描新日志
     */
    public static volatile boolean isShuttingDown = false;

    /**
     * Spring 容器销毁前执行
     */
    @PreDestroy
    public void onShutdown() {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("📢 收到关闭指令，启动优雅停机流程...");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        long startTime = System.currentTimeMillis();

        try {
            // 第一步：关水龙头（停止接收新任务）
            isShuttingDown = true;
            log.info("✅ 第一步：已设置停机标志，停止拉取新的慢日志");

            // 第二步：倒掉桶里的水（清空队列）
            if (clearQueueOnShutdown) {
                int queueSize = analysisExecutor.getThreadPoolExecutor().getQueue().size();
                int discardedTasks = queueSize;

                analysisExecutor.getThreadPoolExecutor().getQueue().clear();

                log.info("✅ 第二步：已清空队列中待处理的任务数: {}", discardedTasks);
            } else {
                log.info("✅ 第二步：保留队列中的任务，等待处理完成");
            }

            // 第三步：等待正在执行的任务
            int activeCount = analysisExecutor.getActiveCount();
            if (activeCount > 0) {
                log.info("✅ 第三步：正在等待 {} 个活跃诊断任务执行完毕...", activeCount);
                log.info("⏳ 最长等待时间由 ThreadPoolConfig.awaitTerminationSeconds 决定");
            } else {
                log.info("✅ 第三步：无活跃任务，可以立即关闭");
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.info("🎉 优雅停机流程完成，总耗时: {} ms", duration);
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        } catch (Exception e) {
            log.error("❌ 优雅停机过程中发生异常", e);
        }
    }
}
```

#### 3.1.3 线程池配置优化

**文件**: `src/main/java/com/dbdoctor/config/ThreadPoolConfig.java` (已存在，需优化)

```java
@Bean("analysisExecutor")
public Executor analysisExecutor(
    @Value("${db-doctor.thread-pool.ai-analysis.core-size:2}") int coreSize,
    @Value("${db-doctor.thread-pool.ai-analysis.max-size:4}") int maxSize,
    @Value("${db-doctor.thread-pool.ai-analysis.queue-capacity:50}") int queueCapacity,
    @Value("${db-doctor.shutdown.await-termination-seconds:50}") int awaitTerminationSeconds
) {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

    executor.setCorePoolSize(coreSize);
    executor.setMaxPoolSize(maxSize);
    executor.setQueueCapacity(queueCapacity);
    executor.setThreadNamePrefix("db-doctor-analysis-");

    // 【关键配置 1】拒绝策略：调用者运行（背压机制）
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

    // 【关键配置 2】停机时等待任务完成
    executor.setWaitForTasksToCompleteOnShutdown(true);

    // 【关键配置 3】等待任务完成的最长时间
    executor.setAwaitTerminationSeconds(awaitTerminationSeconds);

    executor.initialize();

    log.info("🔧 AI 分析线程池初始化完成: coreSize={}, maxSize={}, queueCapacity={}, awaitTermination={}s",
            coreSize, maxSize, queueCapacity, awaitTerminationSeconds);

    return executor;
}
```

#### 3.1.4 定时任务停机感知

**文件**: `src/main/java/com/dbdoctor/service/SlowLogTableMonitor.java` (需修改)

```java
@Scheduled(fixedDelayString = "${db-doctor.slow-log-monitor.poll-interval-ms:60000}")
public void pollSlowLog() {
    // 【新增】停机感知逻辑
    if (ShutdownManager.isShuttingDown) {
        log.debug("正在停机中，跳过本次慢日志扫描");
        return;
    }

    try {
        // 原有的扫描逻辑...
    } catch (Exception e) {
        // 如果不在停机阶段，才记录错误日志
        if (!ShutdownManager.isShuttingDown) {
            log.error("❌ 轮询 mysql.slow_log 表失败", e);
        }
    }
}
```

---

### 3.2 启动自愈机制（Startup Housekeeping）

#### 3.2.1 启动清理器

**文件**: `src/main/java/com/dbdoctor/lifecycle/StartupHousekeeper.java`

```java
package com.dbdoctor.lifecycle;

import com.dbdoctor.model.SlowQueryHistory;
import com.dbdoctor.repository.SlowQueryHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 启动清理器
 * 应用启动后执行一次，清理上次运行遗留的 PENDING 状态记录
 *
 * 核心逻辑：
 * 1. 将所有旧的 PENDING 记录改为 ABANDONED（已放弃）
 * 2. 防止重启后误处理历史数据
 * 3. 保证 PENDING 状态只代表"本次运行中正在处理的任务"
 *
 * @author DB-Doctor
 * @version 2.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StartupHousekeeper implements ApplicationRunner {

    private final SlowQueryHistoryRepository historyRepo;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🧹 DB-Doctor 启动自检开始...");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        try {
            // 将所有 PENDING 状态改为 ABANDONED
            int affectedRows = historyRepo.markPendingAsAbandoned();

            if (affectedRows > 0) {
                log.warn("⚠️  发现 {} 条上次运行中断的记录", affectedRows);
                log.info("📝 已将这些记录状态更新为 ABANDONED（已放弃）");
                log.info("💡 提示：这些记录可在管理后台查看，但不会自动重试");
            } else {
                log.info("✅ 无遗留的 PENDING 记录");
            }

            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.info("🎉 启动自检完成，监控系统已就绪");
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        } catch (Exception e) {
            log.error("❌ 启动自检失败", e);
            // 不抛出异常，允许应用继续启动
        }
    }
}
```

#### 3.2.2 Repository 方法

**文件**: `src/main/java/com/dbdoctor/repository/SlowQueryHistoryRepository.java`

```java
/**
 * 将所有 PENDING 状态的记录改为 ABANDONED
 * 用于应用启动时清理上次运行中断的记录
 *
 * @return 影响的行数
 */
@Modifying
@Transactional
@Query("""
    UPDATE SlowQueryHistory h
    SET h.status = 'ABANDONED',
        h.aiAnalysisReport = CONCAT(
            COALESCE(h.aiAnalysisReport, ''),
            '\n\n**系统说明**: 诊断在程序关闭时中断'
        )
    WHERE h.status = 'PENDING'
""")
int markPendingAsAbandoned();
```

#### 3.2.3 SlowQueryHistory 模型扩展

**文件**: `src/main/java/com/dbdoctor/model/SlowQueryHistory.java` (需修改枚举)

```java
/**
 * 分析状态枚举
 */
public enum AnalysisStatus {
    PENDING,   // 待分析（本次运行中的任务）
    SUCCESS,   // 已生成报告
    ERROR,     // 分析失败（会自动重试）
    ABANDONED  // 已放弃（上次运行中断）
}
```

---

### 3.3 PENDING 任务补扫机制

#### 3.3.1 补扫服务

**文件**: `src/main/java/com/dbdoctor/service/PendingTaskRetryService.java`

```java
package com.dbdoctor.service;

import com.dbdoctor.config.DbDoctorProperties;
import com.dbdoctor.model.SlowQueryHistory;
import com.dbdoctor.repository.SlowQueryHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * PENDING 任务补扫服务
 * 定期扫描并重试处理失败的任务
 *
 * 核心策略：
 * 1. 只处理本次启动后的 PENDING 任务（创建时间 > 应用启动时间）
 * 2. 只处理 PENDING 时间超过 15 分钟的（避免正在进行的任务）
 * 3. 最多重试 3 次，超过后改为 FAILED 状态
 * 4. 每 10 分钟扫描一次
 *
 * @author DB-Doctor
 * @version 2.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PendingTaskRetryService {

    private final SlowQueryHistoryRepository historyRepo;
    private final AnalysisService analysisService;
    private final DbDoctorProperties properties;

    /**
     * 应用启动时间（用于判断是否为本次运行的任务）
     */
    private static LocalDateTime applicationStartTime = LocalDateTime.now();

    /**
     * 定时补扫任务
     * 每 10 分钟执行一次
     */
    @Scheduled(fixedDelayString = "${db-doctor.retry.pending-interval-ms:600000}") // 10 分钟
    public void retryPendingTasks() {
        // 停机感知
        if (com.dbdoctor.lifecycle.ShutdownManager.isShuttingDown) {
            return;
        }

        try {
            log.debug("🔍 开始扫描待重试的 PENDING 任务...");

            // 查询条件：
            // 1. status = PENDING
            // 2. 创建时间 > 应用启动时间（本次运行的任务）
            // 3. lastSeenTime < 15 分钟前（避免正在进行的任务）
            // 4. retryCount < 3（未超过最大重试次数）
            LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(15);
            List<SlowQueryHistory> pendingTasks = historyRepo.findPendingTasksForRetry(
                applicationStartTime,
                cutoffTime,
                properties.getRetry().getMaxAttempts() // 最大重试次数
            );

            if (pendingTasks.isEmpty()) {
                log.debug("✅ 无需要重试的任务");
                return;
            }

            log.info("🔍 发现 {} 个待重试的任务", pendingTasks.size());

            for (SlowQueryHistory history : pendingTasks) {
                try {
                    // 增加重试计数
                    history.setRetryCount(history.getRetryCount() + 1);

                    if (history.getRetryCount() >= properties.getRetry().getMaxAttempts()) {
                        // 超过最大重试次数，标记为 FAILED
                        log.warn("❌ 任务达到最大重试次数，标记为 FAILED: fingerprint={}",
                                history.getSqlFingerprint());
                        history.setStatus(SlowQueryHistory.AnalysisStatus.FAILED);
                        historyRepo.save(history);
                    } else {
                        // 重新提交分析
                        log.info("🔄 重试处理任务: fingerprint={}, retryCount={}",
                                history.getSqlFingerprint(), history.getRetryCount());
                        analysisService.generateReportAndNotify(history);
                    }

                } catch (Exception e) {
                    log.error("❌ 重试任务失败: fingerprint={}",
                            history.getSqlFingerprint(), e);
                }
            }

        } catch (Exception e) {
            log.error("❌ 扫描 PENDING 任务失败", e);
        }
    }
}
```

#### 3.3.2 Repository 查询方法

**文件**: `src/main/java/com/dbdoctor/repository/SlowQueryHistoryRepository.java`

```java
/**
 * 查询需要重试的 PENDING 任务
 *
 * @param createdAfter     创建时间晚于此时间（本次运行的任务）
 * @param lastSeenBefore   最后见到时间早于此时间（超过 15 分钟未更新）
 * @param maxRetryCount    最大重试次数
 * @return 需要重试的任务列表
 */
@Query("""
    SELECT h FROM SlowQueryHistory h
    WHERE h.status = 'PENDING'
      AND h.firstSeenTime > :createdAfter
      AND h.lastSeenTime < :lastSeenBefore
      AND h.retryCount < :maxRetryCount
    ORDER BY h.lastSeenTime ASC
""")
List<SlowQueryHistory> findPendingTasksForRetry(
    @Param("createdAfter") LocalDateTime createdAfter,
    @Param("lastSeenBefore") LocalDateTime lastSeenBefore,
    @Param("maxRetryCount") int maxRetryCount
);
```

#### 3.3.3 模型字段扩展

**文件**: `src/main/java/com/dbdoctor/model/SlowQueryHistory.java`

```java
/**
 * 重试次数
 * 用于 PENDING 任务补扫机制
 */
@Builder.Default
private Integer retryCount = 0;
```

---

### 3.4 并发统计优化（原子自增）

#### 3.4.1 当前问题

**场景**：
```
线程 A 读取：occurrenceCount = 10
线程 B 读取：occurrenceCount = 10
线程 A 更新：occurrenceCount = 11 并提交
线程 B 更新：occurrenceCount = 11 并提交
结果：丢失了 1 次统计 ❌
```

#### 3.4.2 解决方案：数据库原子自增

**文件**: `src/main/java/com/dbdoctor/repository/SlowQueryHistoryRepository.java`

```java
/**
 * 原子自增出现次数
 * 使用数据库层面自增，避免并发导致的统计错误
 *
 * @param fingerprint SQL 指纹
 * @param now         当前时间（更新 lastSeenTime）
 * @return 影响的行数
 */
@Modifying
@Transactional
@Query("""
    UPDATE SlowQueryHistory h
    SET h.occurrenceCount = h.occurrenceCount + 1,
        h.lastSeenTime = :now
    WHERE h.sqlFingerprint = :fingerprint
""")
int incrementOccurrence(
    @Param("fingerprint") String fingerprint,
    @Param("now") LocalDateTime now
);

/**
 * 原子更新统计信息
 * 用于更新耗时、锁时间、行数等统计字段
 *
 * @param fingerprint SQL 指纹
 * @param queryTime   查询耗时
 * @param lockTime    锁等待时间
 * @param rowsSent    返回行数
 * @param rowsExamined 扫描行数
 * @return 影响的行数
 */
@Modifying
@Transactional
@Query("""
    UPDATE SlowQueryHistory h
    SET h.occurrenceCount = h.occurrenceCount + 1,
        h.lastSeenTime = :now,
        h.avgQueryTime = (h.avgQueryTime * h.occurrenceCount + :queryTime) / (h.occurrenceCount + 1),
        h.maxQueryTime = GREATEST(COALESCE(h.maxQueryTime, 0), :queryTime),
        h.avgLockTime = (h.avgLockTime * h.occurrenceCount + :lockTime) / (h.occurrenceCount + 1),
        h.maxLockTime = GREATEST(COALESCE(h.maxLockTime, 0), :lockTime),
        h.avgRowsSent = (h.avgRowsSent * h.occurrenceCount + :rowsSent) / (h.occurrenceCount + 1),
        h.maxRowsSent = GREATEST(COALESCE(h.maxRowsSent, 0), :rowsSent),
        h.maxRowsExamined = GREATEST(COALESCE(h.maxRowsExamined, 0), :rowsExamined)
    WHERE h.sqlFingerprint = :fingerprint
""")
int updateStatistics(
    @Param("fingerprint") String fingerprint,
    @Param("now") LocalDateTime now,
    @Param("queryTime") Double queryTime,
    @Param("lockTime") Double lockTime,
    @Param("rowsSent") Long rowsSent,
    @Param("rowsExamined") Long rowsExamined
);
```

#### 3.4.3 AnalysisService 调用优化

**文件**: `src/main/java/com/dbdoctor/service/AnalysisService.java` (修改 handleExistingQuery)

```java
/**
 * 处理已存在的慢查询（老面孔）
 * 使用原子自增，避免并发统计错误
 */
private void handleExistingQuery(SlowQueryHistory history, SlowQueryLog slowLog, String cleanedSql) {
    String fingerprint = history.getSqlFingerprint();

    // 【优化】使用原子自增更新统计信息
    historyRepo.updateStatistics(
            fingerprint,
            LocalDateTime.now(),  // now
            slowLog.getQueryTime(),
            slowLog.getLockTime(),
            slowLog.getRowsSent(),
            slowLog.getRowsExamined()
    );

    log.info("📋 更新重复 SQL 统计: fingerprint={}, db={}", fingerprint, slowLog.getDbName());

    // 触发报告生成和通知（异步，使用智能通知策略判断）
    generateReportAndNotify(history);
}
```

---

### 3.5 分批拉取保护（防止正常情况下的 OOM）

#### 3.5.1 设计原则：不补发历史数据

> **核心理念**：项目只关注"从启动时刻开始"的慢查询，不做历史数据补充。

**为什么不补发历史？**
- ✅ 避免重启后被历史数据轰炸
- ✅ 逻辑简单，无状态压力
- ✅ 符合"实时监控"定位
- ✅ 历史慢查询对用户价值有限

**启动逻辑**：
```java
@PostConstruct
public void init() {
    // ✅ 游标直接初始化为当前时间
    // 不管停机多久，重启后只监控"现在"及之后产生的慢查询
    this.lastCheckTime = Timestamp.valueOf(LocalDateTime.now());

    log.info("🔍 DB-Doctor 慢查询表监控已启动");
    log.info("   监听时间点: {}", lastCheckTime);
    log.info("   轮询间隔: {} ms", properties.getPollIntervalMs());
    log.info("   每次最大记录数: {}", properties.getMaxRecordsPerPoll());
}
```

#### 3.5.2 为什么还需要分批拉取？

**问题**：虽然不补发历史，但正常情况下也可能有大量慢查询

**场景示例**：
```
用户正在运行一个批量导入脚本
    ↓
产生 5000 条慢查询（在 1 小时内）
    ↓
DB-Doctor 每 60 秒轮询一次
    ↓
如果一次性读取 5000 条：
    - 内存压力大
    - 队列容易满（容量只有 50）
    - 可能触发背压，阻塞扫描线程
```

**解决方案**：每次只读取 100 条，分批处理

```yaml
db-doctor:
  slow-log-monitor:
    # ✅ 分批拉取保护
    # 即使有大量慢查询，每次也只读 100 条
    max-records-per-poll: 100
```

**效果**：
```
第一批：处理 1-100 条，更新游标
等待 60 秒
第二批：处理 101-200 条
...
第五十批：处理 4901-5000 条

总耗时：50 分钟（慢慢消化，不阻塞）
队列压力：每次只有 100 个任务 ✅
```

#### 3.5.3 实现代码

**文件**: `src/main/java/com/dbdoctor/service/SlowLogTableMonitor.java`

```java
@PostConstruct
public void init() {
    // ✅ 游标初始化为当前时间（不补发历史）
    this.lastCheckTime = Timestamp.valueOf(LocalDateTime.now());

    log.info("🔍 DB-Doctor 慢查询表监控已启动");
    log.info("   📢 设计理念：实时监控，不补发历史数据");
    log.info("   ⏰ 监听起始时间: {}", lastCheckTime);
    log.info("   🔄 轮询间隔: {} ms", properties.getPollIntervalMs());
    log.info("   📦 每次最大记录数: {}", properties.getMaxRecordsPerPoll());
}

@Scheduled(fixedDelayString = "${db-doctor.slow-log-monitor.poll-interval-ms:60000}")
public void pollSlowLog() {
    if (ShutdownManager.isShuttingDown) {
        return;
    }

    try {
        String sql = String.format("""
            SELECT
                start_time,
                user_host,
                TIME_TO_SEC(query_time) + MICROSECOND(query_time)/1000000.0 as query_time_sec,
                TIME_TO_SEC(lock_time) + MICROSECOND(lock_time)/1000000.0 as lock_time_sec,
                rows_sent,
                rows_examined,
                db,
                CONVERT(sql_text USING utf8) AS sql_content
            FROM mysql.slow_log
            WHERE start_time > ?
            ORDER BY start_time ASC
            LIMIT %d  -- ✅ 分批拉取，防止 OOM
            """, properties.getMaxRecordsPerPoll());

        List<Map<String, Object>> logs = targetJdbcTemplate.queryForList(sql, lastCheckTime);

        if (logs.isEmpty()) {
            return;
        }

        log.info("🔍 捕获到 {} 条新的慢查询日志", logs.size());

        // 处理慢查询...
    } catch (Exception e) {
        if (!ShutdownManager.isShuttingDown) {
            log.error("❌ 轮询 mysql.slow_log 表失败", e);
        }
    }
}
```

#### 3.5.4 配置文件

**application.yml**

```yaml
db-doctor:
  slow-log-monitor:
    poll-interval-ms: 60000              # 轮询间隔（60秒）
    max-records-per-poll: 100            # ✅ 分批拉取（每次最多 100 条）

    # ❌ 不需要 max-catch-up-days 配置
    # 因为我们不补发历史数据，游标永远从"当前时间"开始
```

**DbDoctorProperties.java**

```java
@Data
@Component
@ConfigurationProperties(prefix = "db-doctor")
public class DbDoctorProperties {

    private SlowLogMonitorConfig slowLogMonitor = new SlowLogMonitorConfig();

    @Data
    public static class SlowLogMonitorConfig {
        private Integer pollIntervalMs = 60000;
        private Integer maxRecordsPerPoll = 100;  // ✅ 只需要这个

        // ❌ 不需要 maxCatchUpDays 字段
    }
}
```

#### 3.5.5 总结

| 场景 | 处理方式 | 说明 |
|------|----------|------|
| 应用重启 | 游标 = 当前时间 | 不补发历史数据 ✅ |
| 正常运行有大量慢查询 | 分批拉取（LIMIT 100） | 防止 OOM ✅ |
| 用户关机 10 天后重启 | 游标 = 当前时间 | 放弃 10 天的数据 ✅ |

**设计理念**：
> **轻量级、实时感知、零负担监控** - 只关注当下，不纠结历史

---

## 📊 第四部分：配置文件汇总

### 4.1 application.yml（通用配置）

```yaml
# 优雅停机配置
server:
  shutdown: graceful

spring:
  lifecycle:
    timeout-per-shutdown-phase: 60s

db-doctor:
  # === 慢查询监控配置 ===
  slow-log-monitor:
    poll-interval-ms: 60000              # 轮询间隔（60秒）
    max-records-per-poll: 100            # 每次最多读取 100 条（分批拉取保护）
    # 注意：不补发历史数据，游标始终从"当前时间"开始

    auto-cleanup:
      enabled: true                      # 自动清理 mysql.slow_log
      cron-expression: "0 0 3 * * ?"     # 每天凌晨 3 点

  # === 智能通知策略配置 ===
  notify:
    enabled-notifiers: email
    notify-interval: 3600                # 通知间隔（1 小时）
    severity-threshold: 3.0              # 严重程度阈值

    # 智能通知策略（冷却期 + 性能恶化 + 高频异常）
    cool-down-hours: 1                   # 冷却期时间（小时）
    degradation-multiplier: 1.5          # 性能恶化倍率
    high-frequency-threshold: 2          # 高频异常阈值（测试用，生产改为 100）

    email:
      enabled: true
      from: DB-Doctor <noreply@example.com>
      to:
        - dba@example.com
      cc: []

  # === PENDING 任务重试配置 ===
  retry:
    enabled: true                        # 是否启用 PENDING 补扫
    max-attempts: 3                      # 最大重试次数
    pending-interval-ms: 600000          # 补扫间隔（10 分钟）

  # === 优雅停机配置 ===
  shutdown:
    await-termination-seconds: 50        # 等待任务完成的最长时间
    clear-queue-on-shutdown: true        # 停机时是否清空队列

  # === 线程池配置 ===
  thread-pool:
    ai-analysis:
      core-size: 2                       # 核心线程数
      max-size: 4                        # 最大线程数
      queue-capacity: 50                 # 队列容量

  # === AI 配置（当前版本已禁用） ===
  ai:
    enabled: false
```

---

## ✅ 第五部分：实施计划与验收标准

### 5.1 实施优先级

| 优先级 | 功能模块 | 预计工时 | 风险等级 |
|--------|----------|----------|----------|
| P0 | 优雅停机机制 | 2 小时 | 低 |
| P0 | 启动自愈机制 | 1 小时 | 低 |
| P1 | PENDING 补扫 | 3 小时 | 中 |
| P1 | 并发统计优化 | 2 小时 | 中 |
| P2 | 分批拉取保护 | 0.5 小时 | 低 |

**总计**: 约 8.5 小时（1-2 个工作日）

### 5.2 实施步骤

#### 第一阶段：核心可靠性（P0）
1. ✅ 实现 `ShutdownManager` - 优雅停机
2. ✅ 修改 `ThreadPoolConfig` - 增加停机配置
3. ✅ 修改 `SlowLogTableMonitor` - 停机感知
4. ✅ 实现 `StartupHousekeeper` - 启动清理
5. ✅ 扩展 `SlowQueryHistory` 枚举 - ABANDONED 状态
6. ✅ 新增 Repository 方法 - `markPendingAsAbandoned()`

**验收标准**：
- [ ] Ctrl+C 停机时，日志显示优雅停机流程
- [ ] 队列中未开始的任务被清空
- [ ] 正在执行的任务完成后才关闭
- [ ] 重启后，旧的 PENDING 记录变为 ABANDONED

#### 第二阶段：补扫与并发（P1）
7. ✅ 实现 `PendingTaskRetryService` - PENDING 补扫
8. ✅ 扩展 `SlowQueryHistory` - retryCount 字段
9. ✅ 新增 Repository 方法 - `findPendingTasksForRetry()`
10. ✅ 新增 Repository 原子自增方法 - `updateStatistics()`
11. ✅ 修改 `AnalysisService` - 使用原子自增

**验收标准**：
- [ ] AI 失败后，PENDING 任务能自动重试
- [ ] 重试 3 次后，状态变为 FAILED
- [ ] 高并发下，统计次数准确（不丢失）

#### 第三阶段：配置优化（P2）
12. ✅ 修改 `application.yml` - 完善所有配置项
13. ✅ 修改 `DbDoctorProperties` - 新增配置字段

**验收标准**：
- [ ] 所有参数可通过配置文件修改
- [ ] 无硬编码的值
- [ ] 明确设计理念：不补发历史数据

### 5.3 测试用例

#### 测试 1：优雅停机
```bash
# 1. 启动应用
java -jar db-doctor.jar

# 2. 触发慢查询（生成一些 PENDING 任务）
mysql> SELECT SLEEP(3);

# 3. 等待 1 分钟，确保任务进入队列

# 4. 按 Ctrl+C 停止

# 预期日志：
# 📢 收到关闭指令，启动优雅停机流程...
# ✅ 第一步：已设置停机标志
# ✅ 第二步：已清空队列中待处理的任务数: 5
# ✅ 第三步：正在等待 2 个活跃诊断任务执行完毕...
# 🎉 优雅停机流程完成
```

#### 测试 2：启动自愈
```bash
# 1. 正常运行后，强制 kill -9（模拟崩溃）
kill -9 <pid>

# 2. 立即重启
java -jar db-doctor.jar

# 预期日志：
# 🧹 DB-Doctor 启动自检开始...
# ⚠️  发现 3 条上次运行中断的记录
# 📝 已将这些记录状态更新为 ABANDONED
# 🎉 启动自检完成，监控系统已就绪
```

#### 测试 3：PENDING 补扫
```bash
# 1. 手动将某条记录改为 PENDING 状态
UPDATE slow_query_history SET status = 'PENDING', retry_count = 0 WHERE id = 1;

# 2. 等待 10 分钟（补扫间隔）

# 预期日志：
# 🔍 发现 1 个待重试的任务
# 🔄 重试处理任务: fingerprint=xxx, retryCount=1
# 📋 生成报告: fingerprint=xxx
# ✅ 报告生成完成
```

#### 测试 4：并发统计
```bash
# 使用 JMeter 或脚本并发触发相同的慢查询
# 同时执行 100 次 SELECT SLEEP(3);

# 验证：
SELECT occurrence_count FROM slow_query_history WHERE sql_fingerprint = 'xxx';

# 预期：occurrence_count = 100（不是 50 或其他数值）
```

---

## 📈 第六部分：性能指标与监控

### 6.1 性能指标

| 指标 | 目标值 | 监控方式 |
|------|--------|----------|
| 慢查询捕获延迟 | < 60 秒 | 日志时间戳对比 |
| 报告生成延迟 | < 30 秒 | AnalysisService 日志 |
| 邮件发送延迟 | < 5 秒 | NotifyService 日志 |
| 队列堆积 | < 50 个 | ThreadPoolExecutor.getQueue().size() |
| PENDING 任务数 | < 10 个 | 定期查询 H2 |

### 6.2 监控端点（未来扩展）

**建议添加 Spring Boot Actuator**：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

**暴露端点**：
- `/actuator/health` - 健康检查
- `/actuator/metrics` - 性能指标
- `/actuator/info` - 版本信息

---

## 🎓 第七部分：架构演进路线图

### V1.0（当前版本）- 原型验证
- ✅ 基础慢查询监控
- ✅ SQL 指纹去重
- ✅ 智能通知策略
- ❌ 无优雅停机
- ❌ 无 PENDING 补扫

### V2.0（本次优化）- 工业级可靠
- ✅ 优雅停机机制
- ✅ 启动自愈机制
- ✅ PENDING 补扫
- ✅ 并发统计优化
- ✅ 完整配置化

### V3.0（未来扩展）- 企业级特性
- ⭐ 断点续传（游标持久化到 H2）
- ⭐ Web 管理界面（Vue3 + Element Plus）
- ⭐ 慢查询趋势分析（按天/周/月统计）
- ⭐ 多数据库支持（PostgreSQL、Oracle）
- ⭐ 分布式部署（支持多实例监控）
- ⭐ 消息队列集成（RabbitMQ/Kafka）

---

## 📝 第八部分：总结与建议

### 8.1 核心设计原则

1. **轻量级优先**
   - 不引入不必要的复杂性
   - 避免过度设计
   - 保持代码简洁可维护

2. **实时性 > 完整性**
   - 不补发历史数据
   - 只关注当下的慢查询
   - 避免重启后被历史数据轰炸

3. **快速失败（Fail-fast）**
   - 优雅停机时清空队列
   - 启动时清理旧 PENDING
   - 达到重试次数后标记 FAILED

4. **数据一致性**
   - 使用数据库原子自增
   - 避免并发统计误差
   - H2 文件锁防止多实例冲突

### 8.2 商业化价值

通过本次优化，DB-Doctor 将具备：

✅ **可靠性** - 优雅停机 + 启动自愈 + 自动重试
✅ **稳定性** - 并发保护 + 背压机制 + 快速失败
✅ **可配置性** - 所有参数可通过配置文件调整
✅ **可观测性** - 完整的日志记录 + 状态管理

**适用场景**：
- 开发者本地监控
- 测试环境慢查询分析
- 中小规模生产环境（单库 QPS < 5000）

**不适用场景**：
- 超大规模分布式数据库（建议使用专业 APM 工具）
- 需要历史数据分析的场景（本项目不补发历史）

### 8.3 后续建议

1. **添加单元测试**
   - 测试优雅停机流程
   - 测试并发原子自增
   - 测试 PENDING 补扫逻辑

2. **添加管理界面**
   - 查看 slow_query_history 表
   - 手动触发 FAILED 任务重试
   - 查看系统运行状态

3. **文档完善**
   - 用户使用手册
   - 部署运维手册
   - 故障排查手册

4. **性能测试**
   - 压测并发场景
   - 验证统计准确性
   - 测试海量积压处理

---

## 📚 附录

### 附录 A：参考资料

- [Spring Boot Graceful Shutdown](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.spring-application.graceful-shutdown)
- [ThreadPoolExecutor Documentation](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ThreadPoolExecutor.html)
- [H2 Database Locking](https://www.h2database.com/html/features.html#connection_modes)

### 附录 B：Gemini AI 核心观点总结

1. **性能瓶颈在 AI 和网络**，不在 Java 层
2. **CallerRunsPolicy 是背压机制**，防止 OOM
3. **优雅停机三部曲**：关水龙头 → 倒掉桶里的水 → 等碗里的饭吃完
4. **启动时清理旧账**：PENDING → ABANDONED
5. **原子自增**：使用 SQL 层面自增，避免并发误差
6. **不补发历史**：只关注当下，避免重启后被历史数据轰炸

### 附录 C：关键配置项速查表

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `server.shutdown` | graceful | 优雅停机开关 |
| `spring.lifecycle.timeout-per-shutdown-phase` | 60s | 停机最大等待时间 |
| `db-doctor.shutdown.await-termination-seconds` | 50 | 线程池等待时间 |
| `db-doctor.shutdown.clear-queue-on-shutdown` | true | 是否清空队列 |
| `db-doctor.retry.max-attempts` | 3 | 最大重试次数 |
| `db-doctor.retry.pending-interval-ms` | 600000 | 补扫间隔（10分钟） |
| `db-doctor.thread-pool.ai-analysis.core-size` | 2 | 核心线程数 |
| `db-doctor.thread-pool.ai-analysis.queue-capacity` | 50 | 队列容量 |

---

**文档结束**

*本文档由 DB-Doctor 开发团队维护，最后更新时间：2026-01-25*
