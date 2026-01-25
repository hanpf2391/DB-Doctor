package com.dbdoctor.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * LangChain4j AI 配置类
 * 用于配置 OpenAI 接口
 *
 * 注意：当前版本已禁用 AI 功能，此配置类不会被加载
 *
 * @author DB-Doctor
 * @version 1.0.0
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "db-doctor.ai.enabled", havingValue = "true", matchIfMissing = false)
public class AiConfig {

    @Value("${langchain4j.open-ai.chat-model.api-key}")
    private String apiKey;

    @Value("${langchain4j.open-ai.chat-model.model-name:gpt-4}")
    private String modelName;

    @Value("${langchain4j.open-ai.chat-model.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    @Value("${langchain4j.open-ai.chat-model.temperature:0.0}")
    private Double temperature;

    @Value("${langchain4j.open-ai.chat-model.timeout:60s}")
    private Duration timeout;

    @Value("${langchain4j.open-ai.chat-model.max-tokens:2000}")
    private Integer maxTokens;

    /**
     * 配置 OpenAI Chat Model
     *
     * @return ChatLanguageModel 实例
     */
    @Bean
    public ChatLanguageModel chatLanguageModel() {
        log.info("初始化 ChatLanguageModel: model={}, baseUrl={}, temperature={}",
                modelName, baseUrl, temperature);

        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .baseUrl(baseUrl)
                .temperature(temperature)
                .timeout(timeout)
                .maxTokens(maxTokens)
                .build();
    }
}
