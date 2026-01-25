package com.dbdoctor.service;

import com.dbdoctor.config.DbDoctorProperties;
import com.dbdoctor.lifecycle.ShutdownManager;
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
        if (ShutdownManager.isShuttingDown) {
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
