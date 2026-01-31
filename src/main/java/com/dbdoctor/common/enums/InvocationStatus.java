package com.dbdoctor.common.enums;

import lombok.Getter;

/**
 * AI 调用状态枚举
 *
 * <p>定义了 AI 调用的所有可能状态</p>
 *
 * @author DB-Doctor
 * @version 2.3.0
 * @since 2.3.0
 */
@Getter
public enum InvocationStatus {

    /**
     * 调用成功
     * <p>AI 模型成功返回响应</p>
     */
    SUCCESS("SUCCESS", "成功"),

    /**
     * 调用失败
     * <p>AI 模型返回错误，或发生其他异常</p>
     */
    FAILED("FAILED", "失败"),

    /**
     * 调用超时
     * <p>AI 模型响应时间超过配置的超时阈值</p>
     */
    TIMEOUT("TIMEOUT", "超时");

    /**
     * 枚举代码（存储在数据库中的值）
     */
    private final String code;

    /**
     * 显示名称（中文，用于前端展示）
     */
    private final String displayName;

    /**
     * 构造函数
     *
     * @param code        枚举代码
     * @param displayName 显示名称
     */
    InvocationStatus(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    /**
     * 根据代码获取状态枚举
     *
     * @param code 枚举代码（如 "SUCCESS"）
     * @return 对应的状态枚举
     * @throws IllegalArgumentException 如果代码不存在
     */
    public static InvocationStatus fromCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("Status code cannot be null");
        }

        for (InvocationStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown status code: " + code);
    }

    /**
     * 验证代码是否有效
     *
     * @param code 待验证的代码
     * @return 如果有效返回 true，否则返回 false
     */
    public static boolean isValidCode(String code) {
        if (code == null) {
            return false;
        }
        for (InvocationStatus status : values()) {
            if (status.code.equals(code)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断是否为成功状态
     *
     * @return 如果是成功状态返回 true
     */
    public boolean isSuccess() {
        return this == SUCCESS;
    }

    /**
     * 判断是否为失败状态
     *
     * @return 如果是失败或超时状态返回 true
     */
    public boolean isFailure() {
        return this == FAILED || this == TIMEOUT;
    }
}
