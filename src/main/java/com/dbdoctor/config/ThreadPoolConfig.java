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

    /**
     * AI 分析线程池
     * 用于异步处理慢查询分析任务
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

        // 拒绝策略：调用者运行，保证任务不丢失
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 等待所有任务结束后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);

        // 等待时间（秒）
        executor.setAwaitTerminationSeconds(60);

        executor.initialize();

        log.info("🔧 AI 分析线程池初始化完成: coreSize={}, maxSize={}, queueCapacity={}",
                coreSize, maxSize, queueCapacity);

        return executor;
    }
}
