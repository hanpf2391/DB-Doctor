package com.dbdoctor.lifecycle;

import com.dbdoctor.config.DynamicDataSourceManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 动态数据源初始化器
 *
 * <p>在应用启动完成后初始化动态数据源</p>
 *
 * @author DB-Doctor
 * @version 3.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(1) // 确保在 SystemConfigInitializer 之后执行
public class DynamicDataSourceInitializer implements ApplicationListener<ApplicationReadyEvent> {

    private final DynamicDataSourceManager dynamicDataSourceManager;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🔄 动态数据源初始化开始...");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        try {
            // 初始化动态数据源（从 H2 数据库读取配置）
            dynamicDataSourceManager.initializeTargetDataSource();

            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.info("✅ 动态数据源初始化完成");
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        } catch (Exception e) {
            log.error("❌ 动态数据源初始化失败", e);
            log.warn("⚠️  目标数据源未初始化，部分功能可能无法使用");
            log.info("💡 提示：请在前端配置数据库连接信息");
        }
    }
}
