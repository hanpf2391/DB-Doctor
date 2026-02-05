package com.dbdoctor.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户实体类
 *
 * <p>用于系统登录认证，支持初始默认账号和密码修改</p>
 *
 * @author DB-Doctor
 * @version 3.1.0
 * @since 3.1.0
 */
@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "sys_user", indexes = {
    @Index(name = "idx_username", columnList = "username", unique = true)
})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 用户名（唯一）
     */
    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    /**
     * 密码（MD5 加密存储）
     */
    @Column(name = "password", nullable = false, length = 64)
    private String password;

    /**
     * 创建时间
     */
    @Column(name = "created_time", nullable = false, updatable = false)
    private java.time.LocalDateTime createdTime;

    /**
     * 最后修改时间
     */
    @Column(name = "updated_time", nullable = false)
    private java.time.LocalDateTime updatedTime;

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
