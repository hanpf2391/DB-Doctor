package com.dbdoctor.monitoring;

import com.dbdoctor.common.enums.AiErrorCategory;
import com.dbdoctor.common.enums.AgentName;
import com.dbdoctor.common.enums.InvocationStatus;
import com.dbdoctor.entity.AiInvocationLog;
import com.dbdoctor.service.AiInvocationLogService;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequest;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponse;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI 监控监听器
 *
 * <p>核心职责：</p>
 * <ul>
 *   <li>监听所有 AI 调用（LangChain4j ChatModelListener）</li>
 *   <li>提取 Token、耗时等指标</li>
 *   <li>异步写入数据库</li>
 * </ul>
 *
 * @author DB-Doctor
 * @version 2.3.0
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
            ChatModelRequest request = context.chatModelRequest();
            String requestId = generateRequestId(request);

            // 记录开始时间
            requestStartTimes.put(requestId, LocalDateTime.now());

            log.debug("[AI监控] 请求开始: model={}, requestId={}",
                    request.model(), requestId);
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
            ChatModelRequest request = context.chatModelRequest();
            ChatResponse response = context.chatResponse();
            String requestId = generateRequestId(request);

            // 获取开始时间
            LocalDateTime startTime = requestStartTimes.remove(requestId);
            if (startTime == null) {
                log.warn("[AI监控] 无法找到请求开始时间: requestId={}", requestId);
                startTime = LocalDateTime.now(); // 使用当前时间作为备选
            }

            LocalDateTime endTime = LocalDateTime.now();
            long durationMs = java.time.Duration.between(startTime, endTime).toMillis();

            // 从 ThreadLocal 获取元数据
            String agentName = AiMonitoringContext.getAgentName();
            String traceId = AiMonitoringContext.getTraceId();

            // 构建日志实体
            AiInvocationLog log = buildInvocationLog(
                    request,
                    startTime,
                    endTime,
                    durationMs,
                    InvocationStatus.SUCCESS.getCode(),
                    null,
                    null,
                    agentName,
                    traceId
            );

            // Token 统计
            if (response.tokenUsage() != null) {
                log.setInputTokens(response.tokenUsage().inputTokenCount());
                log.setOutputTokens(response.tokenUsage().outputTokenCount());
                log.setTotalTokens(response.tokenUsage().totalTokenCount());
            } else {
                log.setInputTokens(0);
                log.setOutputTokens(0);
                log.setTotalTokens(0);
                log.warn("[AI监控] Token 统计信息缺失: model={}", request.model());
            }

            // 异步保存（不阻塞 AI 调用）
            logService.saveAsync(log);

            log.debug("[AI监控] 请求成功: model={}, agent={}, tokens={}, duration={}ms",
                    request.model(), agentName, log.getTotalTokens(), durationMs);

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
            ChatModelRequest request = context.chatModelRequest();
            Throwable error = context.error();
            String requestId = generateRequestId(request);

            // 获取开始时间
            LocalDateTime startTime = requestStartTimes.remove(requestId);
            if (startTime == null) {
                log.warn("[AI监控] 无法找到请求开始时间: requestId={}", requestId);
                startTime = LocalDateTime.now(); // 使用当前时间作为备选
            }

            LocalDateTime endTime = LocalDateTime.now();
            long durationMs = java.time.Duration.between(startTime, endTime).toMillis();

            // 从 ThreadLocal 获取元数据
            String agentName = AiMonitoringContext.getAgentName();
            String traceId = AiMonitoringContext.getTraceId();

            // 确定状态和错误分类
            String status = determineStatus(error);
            String errorCategory = AiErrorCategory.fromErrorMessage(error.getMessage()).getCode();
            String errorMessage = truncateErrorMessage(error.getMessage());

            // 构建日志实体
            AiInvocationLog log = buildInvocationLog(
                    request,
                    startTime,
                    endTime,
                    durationMs,
                    status,
                    errorCategory,
                    errorMessage,
                    agentName,
                    traceId
            );

            // Token 统计（失败时可能没有）
            log.setInputTokens(0);
            log.setOutputTokens(0);
            log.setTotalTokens(0);

            // 异步保存
            logService.saveAsync(log);

            log.error("[AI监控] 请求失败: model={}, agent={}, error={}, duration={}ms",
                    request.model(), agentName, error.getMessage(), durationMs);

        } catch (Exception e) {
            // 记录错误但不抛出异常，避免影响 AI 调用
            log.error("[AI监控] onError 处理失败", e);
        }
    }

    /**
     * 构建 AI 调用日志实体
     *
     * @param request       AI 请求
     * @param startTime     开始时间
     * @param endTime       结束时间
     * @param durationMs    耗时（毫秒）
     * @param status        状态
     * @param errorCategory 错误分类
     * @param errorMessage  错误消息
     * @param agentName     Agent 名称
     * @param traceId       SQL 指纹
     * @return AI 调用日志实体
     */
    private AiInvocationLog buildInvocationLog(
            ChatModelRequest request,
            LocalDateTime startTime,
            LocalDateTime endTime,
            long durationMs,
            String status,
            String errorCategory,
            String errorMessage,
            String agentName,
            String traceId) {

        AiInvocationLog log = new AiInvocationLog();

        // 基本信息
        log.setTraceId(traceId != null ? traceId : "UNKNOWN");
        log.setAgentName(agentName != null ? agentName : "UNKNOWN");
        log.setModelName(request.model());
        log.setProvider(extractProvider(request.model()));

        // 时间信息
        log.setStartTime(startTime);
        log.setEndTime(endTime);
        log.setDurationMs(durationMs);
        log.setCreatedTime(LocalDateTime.now());

        // 状态信息
        log.setStatus(status);
        log.setErrorCategory(errorCategory);
        log.setErrorMessage(errorMessage);

        // 可选：自定义标签
        log.setTags(AiMonitoringContext.getTags());

        return log;
    }

    /**
     * 生成请求 ID
     *
     * @param request AI 请求
     * @return 请求 ID
     */
    private String generateRequestId(ChatModelRequest request) {
        return request.model() + "_" + request.hashCode() + "_" + System.currentTimeMillis();
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
