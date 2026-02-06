package com.dbdoctor.monitoring.health;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * 健康指标状态
 *
 * <p>表示单个组件的健康检查结果</p>
 *
 * @author DB-Doctor
 * @version 3.2.0
 * @since 3.2.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndicatorStatus {

    /**
     * 是否健康
     */
    private boolean healthy;

    /**
     * 状态消息
     */
    private String message;

    /**
     * 详细信息（键值对）
     */
    @Builder.Default
    private Map<String, Object> details = new HashMap<>();

    /**
     * 创建健康状态
     */
    public static IndicatorStatus healthy(String message) {
        return IndicatorStatus.builder()
            .healthy(true)
            .message(message)
            .build();
    }

    /**
     * 创建健康状态（带详细信息）
     */
    public static IndicatorStatus healthy(String message, Map<String, Object> details) {
        return IndicatorStatus.builder()
            .healthy(true)
            .message(message)
            .details(details)
            .build();
    }

    /**
     * 创建不健康状态
     */
    public static IndicatorStatus unhealthy(String message) {
        return IndicatorStatus.builder()
            .healthy(false)
            .message(message)
            .build();
    }

    /**
     * 创建不健康状态（带详细信息）
     */
    public static IndicatorStatus unhealthy(String message, Map<String, Object> details) {
        return IndicatorStatus.builder()
            .healthy(false)
            .message(message)
            .details(details)
            .build();
    }

    /**
     * 添加详细信息
     */
    public void addDetail(String key, Object value) {
        if (this.details == null) {
            this.details = new HashMap<>();
        }
        this.details.put(key, value);
    }
}
