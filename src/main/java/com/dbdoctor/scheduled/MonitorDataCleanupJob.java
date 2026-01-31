package com.dbdoctor.scheduled;

import com.dbdoctor.config.DbDoctorProperties;
import com.dbdoctor.service.AiInvocationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * AI 监控数据清理定时任务
 *
 * <p>定期清理超过保留期的 AI 监控数据</p>
 *
 * @author DB-Doctor
 * @version 2.3.0
 * @since 2.3.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MonitorDataCleanupJob {

    private final AiInvocationLogService logService;
    private final DbDoctorProperties properties;

    /**
     * 定时清理 AI 监控数据
     *
     * <p>每天凌晨 2 点执行一次</p>
     * <p>Cron 表达式：0 0 2 * * ?</p>
     *
     * 清理规则：
     * - 删除 created_time 早于（当前时间 - retention-days）的数据
     * - 默认保留 90 天，可通过配置修改
     */
    @Scheduled(cron = "${db-doctor.monitoring.auto-cleanup.cron-expression:0 0 2 * * ?}")
    public void cleanupExpiredMonitorData() {
        // 检查是否启用自动清理
        if (properties.getMonitoring() == null ||
            properties.getMonitoring().getAutoCleanup() == null ||
            !properties.getMonitoring().getAutoCleanup().getEnabled()) {
            log.debug("[AI监控] 自动清理未启用，跳过本次执行");
            return;
        }

        log.info("[AI监控] 开始执行数据清理定时任务");

        try {
            // 从配置中获取保留天数（默认 90 天）
            int retentionDays = getRetentionDays();
            LocalDateTime beforeTime = LocalDateTime.now().minusDays(retentionDays);

            log.info("[AI监控] 清理 {} 天前的数据（删除 {} 之前的数据）",
                    retentionDays, beforeTime);

            // 执行删除
            int deletedCount = logService.deleteByCreatedTimeBefore(beforeTime);

            log.info("[AI监控] 数据清理完成：删除 {} 条记录", deletedCount);

        } catch (Exception e) {
            log.error("[AI监控] 数据清理失败", e);
        }
    }

    /**
     * 获取数据保留天数
     *
     * @return 保留天数
     */
    private int getRetentionDays() {
        // 优先从 monitoring 配置读取
        if (properties.getMonitoring() != null &&
            properties.getMonitoring().getRetentionDays() != null) {
            return properties.getMonitoring().getRetentionDays();
        }

        // 默认 90 天
        return 90;
    }
}
