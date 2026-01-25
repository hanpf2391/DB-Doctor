package com.dbdoctor.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池配置类
 * 配置 AI 分析的异步线程池
 *
 * @author DB-Doctor
 * @version 2.0.0
 */
@Slf4j
@Configuration
@EnableAsync
public class ThreadPoolConfig {

    @Value("${db-doctor.thread-pool.ai-analysis.core-size:2}")
    private int coreSize;

    @Value("${db-doctor.thread-pool.ai-analysis.max-size:4}")
    private int maxSize;

    @Value("${db-doctor.thread-pool.ai-analysis.queue-capacity:50}")
    private int queueCapacity;

    @Value("${db-doctor.shutdown.await-termination-seconds:50}")
    private int awaitTerminationSeconds;

    /**
     * AI 分析线程池
     * 用于异步处理慢查询分析任务
     *
     * 优化点：
     * 1. 拒绝策略：CallerRunsPolicy（背压机制）
     * 2. 优雅停机：等待任务完成后才关闭
     */
    @Bean("analysisExecutor")
    public Executor analysisExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 核心线程数
        executor.setCorePoolSize(coreSize);

        // 最大线程数
        executor.setMaxPoolSize(maxSize);

        // 队列容量
        executor.setQueueCapacity(queueCapacity);

        // 线程名称前缀
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
}
