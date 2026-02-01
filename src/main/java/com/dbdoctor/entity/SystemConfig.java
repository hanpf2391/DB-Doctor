package com.dbdoctor.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 系统配置实体类
 *
 * <p>支持配置从 application.yml 迁移到数据库，实现页面配置和热加载</p>
 *
 * @author DB-Doctor
 * @version 2.4.0
 * @since 2.2.0
 */
@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "system_config", indexes = {
    @Index(name = "idx_config_group", columnList = "config_group"),
    @Index(name = "idx_is_enabled", columnList = "is_enabled")
})
public class SystemConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 配置键（如：database.url）
     */
    @Column(name = "config_key", nullable = false, unique = true, length = 100)
    private String configKey;

    /**
     * 配置分组（database/log/ai/notification/scheduler）
     */
    @Column(name = "config_group", nullable = false, length = 50)
    private String configGroup;

    /**
     * 数据类型（string/number/boolean/json/password）
     */
    @Column(name = "config_type", nullable = false, length = 20)
    private String configType;

    /**
     * 配置值（敏感信息加密存储）
     */
    @Column(name = "config_value", columnDefinition = "TEXT")
    private String configValue;

    /**
     * 默认值
     */
    @Column(name = "default_value", columnDefinition = "TEXT")
    private String defaultValue;

    /**
     * 配置名称（中文显示）
     */
    @Column(name = "config_name", nullable = false, length = 200)
    private String configName;

    /**
     * 配置说明
     */
    @Column(name = "config_description", columnDefinition = "TEXT")
    private String configDescription;

    /**
     * 是否必填
     */
    @Column(name = "is_required", nullable = false)
    private Boolean isRequired;

    /**
     * 是否敏感信息（需要加密）
     */
    @Column(name = "is_sensitive", nullable = false)
    private Boolean isSensitive;

    /**
     * 是否启用
     */
    @Column(name = "is_enabled", nullable = false)
    private Boolean isEnabled;

    /**
     * 校验正则表达式
     */
    @Column(name = "validation_regex", length = 500)
    private String validationRegex;

    /**
     * 最小值（数字类型）
     */
    @Column(name = "validation_min")
    private Double validationMin;

    /**
     * 最大值（数字类型）
     */
    @Column(name = "validation_max")
    private Double validationMax;

    /**
     * 允许的值列表（JSON数组格式）
     */
    @Column(name = "allowed_values", columnDefinition = "TEXT")
    private String allowedValues;

    /**
     * 显示顺序
     */
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    /**
     * 输入框类型（text/number/boolean/textarea/select/password）
     */
    @Column(name = "input_type", length = 20)
    private String inputType;

    /**
     * 输入框占位符
     */
    @Column(name = "ui_placeholder", length = 200)
    private String uiPlaceholder;

    /**
     * 创建时间
     */
    @Column(name = "created_time", nullable = false, updatable = false)
    private java.time.LocalDateTime createdTime;

    /**
     * 更新时间
     */
    @Column(name = "updated_time", nullable = false)
    private java.time.LocalDateTime updatedTime;

    /**
     * 更新者
     */
    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    // ==================== 兼容旧版本的getter方法 ====================

    /**
     * @deprecated 使用 {@link #getConfigGroup()} 替代
     */
    @Deprecated
    public String getConfigCategory() {
        return configGroup;
    }

    /**
     * @deprecated 使用 {@link #setConfigGroup(String)} 替代
     */
    @Deprecated
    public void setConfigCategory(String configCategory) {
        this.configGroup = configCategory;
    }

    /**
     * @deprecated 使用 {@link #getConfigDescription()} 替代
     */
    @Deprecated
    public String getDescription() {
        return configDescription;
    }

    /**
     * @deprecated 使用 {@link #setConfigDescription(String)} 替代
     */
    @Deprecated
    public void setDescription(String description) {
        this.configDescription = description;
    }

    @PrePersist
    protected void onCreate() {
        createdTime = java.time.LocalDateTime.now();
        updatedTime = java.time.LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedTime = java.time.LocalDateTime.now();
    }
}
