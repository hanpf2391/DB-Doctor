package com.dbdoctor.service;

import com.dbdoctor.dto.ChangePasswordRequest;
import com.dbdoctor.dto.LoginRequest;
import com.dbdoctor.dto.LoginResponse;

/**
 * 认证服务接口
 *
 * @author DB-Doctor
 * @version 3.1.0
 * @since 3.1.0
 */
public interface AuthenticationService {

    /**
     * 用户登录
     *
     * @param request 登录请求
     * @return 登录响应
     * @throws IllegalArgumentException 用户名或密码错误
     */
    LoginResponse login(LoginRequest request);

    /**
     * 修改密码
     *
     * @param username 用户名
     * @param request  修改密码请求
     * @throws IllegalArgumentException 旧密码错误或新密码不一致
     */
    void changePassword(String username, ChangePasswordRequest request);

    /**
     * 初始化默认用户（如果不存在）
     *
     * @param defaultUsername 默认用户名
     * @param defaultPassword 默认密码（明文，会被 MD5 加密）
     */
    void initializeDefaultUser(String defaultUsername, String defaultPassword);

    /**
     * 验证用户是否存在
     *
     * @param username 用户名
     * @return 是否存在
     */
    boolean userExists(String username);
}
