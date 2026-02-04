package com.dbdoctor.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 数据源连接状态持有者
 *
 * <p>功能：</p>
 * <ul>
 *   <li>维护数据源连接状态（内存）</li>
 *   <li>由 SlowLogTableMonitor 定时任务更新状态</li>
 *   <li>提供状态查询接口</li>
 * </ul>
 *
 * @author DB-Doctor
 * @version 3.0.0
 */
@Slf4j
@Component
public class DataSourceStatusHolder {

    /**
     * 连接状态
     */
    private volatile boolean connected = false;

    /**
     * 最后一次错误信息
     */
    private volatile String lastError = null;

    /**
     * 最后一次检查时间
     */
    private volatile LocalDateTime lastCheckTime = null;

    /**
     * 最后一次成功连接时间
     */
    private volatile LocalDateTime lastSuccessTime = null;

    /**
     * 更新成功状态（定时任务调用）
     */
    public void updateSuccess() {
        boolean wasConnected = this.connected;
        this.connected = true;
        this.lastError = null;
        this.lastCheckTime = LocalDateTime.now();

        if (!wasConnected) {
            log.info("✅ [数据源状态] 连接恢复");
            this.lastSuccessTime = LocalDateTime.now();
        }
    }

    /**
     * 更新失败状态（定时任务调用）
     *
     * @param error 错误信息
     */
    public void updateFailure(String error) {
        boolean wasConnected = this.connected;
        this.connected = false;
        this.lastError = error;
        this.lastCheckTime = LocalDateTime.now();

        if (wasConnected) {
            log.error("❌ [数据源状态] 连接失败: {}", error);
        }
    }

    /**
     * 获取当前状态
     *
     * @return 状态信息
     */
    public DataSourceStatusVO getStatus() {
        return new DataSourceStatusVO(
            connected,
            lastError,
            lastCheckTime,
            lastSuccessTime
        );
    }

    /**
     * 数据源状态 VO
     */
    @Data
    public static class DataSourceStatusVO {
        /**
         * 是否已连接
         */
        private final boolean connected;

        /**
         * 最后一次错误信息
         */
        private final String lastError;

        /**
         * 最后一次检查时间
         */
        private final LocalDateTime lastCheckTime;

        /**
         * 最后一次成功连接时间
         */
        private final LocalDateTime lastSuccessTime;

        public DataSourceStatusVO(boolean connected, String lastError,
                                   LocalDateTime lastCheckTime, LocalDateTime lastSuccessTime) {
            this.connected = connected;
            this.lastError = lastError;
            this.lastCheckTime = lastCheckTime;
            this.lastSuccessTime = lastSuccessTime;
        }
    }
}
