import request from './index'

/**
 * 数据库实例管理 API (v3.1.0)
 */

export interface DatabaseInstance {
  id?: number
  instanceName: string
  instanceType: string
  url: string
  username: string
  password?: string
  description?: string
  environment?: string
  tags?: string
  isValid?: boolean
  lastValidatedAt?: string
  validationError?: string
  isEnabled?: boolean
  isDefault?: boolean
  createdAt?: string
  updatedAt?: string
}

export interface EnvCheckReport {
  overallPassed: boolean
  connectionSuccess: boolean
  connectionError?: string
  summary?: string
  items?: Array<{
    name: string
    passed: boolean
    currentValue?: string
    errorMessage?: string
    fixCommand?: string
  }>
  availableDatabases?: string[]
}

/**
 * 获取所有数据库实例
 */
export function getAllDatabaseInstances(): Promise<DatabaseInstance[]> {
  return request({
    url: '/database-instances',
    method: 'get'
  })
}

/**
 * 根据ID查询数据库实例
 */
export function getDatabaseInstanceById(id: number): Promise<DatabaseInstance> {
  return request({
    url: `/database-instances/${id}`,
    method: 'get'
  })
}

/**
 * 创建数据库实例
 */
export function createDatabaseInstance(data: DatabaseInstance): Promise<DatabaseInstance> {
  return request({
    url: '/database-instances',
    method: 'post',
    data
  })
}

/**
 * 更新数据库实例
 */
export function updateDatabaseInstance(id: number, data: DatabaseInstance): Promise<DatabaseInstance> {
  return request({
    url: `/database-instances/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除数据库实例
 */
export function deleteDatabaseInstance(id: number): Promise<void> {
  return request({
    url: `/database-instances/${id}`,
    method: 'delete'
  })
}

/**
 * 验证数据库连接
 */
export function validateDatabaseConnection(id: number): Promise<EnvCheckReport> {
  return request({
    url: `/database-instances/${id}/validate`,
    method: 'post'
  })
}

/**
 * 设置默认数据库实例
 */
export function setDefaultDatabaseInstance(id: number): Promise<void> {
  return request({
    url: `/database-instances/${id}/set-default`,
    method: 'post'
  })
}

/**
 * 启用/禁用数据库实例
 */
export function setDatabaseInstanceEnabled(id: number, enabled: boolean): Promise<void> {
  return request({
    url: `/database-instances/${id}/enabled`,
    method: 'put',
    params: { enabled }
  })
}
