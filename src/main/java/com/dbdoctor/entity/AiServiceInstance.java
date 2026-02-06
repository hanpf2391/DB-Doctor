package com.dbdoctor.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AI服务实例
 *
 * 设计理念：
 * - 配置录入与配置使用分离
 * - 预先录入多个AI服务配置
 * - 支持配置验证（测试连接）
 * - 配置使用时选择实例
 *
 * 使用场景：
 * - OpenAI GPT-4、GPT-3.5
 * - Ollama 本地模型
 * - DeepSeek 在线服务
 * - 不同用途使用不同模型（诊断、推理、编码）
 *
 * @author DB-Doctor
 * @version 3.1.0
 */
@Entity
@Table(name = "ai_service_instances")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiServiceInstance {

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 实例名称（唯一标识）
     * 例如：OpenAI-GPT4、Ollama-Qwen、DeepSeek-Coder
     */
    @Column(name = "instance_name", nullable = false, unique = true, length = 100)
    private String instanceName;

    /**
     * AI服务提供商
     * 支持：openai, ollama, deepseek, anthropic, azure
     */
    @Column(name = "provider", nullable = false, length = 50)
    private String provider;

    /**
     * 部署类型
     * local: 本地部署（Ollama、LM Studio 等）
     * cloud: 云端API（OpenAI、DeepSeek、Anthropic 等）
     */
    @Column(name = "deployment_type", nullable = false, length = 20)
    @Builder.Default
    private String deploymentType = DeploymentType.CLOUD.getValue(); // 默认为云端部署

    /**
     * API基础URL
     */
    @Column(name = "base_url", length = 500)
    private String baseUrl;

    /**
     * 加密后的API密钥
     * 使用 AES 加密存储
     */
    @Column(name = "api_key", length = 500)
    private String apiKey;

    /**
     * 模型名称
     */
    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    /**
     * 温度参数（0.0-1.0）
     */
    @Column(name = "temperature")
    private Double temperature;

    /**
     * 最大Token数
     */
    @Column(name = "max_tokens")
    private Integer maxTokens;

    /**
     * 超时时间（秒）
     */
    @Column(name = "timeout_seconds")
    private Integer timeoutSeconds;

    /**
     * 实例描述
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * 能力标签（JSON格式）
     * 例如：["推理", "编码", "分析", "对话"]
     */
    @Column(name = "capability_tags", columnDefinition = "JSON")
    private String capabilityTags;

    /**
     * 是否已验证连接
     */
    @Column(name = "is_valid", nullable = false)
    @Builder.Default
    private Boolean isValid = false;

    /**
     * 最后验证时间
     */
    @Column(name = "last_validated_at")
    private LocalDateTime lastValidatedAt;

    /**
     * 验证错误信息
     */
    @Column(name = "validation_error", length = 500)
    private String validationError;

    /**
     * 是否启用
     */
    @Column(name = "is_enabled", nullable = false)
    @Builder.Default
    private Boolean isEnabled = true;

    /**
     * 是否为默认实例
     */
    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private Boolean isDefault = false;

    /**
     * 创建人
     */
    @Column(name = "created_by", length = 50)
    private String createdBy;

    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新人
     */
    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    /**
     * 更新时间
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Provider 枚举
     */
    public enum Provider {
        OPENAI("openai"),
        OLLAMA("ollama"),
        DEEPSEEK("deepseek"),
        ANTHROPIC("anthropic"),
        AZURE("azure");

        private final String value;

        Provider(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * 部署类型枚举
     */
    public enum DeploymentType {
        LOCAL("local", "本地部署"),
        CLOUD("cloud", "云端API");

        private final String value;
        private final String label;

        DeploymentType(String value, String label) {
            this.value = value;
            this.label = label;
        }

        public String getValue() {
            return value;
        }

        public String getLabel() {
            return label;
        }
    }
}
