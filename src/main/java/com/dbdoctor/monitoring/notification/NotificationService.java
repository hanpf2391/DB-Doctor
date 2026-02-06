package com.dbdoctor.monitoring.notification;

import com.dbdoctor.entity.AlertHistory;

/**
 * 通知服务接口
 *
 * @author DB-Doctor
 * @version 3.2.0
 * @since 3.2.0
 */
public interface NotificationService {

    /**
     * 发送告警通知
     *
     * @param alert 告警对象
     * @return 通知结果
     */
    NotificationResult sendNotification(AlertHistory alert);

    /**
     * 发送测试通知
     *
     * @param channel 通知渠道
     * @return 是否成功
     */
    boolean sendTestNotification(NotificationChannel channel);
}
