package com.dbdoctor.monitoring;

import java.util.HashMap;
import java.util.Map;

/**
 * AI 监控上下文持有者（ThreadLocal）- 增强版
 *
 * <p>用于在 AI 调用前后传递业务元数据（traceId, agentName, prompt, response 等）</p>
 *
 * <p>v2.3.1 新增功能：</p>
 * <ul>
 *   <li>支持传递 Prompt（用于 Token 估算）</li>
 *   <li>支持传递 Response（用于 Token 统计）</li>
 *   <li>支持传递模型名称</li>
 * </ul>
 *
 * <p>使用场景：</p>
 * <pre>
 * // 调用前设置
 * AiContextHolder.set("traceId", template.getSqlFingerprint());
 * AiContextHolder.set("agentName", "DiagnosisAgent");
 * AiContextHolder.setPrompt(prompt);  // 🆕 设置 Prompt
 *
 * // 调用 AI
 * String result = agent.analyzeSlowLog(prompt);
 * AiContextHolder.setResponse(result);  // 🆕 设置 Response
 *
 * // 调用后清理
 * AiContextHolder.clear();
 * </pre>
 *
 * @author DB-Doctor
 * @version 2.3.1
 * @since 2.3.0
 */
public class AiContextHolder {

    /**
     * ThreadLocal 上下文
     */
    private static final ThreadLocal<Map<String, String>> CONTEXT = ThreadLocal.withInitial(HashMap::new);

    // ===== 预定义的键名常量 =====

    /**
     * Agent 名称键
     */
    public static final String KEY_AGENT_NAME = "agentName";

    /**
     * SQL 指纹键
     */
    public static final String KEY_TRACE_ID = "traceId";

    /**
     * Prompt 键（用于 Token 估算）- 🆕
     */
    public static final String KEY_PROMPT = "prompt";

    /**
     * Response 键（用于 Token 统计）- 🆕
     */
    public static final String KEY_RESPONSE = "response";

    /**
     * 模型名称键 - 🆕
     */
    public static final String KEY_MODEL_NAME = "modelName";

    /**
     * 设置元数据
     *
     * @param key   键
     * @param value 值
     */
    public static void set(String key, String value) {
        CONTEXT.get().put(key, value);
    }

    /**
     * 获取元数据
     *
     * @param key 键
     * @return 值，如果不存在返回 null
     */
    public static String get(String key) {
        return CONTEXT.get().get(key);
    }

    /**
     * 获取元数据（带默认值）
     *
     * @param key          键
     * @param defaultValue 默认值
     * @return 值，如果不存在返回默认值
     */
    public static String get(String key, String defaultValue) {
        String value = CONTEXT.get().get(key);
        return value != null ? value : defaultValue;
    }

    /**
     * 检查上下文是否已设置
     *
     * @return 如果至少有一个键已设置返回 true
     */
    public static boolean isSet() {
        return !CONTEXT.get().isEmpty();
    }

    /**
     * 清理上下文
     *
     * <p><strong>重要：</strong>每次 AI 调用完成后必须调用此方法，避免内存泄漏</p>
     */
    public static void clear() {
        CONTEXT.remove();
    }

    /**
     * 获取所有元数据
     *
     * @return 元数据 Map
     */
    public static Map<String, String> getAll() {
        return new HashMap<>(CONTEXT.get());
    }

    /**
     * 批量设置元数据
     *
     * @param metadata 元数据 Map
     */
    public static void setAll(Map<String, String> metadata) {
        if (metadata != null) {
            CONTEXT.get().putAll(metadata);
        }
    }

    /**
     * 获取上下文摘要（用于调试）
     *
     * @return 上下文摘要字符串
     */
    public static String getSummary() {
        return String.format("AiContextHolder{context=%s}", CONTEXT.get());
    }

    // ===== 便捷方法（推荐使用） =====

    /**
     * 设置 Agent 名称
     *
     * @param agentName Agent 名称
     */
    public static void setAgentName(String agentName) {
        set(KEY_AGENT_NAME, agentName);
    }

    /**
     * 设置 SQL 指纹
     *
     * @param traceId SQL 指纹
     */
    public static void setTraceId(String traceId) {
        set(KEY_TRACE_ID, traceId);
    }

    /**
     * 设置 Prompt（用于 Token 估算）- 🆕
     *
     * @param prompt AI 输入的 Prompt
     */
    public static void setPrompt(String prompt) {
        set(KEY_PROMPT, prompt);
    }

    /**
     * 获取 Prompt - 🆕
     *
     * @return Prompt 文本，如果不存在返回空字符串
     */
    public static String getPrompt() {
        return get(KEY_PROMPT, "");
    }

    /**
     * 设置 Response（用于 Token 统计）- 🆕
     *
     * @param response AI 输出的 Response
     */
    public static void setResponse(String response) {
        set(KEY_RESPONSE, response);
    }

    /**
     * 获取 Response - 🆕
     *
     * @return Response 文本，如果不存在返回空字符串
     */
    public static String getResponse() {
        return get(KEY_RESPONSE, "");
    }

    /**
     * 设置模型名称 - 🆕
     *
     * @param modelName 模型名称
     */
    public static void setModelName(String modelName) {
        set(KEY_MODEL_NAME, modelName);
    }

    /**
     * 获取模型名称 - 🆕
     *
     * @return 模型名称，如果不存在返回 "unknown"
     */
    public static String getModelName() {
        return get(KEY_MODEL_NAME, "unknown");
    }
}
