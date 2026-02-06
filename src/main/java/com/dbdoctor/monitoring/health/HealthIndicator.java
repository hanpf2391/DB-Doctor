package com.dbdoctor.monitoring.health;

import java.util.Map;

/**
 * 健康指标接口
 *
 * <p>系统各组件需要实现此接口以提供健康状态检查功能</p>
 *
 * @author DB-Doctor
 * @version 3.2.0
 * @since 3.2.0
 */
public interface HealthIndicator {

    /**
     * 获取指标名称
     *
     * @return 指标名称
     */
    String getName();

    /**
     * 获取指标显示名称
     *
     * @return 显示名称
     */
    String getDisplayName();

    /**
     * 执行健康检查
     *
     * @return 健康状态结果
     */
    IndicatorStatus check();
}
