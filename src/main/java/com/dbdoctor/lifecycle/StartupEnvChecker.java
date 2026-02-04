package com.dbdoctor.lifecycle;

import com.dbdoctor.check.MySqlEnvChecker;
import com.dbdoctor.model.EnvCheckReport;
import com.dbdoctor.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * 启动时环境检查器
 *
 * 核心逻辑：
 * 1. 检查 H2 中是否有数据库配置
 * 2. 如果有配置 → 执行环境检查，失败则警告但允许启动
 * 3. 如果无配置 → 跳过检查，提示用户配置
 *
 * @author DB-Doctor
 * @version 3.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StartupEnvChecker implements ApplicationListener<ApplicationReadyEvent> {

    private final SystemConfigService configService;
    private final MySqlEnvChecker envChecker;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🔍 [启动检查] 开始检查 MySQL 环境配置...");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        try {
            // 1. 检查 H2 中是否有数据库配置
            boolean hasConfig = hasDatabaseConfig();

            if (!hasConfig) {
                // 场景1：首次启动，无配置
                log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                log.info("📝 [启动检查] 首次启动，未检测到数据库配置");
                log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                log.info("┌─────────────────────────────────────────────────────────┐");
                log.info("│ 🔧 下一步操作：                                           │");
                log.info("│   1. 访问 http://localhost:8080 或前端页面                │");
                log.info("│   2. 进入「设置中心」→「目标数据库」                    │");
                log.info("│   3. 点击「测试连接」验证数据库配置                      │");
                log.info("│   4. 测试通过后点击「保存配置」                         │");
                log.info("│   5. 重启服务使配置生效                                   │");
                log.info("└─────────────────────────────────────────────────────────┘");
                log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                log.info("⏭️  [启动检查] 跳过环境检查（等待用户配置）");
                log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                return;
            }

            // 场景2：非首次启动，有配置
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.info("📋 [启动检查] 检测到数据库配置，开始环境验证...");
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            // 从 H2 读取配置
            String url = configService.getDecryptedValue("database.url");
            String username = configService.getDecryptedValue("database.username");
            String password = configService.getDecryptedValue("database.password");

            // 执行环境检查
            EnvCheckReport report = envChecker.checkFully(url, username, password);

            // 根据检查结果输出日志
            if (report.isOverallPassed()) {
                // 环境检查通过
                log.info("┌─────────────────────────────────────────────────────────┐");
                log.info("│ ✅ 环境检查通过                                          │");
                log.info("│                                                         │");
                log.info("│ 🔍 慢查询监控：已启用                                   │");
                log.info("│ 📊 定时任务：运行中                                     │");
                log.info("│                                                         │");
                log.info("│ 🎉 DB-Doctor 已就绪，开始监听慢查询！                  │");
                log.info("└─────────────────────────────────────────────────────────┘");

            } else {
                // 环境检查失败（但允许启动）
                log.warn("┌─────────────────────────────────────────────────────────┐");
                log.warn("│ ⚠️  环境检查发现问题                                     │");
                log.warn("│                                                         │");

                if (!report.isConnectionSuccess()) {
                    log.warn("│ ❌ 数据库连接失败                                       │");
                    log.warn("│    {}                                     │",
                        report.getConnectionError().replace("\n", "\n│    "));
                }

                // 打印未通过的检查项
                if (report.getItems() != null) {
                    report.getItems().stream()
                        .filter(item -> !item.isPassed())
                        .forEach(item -> {
                            log.warn("│ ❌ {} = {}", item.getName(), item.getCurrentValue());
                            log.warn("│    建议：{}", item.getErrorMessage().replace("\n", "\n│    "));
                            if (item.getFixCommand() != null) {
                                log.warn("│    修复：{}", item.getFixCommand());
                            }
                        });
                }

                log.warn("│                                                         │");
                log.warn("│ ⚠️  警告：环境未正确配置，DB-Doctor 可能无法正常工作      │");
                log.warn("│ ⚠️  建议：请修复上述问题后重启服务                       │");
                log.warn("└─────────────────────────────────────────────────────────┘");
            }

            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.info("✅ [启动检查] 检查完成");
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        } catch (Exception e) {
            log.error("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.error("❌ [启动检查] 检查过程发生错误", e);
            log.error("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.warn("⚠️  [启动检查] 无法验证环境，DB-Doctor 可能无法正常工作");
        }
    }

    /**
     * 检查 H2 中是否有数据库配置
     */
    private boolean hasDatabaseConfig() {
        try {
            String url = configService.getDecryptedValue("database.url");
            String username = configService.getDecryptedValue("database.username");
            String password = configService.getDecryptedValue("database.password");

            return url != null && !url.trim().isEmpty()
                && username != null && !username.trim().isEmpty()
                && password != null && !password.trim().isEmpty();

        } catch (Exception e) {
            log.debug("检查数据库配置时发生异常: {}", e.getMessage());
            return false;
        }
    }
}
