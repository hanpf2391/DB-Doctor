package com.dbdoctor.config;

import com.dbdoctor.agent.DiagnosticTools;
import com.dbdoctor.agent.DiagnosticToolsImpl;
import com.dbdoctor.agent.SqlDiagnosticsTools;
import com.dbdoctor.agent.DBAgent;
import com.dbdoctor.agent.ReasoningAgent;
import com.dbdoctor.agent.CodingAgent;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;

/**
 * AI 配置类 - 动态模型工厂
 * 支持 Ollama 本地模型、OpenAI 兼容云端 API（DeepSeek、硅基流动等）
 *
 * @author DB-Doctor
 * @version 2.2.0
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "db-doctor.ai.enabled", havingValue = "true", matchIfMissing = false)
public class AiConfig {

    @Autowired
    private AiProperties properties;

    @Autowired
    private SqlDiagnosticsTools sqlDiagnosticsTools;

    @Autowired
    @Qualifier("targetJdbcTemplate")
    private JdbcTemplate targetJdbcTemplate;

    /**
     * 配置主治医生的 ChatLanguageModel
     * 支持工具调用，必须使用支持 Tool Calling 的模型
     *
     * @return ChatLanguageModel 实例
     */
    @Bean
    public ChatLanguageModel diagnosisChatLanguageModel() {
        AiProperties.AgentConfig config = properties.getDiagnosis();
        log.info("初始化主治医生 ChatLanguageModel: provider={}, model={}",
                config.getProvider(), config.getModelName());
        return createModel(config);
    }

    /**
     * 配置推理专家的 ChatLanguageModel
     * 不需要工具调用，可以使用任意模型
     *
     * @return ChatLanguageModel 实例
     */
    @Bean
    public ChatLanguageModel reasoningChatLanguageModel() {
        AiProperties.AgentConfig config = properties.getReasoning();
        log.info("初始化推理专家 ChatLanguageModel: provider={}, model={}",
                config.getProvider(), config.getModelName());
        return createModel(config);
    }

    /**
     * 配置编码专家的 ChatLanguageModel
     * 不需要工具调用，可以使用任意模型
     *
     * @return ChatLanguageModel 实例
     */
    @Bean
    public ChatLanguageModel codingChatLanguageModel() {
        AiProperties.AgentConfig config = properties.getCoding();
        log.info("初始化编码专家 ChatLanguageModel: provider={}, model={}",
                config.getProvider(), config.getModelName());
        return createModel(config);
    }

    /**
     * 配置 DBAgent Bean（主治医生）
     *
     * 核心特性：
     * - 使用非代理的 DiagnosticToolsImpl 实例注册工具
     * - 避免 Spring CGLIB 代理导致 LangChain4j 无法识别工具方法
     *
     * @return DBAgent 实例
     */
    @Bean
    public DBAgent dbAgent(@Qualifier("diagnosisChatLanguageModel") ChatLanguageModel chatLanguageModel) {
        log.info("初始化 DBAgent（主治医生）: 绑定 ChatLanguageModel 和诊断工具箱");

        // 创建非代理的工具实例（避免 Spring CGLIB 代理干扰）
        DiagnosticTools diagnosticTools = new DiagnosticToolsImpl(targetJdbcTemplate);
        log.info("✅ 工具注册: 使用非代理的 DiagnosticToolsImpl 实例，工具方法可被 LangChain4j 正确识别");

        return AiServices.builder(DBAgent.class)
                .chatLanguageModel(chatLanguageModel)
                .tools(diagnosticTools)  // 注入非代理的诊断工具箱
                .build();
    }

    /**
     * 配置 ReasoningAgent Bean（推理专家）
     *
     * @return ReasoningAgent 实例
     */
    @Bean
    public ReasoningAgent reasoningAgent(@Qualifier("reasoningChatLanguageModel") ChatLanguageModel reasoningChatLanguageModel) {
        log.info("初始化 ReasoningAgent（推理专家）");

        ReasoningAgent agent = AiServices.builder(ReasoningAgent.class)
                .chatLanguageModel(reasoningChatLanguageModel)
                .build();

        // 注入到 SqlDiagnosticsTools（方便工具类调用专家）
        sqlDiagnosticsTools.setReasoningAgent(agent);

        return agent;
    }

    /**
     * 配置 CodingAgent Bean（编码专家）
     *
     * @return CodingAgent 实例
     */
    @Bean
    public CodingAgent codingAgent(@Qualifier("codingChatLanguageModel") ChatLanguageModel codingChatLanguageModel) {
        log.info("初始化 CodingAgent（编码专家）");

        CodingAgent agent = AiServices.builder(CodingAgent.class)
                .chatLanguageModel(codingChatLanguageModel)
                .build();

        // 注入到 SqlDiagnosticsTools（方便工具类调用专家）
        sqlDiagnosticsTools.setCodingAgent(agent);

        return agent;
    }

    // === 辅助方法 ===

    /**
     * 动态模型工厂：根据配置创建 ChatLanguageModel
     *
     * 支持的供应商：
     * - ollama：本地 Ollama 模型（使用 OllamaChatModel）
     * - openai/deepseek/aliyun：OpenAI 兼容的云端 API（使用 OpenAiChatModel）
     *
     * @param config Agent 配置
     * @return ChatLanguageModel 实例
     */
    private ChatLanguageModel createModel(AiProperties.AgentConfig config) {
        String provider = config.getProvider();
        String baseUrl = config.getBaseUrl();
        String apiKey = config.getApiKey();
        String modelName = config.getModelName();
        Double temperature = config.getTemperature();
        Long timeoutSeconds = config.getTimeoutSeconds();

        log.debug("创建模型: provider={}, baseUrl={}, model={}, temperature={}",
                provider, baseUrl, modelName, temperature);

        // 分支 A：本地 Ollama 模型
        if ("ollama".equalsIgnoreCase(provider)) {
            log.info("使用 OllamaChatModel（原生客户端，完美支持工具调用）");
            return dev.langchain4j.model.ollama.OllamaChatModel.builder()
                    .baseUrl(baseUrl)
                    .modelName(modelName)
                    .temperature(temperature)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .logRequests(true)   // 启用请求日志
                    .logResponses(true)  // 启用响应日志
                    .build();
        }

        // 分支 B：OpenAI 兼容协议（DeepSeek、硅基流动、OpenAI 等）
        else if ("openai".equalsIgnoreCase(provider)
                || "deepseek".equalsIgnoreCase(provider)
                || "aliyun".equalsIgnoreCase(provider)
                || "siliconflow".equalsIgnoreCase(provider)) {
            log.info("使用 OpenAiChatModel（OpenAI 兼容协议）");
            return OpenAiChatModel.builder()
                    .baseUrl(baseUrl)
                    .apiKey(apiKey)
                    .modelName(modelName)
                    .temperature(temperature)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .logRequests(true)
                    .logResponses(true)
                    .build();
        }

        throw new IllegalArgumentException("❌ 不支持的 AI 供应商: " + provider + "。支持的选项: ollama, openai, deepseek, aliyun, siliconflow");
    }
}
