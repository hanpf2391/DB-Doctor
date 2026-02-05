package com.dbdoctor.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 数据库连接实例
 *
 * 设计理念：
 * - 配置录入与配置使用分离
 * - 预先录入多个数据库连接配置
 * - 支持配置验证（测试连接）
 * - 配置使用时选择实例，而非重复输入
 *
 * 使用场景：
 * - 生产环境、测试环境、开发环境数据库
 * - 多个业务数据库（核心业务库、报表库、日志库）
 * - 一键切换数据库连接
 *
 * @author DB-Doctor
 * @version 3.1.0
 */
@Entity
@Table(name = "database_instances")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DatabaseInstance {

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 实例名称（唯一标识）
     * 例如：生产数据库、测试数据库、核心业务库
     */
    @Column(name = "instance_name", nullable = false, unique = true, length = 100)
    private String instanceName;

    /**
     * 实例类型
     * 支持：mysql, postgresql, oracle
     */
    @Column(name = "instance_type", nullable = false, length = 20)
    private String instanceType;

    /**
     * 数据库连接URL
     */
    @Column(name = "url", nullable = false, length = 500)
    private String url;

    /**
     * 用户名
     */
    @Column(name = "username", nullable = false, length = 100)
    private String username;

    /**
     * 加密后的密码
     * 使用 AES 加密存储
     */
    @Column(name = "password", nullable = false, length = 500)
    private String password;

    /**
     * 实例描述
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * 环境标识
     */
    @Column(name = "environment", length = 20)
    private String environment;

    /**
     * 标签列表（JSON格式）
     * 例如：["主库", "核心业务", "高可用"]
     */
    @Column(name = "tags", columnDefinition = "JSON")
    private String tags;

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
     * 环境枚举
     */
    public enum Environment {
        PRODUCTION("production"),
        STAGING("staging"),
        DEVELOPMENT("development"),
        TESTING("testing");

        private final String value;

        Environment(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * 实例类型枚举
     */
    public enum InstanceType {
        MYSQL("mysql"),
        POSTGRESQL("postgresql"),
        ORACLE("oracle");

        private final String value;

        InstanceType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
