import request from './index'
import type { ConfigData, SaveConfigResponse } from './types'

/**
 * 配置相关 API
 */

/**
 * 获取所有配置
 */
export function getAllConfigs(): Promise<ConfigData> {
  return request({
    url: '/config/all',
    method: 'get'
  })
}

/**
 * 根据分类获取配置
 */
export function getConfigsByCategory(category: string): Promise<Record<string, string>> {
  return request({
    url: `/config/category/${category}`,
    method: 'get'
  })
}

/**
 * 保存配置并触发热重载
 */
export function saveConfig(
  category: string,
  configs: Record<string, string>
): Promise<SaveConfigResponse> {
  return request({
    url: '/config/save',
    method: 'post',
    data: {
      category,
      configs
    }
  })
}

/**
 * 测试数据库连接
 */
export function testDbConnection(params: {
  host: string
  port: number
  username: string
  password: string
  database?: string
}) {
  return request({
    url: '/config/test/db',
    method: 'post',
    data: params
  })
}

/**
 * 测试 AI 连通性
 */
export function testAiConnection(params: {
  agentType: string
  provider: string
  baseUrl: string
  modelName: string
  apiKey?: string
}) {
  return request({
    url: '/config/test/ai',
    method: 'post',
    data: params
  })
}

/**
 * 报表相关 API
 */

/**
 * 获取报表列表
 */
export function getReports(params: {
  page?: number
  size?: number
  dbName?: string
  severity?: string
}) {
  return request({
    url: '/reports',
    method: 'get',
    params
  })
}

/**
 * 获取报告详情
 */
export function getReportDetail(id: number) {
  return request({
    url: `/reports/${id}`,
    method: 'get'
  })
}

/**
 * 重新分析报告
 */
export function reanalyzeReport(id: number) {
  return request({
    url: `/reports/${id}/reanalyze`,
    method: 'post'
  })
}

/**
 * 系统维护相关 API
 */

/**
 * 清理历史数据
 */
export function cleanupHistory(days: number) {
  return request({
    url: '/maintenance/cleanup',
    method: 'delete',
    params: { days }
  })
}

/**
 * 重置系统
 */
export function resetSystem() {
  return request({
    url: '/maintenance/reset',
    method: 'post'
  })
}

/**
 * 获取系统信息
 */
export function getSystemInfo() {
  return request({
    url: '/system/info',
    method: 'get'
  })
}

/**
 * 通知相关 API
 */

/**
 * 发送测试邮件
 */
export function sendTestEmail(params: {
  to: string[]
  subject?: string
}) {
  return request({
    url: '/notify/test',
    method: 'post',
    data: params
  })
}
