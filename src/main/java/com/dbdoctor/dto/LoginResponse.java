package com.dbdoctor.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录响应 DTO
 *
 * @author DB-Doctor
 * @version 3.1.0
 * @since 3.1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    /**
     * 用户名
     */
    private String username;

    /**
     * 登录时间
     */
    private String loginTime;

    /**
     * 认证令牌（简单实现，生产环境建议使用 JWT）
     */
    private String token;

    public LoginResponse(String username, String token) {
        this.username = username;
        this.token = token;
        this.loginTime = java.time.LocalDateTime.now().toString();
    }
}
