package com.dbdoctor.service;

import org.springframework.stereotype.Service;

/**
 * 通知服务
 * 负责将 AI 生成的诊断报告发送给用户（邮件、Webhook）
 *
 * TODO: 在第5步"通知模块开发"中实现
 *
 * @author DB-Doctor
 * @version 1.0.0
 */
@Service
public class NotifyService {

    // TODO: 注入所有 Notifier 实现
    // TODO: 实现通知频率控制

    /**
     * 发送通知到所有启用的渠道
     *
     * TODO: 使用 mysql.slow_log 表作为数据源后，重新定义方法参数
     *
     * @param report AI 生成的诊断报告（Markdown 格式）
     */
    public void sendNotification(String report) {
        // TODO: 实现通知发送逻辑
        // 1. 检查通知频率控制
        // 2. 并发发送到所有启用的通知渠道
    }
}
