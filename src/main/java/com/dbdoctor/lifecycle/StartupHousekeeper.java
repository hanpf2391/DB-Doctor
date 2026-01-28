package com.dbdoctor.lifecycle;

import com.dbdoctor.entity.SlowQueryTemplate;
import com.dbdoctor.repository.SlowQueryTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 启动清理器（V2.1.0 - 使用 Template 架构）
 * 应用启动后执行一次，清理上次运行遗留的 PENDING 状态记录
 *
 * 核心逻辑：
 * 1. 将所有旧的 PENDING 记录改为 ABANDONED（已放弃）
 * 2. 防止重启后误处理历史数据
 * 3. 保证 PENDING 状态只代表"本次运行中正在处理的任务"
 *
 * 设计理念：
 * - 不补发历史数据，重启后放弃上次运行中断的任务
 * - ABANDONED 状态可在管理后台查看，但不会自动重试
 *
 * @author DB-Doctor
 * @version 2.1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StartupHousekeeper implements ApplicationRunner {

    private final SlowQueryTemplateRepository templateRepo;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🧹 DB-Doctor 启动自检开始...");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        try {
            // 将所有 PENDING 状态改为 ABANDONED
            int affectedRows = templateRepo.markPendingAsAbandoned();

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
