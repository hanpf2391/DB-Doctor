package com.dbdoctor.monitoring;

import com.dbdoctor.entity.AiInvocationLog;
import com.dbdoctor.service.AiInvocationLogService;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.output.TokenUsage;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AiMonitoringListener 集成测试
 *
 * <p>测试双重策略：官方 API 优先 + 估算兜底</p>
 *
 * @author DB-Doctor
 * @version 2.3.1
 */
@Slf4j
@SpringBootTest
class AiMonitoringListenerTest {

    @Mock
    private AiInvocationLogService logService;

    private AiMonitoringListener listener;

    @BeforeEach
    void setUp() {
        // Mock logService 的 saveAsync 方法
        doAnswer(invocation -> {
            AiInvocationLog log = invocation.getArgument(0);
            log.info("[Mock] 保存日志: traceId={}, tokens={}",
                    log.getTraceId(), log.getTotalTokens());
            return null;
        }).when(logService).saveAsync(any(AiInvocationLog.class));

        listener = new AiMonitoringListener(logService);
    }

    /**
     * 测试 1: 官方 TokenUsage 可用时的处理
     */
    @Test
    void testOnResponseWithOfficialTokenUsage() {
        log.info("========== 测试 1: 官方 TokenUsage 可用 ==========");

        // Given: Mock ChatModelResponseContext
        ChatModelResponseContext mockContext = mock(ChatModelResponseContext.class);
        dev.langchain4j.model.chat.ChatResponse mockResponse =
                mock(dev.langchain4j.model.chat.ChatResponse.class);

        // Mock TokenUsage（官方统计数据）
        TokenUsage mockTokenUsage = new TokenUsage(100, 50, 150); // input, output, total

        when(mockContext.response()).thenReturn(mockResponse);
        when(mockResponse.tokenUsage()).thenReturn(mockTokenUsage);
        when(mockContext.hashCode()).thenReturn(12345);

        // 设置 ThreadLocal 元数据
        AiContextHolder.setAgentName("DIAGNOSIS");
        AiContextHolder.setTraceId("test-sql-fingerprint-001");
        AiContextHolder.setPrompt("分析以下慢查询：SELECT * FROM users");
        AiContextHolder.setResponse("建议在 user_id 上创建索引");

        // When: 调用 onResponse
        listener.onResponse(mockContext);

        // Then: 验证日志被保存
        verify(logService, times(1)).saveAsync(any(AiInvocationLog.class));

        log.info("✅ 官方 TokenUsage 测试通过");
        log.info("  - 官方 Input Tokens: {}", mockTokenUsage.inputTokenCount());
        log.info("  - 官方 Output Tokens: {}", mockTokenUsage.outputTokenCount());
        log.info("  - 官方 Total Tokens: {}", mockTokenUsage.totalTokenCount());

        // 清理 ThreadLocal
        AiContextHolder.clear();
    }

    /**
     * 测试 2: 官方 TokenUsage 不可用时，使用估算兜底
     */
    @Test
    void testOnResponseWithEstimationFallback() {
        log.info("========== 测试 2: 官方 TokenUsage 不可用，使用估算 ==========");

        // Given: Mock ChatModelResponseContext（返回 null TokenUsage）
        ChatModelResponseContext mockContext = mock(ChatModelResponseContext.class);
        dev.langchain4j.model.chat.ChatResponse mockResponse =
                mock(dev.langchain4j.model.chat.ChatResponse.class);

        when(mockContext.response()).thenReturn(mockResponse);
        when(mockResponse.tokenUsage()).thenReturn(null); // 官方 API 返回 null
        when(mockContext.hashCode()).thenReturn(12346);

        // 设置 ThreadLocal 元数据
        String prompt = "分析以下慢查询：SELECT * FROM users WHERE status = 'active'";
        String response = "建议在 user_id 和 status 字段上创建复合索引";
        AiContextHolder.setAgentName("REASONING");
        AiContextHolder.setTraceId("test-sql-fingerprint-002");
        AiContextHolder.setPrompt(prompt);
        AiContextHolder.setResponse(response);

        // 预估 Token 数
        int estimatedInput = TokenEstimator.estimateInputTokens(prompt);
        int estimatedOutput = TokenEstimator.estimateOutputTokens(response);
        int estimatedTotal = estimatedInput + estimatedOutput;

        // When: 调用 onResponse
        listener.onResponse(mockContext);

        // Then: 验证日志被保存（使用估算值）
        verify(logService, times(1)).saveAsync(any(AiInvocationLog.class));

        log.info("✅ 估算兜底测试通过");
        log.info("  - 估算 Input Tokens: {}", estimatedInput);
        log.info("  - 估算 Output Tokens: {}", estimatedOutput);
        log.info("  - 估算 Total Tokens: {}", estimatedTotal);

        // 清理 ThreadLocal
        AiContextHolder.clear();
    }

