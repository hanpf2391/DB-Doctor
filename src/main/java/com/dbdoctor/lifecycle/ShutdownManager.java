package com.dbdoctor.lifecycle;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.util.concurrent.Executor;

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
    private Executor analysisExecutor;

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
            if (clearQueueOnShutdown && analysisExecutor instanceof ThreadPoolTaskExecutor) {
                ThreadPoolTaskExecutor taskExecutor = (ThreadPoolTaskExecutor) analysisExecutor;
                int queueSize = taskExecutor.getThreadPoolExecutor().getQueue().size();
                int discardedTasks = queueSize;

                taskExecutor.getThreadPoolExecutor().getQueue().clear();

                log.info("✅ 第二步：已清空队列中待处理的任务数: {}", discardedTasks);
            } else {
                log.info("✅ 第二步：保留队列中的任务，等待处理完成");
            }

            // 第三步：等待正在执行的任务
            if (analysisExecutor instanceof ThreadPoolTaskExecutor) {
                ThreadPoolTaskExecutor taskExecutor = (ThreadPoolTaskExecutor) analysisExecutor;
                int activeCount = taskExecutor.getActiveCount();
                if (activeCount > 0) {
                    log.info("✅ 第三步：正在等待 {} 个活跃诊断任务执行完毕...", activeCount);
                    log.info("⏳ 最长等待时间由 ThreadPoolConfig.awaitTerminationSeconds 决定");
                } else {
                    log.info("✅ 第三步：无活跃任务，可以立即关闭");
                }
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
