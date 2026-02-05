/**
 * 配置类型定义
 */

export interface ConfigData {
  configs?: Record<string, string>
  targetDb?: DbConfig
  ai?: AiConfig
  monitor?: MonitorConfig
  notify?: NotifyConfig
}

export interface DbConfig {
  type: string
  host: string
  port: number
  username: string
  password: string
}

export interface AiConfig {
  enabled: boolean
  diagnosis: AgentConfig
  reasoning: AgentConfig
  coding: AgentConfig
}

export interface AgentConfig {
  provider: string
  baseUrl: string
  modelName: string
  temperature: number
  apiKey?: string
}

export interface MonitorConfig {
  slowLogThreshold: number
  batchSize: number
  adaptivePolling: boolean
}

export interface NotifyConfig {
  mode: string
  severityThreshold: number
  emails: string
  smtp?: {
    host: string
    port: number
    username: string
    password: string
  }
}

export interface SaveConfigResponse {
  requiresRestart: boolean
  refreshedBeans: string[]
}

/**
 * 认证类型定义
 */

// 登录请求
export interface LoginRequest {
  username: string
  password: string
}

// 登录响应
export interface LoginResponse {
  username: string
  token: string
  loginTime: string
}

// 修改密码请求
export interface ChangePasswordRequest {
  oldPassword: string
  newPassword: string
  confirmPassword: string
  newUsername?: string  // 可选，不修改用户名时不传
}

// API 统一响应格式
export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
}

