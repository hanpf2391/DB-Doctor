package com.dbdoctor.monitoring.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知发送结果
 *
 * <p>表示告警通知的发送结果（可能包含多个渠道）</p>
 *
 * @author DB-Doctor
 * @version 3.2.0
 * @since 3.2.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResult {

    /**
     * 告警ID
     */
    private Long alertId;

    /**
     * 发送时间
     */
    private LocalDateTime sentAt;

    /**
     * 是否全部成功
     */
    private boolean success;

    /**
     * 各渠道发送结果
     */
    private List<ChannelResult> channelResults;

    /**
     * 成功的渠道数量
     */
    public int getSuccessCount() {
        if (channelResults == null) {
            return 0;
        }
        return (int) channelResults.stream()
            .filter(ChannelResult::isSuccess)
            .count();
    }

    /**
     * 失败的渠道数量
     */
    public int getFailedCount() {
        if (channelResults == null) {
            return 0;
        }
        return (int) channelResults.stream()
            .filter(result -> !result.isSuccess())
            .count();
    }
}
