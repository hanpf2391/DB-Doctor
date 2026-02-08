package com.dbdoctor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 定时批量通知配置 DTO
 *
 * <p>包含 Cron 表达式和启用的通知渠道</p>
 *
 * @author DB-Doctor
 * @version 3.2.0
 * @since 3.2.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationScheduleConfigDTO {

    /**
     * Cron 表达式
     * <p>格式: 秒 分 时 日 月 周 [年]</p>
     * <p>支持 6 段或 7 段格式</p>
     * <p>示例: "0 0 * * * ?" 表示每小时执行一次</p>
     */
    @NotBlank(message = "Cron 表达式不能为空")
    @Pattern(regexp = "^[0-9?*/\\-,]+(\\s+[0-9?*/\\-,]+){5,6}$",
             message = "Cron 表达式格式不正确，格式: 秒 分 时 日 月 周 [年]")
    private String batchCron;

    /**
     * 启用的通知渠道列表
     * <p>可选值: EMAIL, DINGTALK, WECOM, FEISHU</p>
     */
    private List<String> enabledChannels;

    /**
     * 验证渠道列表
     *
     * @throws IllegalArgumentException 如果渠道列表为空
     */
    public void validateChannels() {
        if (enabledChannels == null || enabledChannels.isEmpty()) {
            throw new IllegalArgumentException("至少启用一个通知渠道");
        }

        // 验证渠道名称是否合法
        List<String> validChannels = List.of("EMAIL", "DINGTALK", "WECOM", "FEISHU");
        for (String channel : enabledChannels) {
            if (!validChannels.contains(channel)) {
                throw new IllegalArgumentException("无效的通知渠道: " + channel);
            }
        }
    }
}
