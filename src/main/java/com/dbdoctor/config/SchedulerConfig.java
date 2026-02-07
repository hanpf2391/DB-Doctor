package com.dbdoctor.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * 调度器配置
 * 提供动态任务调度支持
 *
 * @author DB-Doctor
 * @version 3.2.0
 * @since 3.2.0
 */
@Configuration
public class SchedulerConfig {

    /**
     * 创建任务调度器 Bean
     * 用于动态调度定时任务
     */
    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1); // 单线程池，避免任务重复执行
        scheduler.setThreadNamePrefix("dynamic-scheduler-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(60);
        scheduler.initialize();
        return scheduler;
    }
}
