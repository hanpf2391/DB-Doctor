import request from './index'
import type { ConfigData, SaveConfigResponse } from './types'

/**
 * 系统配置相关 API (v2.4.0)
 */

/**
 * 获取所有配置
 */
export function getAllConfigs(): Promise<ConfigData> {
  return request({
    url: '/system/config',
    method: 'get'
  })
}

/**
 * 根据分组获取配置
 */
export function getConfigsByGroup(group: string): Promise<Record<string, string>> {
  return request({
    url: `/system/config/group/${group}`,
    method: 'get'
  })
}

/**
 * 获取单个配置值
 */
export function getConfigValue(configKey: string): Promise<string> {
  return request({
    url: `/system/config/value/${configKey}`,
    method: 'get'
  })
}

/**
 * 更新单个配置
 */
export function updateConfig(params: {
  configKey: string
  configValue: string
  updatedBy?: string
}): Promise<SaveConfigResponse> {
  return request({
    url: '/system/config/update',
    method: 'post',
    data: params
  })
}

/**
 * 批量更新配置
 */
export function batchUpdateConfigs(params: {
  configs: Record<string, string>
  updatedBy?: string
}): Promise<SaveConfigResponse> {
  return request({
    url: '/system/config/batch-update',
    method: 'post',
    data: params
  })
}

/**
 * 测试数据库连接
 */
export function testDatabaseConnection(params: {
  url: string
  username: string
  password: string
}) {
  return request({
    url: '/system/config/test-database',
    method: 'post',
    data: params
  })
}

/**
 * 检查配置完整性
 */
export function checkGroupCompleteness(group: string) {
  return request({
    url: `/system/config/check-completeness/${group}`,
    method: 'get'
  })
}

/**
 * 检查所有配置完整性
 */
export function checkAllCompleteness() {
  return request({
    url: '/system/config/check-completeness',
    method: 'get'
  })
}

/**
 * 获取数据库配置（脱敏）
 */
export function getDatabaseConfig() {
  return request({
    url: '/system/config/database',
    method: 'get'
  })
}

/**
 * 获取监听的数据库列表
 */
export function getMonitoredDatabases() {
  return request({
    url: '/system/config/monitored-databases',
    method: 'get'
  })
}

/**
 * 获取系统初始化状态
 */
export function getInitializationStatus() {
  return request({
    url: '/system/config/initialization-status',
    method: 'get'
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
