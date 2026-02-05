import request from './index'

/**
 * AI服务实例管理 API (v3.1.0)
 */

export interface AiServiceInstance {
  id?: number
  instanceName: string
  provider: string
  deploymentType?: string
  baseUrl?: string
  apiKey?: string
  modelName: string
  temperature?: number
  maxTokens?: number
  timeoutSeconds?: number
  description?: string
  capabilityTags?: string
  isValid?: boolean
  lastValidatedAt?: string
  validationError?: string
  isEnabled?: boolean
  isDefault?: boolean
  createdAt?: string
  updatedAt?: string
  _toggling?: boolean
}

/**
 * 获取所有AI服务实例
 */
export function getAllAiServiceInstances(): Promise<AiServiceInstance[]> {
  return request({
    url: '/ai-service-instances',
    method: 'get'
  })
}

/**
 * 根据ID查询AI服务实例
 */
export function getAiServiceInstanceById(id: number): Promise<AiServiceInstance> {
  return request({
    url: `/ai-service-instances/${id}`,
    method: 'get'
  })
}

/**
 * 创建AI服务实例
 */
export function createAiServiceInstance(data: AiServiceInstance): Promise<AiServiceInstance> {
  return request({
    url: '/ai-service-instances',
    method: 'post',
    data
  })
}

/**
 * 更新AI服务实例
 */
export function updateAiServiceInstance(id: number, data: AiServiceInstance): Promise<AiServiceInstance> {
  return request({
    url: `/ai-service-instances/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除AI服务实例
 */
export function deleteAiServiceInstance(id: number): Promise<void> {
  return request({
    url: `/ai-service-instances/${id}`,
    method: 'delete'
  })
}

/**
 * 设置默认AI服务实例
 */
export function setDefaultAiServiceInstance(id: number): Promise<void> {
  return request({
    url: `/ai-service-instances/${id}/set-default`,
    method: 'post'
  })
}

/**
 * 启用/禁用AI服务实例
 */
export function setAiServiceInstanceEnabled(id: number, enabled: boolean): Promise<void> {
  return request({
    url: `/ai-service-instances/${id}/enabled`,
    method: 'put',
    params: { enabled }
  })
}
