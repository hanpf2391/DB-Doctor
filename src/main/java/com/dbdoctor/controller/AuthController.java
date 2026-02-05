package com.dbdoctor.controller;

import com.dbdoctor.common.Result;
import com.dbdoctor.dto.ChangePasswordRequest;
import com.dbdoctor.dto.LoginRequest;
import com.dbdoctor.dto.LoginResponse;
import com.dbdoctor.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 *
 * <p>提供用户登录、修改密码等认证相关 API</p>
 *
 * @author DB-Doctor
 * @version 3.1.0
 * @since 3.1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationService authenticationService;

    /**
     * 用户登录
     *
     * @param request 登录请求
     * @return 登录响应（包含 token）
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        try {
            LoginResponse response = authenticationService.login(request);
            return Result.success("登录成功", response);
        } catch (IllegalArgumentException e) {
            log.warn("[登录API] 登录失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("[登录API] 登录失败", e);
            return Result.error("登录失败: " + e.getMessage());
        }
    }

    /**
     * 修改密码
     *
     * @param request 修改密码请求
     * @return 操作结果
     */
    @PostMapping("/change-password")
    public Result<Void> changePassword(@RequestBody ChangePasswordRequest request) {
        try {
            // TODO: 从 session 或 token 中获取当前登录用户
            // 简化实现：暂时固定使用 dbdoctor 用户
            String username = "dbdoctor";

            authenticationService.changePassword(username, request);
            return Result.success("密码修改成功", null);
        } catch (IllegalArgumentException e) {
            log.warn("[密码修改API] 修改失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("[密码修改API] 修改失败", e);
            return Result.error("密码修改失败: " + e.getMessage());
        }
    }

    /**
     * 检查用户是否存在
     *
     * @param username 用户名
     * @return 是否存在
     */
    @GetMapping("/exists/{username}")
    public Result<Boolean> checkUserExists(@PathVariable String username) {
        boolean exists = authenticationService.userExists(username);
        return Result.success(exists);
    }
}
