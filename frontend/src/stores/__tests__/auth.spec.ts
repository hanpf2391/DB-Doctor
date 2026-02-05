/**
 * 认证 Store 测试
 * TDD 测试：验证认证状态管理功能
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from '../auth'
import * as authApi from '@/api/auth'
import type { LoginResponse } from '@/api/types'

// Mock auth API
vi.mock('@/api/auth', () => ({
  login: vi.fn(),
  logout: vi.fn(),
  changePassword: vi.fn()
}))

const mockLoginApi = vi.mocked(authApi.login)
const mockLogoutApi = vi.mocked(authApi.logout)
const mockChangePasswordApi = vi.mocked(authApi.changePassword)

describe('认证 Store 测试', () => {
  beforeEach(() => {
    // 创建新的 Pinia 实例
    setActivePinia(createPinia())
    vi.clearAllMocks()

    // 清除 localStorage
    localStorage.clear()
  })

  afterEach(() => {
    vi.restoreAllMocks()
    localStorage.clear()
  })

  describe('初始状态', () => {
    it('未登录时应该返回正确的状态', () => {
      // Given
      const store = useAuthStore()

      // Then
      expect(store.isAuthenticated).toBe(false)
      expect(store.token).toBeNull()
      expect(store.username).toBeNull()
      expect(store.currentUser).toBeNull()
    })

    it('应该从 localStorage 恢复登录状态', () => {
      // Given - 设置 localStorage
      const mockToken = 'test-token-123'
      const mockUser = 'dbdoctor'
      const mockTime = '2024-01-22T10:30:00'

      localStorage.setItem('dbdoctor_token', mockToken)
      localStorage.setItem('dbdoctor_user', mockUser)
      localStorage.setItem('dbdoctor_login_time', mockTime)

      // When - 创建 store
      const store = useAuthStore()
      store.restoreSession()

      // Then
      expect(store.token).toBe(mockToken)
      expect(store.username).toBe(mockUser)
      expect(store.loginTime).toBe(mockTime)
      expect(store.isAuthenticated).toBe(true)
      expect(store.currentUser).toEqual({
        username: mockUser,
        loginTime: mockTime
      })
    })
  })

  describe('login', () => {
    it('应该成功登录并保存认证信息', async () => {
      // Given
      const store = useAuthStore()
      const credentials = {
        username: 'dbdoctor',
        password: 'dbdoctor'
      }

      const mockResponse: LoginResponse = {
        username: 'dbdoctor',
        token: 'test-token-123',
        loginTime: '2024-01-22T10:30:00'
      }

      mockLoginApi.mockResolvedValueOnce(mockResponse)

      // When
      const result = await store.login(credentials)

      // Then
      expect(result).toEqual(mockResponse)
      expect(store.token).toBe(mockResponse.token)
      expect(store.username).toBe(mockResponse.username)
      expect(store.loginTime).toBe(mockResponse.loginTime)
      expect(store.isAuthenticated).toBe(true)
      expect(store.error).toBeNull()

      // 验证 localStorage 已保存
      expect(localStorage.getItem('dbdoctor_token')).toBe(mockResponse.token)
      expect(localStorage.getItem('dbdoctor_user')).toBe(mockResponse.username)
      expect(localStorage.getItem('dbdoctor_login_time')).toBe(mockResponse.loginTime)
    })

    it('应该处理登录失败', async () => {
      // Given
      const store = useAuthStore()
      const credentials = {
        username: 'wrong',
        password: 'wrong'
      }

      const errorMessage = '用户名或密码错误'
      mockLoginApi.mockRejectedValueOnce(new Error(errorMessage))

      // When & Then
      await expect(store.login(credentials)).rejects.toThrow(errorMessage)
      expect(store.error).toBe(errorMessage)
      expect(store.isAuthenticated).toBe(false)
      expect(store.token).toBeNull()
    })

    it('登录时 loading 状态应该正确', async () => {
      // Given
      const store = useAuthStore()
      const credentials = {
        username: 'dbdoctor',
        password: 'dbdoctor'
      }

      const mockResponse: LoginResponse = {
        username: 'dbdoctor',
        token: 'test-token',
        loginTime: '2024-01-22T10:30:00'
      }

      // 让 login API 延迟返回
      mockLoginApi.mockImplementationOnce(
        () =>
          new Promise((resolve) => {
            setTimeout(() => resolve(mockResponse), 100)
          })
      )

      // When
      const loginPromise = store.login(credentials)

      // Then - loading 应该为 true
      expect(store.loading).toBe(true)

      // 等待完成
      await loginPromise

      // Then - loading 应该为 false
      expect(store.loading).toBe(false)
    })
  })

  describe('logout', () => {
    it('应该成功登出并清除认证信息', async () => {
      // Given - 已登录状态
      const store = useAuthStore()
      localStorage.setItem('dbdoctor_token', 'test-token')
      localStorage.setItem('dbdoctor_user', 'dbdoctor')
      localStorage.setItem('dbdoctor_login_time', '2024-01-22T10:30:00')

      store.token = 'test-token'
      store.username = 'dbdoctor'
      store.loginTime = '2024-01-22T10:30:00'

      mockLogoutApi.mockResolvedValueOnce(undefined)

      // When
      await store.logout()

      // Then
      expect(store.token).toBeNull()
      expect(store.username).toBeNull()
      expect(store.loginTime).toBeNull()
      expect(store.isAuthenticated).toBe(false)
      expect(store.currentUser).toBeNull()

      // 验证 localStorage 已清除
      expect(localStorage.getItem('dbdoctor_token')).toBeNull()
      expect(localStorage.getItem('dbdoctor_user')).toBeNull()
      expect(localStorage.getItem('dbdoctor_login_time')).toBeNull()

      expect(mockLogoutApi).toHaveBeenCalledTimes(1)
    })

    it('即使后端登出失败也应该清除本地认证信息', async () => {
      // Given
      const store = useAuthStore()
      store.token = 'test-token'
      store.username = 'dbdoctor'

      mockLogoutApi.mockRejectedValueOnce(new Error('网络错误'))

      // When
      await store.logout()

      // Then - 本地信息仍然应该被清除
      expect(store.token).toBeNull()
      expect(store.username).toBeNull()
      expect(store.isAuthenticated).toBe(false)
    })
  })

  describe('changePassword', () => {
    it('应该成功修改密码', async () => {
      // Given
      const store = useAuthStore()
      const passwordData = {
        oldPassword: 'dbdoctor',
        newPassword: 'newpass123',
        confirmPassword: 'newpass123'
      }

      mockChangePasswordApi.mockResolvedValueOnce(undefined)

      // When
      await store.changePassword(passwordData)

      // Then
      expect(mockChangePasswordApi).toHaveBeenCalledTimes(1)
      expect(mockChangePasswordApi).toHaveBeenCalledWith(passwordData)
      expect(store.error).toBeNull()
    })

    it('应该处理修改密码失败', async () => {
      // Given
      const store = useAuthStore()
      const passwordData = {
        oldPassword: 'wrong',
        newPassword: 'newpass123',
        confirmPassword: 'newpass123'
      }

      const errorMessage = '旧密码错误'
      mockChangePasswordApi.mockRejectedValueOnce(new Error(errorMessage))

      // When & Then
      await expect(store.changePassword(passwordData)).rejects.toThrow(errorMessage)
      expect(store.error).toBe(errorMessage)
    })
  })

  describe('clearError', () => {
    it('应该清除错误信息', () => {
      // Given
      const store = useAuthStore()
      store.error = '测试错误'

      // When
      store.clearError()

      // Then
      expect(store.error).toBeNull()
    })
  })

  describe('isAuthenticated 计算属性', () => {
    it('有 token 时返回 true', () => {
      // Given
      const store = useAuthStore()
      store.token = 'test-token'

      // Then
      expect(store.isAuthenticated).toBe(true)
    })

    it('无 token 时返回 false', () => {
      // Given
      const store = useAuthStore()
      store.token = null

      // Then
      expect(store.isAuthenticated).toBe(false)
    })
  })

  describe('currentUser 计算属性', () => {
    it('有用户信息时返回用户对象', () => {
      // Given
      const store = useAuthStore()
      store.username = 'dbdoctor'
      store.loginTime = '2024-01-22T10:30:00'

      // Then
      expect(store.currentUser).toEqual({
        username: 'dbdoctor',
        loginTime: '2024-01-22T10:30:00'
      })
    })

    it('无用户信息时返回 null', () => {
      // Given
      const store = useAuthStore()
      store.username = null

      // Then
      expect(store.currentUser).toBeNull()
    })
  })
})
