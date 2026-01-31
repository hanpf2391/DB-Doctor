package com.dbdoctor.common.enums;

import lombok.Getter;

/**
 * Agent 角色枚举
 *
 * <p>定义了 DB-Doctor 多 Agent 架构中的所有 AI Agent 角色</p>
 *
 * @author DB-Doctor
 * @version 2.3.0
 * @since 2.3.0
 */
@Getter
public enum AgentName {

    /**
     * 主治医生 Agent
     * <p>职责：初步诊断、证据收集、慢查询基础分析</p>
     */
    DIAGNOSIS("DIAGNOSIS", "主治医生", "DBAgent"),

    /**
     * 推理专家 Agent
     * <p>职责：深度推理、根因分析、复杂问题诊断</p>
     */
    REASONING("REASONING", "推理专家", "ReasoningAgent"),

    /**
     * 编码专家 Agent
     * <p>职责：生成优化代码、SQL 改写建议</p>
     */
    CODING("CODING", "编码专家", "CodingAgent");

    /**
     * 枚举代码（存储在数据库中的值）
     */
    private final String code;

    /**
     * 显示名称（中文，用于前端展示）
     */
    private final String displayName;

    /**
     * 对应的类名（Java 类名）
     */
    private final String className;

    /**
     * 构造函数
     *
     * @param code        枚举代码
     * @param displayName 显示名称
     * @param className   类名
     */
    AgentName(String code, String displayName, String className) {
        this.code = code;
        this.displayName = displayName;
        this.className = className;
    }

    /**
     * 根据类名获取 Agent 枚举
     *
     * @param className Java 类名（如 "DBAgent"）
     * @return 对应的 Agent 枚举
     * @throws IllegalArgumentException 如果类名不存在
     */
    public static AgentName fromClassName(String className) {
        if (className == null) {
            throw new IllegalArgumentException("Agent class name cannot be null");
        }

        for (AgentName agent : values()) {
            if (agent.className.equalsIgnoreCase(className)) {
                return agent;
            }
        }
        throw new IllegalArgumentException("Unknown Agent class: " + className);
    }

    /**
     * 根据代码获取 Agent 枚举
     *
     * @param code 枚举代码（如 "DIAGNOSIS"）
     * @return 对应的 Agent 枚举
     * @throws IllegalArgumentException 如果代码不存在
     */
    public static AgentName fromCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("Agent code cannot be null");
        }

        for (AgentName agent : values()) {
            if (agent.code.equals(code)) {
                return agent;
            }
        }
        throw new IllegalArgumentException("Unknown Agent code: " + code);
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
        for (AgentName agent : values()) {
            if (agent.code.equals(code)) {
                return true;
            }
        }
        return false;
    }
}
