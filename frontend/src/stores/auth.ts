/**
 * 认证状态管理
 * 管理用户登录状态、token 等信息
 */

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { LoginRequest, LoginResponse, ChangePasswordRequest } from '@/api/types'
import * as authApi from '@/api/auth'

/**
 * 本地存储键名
 */
const STORAGE_KEY = {
  TOKEN: 'dbdoctor_token',
  USER: 'dbdoctor_user',
  LOGIN_TIME: 'dbdoctor_login_time'
}

export const useAuthStore = defineStore('auth', () => {
  // 状态
  const token = ref<string | null>(localStorage.getItem(STORAGE_KEY.TOKEN))
  const username = ref<string | null>(localStorage.getItem(STORAGE_KEY.USER))
  const loginTime = ref<string | null>(localStorage.getItem(STORAGE_KEY.LOGIN_TIME))
  const loading = ref(false)
  const error = ref<string | null>(null)

  // 计算属性
  const isAuthenticated = computed(() => !!token.value)
  const currentUser = computed(() =>
    username.value
      ? {
          username: username.value,
          loginTime: loginTime.value
        }
      : null
  )

  // Actions

  /**
   * 用户登录
   */
  async function login(credentials: LoginRequest) {
    loading.value = true
    error.value = null

    try {
      const response = await authApi.login(credentials)

      // 保存认证信息
      token.value = response.token
      username.value = response.username
      loginTime.value = response.loginTime

      // 持久化到 localStorage
      localStorage.setItem(STORAGE_KEY.TOKEN, response.token)
      localStorage.setItem(STORAGE_KEY.USER, response.username)
      localStorage.setItem(STORAGE_KEY.LOGIN_TIME, response.loginTime)

      return response
    } catch (err: any) {
      error.value = err.message || '登录失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  /**
   * 用户登出
   */
  async function logout() {
    try {
      await authApi.logout()
    } catch (err) {
      console.error('登出失败:', err)
    } finally {
      // 清除认证信息
      token.value = null
      username.value = null
      loginTime.value = null
      error.value = null

      // 清除 localStorage
      localStorage.removeItem(STORAGE_KEY.TOKEN)
      localStorage.removeItem(STORAGE_KEY.USER)
      localStorage.removeItem(STORAGE_KEY.LOGIN_TIME)
    }
  }

  /**
   * 修改密码
   */
  async function changePassword(passwordData: ChangePasswordRequest) {
    loading.value = true
    error.value = null

    try {
      await authApi.changePassword(passwordData)
    } catch (err: any) {
      error.value = err.message || '修改密码失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  /**
   * 从 localStorage 恢复登录状态（应用启动时调用）
   */
  function restoreSession() {
    const savedToken = localStorage.getItem(STORAGE_KEY.TOKEN)
    const savedUser = localStorage.getItem(STORAGE_KEY.USER)
    const savedTime = localStorage.getItem(STORAGE_KEY.LOGIN_TIME)

    if (savedToken && savedUser) {
      token.value = savedToken
      username.value = savedUser
      loginTime.value = savedTime
    }
  }

  /**
   * 清除错误信息
   */
  function clearError() {
    error.value = null
  }

  return {
    // 状态
    token,
    username,
    loginTime,
    loading,
    error,

    // 计算属性
    isAuthenticated,
    currentUser,

    // Actions
    login,
    logout,
    changePassword,
    restoreSession,
    clearError
  }
})
