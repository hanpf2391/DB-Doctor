package com.dbdoctor.config;

import com.dbdoctor.agent.DiagnosticTools;
import com.dbdoctor.agent.DiagnosticToolsImpl;
import com.dbdoctor.agent.DBAgent;
import com.dbdoctor.agent.ReasoningAgent;
import com.dbdoctor.agent.CodingAgent;
import com.dbdoctor.monitoring.AiMonitoringListener;
import com.dbdoctor.service.AiConfigManagementService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.util.List;

/**
 * AI 配置类 - 动态模型工厂（数据库配置驱动，懒加载模式）
 * 支持 Ollama 本地模型、OpenAI 兼容云端 API（DeepSeek、硅基流动等）
 *
 * 配置说明：
 * - AI 功能的启用/禁用由数据库配置 ai.enabled 控制
 * - AI Bean 采用懒加载模式，每次使用时从数据库读取最新配置
 * - 支持运行时热加载，无需重启应用
 * - 默认禁用，需要在系统设置中启用
 *
 * @author DB-Doctor
 * @version 3.1.0
 */
@Slf4j
@Configuration
public class AiConfig {

    @Autowired
    private AiConfigManagementService aiConfigService;

    @Autowired
    @Qualifier("targetJdbcTemplate")
    private JdbcTemplate targetJdbcTemplate;

    /**
     * AI 监控监听器（可选，如果监控功能未启用则为 null）
     */
    @Autowired(required = false)
    private AiMonitoringListener aiMonitoringListener;

    /**
     * 配置主治医生的 ChatLanguageModel（从数据库读取配置）
     * 支持工具调用，必须使用支持 Tool Calling 的模型
     *
     * @return ChatLanguageModel 实例
     */
    @Bean
    public ChatLanguageModel diagnosisChatLanguageModel() {
        log.info("🔍 [数据库配置] 创建主治医生 ChatLanguageModel");

        // 检查 AI 是否启用
        if (!aiConfigService.isAiEnabled()) {
            log.warn("⚠️ AI 功能未启用，返回默认模型（将在首次使用时从数据库加载）");
            // 返回一个默认的模型，后续使用时会从数据库重新加载
            return dev.langchain4j.model.ollama.OllamaChatModel.builder()
                    .baseUrl("http://localhost:11434")
                    .modelName("qwen2.5:7b")
                    .temperature(0.0)
                    .timeout(Duration.ofSeconds(60))
                    .build();
        }

        // 从数据库读取配置
        String provider = aiConfigService.getAiProvider();
        String baseUrl = aiConfigService.getBaseUrl();
        String apiKey = aiConfigService.getApiKey();
        String modelName = aiConfigService.getDiagnosisModelName();
        Long timeoutSeconds = aiConfigService.getTimeoutSeconds();

        log.info("📊 [从数据库读取配置] agent=diagnosis, provider={}, model={}, baseUrl={}",
                provider, modelName, baseUrl);

        return createModelInternal(provider, baseUrl, apiKey, modelName, 0.0, timeoutSeconds);
    }

    /**
     * 配置推理专家的 ChatLanguageModel（从数据库读取配置）
     * 不需要工具调用，可以使用任意模型
     *
     * @return ChatLanguageModel 实例
     */
    @Bean
    public ChatLanguageModel reasoningChatLanguageModel() {
        log.info("🔍 [数据库配置] 创建推理专家 ChatLanguageModel");

        // 检查 AI 是否启用
        if (!aiConfigService.isAiEnabled()) {
            log.warn("⚠️ AI 功能未启用，返回默认模型");
            return dev.langchain4j.model.ollama.OllamaChatModel.builder()
                    .baseUrl("http://localhost:11434")
                    .modelName("deepseek-r1:7b")
                    .temperature(0.0)
                    .timeout(Duration.ofSeconds(60))
                    .build();
        }

        // 从数据库读取配置
        String provider = aiConfigService.getAiProvider();
        String baseUrl = aiConfigService.getBaseUrl();
        String apiKey = aiConfigService.getApiKey();
        String modelName = aiConfigService.getReasoningModelName();
        Long timeoutSeconds = aiConfigService.getTimeoutSeconds();

        log.info("📊 [从数据库读取配置] agent=reasoning, provider={}, model={}, baseUrl={}",
                provider, modelName, baseUrl);

        return createModelInternal(provider, baseUrl, apiKey, modelName, 0.0, timeoutSeconds);
    }

