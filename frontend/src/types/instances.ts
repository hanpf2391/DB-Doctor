/**
 * 实例管理相关类型定义
 */

// ==================== 数据库实例 ====================

export type EnvironmentType = 'production' | 'testing' | 'development'

export interface DatabaseInstance {
  // 基本信息
  id: string
  name: string
  host: string
  port: number
  username: string
  password: string
  database?: string

  // 配置
  environment: EnvironmentType
  isEnabled: boolean
  isDefault: boolean

  // 验证信息
  isValid: boolean
  lastValidatedAt?: Date

  // UI 状态（不存储到后端）
  _toggling?: boolean
  _validating?: boolean
}

export interface DatabaseInstanceForm {
  id?: string
  name: string
  host: string
  port: number
  username: string
  password: string
  database?: string
  environment: EnvironmentType
  isDefault?: boolean
}

export interface DatabaseTestResult {
  success: boolean
  latency: number
  version?: string
  database?: string
  databaseCount?: number
  environmentChecks?: EnvironmentCheck[]
  error?: string
}

export interface EnvironmentCheck {
  name: string
  displayName: string
  status: 'pass' | 'fail' | 'warning'
  currentValue: string
  description: string
  fixSql?: string
  fixDescription?: string
}

// ==================== AI 服务实例 ====================

export type AgentType = 'diagnosis' | 'reasoning' | 'coding'
export type ProviderType = 'openai' | 'ollama' | 'deepseek' | 'anthropic' | 'azure'

export interface AiServiceInstance {
  // 基本信息
  id: string
  agentType: AgentType
  provider: ProviderType

  // 配置
  baseUrl: string
  apiKey: string
  model: string
  temperature?: number
  maxTokens?: number

  // 状态
  isEnabled: boolean

  // 验证信息
  isValid: boolean
  lastValidatedAt?: Date

  // 元数据
  createdAt: Date
  updatedAt: Date
}

export interface AiServiceForm {
  id?: string
  agentType: AgentType
  provider: ProviderType
  baseUrl: string
  apiKey: string
  model: string
  temperature?: number
  maxTokens?: number
}

export interface AiServiceTestResult {
  success: boolean
  latency: number
  response?: string
  error?: string
}

// ==================== 环境类型辅助函数 ====================

export const ENVIRONMENT_CONFIG = {
  production: {
    label: '生产环境',
    color: 'danger',
    icon: 'CircleFilled'
  },
  testing: {
    label: '测试环境',
    color: 'warning',
    icon: 'WarningFilled'
  },
  development: {
    label: '开发环境',
    color: 'success',
    icon: 'CircleCheckFilled'
  }
} as const

export function getEnvironmentLabel(env: EnvironmentType): string {
  return ENVIRONMENT_CONFIG[env].label
}

export function getEnvironmentColor(env: EnvironmentType): string {
  return ENVIRONMENT_CONFIG[env].color
}

// ==================== Agent 类型辅助函数 ====================

export const AGENT_CONFIG = {
  diagnosis: {
    name: '主治医生',
    description: '负责慢查询诊断分析',
    icon: '🩺',
    color: '#667eea'
  },
  reasoning: {
    name: '推理专家',
    description: '负责复杂推理和规划',
    icon: '🧠',
    color: '#764ba2'
  },
  coding: {
    name: '编码专家',
    description: '负责 SQL 优化建议',
    icon: '💻',
    color: '#10b981'
  }
} as const

export function getAgentName(type: AgentType): string {
  return AGENT_CONFIG[type].name
}

export function getAgentDescription(type: AgentType): string {
  return AGENT_CONFIG[type].description
}

// ==================== 提供商类型辅助函数 ====================

export const PROVIDER_CONFIG = {
  openai: {
    name: 'OpenAI',
    defaultBaseUrl: 'https://api.openai.com/v1',
    defaultModel: 'gpt-4-turbo',
    color: 'success'
  },
  ollama: {
    name: 'Ollama',
    defaultBaseUrl: 'http://localhost:11434',
    defaultModel: 'llama2',
    color: 'primary'
  },
  deepseek: {
    name: 'DeepSeek',
    defaultBaseUrl: 'https://api.deepseek.com',
    defaultModel: 'deepseek-chat',
    color: 'warning'
  },
  anthropic: {
    name: 'Anthropic',
    defaultBaseUrl: 'https://api.anthropic.com',
    defaultModel: 'claude-3-opus-20240229',
    color: 'info'
  },
  azure: {
    name: 'Azure OpenAI',
    defaultBaseUrl: 'https://your-resource.openai.azure.com',
    defaultModel: 'gpt-4',
    color: 'info'
  }
} as const

export function getProviderName(provider: ProviderType): string {
  return PROVIDER_CONFIG[provider].name
}

export function getProviderDefaults(provider: ProviderType) {
  return {
    baseUrl: PROVIDER_CONFIG[provider].defaultBaseUrl,
    model: PROVIDER_CONFIG[provider].defaultModel
  }
}
