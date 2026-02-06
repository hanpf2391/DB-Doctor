package com.dbdoctor.monitoring.notification;

import com.dbdoctor.entity.AlertHistory;

/**
 * 通知器接口
 *
 * <p>所有通知渠道都需要实现此接口</p>
 *
 * @author DB-Doctor
 * @version 3.2.0
 * @since 3.2.0
 */
public interface Notifier {

    /**
     * 判断是否支持该渠道
     *
     * @param channel 通知渠道
     * @return 是否支持
     */
    boolean supports(NotificationChannel channel);

    /**
     * 发送通知
     *
     * @param alert 告警对象
     * @return 发送结果
     */
    ChannelResult notify(AlertHistory alert);
}