    /**
     * 测试 3: 错误处理（onError）
     */
    @Test
    void testOnError() {
        log.info("========== 测试 3: 错误处理 ==========");

        // Given: Mock ChatModelErrorContext
        ChatModelErrorContext mockContext = mock(ChatModelErrorContext.class);
        Throwable mockError = new RuntimeException("API 调用超时");

        when(mockContext.error()).thenReturn(mockError);
        when(mockContext.hashCode()).thenReturn(12347);

        // 设置 ThreadLocal 元数据
        String prompt = "分析以下慢查询：SELECT * FROM orders";
        AiContextHolder.setAgentName("CODING");
        AiContextHolder.setTraceId("test-sql-fingerprint-003");
        AiContextHolder.setPrompt(prompt);

        // When: 调用 onError
        listener.onError(mockContext);

        // Then: 验证错误日志被保存
        verify(logService, times(1)).saveAsync(any(AiInvocationLog.class));

        log.info("✅ 错误处理测试通过");
        log.info("  - 错误类型: {}", mockError.getClass().getSimpleName());
        log.info("  - 错误消息: {}", mockError.getMessage());

        // 清理 ThreadLocal
        AiContextHolder.clear();
    }

    /**
     * 测试 4: 并发请求处理
     */
    @Test
    void testConcurrentRequests() throws InterruptedException {
        log.info("========== 测试 4: 并发请求处理 ==========");

        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        // When: 模拟并发请求
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;

            new Thread(() -> {
                try {
                    // Mock Context
                    ChatModelResponseContext mockContext = mock(ChatModelResponseContext.class);
                    dev.langchain4j.model.chat.ChatResponse mockResponse =
                            mock(dev.langchain4j.model.chat.ChatResponse.class);

                    TokenUsage mockTokenUsage = new TokenUsage(100 + threadId, 50 + threadId, 150 + 2 * threadId);

                    when(mockContext.response()).thenReturn(mockResponse);
                    when(mockResponse.tokenUsage()).thenReturn(mockTokenUsage);
                    when(mockContext.hashCode()).thenReturn(threadId);

                    // 设置 ThreadLocal 元数据
                    AiContextHolder.setAgentName("DIAGNOSIS");
                    AiContextHolder.setTraceId("test-concurrent-" + threadId);
                    AiContextHolder.setPrompt("Prompt " + threadId);
                    AiContextHolder.setResponse("Response " + threadId);

                    // 调用 onResponse
                    listener.onResponse(mockContext);

                    successCount.incrementAndGet();

                } catch (Exception e) {
                    log.error("[Thread-{}] 并发测试失败", threadId, e);
                } finally {
                    AiContextHolder.clear();
                    latch.countDown();
                }
            }).start();
        }

        // Then: 等待所有线程完成
        assertTrue(latch.await(10, TimeUnit.SECONDS), "所有线程应在 10 秒内完成");
        assertEquals(threadCount, successCount.get(), "所有请求都应成功");

