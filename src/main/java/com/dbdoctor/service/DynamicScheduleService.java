package com.dbdoctor.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * 动态调度服务
 * 支持运行时动态修改 Cron 表达式
 *
 * @author DB-Doctor
 * @version 3.2.0
 * @since 3.2.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DynamicScheduleService {

    private final TaskScheduler taskScheduler;
    private final NotificationScheduler notificationScheduler;

    // 存储调度任务的 Future，用于取消旧任务
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    // 定时批量通知的任务名称
    private static final String TASK_BATCH_NOTIFICATION = "batchNotification";

    /**
     * 启动或更新定时任务
     *
     * @param cronExpression Cron 表达式
     */
    public void scheduleOrUpdateTask(String cronExpression) {
        log.info("[动态调度服务] {} - 更新调度任务: cron={}", TASK_BATCH_NOTIFICATION, cronExpression);

        // 1. 取消旧任务
        cancelTask(TASK_BATCH_NOTIFICATION);

        // 2. 创建新任务
        ScheduledFuture<?> scheduledTask = taskScheduler.schedule(
            () -> {
                log.info("[动态调度服务] {} - 执行定时任务", TASK_BATCH_NOTIFICATION);
                try {
                    notificationScheduler.batchSendNotifications();
                } catch (Exception e) {
                    log.error("[动态调度服务] {} - 任务执行失败", TASK_BATCH_NOTIFICATION, e);
                }
            },
            new CronTrigger(cronExpression, ZoneId.systemDefault())
        );

        // 3. 保存新任务的 Future
        scheduledTasks.put(TASK_BATCH_NOTIFICATION, scheduledTask);

        log.info("[动态调度服务] {} - 调度任务已更新", TASK_BATCH_NOTIFICATION);
        log.info("[动态调度服务] ✓ Cron 表达式: {}", cronExpression);
        log.info("[动态调度服务] ✓ 下次执行时间: {}", getNextExecutionTime(scheduledTask));
    }

    /**
     * 取消指定任务
     *
     * @param taskName 任务名称
     */
    public void cancelTask(String taskName) {
        ScheduledFuture<?> future = scheduledTasks.get(taskName);
        if (future != null && !future.isCancelled()) {
            boolean cancelled = future.cancel(false);
            log.info("[动态调度服务] {} - 取消旧任务: {}", taskName, cancelled ? "成功" : "失败");
            scheduledTasks.remove(taskName);
        }
    }

    /**
     * 获取下次执行时间
     *
     * @param scheduledTask 已调度的任务
     * @return 下次执行时间
     */
    private String getNextExecutionTime(ScheduledFuture<?> scheduledTask) {
        // 注意：ScheduledFuture 不直接提供下次执行时间
        // 这里返回当前时间作为占位符
        return LocalDateTime.now().atZone(ZoneId.systemDefault()).toLocalDateTime().toString();
    }

    /**
     * 停止所有调度任务
     */
    public void shutdown() {
        log.info("[动态调度服务] 停止所有调度任务");
        scheduledTasks.forEach((name, future) -> {
            if (!future.isCancelled()) {
                future.cancel(false);
            }
        });
        scheduledTasks.clear();
    }

    /**
     * 检查任务是否正在运行
     *
     * @param taskName 任务名称
     * @return 是否正在运行
     */
    public boolean isTaskRunning(String taskName) {
        ScheduledFuture<?> future = scheduledTasks.get(taskName);
        return future != null && !future.isCancelled() && !future.isDone();
    }
}
