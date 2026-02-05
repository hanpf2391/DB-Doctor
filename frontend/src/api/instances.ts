/**
 * 实例管理 API 接口
 */
import axios from 'axios'
import type {
  DatabaseInstance,
  DatabaseInstanceForm,
  DatabaseTestResult,
  AiServiceInstance,
  AiServiceForm,
  AiServiceTestResult
} from '@/types/instances'

const API_BASE = '/api'

// ==================== 数据库实例 API ====================

/**
 * 获取数据库实例列表
 */
export async function fetchDatabaseInstances(): Promise<DatabaseInstance[]> {
  const response = await axios.get(`${API_BASE}/instances/database`)
  return response.data.data
}

/**
 * 测试数据库连接（环境检查）
 */
export async function testDatabaseConnection(
  config: Pick<DatabaseInstanceForm, 'host' | 'port' | 'username' | 'password' | 'database'>
): Promise<DatabaseTestResult> {
  const response = await axios.post(`${API_BASE}/environment/test-connection`, config)
  return response.data.data
}

/**
 * 保存数据库实例
 */
export async function saveDatabaseInstance(
  form: DatabaseInstanceForm
): Promise<DatabaseInstance> {
  if (form.id) {
    // 更新
    const response = await axios.put(`${API_BASE}/instances/database/${form.id}`, form)
    return response.data.data
  } else {
    // 新增
    const response = await axios.post(`${API_BASE}/instances/database`, form)
    return response.data.data
  }
}

/**
 * 删除数据库实例
 */
export async function deleteDatabaseInstance(id: string): Promise<void> {
  await axios.delete(`${API_BASE}/instances/database/${id}`)
}

/**
 * 切换数据库实例启用状态
 */
export async function toggleDatabaseInstance(
  id: string,
  enabled: boolean
): Promise<void> {
  await axios.patch(`${API_BASE}/instances/database/${id}/toggle`, { enabled })
}

// ==================== AI 服务实例 API ====================

/**
 * 获取 AI 服务配置
 */
export async function fetchAiServiceInstances(): Promise<Record<string, AiServiceInstance>> {
  const response = await axios.get(`${API_BASE}/instances/ai-service`)
  return response.data.data
}

/**
 * 测试 AI 服务连接
 */
export async function testAiService(
  config: Pick<AiServiceForm, 'provider' | 'baseUrl' | 'apiKey' | 'model' | 'temperature' | 'maxTokens'>
): Promise<AiServiceTestResult> {
  const response = await axios.post(`${API_BASE}/instances/ai-service/test`, config)
  return response.data.data
}

/**
 * 保存 AI 服务配置
 */
export async function saveAiServiceInstance(
  form: AiServiceForm
): Promise<AiServiceInstance> {
  if (form.id) {
    // 更新
    const response = await axios.put(`${API_BASE}/instances/ai-service/${form.id}`, form)
    return response.data.data
  } else {
    // 新增
    const response = await axios.post(`${API_BASE}/instances/ai-service`, form)
    return response.data.data
  }
}

/**
 * 删除 AI 服务配置
 */
export async function deleteAiServiceInstance(id: string): Promise<void> {
  await axios.delete(`${API_BASE}/instances/ai-service/${id}`)
}

/**
 * 切换 AI 服务启用状态
 */
export async function toggleAiServiceInstance(
  id: string,
  enabled: boolean
): Promise<void> {
  await axios.patch(`${API_BASE}/instances/ai-service/${id}/toggle`, { enabled })
}
