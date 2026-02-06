package com.dbdoctor.monitoring.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通知发送结果
 *
 * <p>表示单个通知渠道的发送结果</p>
 *
 * @author DB-Doctor
 * @version 3.2.0
 * @since 3.2.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelResult {

    /**
     * 通知渠道
     */
    private NotificationChannel channel;

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 错误消息（失败时）
     */
    private String errorMessage;

    /**
     * 响应内容（可选）
     */
    private String response;

    /**
     * 创建成功结果
     */
    public static ChannelResult success(NotificationChannel channel) {
        return ChannelResult.builder()
            .channel(channel)
            .success(true)
            .build();
    }

    /**
     * 创建成功结果（带响应）
     */
    public static ChannelResult success(NotificationChannel channel, String response) {
        return ChannelResult.builder()
            .channel(channel)
            .success(true)
            .response(response)
            .build();
    }

    /**
     * 创建失败结果
     */
    public static ChannelResult failed(NotificationChannel channel, String errorMessage) {
        return ChannelResult.builder()
            .channel(channel)
            .success(false)
            .errorMessage(errorMessage)
            .build();
    }
}