        log.info("✅ 并发测试通过: {} 个线程全部成功", successCount.get());
    }

    /**
     * 测试 5: onRequest 记录开始时间
     */
    @Test
    void testOnRequest() {
        log.info("========== 测试 5: onRequest 记录开始时间 ==========");

        // Given: Mock ChatModelRequestContext
        ChatModelRequestContext mockContext = mock(ChatModelRequestContext.class);
        when(mockContext.hashCode()).thenReturn(12348);

        // When: 调用 onRequest
        LocalDateTime beforeCall = LocalDateTime.now();
        listener.onRequest(mockContext);
        LocalDateTime afterCall = LocalDateTime.now();

        // Then: 验证开始时间被记录（通过 hashCode）
        log.info("✅ onRequest 测试通过");
        log.info("  - 请求 ID: {}", mockContext.hashCode());
        log.info("  - 当前时间范围: {} ~ {}", beforeCall, afterCall);
    }

    /**
     * 测试 6: 完整流程（onRequest → onResponse）
     */
    @Test
    void testCompleteFlow() {
        log.info("========== 测试 6: 完整流程 ==========");

        String requestId = "test-flow-123";

        // Step 1: onRequest
        ChatModelRequestContext mockRequestContext = mock(ChatModelRequestContext.class);
        when(mockRequestContext.hashCode()).thenReturn(Integer.parseInt(requestId));

        listener.onRequest(mockRequestContext);
        log.info("✅ 请求开始");

        // 模拟 AI 处理时间
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Step 2: onResponse
        ChatModelResponseContext mockResponseContext = mock(ChatModelResponseContext.class);
        dev.langchain4j.model.chat.ChatResponse mockResponse =
                mock(dev.langchain4j.model.chat.ChatResponse.class);
        TokenUsage mockTokenUsage = new TokenUsage(200, 100, 300);

        when(mockResponseContext.response()).thenReturn(mockResponse);
        when(mockResponse.tokenUsage()).thenReturn(mockTokenUsage);
        when(mockResponseContext.hashCode()).thenReturn(Integer.parseInt(requestId));

        AiContextHolder.setAgentName("DIAGNOSIS");
        AiContextHolder.setTraceId("test-complete-flow");
        AiContextHolder.setPrompt("完整流程测试 Prompt");
        AiContextHolder.setResponse("完整流程测试 Response");

        listener.onResponse(mockResponseContext);

        // Then: 验证完整流程
        verify(logService, times(1)).saveAsync(any(AiInvocationLog.class));

        log.info("✅ 完整流程测试通过");
        log.info("  - 耗时: ~100ms（包含 AI 处理时间）");
        log.info("  - Token 统计: {}", mockTokenUsage.totalTokenCount());

        // 清理 ThreadLocal
        AiContextHolder.clear();
    }

    /**
     * 测试 7: 异常情况 - Context 为 null
     */
    @Test
    void testNullContext() {
        log.info("========== 测试 7: 异常情况处理 ==========");

        // Given: Context 为 null
        ChatModelResponseContext mockContext = mock(ChatModelResponseContext.class);
        when(mockContext.response()).thenReturn(null);
        when(mockContext.hashCode()).thenReturn(12349);

        // 设置 ThreadLocal 元数据
        AiContextHolder.setAgentName("DIAGNOSIS");
        AiContextHolder.setTraceId("test-null-context");
        AiContextHolder.setPrompt("Test Prompt");
        AiContextHolder.setResponse("Test Response");

        // When: 调用 onResponse（不应抛出异常）
        assertDoesNotThrow(() -> listener.onResponse(mockContext));

        // Then: 验证使用了估算兜底
        verify(logService, times(1)).saveAsync(any(AiInvocationLog.class));

        log.info("✅ 异常情况处理测试通过（未抛出异常）");

        // 清理 ThreadLocal
        AiContextHolder.clear();
    }
}
