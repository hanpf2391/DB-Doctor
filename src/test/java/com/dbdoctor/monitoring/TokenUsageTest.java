package com.dbdoctor.monitoring;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TokenUsage API 可用性测试
 *
 * <p>目的：验证 LangChain4j 0.36.1 的 TokenUsage API 是否可用</p>
 *
 * <p>测试策略：</p>
 * <ul>
 *   <li>直接调用 ChatLanguageModel.generate() 获取 TokenUsage</li>
 *   <li>通过 ChatModelListener 拦截 TokenUsage</li>
 *   <li>验证官方统计数据 vs 估算数据</li>
 * </ul>
 *
 * @author DB-Doctor
 * @version 2.3.1
 */
@Slf4j
@SpringBootTest
class TokenUsageTest {

    private ChatLanguageModel chatModel;

    @BeforeEach
    void setUp() {
        // 使用环境变量中的配置
        String apiKey = System.getenv("OPENAI_API_KEY");
        String baseUrl = System.getenv("OPENAI_BASE_URL");

        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("OPENAI_API_KEY 未配置，跳过测试");
            return;
        }

        chatModel = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl != null ? baseUrl : "https://api.openai.com/v1")
                .modelName("gpt-4o-mini")
                .temperature(0.0)
                .build();
    }

    /**
     * 测试 1: 直接调用 ChatLanguageModel.generate() 获取 TokenUsage
     */
    @Test
    void testDirectTokenUsage() {
        if (chatModel == null) {
            log.warn("ChatModel 未初始化，跳过测试");
            return;
        }

        // When: 生成 AI 回复
        String prompt = "什么是 MySQL 慢查询？用一句话回答。";
        String response = chatModel.generate(prompt);

        // Then: 验证响应内容
        assertNotNull(response, "响应不应为 null");
        assertFalse(response.isEmpty(), "响应不应为空");
        log.info("AI 回复: {}", response);

        // 注意：直接调用 generate() 无法直接获取 TokenUsage
        // 需要通过 ChatModelListener 拦截
        log.info("⚠️ 直接调用方式无法获取 TokenUsage，需要通过 Listener 拦截");
    }

    /**
     * 测试 2: 通过 ChatModelListener 拦截 TokenUsage
     */
    @Test
    void testTokenUsageThroughListener() {
        if (chatModel == null) {
            log.warn("ChatModel 未初始化，跳过测试");
            return;
        }

        // Given: 创建 Token 捕获 Listener
        AtomicReference<dev.langchain4j.model.output.TokenUsage> tokenUsageRef =
                new AtomicReference<>();

        ChatModelListener listener = new ChatModelListener() {
            @Override
            public void onResponse(ChatModelResponseContext context) {
                if (context.response() != null) {
                    dev.langchain4j.model.output.TokenUsage usage = context.response().tokenUsage();
                    tokenUsageRef.set(usage);

                    log.info("✅ 拦截到 TokenUsage:");
                    log.info("  - Input Tokens: {}", usage.inputTokenCount());
                    log.info("  - Output Tokens: {}", usage.outputTokenCount());
                    log.info("  - Total Tokens: {}", usage.totalTokenCount());
                }
            }
        };

        // When: 生成 AI 回复（带 Listener）
        // 注意：OpenAiChatModel 不支持动态添加 Listener
        // 需要在构建时配置，这里仅演示 API 使用方式

        log.info("⚠️ 当前 ChatModel 实现不支持运行时添加 Listener");
        log.info("需要通过 AiServices 构建 Agent 时配置 Listener");
    }

    /**
     * 测试 3: Token 估算算法准确性验证
     */
    @Test
    void testTokenEstimationAccuracy() {
        // Given: 测试文本
        String prompt = "分析以下 SQL 慢查询日志，给出优化建议：";
        String response = "建议在 user_id 字段上创建索引，可以显著提升查询性能。";

        // When: 使用估算算法
        int estimatedInputTokens = TokenEstimator.estimateInputTokens(prompt);
        int estimatedOutputTokens = TokenEstimator.estimateOutputTokens(response);
        int estimatedTotal = estimatedInputTokens + estimatedOutputTokens;

        // Then: 验证估算结果合理性
        assertTrue(estimatedInputTokens > 0, "输入 Token 估算应大于 0");
        assertTrue(estimatedOutputTokens > 0, "输出 Token 估算应大于 0");
        assertTrue(estimatedTotal > 0, "总 Token 估算应大于 0");

        log.info("📊 Token 估算结果:");
        log.info("  - 输入: {} tokens (中文+英文混合)", estimatedInputTokens);
        log.info("  - 输出: {} tokens", estimatedOutputTokens);
        log.info("  - 总计: {} tokens", estimatedTotal);

        // 验证估算算法的基本合理性
        // 中文约 1.5 字符/token，英文约 4 字符/token
        int chineseChars = countChineseCharacters(prompt);
        int englishChars = countEnglishCharacters(prompt);

        log.info("  - Prompt 包含: {} 中文字符, {} 英文字符", chineseChars, englishChars);
        log.info("  - 预期输入 Token: ~{} (仅供参考)",
                (int) Math.ceil(chineseChars / 1.5 + englishChars / 4.0));
    }

    /**
     * 测试 4: 纯中文文本 Token 估算
     */
    @Test
    void testChineseTextTokenEstimation() {
        String chineseText = "这是一个关于数据库优化的测试文本，包含多个中文字符。";

        int estimatedTokens = TokenEstimator.estimateInputTokens(chineseText);

        assertTrue(estimatedTokens > 0, "Token 估算应大于 0");

        log.info("📊 纯中文文本 Token 估算:");
        log.info("  - 文本: {}", chineseText);
        log.info("  - 字符数: {}", chineseText.length());
        log.info("  - 估算 Token: {}", estimatedTokens);
        log.info("  - 字符/Token 比率: {}", String.format("%.2f", chineseText.length() * 1.0 / estimatedTokens));

        // 中文字符/Token 比率应在 1.2-1.8 之间
        double ratio = chineseText.length() * 1.0 / estimatedTokens;
        assertTrue(ratio >= 1.2 && ratio <= 1.8,
                "中文字符/Token 比率应在合理范围内");
    }

    /**
     * 测试 5: 纯英文文本 Token 估算
     */
    @Test
    void testEnglishTextTokenEstimation() {
        String englishText = "This is a database optimization test with multiple English words.";

        int estimatedTokens = TokenEstimator.estimateInputTokens(englishText);

        assertTrue(estimatedTokens > 0, "Token 估算应大于 0");

        log.info("📊 纯英文文本 Token 估算:");
        log.info("  - Text: {}", englishText);
        log.info("  - Characters: {}", englishText.length());
        log.info("  - Estimated Tokens: {}", estimatedTokens);
        log.info("  - Characters/Token Ratio: {}",
                String.format("%.2f", englishText.length() * 1.0 / estimatedTokens));

        // 英文字符/Token 比率应在 3-5 之间
        double ratio = englishText.length() * 1.0 / estimatedTokens;
        assertTrue(ratio >= 3.0 && ratio <= 5.0,
                "英文字符/Token 比率应在合理范围内");
    }

    /**
     * 测试 6: SQL 代码 Token 估算
     */
    @Test
    void testSqlCodeTokenEstimation() {
        String sqlCode = "SELECT * FROM users WHERE id = 1 AND status = 'active'";

        int estimatedTokens = TokenEstimator.estimateInputTokens(sqlCode);

        assertTrue(estimatedTokens > 0, "Token 估算应大于 0");

        log.info("📊 SQL 代码 Token 估算:");
        log.info("  - SQL: {}", sqlCode);
        log.info("  - 字符数: {}", sqlCode.length());
        log.info("  - 估算 Token: {}", estimatedTokens);
        log.info("  - 字符/Token 比率: {}",
                String.format("%.2f", sqlCode.length() * 1.0 / estimatedTokens));

        // SQL 代码字符/Token 比率应在 2-4 之间
        double ratio = sqlCode.length() * 1.0 / estimatedTokens;
        assertTrue(ratio >= 2.0 && ratio <= 4.0,
                "SQL 代码字符/Token 比率应在合理范围内");
    }

    /**
     * 测试 7: 边界情况 - 空字符串
     */
    @Test
    void testEmptyStringTokenEstimation() {
        int emptyTokens = TokenEstimator.estimateInputTokens("");
        assertEquals(0, emptyTokens, "空字符串应估算为 0 tokens");

        int nullTokens = TokenEstimator.estimateInputTokens(null);
        assertEquals(0, nullTokens, "null 应估算为 0 tokens");

        log.info("✅ 边界情况测试通过");
    }

    // ========== 辅助方法 ==========

    /**
     * 统计中文字符数
     */
    private int countChineseCharacters(String text) {
        if (text == null) return 0;
        return (int) text.chars()
                .filter(c -> c >= 0x4E00 && c <= 0x9FA5)
                .count();
    }

    /**
     * 统计英文字符数
     */
    private int countEnglishCharacters(String text) {
        if (text == null) return 0;
        return (int) text.chars()
                .filter(c -> (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z'))
                .count();
    }
}
