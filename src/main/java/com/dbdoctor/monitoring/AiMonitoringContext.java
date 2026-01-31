package com.dbdoctor.monitoring;

/**
 * AI 监控上下文（ThreadLocal）
 *
 * <p>用于在 Agent 调用前后传递元数据给监听器</p>
 *
 * <p>使用场景：</p>
 * <pre>
 * // 调用前设置
 * AiMonitoringContext.setAgentName(AgentName.DIAGNOSIS.getCode());
 * AiMonitoringContext.setTraceId(context.getSqlFingerprint());
 *
 * // 调用 AI
 * String result = diagnosisAgent.analyzeSlowLog(prompt);
 *
 * // 调用后清理
 * AiMonitoringContext.clear();
 * </pre>
 *
 * @author DB-Doctor
 * @version 2.3.0
 * @since 2.3.0
 */
public class AiMonitoringContext {

    /**
     * Agent 名称（ThreadLocal）
     */
    private static final ThreadLocal<String> AGENT_NAME = new ThreadLocal<>();

    /**
     * SQL 指纹（ThreadLocal）
     */
    private static final ThreadLocal<String> TRACE_ID = new ThreadLocal<>();

    /**
     * 模型名称（ThreadLocal，可选）
     */
    private static final ThreadLocal<String> MODEL_NAME = new ThreadLocal<>();

    /**
     * 自定义标签（ThreadLocal，可选）
     */
    private static final ThreadLocal<String> TAGS = new ThreadLocal<>();

    /**
     * 私有构造函数（工具类）
     */
    private AiMonitoringContext() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * 设置 Agent 名称
     *
     * @param agentName Agent 角色代码（如 "DIAGNOSIS"）
     */
    public static void setAgentName(String agentName) {
        AGENT_NAME.set(agentName);
    }

    /**
     * 获取 Agent 名称
     *
     * @return Agent 角色代码，如果未设置返回 null
     */
    public static String getAgentName() {
        return AGENT_NAME.get();
    }

    /**
     * 设置 SQL 指纹
     *
     * @param traceId SQL 指纹
     */
    public static void setTraceId(String traceId) {
        TRACE_ID.set(traceId);
    }

    /**
     * 获取 SQL 指纹
     *
     * @return SQL 指纹，如果未设置返回 null
     */
    public static String getTraceId() {
        return TRACE_ID.get();
    }

    /**
     * 设置模型名称
     *
     * @param modelName 模型名称（如 "qwen2.5:7b"）
     */
    public static void setModelName(String modelName) {
        MODEL_NAME.set(modelName);
    }

    /**
     * 获取模型名称
     *
     * @return 模型名称，如果未设置返回 null
     */
    public static String getModelName() {
        return MODEL_NAME.get();
    }

    /**
     * 设置自定义标签
     *
     * @param tags JSON 格式的标签（如 {"env":"prod","version":"v2.3.0"}）
     */
    public static void setTags(String tags) {
        TAGS.set(tags);
    }

    /**
     * 获取自定义标签
     *
     * @return JSON 格式的标签，如果未设置返回 null
     */
    public static String getTags() {
        return TAGS.get();
    }

    /**
     * 清理上下文
     *
     * <p><strong>重要：</strong>每次 AI 调用完成后必须调用此方法，避免内存泄漏</p>
     */
    public static void clear() {
        AGENT_NAME.remove();
        TRACE_ID.remove();
        MODEL_NAME.remove();
        TAGS.remove();
    }

    /**
     * 检查上下文是否已设置
     *
     * @return 如果至少有一个字段已设置返回 true
     */
    public static boolean isSet() {
        return AGENT_NAME.get() != null ||
               TRACE_ID.get() != null ||
               MODEL_NAME.get() != null ||
               TAGS.get() != null;
    }

    /**
     * 获取上下文摘要（用于调试）
     *
     * @return 上下文摘要字符串
     */
    public static String getSummary() {
        return String.format("AiMonitoringContext{agentName='%s', traceId='%s', modelName='%s', tags='%s'}",
                getAgentName(), getTraceId(), getModelName(), getTags());
    }
}
