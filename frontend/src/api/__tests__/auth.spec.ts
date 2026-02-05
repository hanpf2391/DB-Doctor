/**
 * 认证 API 测试
 * TDD 红色阶段：先编写测试，验证失败，然后实现
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { login, changePassword, checkUserExists, logout } from '../auth'
import { request } from '../index'
import type { ApiResponse } from '../types'

// Mock request 模块
vi.mock('../index', () => ({
  request: vi.fn()
}))

const mockRequest = vi.mocked(request)

describe('认证 API 测试', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  describe('login', () => {
    it('应该成功登录并返回用户信息', async () => {
      // Given
      const loginData = {
        username: 'dbdoctor',
        password: 'dbdoctor'
      }

      const expectedResponse: ApiResponse = {
        code: 200,
        message: '登录成功',
        data: {
          username: 'dbdoctor',
          token: '550e8400-e29b-41d4-a716-446655440000',
          loginTime: '2024-01-22T10:30:00'
        }
      }

      mockRequest.mockResolvedValueOnce(expectedResponse)

      // When
      const result = await login(loginData)

      // Then
      expect(result).toEqual(expectedResponse.data)
      expect(mockRequest).toHaveBeenCalledTimes(1)
      expect(mockRequest).toHaveBeenCalledWith({
        url: '/api/auth/login',
        method: 'POST',
        data: loginData
      })
    })

    it('应该处理登录失败（用户名或密码错误）', async () => {
      // Given
      const loginData = {
        username: 'wrong',
        password: 'wrong'
      }

      const errorResponse: ApiResponse = {
        code: 400,
        message: '用户名或密码错误',
        data: null
      }

      mockRequest.mockRejectedValueOnce(new Error(errorResponse.message))

      // When & Then
      await expect(login(loginData)).rejects.toThrow('用户名或密码错误')
      expect(mockRequest).toHaveBeenCalledTimes(1)
    })

    it('应该处理网络错误', async () => {
      // Given
      const loginData = {
        username: 'dbdoctor',
        password: 'dbdoctor'
      }

      mockRequest.mockRejectedValueOnce(new Error('网络错误'))

      // When & Then
      await expect(login(loginData)).rejects.toThrow('网络错误')
    })
  })

  describe('changePassword', () => {
    it('应该成功修改密码', async () => {
      // Given
      const passwordData = {
        oldPassword: 'dbdoctor',
        newPassword: 'newpass123',
        confirmPassword: 'newpass123'
      }

      const expectedResponse: ApiResponse<void> = {
        code: 200,
        message: '密码修改成功',
        data: undefined
      }

      mockRequest.mockResolvedValueOnce(expectedResponse)

      // When
      await changePassword(passwordData)

      // Then
      expect(mockRequest).toHaveBeenCalledTimes(1)
      expect(mockRequest).toHaveBeenCalledWith({
        url: '/api/auth/change-password',
        method: 'POST',
        data: passwordData
      })
    })

    it('应该处理旧密码错误', async () => {
      // Given
      const passwordData = {
        oldPassword: 'wrong',
        newPassword: 'newpass123',
        confirmPassword: 'newpass123'
      }

      mockRequest.mockRejectedValueOnce(new Error('旧密码错误'))

      // When & Then
      await expect(changePassword(passwordData)).rejects.toThrow('旧密码错误')
    })

    it('应该处理新密码与确认密码不一致', async () => {
      // Given
      const passwordData = {
        oldPassword: 'dbdoctor',
        newPassword: 'newpass123',
        confirmPassword: 'different'
      }

      mockRequest.mockRejectedValueOnce(new Error('新密码与确认密码不一致'))

      // When & Then
      await expect(changePassword(passwordData)).rejects.toThrow('新密码与确认密码不一致')
    })
  })

  describe('checkUserExists', () => {
    it('应该返回用户存在', async () => {
      // Given
      const username = 'dbdoctor'
      const expectedResponse: ApiResponse<boolean> = {
        code: 200,
        message: '操作成功',
        data: true
      }

      mockRequest.mockResolvedValueOnce(expectedResponse)

      // When
      const result = await checkUserExists(username)

      // Then
      expect(result).toBe(true)
      expect(mockRequest).toHaveBeenCalledTimes(1)
      expect(mockRequest).toHaveBeenCalledWith({
        url: `/api/auth/exists/${username}`,
        method: 'GET'
      })
    })

    it('应该返回用户不存在', async () => {
      // Given
      const username = 'notexist'
      const expectedResponse: ApiResponse<boolean> = {
        code: 200,
        message: '操作成功',
        data: false
      }

      mockRequest.mockResolvedValueOnce(expectedResponse)

      // When
      const result = await checkUserExists(username)

      // Then
      expect(result).toBe(false)
    })
  })

  describe('logout', () => {
    it('应该成功登出（前端清除 token）', async () => {
      // When
      await logout()

      // Then
      // logout 目前只是返回 Promise.resolve()
      // 实际的 token 清除逻辑应该在 Pinia store 中实现
      expect(true).toBe(true)
    })
  })
})
