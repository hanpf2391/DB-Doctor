package com.dbdoctor.monitoring;

import com.dbdoctor.common.enums.AiErrorCategory;
import com.dbdoctor.common.enums.InvocationStatus;
import com.dbdoctor.entity.AiInvocationLog;
import com.dbdoctor.service.AiInvocationLogService;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI 监控监听器 - v2.3.1 增强版
 *
 * <p>核心改进：</p>
 * <ul>
 *   <li>使用 "官方 API + 估算兜底" 的双重策略获取 Token</li>
 *   <li>从 AiContextHolder 获取 Prompt 和 Response</li>
 *   <li>改进 Token 统计准确性</li>
 * </ul>
 *
 * <p>Token 获取策略：</p>
 * <pre>
 * 1. 尝试从官方 API 获取 TokenUsage
 * 2. 如果官方有数据，直接使用
 * 3. 如果官方没数据（Ollama 常见情况），启动 TokenEstimator 估算兜底
 * </pre>
 *
 * @author DB-Doctor
 * @version 2.3.1
 * @since 2.3.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiMonitoringListener implements ChatModelListener {

    private final AiInvocationLogService logService;

    /**
     * 存储请求开始时间（thread-safe）
     * Key: requestId, Value: startTime
     */
    private final ConcurrentHashMap<String, LocalDateTime> requestStartTimes = new ConcurrentHashMap<>();

    /**
     * 请求拦截：记录开始时间
     *
     * @param context 请求上下文
     */
    @Override
    public void onRequest(ChatModelRequestContext context) {
        try {
            // 使用内存地址作为请求 ID
            String requestId = String.valueOf(System.identityHashCode(context));
            String requestContextId = String.valueOf(System.identityHashCode(context.request()));

            // 记录开始时间（保存两个 key 以确保匹配）
            requestStartTimes.put(requestId, LocalDateTime.now());
            requestStartTimes.put(requestContextId, LocalDateTime.now());

            log.debug("[AI监控] 请求开始: requestId={}, requestContextId={}", requestId, requestContextId);
        } catch (Exception e) {
            // 记录错误但不抛出异常，避免影响 AI 调用
            log.error("[AI监控] onRequest 处理失败", e);
        }
    }

    /**
     * 响应拦截：提取指标并保存
     *
     * @param context 响应上下文
     */
    @Override
    public void onResponse(ChatModelResponseContext context) {
        try {
            // 尝试多个方式获取 requestId
            Object requestContext = context.request();
            String contextId = String.valueOf(System.identityHashCode(context));
            String requestContextId = String.valueOf(System.identityHashCode(requestContext));

            // 尝试从两个可能的 key 中获取开始时间
            LocalDateTime startTime = requestStartTimes.remove(contextId);
            if (startTime == null) {
                startTime = requestStartTimes.remove(requestContextId);
            }

            if (startTime == null) {
                log.warn("[AI监控] 无法找到请求开始时间: contextId={}, requestContextId={}", contextId, requestContextId);
                startTime = LocalDateTime.now(); // 使用当前时间作为备选
            }

            LocalDateTime endTime = LocalDateTime.now();
            long durationMs = java.time.Duration.between(startTime, endTime).toMillis();

            // 从 ThreadLocal 获取元数据
            String agentName = AiContextHolder.get(AiContextHolder.KEY_AGENT_NAME, "UNKNOWN");
            String traceId = AiContextHolder.get(AiContextHolder.KEY_TRACE_ID, "UNKNOWN");
            String modelName = AiContextHolder.getModelName();

            // 构建日志实体
            AiInvocationLog invocationLog = new AiInvocationLog();

            // 基本信息
            invocationLog.setTraceId(traceId != null ? traceId : "UNKNOWN");
            invocationLog.setAgentName(agentName != null ? agentName : "UNKNOWN");
            invocationLog.setModelName(modelName);
            invocationLog.setProvider(extractProvider(modelName));

            // 时间信息
            invocationLog.setStartTime(startTime);
            invocationLog.setEndTime(endTime);
            invocationLog.setDurationMs(durationMs);
            invocationLog.setCreatedTime(LocalDateTime.now());

            // 状态信息
            invocationLog.setStatus(InvocationStatus.SUCCESS.getCode());

            // 🆕 Token 统计（官方 API + 估算兜底）- v2.3.2 升级
            int inputTokens = 0;
            int outputTokens = 0;
            int totalTokens = 0;

            // 策略 1: 尝试从官方 API 获取（0.36.1 支持）
            dev.langchain4j.model.output.TokenUsage usage = null;
            try {
                if (context.response() != null) {
                    usage = context.response().tokenUsage();
                }
            } catch (Exception e) {
                log.debug("[AI监控] 获取官方 TokenUsage 失败: {}", e.getMessage());
            }

            if (usage != null && usage.totalTokenCount() > 0) {
                inputTokens = usage.inputTokenCount();
                outputTokens = usage.outputTokenCount();
                totalTokens = usage.totalTokenCount();

                log.info("[AI监控] ✅ 使用官方 Token 统计: in={}, out={}, total={}",
                        inputTokens, outputTokens, totalTokens);
            } else {
                log.debug("[AI监控] 官方 TokenUsage 不可用（可能 Ollama 不支持），将使用估算算法");
            }

            // 策略 2: 估算兜底（Ollama 或官方 API 失败时）
            if (totalTokens == 0) {
                String prompt = AiContextHolder.getPrompt();
                String response = AiContextHolder.getResponse();

                inputTokens = TokenEstimator.estimateInputTokens(prompt);
                outputTokens = TokenEstimator.estimateOutputTokens(response);
                totalTokens = inputTokens + outputTokens;

                log.debug("[AI监控] ⚠️ 使用 Token 估算: in={}, out={}, total={}",
                        inputTokens, outputTokens, totalTokens);
            }

            invocationLog.setInputTokens(inputTokens);
            invocationLog.setOutputTokens(outputTokens);
            invocationLog.setTotalTokens(totalTokens);

            // 异步保存（不阻塞 AI 调用）
            logService.saveAsync(invocationLog);

            log.debug("[AI监控] 请求成功: model={}, agent={}, tokens={}, duration={}ms",
                    modelName, agentName, totalTokens, durationMs);

        } catch (Exception e) {
            // 记录错误但不抛出异常，避免影响 AI 调用
            log.error("[AI监控] onResponse 处理失败", e);
        }
    }

    /**
     * 错误拦截：记录失败信息
     *
     * @param context 错误上下文
     */
    @Override
    public void onError(ChatModelErrorContext context) {
        try {
            String requestId = String.valueOf(context.hashCode());
            Throwable error = context.error();

            // 获取开始时间
            LocalDateTime startTime = requestStartTimes.remove(requestId);
            if (startTime == null) {
                log.warn("[AI监控] 无法找到请求开始时间: requestId={}", requestId);
                startTime = LocalDateTime.now(); // 使用当前时间作为备选
            }

            LocalDateTime endTime = LocalDateTime.now();
            long durationMs = java.time.Duration.between(startTime, endTime).toMillis();

            // 从 ThreadLocal 获取元数据
            String agentName = AiContextHolder.get(AiContextHolder.KEY_AGENT_NAME, "UNKNOWN");
            String traceId = AiContextHolder.get(AiContextHolder.KEY_TRACE_ID, "UNKNOWN");
            String modelName = AiContextHolder.getModelName();

            // 确定状态和错误分类
            String status = determineStatus(error);
            String errorCategory = AiErrorCategory.fromErrorMessage(error.getMessage()).getCode();
            String errorMessage = truncateErrorMessage(error.getMessage());

            // 构建日志实体
            AiInvocationLog invocationLog = new AiInvocationLog();

            // 基本信息
            invocationLog.setTraceId(traceId != null ? traceId : "UNKNOWN");
            invocationLog.setAgentName(agentName != null ? agentName : "UNKNOWN");
            invocationLog.setModelName(modelName);
            invocationLog.setProvider(extractProvider(modelName));

            // 时间信息
            invocationLog.setStartTime(startTime);
            invocationLog.setEndTime(endTime);
            invocationLog.setDurationMs(durationMs);
            invocationLog.setCreatedTime(LocalDateTime.now());

            // 状态信息
            invocationLog.setStatus(status);
            invocationLog.setErrorCategory(errorCategory);
            invocationLog.setErrorMessage(errorMessage);

            // 🆕 Token 统计（失败时只估算输入 Token）
            String prompt = AiContextHolder.getPrompt();
            int inputTokens = TokenEstimator.estimateInputTokens(prompt);
            invocationLog.setInputTokens(inputTokens);
            invocationLog.setOutputTokens(0);
            invocationLog.setTotalTokens(inputTokens);

            // 异步保存
            logService.saveAsync(invocationLog);

            log.error("[AI监控] 请求失败: model={}, agent={}, error={}, duration={}ms",
                    modelName, agentName, error.getMessage(), durationMs);

        } catch (Exception e) {
            // 记录错误但不抛出异常，避免影响 AI 调用
            log.error("[AI监控] onError 处理失败", e);
        }
    }

    /**
     * 从模型名称提取供应商
     *
     * @param modelName 模型名称
     * @return 供应商代码
     */
    private String extractProvider(String modelName) {
        if (modelName == null) {
            return "unknown";
        }

        String lowerModel = modelName.toLowerCase();

        if (lowerModel.contains("qwen") || lowerModel.contains("deepseek") ||
            lowerModel.contains("llama") || lowerModel.contains("mistral")) {
            return "ollama";
        } else if (lowerModel.contains("gpt")) {
            return "openai";
        } else if (lowerModel.contains("deepseek")) {
            return "deepseek";
        }

        return "unknown";
    }

    /**
     * 根据异常确定状态
     *
     * @param error 异常对象
     * @return 状态代码
     */
    private String determineStatus(Throwable error) {
        if (error == null) {
            return InvocationStatus.FAILED.getCode();
        }

        String message = error.getMessage();
        if (message != null && message.toLowerCase().contains("timeout")) {
            return InvocationStatus.TIMEOUT.getCode();
        }

        return InvocationStatus.FAILED.getCode();
    }

    /**
     * 截断错误消息（避免数据库字段溢出）
     *
     * @param errorMessage 原始错误消息
     * @return 截断后的错误消息
     */
    private String truncateErrorMessage(String errorMessage) {
        if (errorMessage == null) {
            return null;
        }

        // TEXT 类型字段在 MySQL 中最大 65535 字节，约 21845 个 UTF-8 字符
        // 为了安全，限制在 10000 个字符
        int maxLength = 10000;
        if (errorMessage.length() > maxLength) {
            return errorMessage.substring(0, maxLength) + "... (truncated)";
        }

        return errorMessage;
    }
}
