package com.dbdoctor.common.enums;

import lombok.Getter;

/**
 * 响应码枚举
 * 定义系统中所有响应状态码
 *
 * @author DB-Doctor
 * @version 1.0.0
 */
@Getter
public enum ResponseCode {

    // ========== 通用 ==========
    SUCCESS(200, "操作成功"),
    ERROR(500, "操作失败"),
    PARAM_ERROR(400, "参数错误"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),

    // ========== 业务错误 ==========
    DATABASE_ERROR(1001, "数据库操作失败"),
    LOG_PARSE_ERROR(1002, "日志解析失败"),
    AI_ANALYSIS_ERROR(1003, "AI 分析失败"),
    NOTIFY_SEND_ERROR(1004, "通知发送失败"),

    // ========== 日志监听相关 ==========
    LOG_FILE_NOT_FOUND(2001, "日志文件不存在"),
    LOG_FILE_READ_ERROR(2002, "日志文件读取失败"),
    LOG_FORMAT_INVALID(2003, "日志格式无效"),

    // ========== 数据库相关 ==========
    DB_CONNECTION_ERROR(3001, "数据库连接失败"),
    DB_QUERY_ERROR(3002, "数据库查询失败"),
    DB_EXECUTE_PLAN_ERROR(3003, "执行计划获取失败"),

    // ========== AI 相关 ==========
    AI_API_KEY_INVALID(4001, "AI API Key 无效"),
    AI_API_CALL_ERROR(4002, "AI API 调用失败"),
    AI_MODEL_NOT_AVAILABLE(4003, "AI 模型不可用"),
    AI_TIMEOUT(4004, "AI 调用超时"),

    // ========== 通知相关 ==========
    EMAIL_SEND_ERROR(5001, "邮件发送失败"),
    WEBHOOK_SEND_ERROR(5002, "Webhook 发送失败"),

    // ========== 配置相关 ==========
    CONFIG_MISSING(6001, "配置缺失"),
    CONFIG_INVALID(6002, "配置无效");

    /**
     * 响应码
     */
    private final Integer code;

    /**
     * 响应消息
     */
    private final String message;

    ResponseCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * 根据响应码获取枚举
     */
    public static ResponseCode getByCode(Integer code) {
        for (ResponseCode responseCode : values()) {
            if (responseCode.getCode().equals(code)) {
                return responseCode;
            }
        }
        return ERROR;
    }
}
