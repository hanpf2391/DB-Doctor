package com.dbdoctor.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 监控线程池配置
 *
 * <p>为 AI 监控日志的异步写入提供专用线程池</p>
 *
 * @author DB-Doctor
 * @version 2.3.0
 * @since 2.3.0
 */
@Slf4j
@Configuration
@EnableAsync
public class MonitoringConfig implements AsyncConfigurer {

    /**
     * 监控日志异步写入线程池
     *
     * <p>核心配置：</p>
     * <ul>
     *   <li>核心线程数：2</li>
     *   <li>最大线程数：5</li>
     *   <li>队列容量：1000</li>
     *   <li>拒绝策略：CallerRunsPolicy（主线程执行，保证不丢数据）</li>
     * </ul>
     *
     * @return 线程池执行器
     */
    @Bean("monitoringExecutor")
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 核心线程数
        executor.setCorePoolSize(2);

        // 最大线程数
        executor.setMaxPoolSize(5);

        // 队列容量
        executor.setQueueCapacity(1000);

        // 线程空闲时间（秒）
        executor.setKeepAliveSeconds(60);

        // 线程名称前缀
        executor.setThreadNamePrefix("ai-monitor-");

        // 拒绝策略：由调用线程执行（保证不丢数据）
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 等待所有任务完成后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);

        // 等待时间（秒）
        executor.setAwaitTerminationSeconds(60);

        executor.initialize();

        log.info("✅ 监控线程池已初始化: corePoolSize={}, maxPoolSize={}, queueCapacity={}",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());

        return executor;
    }

    /**
     * 异步异常处理器
     *
     * @return 异常处理器
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new AsyncUncaughtExceptionHandler() {
            @Override
            public void handleUncaughtException(Throwable ex, java.lang.reflect.Method method, Object... params) {
                log.error("[AI监控] 异步任务执行异常: method={}, message={}",
                        method.getName(), ex.getMessage(), ex);

                // 这里可以添加告警逻辑，例如发送邮件、钉钉通知等
            }
        };
    }
}
