/**
 * 配置类型定义
 */

export interface ConfigData {
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
