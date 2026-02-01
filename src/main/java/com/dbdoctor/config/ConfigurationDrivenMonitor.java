package com.dbdoctor.config;

import com.dbdoctor.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 配置驱动的启动检查器
 *
 * <p>核心功能：</p>
 * <ul>
 *   <li>应用启动后检查数据库配置是否完成</li>
 *   <li>配置未完成时记录警告日志</li>
 *   <li>配置完成后确认系统可以正常工作</li>
 * </ul>
 *
 * <p>注意：</p>
 * <pre>
 * 1. 不手动控制 SlowLogTableMonitor 的启动（它使用 @Scheduled 自动运行）
 * 2. 只检查配置完整性并记录日志
 * 3. 用户需要在页面配置数据库后重启服务
 * </pre>
 *
 * @author DB-Doctor
 * @version 2.4.0
 * @since 2.4.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConfigurationDrivenMonitor implements ApplicationListener<ApplicationReadyEvent> {

    private final SystemConfigService configService;

    /**
     * 应用启动完成后执行
     *
     * @param event 应用就绪事件
     */
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("[配置驱动启动] 开始检查数据库配置状态...");

        try {
            // 检查数据库配置是否完成
            Map<String, Object> dbStatus = configService.checkGroupCompleteness("database");

            boolean isComplete = (Boolean) dbStatus.get("isComplete");

            if (!isComplete) {
                long missingCount = (Long) dbStatus.get("missingRequiredCount");
                log.warn("┌─────────────────────────────────────────────────────────┐");
                log.warn("│ ⚠️  数据库配置未完成，缺少 {} 个必填项                      │", missingCount);
                log.warn("│                                                         │");
                log.warn("│ 📝 下一步操作：                                           │");
                log.warn("│   1. 访问 http://localhost:8080 或前端页面                │");
                log.warn("│   2. 进入「设置中心」→「目标数据库」                    │");
                log.warn("│   3. 填写数据库连接信息并测试连接                       │");
                log.warn("│   4. 保存配置并重启服务                                   │");
                log.warn("│                                                         │");
                log.warn("│ 📌 提示：配置保存在数据库中，重启后自动加载               │");
                log.warn("└─────────────────────────────────────────────────────────┘");
                return;
            }

            // 配置已完成
            log.info("┌─────────────────────────────────────────────────────────┐");
            log.info("│ ✅ 数据库配置检查通过                                     │");
            log.info("│                                                         │");
            log.info("│ 🔍 慢查询监控：自动启动中...                             │");
            log.info("│ 📊 定时任务：运行中                                       │");
            log.info("│                                                         │");
            log.info("│ 🎉 DB-Doctor 已就绪，开始监听慢查询！                    │");
            log.info("└─────────────────────────────────────────────────────────┘");

        } catch (Exception e) {
            log.error("[配置驱动启动] 检查配置时发生错误", e);
            log.warn("[配置驱动启动] 慢查询监听可能无法正常工作，请检查配置");
        }
    }

    /**
     * 重新加载配置（热加载）
     *
     * <p>当用户在页面更新配置后调用</p>
     */
    public void reloadConfiguration() {
        log.info("[配置驱动启动] 收到配置更新请求");
        log.info("[配置驱动启动] 配置已保存，请重启服务以应用新配置");
    }
}
