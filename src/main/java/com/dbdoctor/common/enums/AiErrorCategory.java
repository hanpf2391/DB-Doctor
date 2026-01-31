package com.dbdoctor.common.enums;

import lombok.Getter;

/**
 * AI 调用错误分类枚举
 *
 * <p>定义了 AI 调用过程中可能出现的各种错误类型，用于监控和统计</p>
 *
 * @author DB-Doctor
 * @version 2.3.0
 * @since 2.3.0
 */
@Getter
public enum AiErrorCategory {

    /**
     * 超时错误
     * <p>AI 模型响应时间超过配置的超时阈值</p>
     */
    TIMEOUT("TIMEOUT", "超时"),

    /**
     * API 错误
     * <p>AI 模型 API 返回 5xx 服务器错误</p>
     */
    API_ERROR("API_ERROR", "API错误"),

    /**
     * 速率限制
     * <p>超过了 AI 模型 API 的调用速率限制</p>
     */
    RATE_LIMIT("RATE_LIMIT", "速率限制"),

    /**
     * 网络错误
     * <p>网络连接问题，如连接超时、DNS 解析失败等</p>
     */
    NETWORK_ERROR("NETWORK_ERROR", "网络错误"),

    /**
     * 配置错误
     * <p>配置问题，如 API Key 无效、模型名称错误、URL 配置错误等</p>
     */
    CONFIG_ERROR("CONFIG_ERROR", "配置错误"),

    /**
     * 认证错误
     * <p>认证失败，如 API Key 错误、权限不足等</p>
     */
    AUTH_ERROR("AUTH_ERROR", "认证错误"),

    /**
     * 内容过滤错误
     * <p>AI 模型内容审核机制拦截了请求</p>
     */
    CONTENT_FILTER("CONTENT_FILTER", "内容过滤"),

    /**
     * 令牌错误
     * <p>Token 数量超过模型限制</p>
     */
    TOKEN_LIMIT("TOKEN_LIMIT", "令牌超限"),

    /**
     * 未知错误
     * <p>无法归类到上述分类的其他错误</p>
     */
    UNKNOWN("UNKNOWN", "未知错误");

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
    AiErrorCategory(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    /**
     * 根据代码获取错误分类枚举
     *
     * @param code 枚举代码（如 "TIMEOUT"）
     * @return 对应的错误分类枚举
     * @throws IllegalArgumentException 如果代码不存在
     */
    public static AiErrorCategory fromCode(String code) {
        if (code == null) {
            return UNKNOWN;
        }

        for (AiErrorCategory category : values()) {
            if (category.code.equals(code)) {
                return category;
            }
        }
        return UNKNOWN;
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
        for (AiErrorCategory category : values()) {
            if (category.code.equals(code)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 根据错误消息自动分类
     *
     * @param errorMessage 错误消息
     * @return 错误分类
     */
    public static AiErrorCategory fromErrorMessage(String errorMessage) {
        if (errorMessage == null || errorMessage.isEmpty()) {
            return UNKNOWN;
        }

        String lowerMessage = errorMessage.toLowerCase();

        // 超时检测
        if (lowerMessage.contains("timeout") ||
            lowerMessage.contains("timed out") ||
            lowerMessage.contains("超时")) {
            return TIMEOUT;
        }

        // 速率限制检测
        if (lowerMessage.contains("rate limit") ||
            lowerMessage.contains("too many requests") ||
            lowerMessage.contains("429")) {
            return RATE_LIMIT;
        }

        // 网络错误检测
        if (lowerMessage.contains("connection") ||
            lowerMessage.contains("network") ||
            lowerMessage.contains("connect timeout") ||
            lowerMessage.contains("no route to host") ||
            lowerMessage.contains("网络")) {
            return NETWORK_ERROR;
        }

        // 认证错误检测
        if (lowerMessage.contains("401") ||
            lowerMessage.contains("403") ||
            lowerMessage.contains("unauthorized") ||
            lowerMessage.contains("forbidden") ||
            lowerMessage.contains("invalid api key") ||
            lowerMessage.contains("认证")) {
            return AUTH_ERROR;
        }

        // 配置错误检测
        if (lowerMessage.contains("404") ||
            lowerMessage.contains("not found") ||
            lowerMessage.contains("invalid model") ||
            lowerMessage.contains("配置")) {
            return CONFIG_ERROR;
        }

        // Token 限制检测
        if (lowerMessage.contains("token") &&
            (lowerMessage.contains("exceed") ||
             lowerMessage.contains("limit") ||
             lowerMessage.contains("too long"))) {
            return TOKEN_LIMIT;
        }

        // 内容过滤检测
        if (lowerMessage.contains("content filter") ||
            lowerMessage.contains("safety") ||
            lowerMessage.contains("policy violation")) {
            return CONTENT_FILTER;
        }

        // API 服务器错误检测
        if (lowerMessage.contains("500") ||
            lowerMessage.contains("502") ||
            lowerMessage.contains("503") ||
            lowerMessage.contains("internal server error")) {
            return API_ERROR;
        }

        return UNKNOWN;
    }

    /**
     * 判断是否为可重试错误
     *
     * <p>某些错误类型（如超时、网络错误、速率限制）可以通过重试来解决</p>
     *
     * @return 如果可重试返回 true
     */
    public boolean isRetryable() {
        return this == TIMEOUT ||
               this == NETWORK_ERROR ||
               this == RATE_LIMIT ||
               this == API_ERROR;
    }

    /**
     * 判断是否为配置错误
     *
     * <p>配置错误需要人工介入修复，重试无意义</p>
     *
     * @return 如果是配置错误返回 true
     */
    public boolean isConfigIssue() {
        return this == CONFIG_ERROR ||
               this == AUTH_ERROR;
    }
}
