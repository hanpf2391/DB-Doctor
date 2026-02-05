/**
 * 认证 API
 * 提供登录、登出、修改密码等认证相关接口
 */

import type { ApiResponse, ChangePasswordRequest, LoginRequest, LoginResponse } from './types'
import { request } from './index'

/**
 * 用户登录
 * @param data 登录请求
 * @returns 登录响应
 */
export async function login(data: LoginRequest): Promise<LoginResponse> {
  return await request<LoginResponse>({
    url: '/auth/login',
    method: 'POST',
    data
  })
}

/**
 * 修改密码
 * @param data 修改密码请求
 * @returns 操作结果
 */
export async function changePassword(data: ChangePasswordRequest): Promise<void> {
  await request<void>({
    url: '/auth/change-password',
    method: 'POST',
    data
  })
}

/**
 * 检查用户是否存在
 * @param username 用户名
 * @returns 是否存在
 */
export async function checkUserExists(username: string): Promise<boolean> {
  return await request<boolean>({
    url: `/auth/exists/${username}`,
    method: 'GET'
  })
}

/**
 * 用户登出
 * @returns 操作结果
 */
export async function logout(): Promise<void> {
  // 前端清除 token 即可，后端暂时不需要登出接口
  // TODO: 如果后端实现了 JWT 黑名单，需要调用后端登出接口
  return Promise.resolve()
}