    /**
     * 配置编码专家的 ChatLanguageModel（从数据库读取配置）
     * 不需要工具调用，可以使用任意模型
     *
     * @return ChatLanguageModel 实例
     */
    @Bean
    public ChatLanguageModel codingChatLanguageModel() {
        log.info("🔍 [数据库配置] 创建编码专家 ChatLanguageModel");

        // 检查 AI 是否启用
        if (!aiConfigService.isAiEnabled()) {
            log.warn("⚠️ AI 功能未启用，返回默认模型");
            return dev.langchain4j.model.ollama.OllamaChatModel.builder()
                    .baseUrl("http://localhost:11434")
                    .modelName("deepseek-coder:6.7b")
                    .temperature(0.0)
                    .timeout(Duration.ofSeconds(60))
                    .build();
        }

        // 从数据库读取配置
        String provider = aiConfigService.getAiProvider();
        String baseUrl = aiConfigService.getBaseUrl();
        String apiKey = aiConfigService.getApiKey();
        String modelName = aiConfigService.getCodingModelName();
        Long timeoutSeconds = aiConfigService.getTimeoutSeconds();

        log.info("📊 [从数据库读取配置] agent=coding, provider={}, model={}, baseUrl={}",
                provider, modelName, baseUrl);

        return createModelInternal(provider, baseUrl, apiKey, modelName, 0.0, timeoutSeconds);
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

        return agent;
    }

    /**
     * 配置 DiagnosticTools Bean（诊断工具箱）
     *
     * 注意：返回的是 DiagnosticToolsImpl 实例，作为 Spring Bean 管理
     * 但在 DBAgent 中仍会创建新的非代理实例用于 LangChain4j 工具调用
     *
     * @return DiagnosticTools 实例
     */
    @Bean
    public DiagnosticTools diagnosticTools() {
        log.info("初始化 DiagnosticTools Bean（用于 MultiAgentCoordinator）");
        return new DiagnosticToolsImpl(targetJdbcTemplate);
    }

    // === 辅助方法 ===

    /**
     * 动态模型工厂：根据配置创建 ChatLanguageModel
     *
     * 支持的供应商：
     * - ollama：本地 Ollama 模型（使用 OllamaChatModel）
     * - openai/deepseek/aliyun：OpenAI 兼容的云端 API（使用 OpenAiChatModel）
     *
     * @param provider 供应商
     * @param baseUrl API 基础 URL
     * @param apiKey API 密钥
     * @param modelName 模型名称
     * @param temperature 温度参数
     * @param timeoutSeconds 超时时间（秒）
     * @return ChatLanguageModel 实例
     */
    private ChatLanguageModel createModelInternal(
            String provider,
            String baseUrl,
            String apiKey,
            String modelName,
            Double temperature,
            Long timeoutSeconds
    ) {
        log.debug("创建模型: provider={}, baseUrl={}, model={}, temperature={}",
                provider, baseUrl, modelName, temperature);

        // 分支 A：本地 Ollama 模型
        if ("ollama".equalsIgnoreCase(provider)) {
            log.info("使用 OllamaChatModel（原生客户端，完美支持工具调用）");
            dev.langchain4j.model.ollama.OllamaChatModel.OllamaChatModelBuilder builder =
                    dev.langchain4j.model.ollama.OllamaChatModel.builder()
                    .baseUrl(baseUrl)
                    .modelName(modelName)
                    .temperature(temperature)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .logRequests(true)   // 启用请求日志
                    .logResponses(true); // 启用响应日志

            // 注入监控监听器（如果存在）
            if (aiMonitoringListener != null) {
                builder.listeners(List.of(aiMonitoringListener));
                log.debug("✅ AI 监控监听器已注入到 OllamaChatModel");
            }

            return builder.build();
        }

        // 分支 B：OpenAI 兼容协议（DeepSeek、硅基流动、OpenAI 等）
        else if ("openai".equalsIgnoreCase(provider)
                || "deepseek".equalsIgnoreCase(provider)
                || "aliyun".equalsIgnoreCase(provider)
                || "siliconflow".equalsIgnoreCase(provider)) {
            log.info("使用 OpenAiChatModel（OpenAI 兼容协议）");
            OpenAiChatModel.OpenAiChatModelBuilder builder =
                    OpenAiChatModel.builder()
                    .baseUrl(baseUrl)
                    .apiKey(apiKey)
                    .modelName(modelName)
                    .temperature(temperature)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .logRequests(true)
                    .logResponses(true);

            // 注入监控监听器（如果存在）
            if (aiMonitoringListener != null) {
                builder.listeners(List.of(aiMonitoringListener));
                log.debug("✅ AI 监控监听器已注入到 OpenAiChatModel");
            }

            return builder.build();
        }

        throw new IllegalArgumentException("❌ 不支持的 AI 供应商: " + provider + "。支持的选项: ollama, openai, deepseek, aliyun, siliconflow");
    }
}
