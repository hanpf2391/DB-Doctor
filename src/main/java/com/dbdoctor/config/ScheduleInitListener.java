package com.dbdoctor.config;

import com.dbdoctor.service.DynamicScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 调度任务初始化监听器
 * 在应用启动后自动初始化定时任务
 *
 * @author DB-Doctor
 * @version 3.2.0
 * @since 3.2.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleInitListener {

    private final DynamicScheduleService dynamicScheduleService;
    private final DbDoctorProperties properties;

    /**
     * 应用启动完成后初始化调度任务
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("========================================");
        log.info("📬 初始化定时批量通知任务");
        log.info("========================================");

        // 从配置文件读取 Cron 表达式
        String cronExpression = properties.getNotify().getBatchCron();
        log.info("✓ Cron 表达式: {}", cronExpression);

        // 启动动态调度
        dynamicScheduleService.scheduleOrUpdateTask(cronExpression);

        log.info("✅ 定时任务初始化完成");
        log.info("========================================");
    }
}
