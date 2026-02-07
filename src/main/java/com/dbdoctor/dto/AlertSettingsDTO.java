package com.dbdoctor.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 告警设置 DTO
 *
 * <p>包含3个核心告警参数的配置</p>
 *
 * @author DB-Doctor
 * @version 3.2.0
 * @since 3.2.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertSettingsDTO {

    /**
     * 严重程度阈值（秒）
     * <p>平均查询耗时低于此值不发送通知</p>
     * <p>取值范围: 1.0-10.0</p>
     */
    @DecimalMin(value = "1.0", message = "严重程度阈值不能小于 1.0")
    @DecimalMax(value = "10.0", message = "严重程度阈值不能大于 10.0")
    private Double severityThreshold;

    /**
     * 冷却期（小时）
     * <p>同一 SQL 两次通知的最小间隔时间</p>
     * <p>取值范围: 1-168（1小时-1周）</p>
     */
    @Min(value = 1, message = "冷却期不能小于 1 小时")
    @Max(value = 168, message = "冷却期不能大于 168 小时（1周）")
    private Integer coolDownHours;

    /**
     * 性能恶化倍率
     * <p>当前耗时/上次通知耗时 >= 此值时触发二次唤醒通知</p>
     * <p>取值范围: 1.1-10.0</p>
     */
    @DecimalMin(value = "1.1", message = "性能恶化倍率不能小于 1.1")
    @DecimalMax(value = "10.0", message = "性能恶化倍率不能大于 10.0")
    private Double degradationMultiplier;
}
