package com.dbdoctor.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI 配置属性
 * 支持多供应商：ollama, openai, deepseek, aliyun
 *
 * @author DB-Doctor
 * @version 2.2.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "db-doctor.ai")
public class AiProperties {

    /**
     * 全局开关
     */
    private boolean enabled = false;

    /**
     * 主治医生配置
     */
    private AgentConfig diagnosis;

    /**
     * 推理专家配置
     */
    private AgentConfig reasoning;

    /**
     * 编码专家配置
     */
    private AgentConfig coding;

    /**
     * Agent 配置
     */
    @Data
    public static class AgentConfig {
        /**
         * 供应商类型: ollama, openai, deepseek, aliyun
         */
        private String provider = "ollama";

        /**
         * API 基础 URL
         */
        private String baseUrl = "http://localhost:11434";

        /**
         * API Key (Ollama 可填任意值)
         */
        private String apiKey = "";

        /**
         * 模型名称
         */
        private String modelName = "qwen2.5:7b";

        /**
         * 温度参数 (0.0-1.0)
         */
        private Double temperature = 0.0;

        /**
         * 超时时间（秒）
         */
        private Long timeoutSeconds = 60L;

        // 无参构造函数
        public AgentConfig() {}

        // 全参构造函数（用于热重载）
        public AgentConfig(String provider, String baseUrl, String modelName, double temperature, String apiKey) {
            this.provider = provider;
            this.baseUrl = baseUrl;
            this.modelName = modelName;
            this.temperature = temperature;
            this.apiKey = apiKey;
        }
    }
}
