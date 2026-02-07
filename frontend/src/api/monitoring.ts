/**
 * 监控与告警 API
 *
 * @author DB-Doctor
 * @version 3.2.0
 */

import { request } from './index'

/**
 * 系统健康 API
 */
export const systemHealthApi = {
  /**
   * 获取系统健康状态
   */
  getHealthStatus: () => {
    return request({
      url: '/monitoring/health',
      method: 'get'
    })
  },

  /**
   * 获取系统信息
   */
  getSystemInfo: () => {
    return request({
      url: '/monitoring/system-info',
      method: 'get'
    })
  },

  /**
   * 刷新健康检查
   */
  refreshHealth: () => {
    return request({
      url: '/monitoring/health/refresh',
      method: 'post'
    })
  },

  /**
   * 获取性能指标
   */
  getMetrics: () => {
    return request({
      url: '/monitoring/metrics',
      method: 'get'
    })
  }
}

/**
 * 告警 API
 */
export const alertApi = {
  /**
   * 查询告警列表
   */
  getAlerts: (params: {
    page?: number
    size?: number
    severity?: string
    status?: string
  }) => {
    return request({
      url: '/alerts',
      method: 'get',
      params
    })
  },

  /**
   * 查询告警详情
   */
  getAlertDetail: (id: number) => {
    return request({
      url: `/alerts/${id}`,
      method: 'get'
    })
  },

  /**
   * 标记告警已解决
   */
  resolveAlert: (id: number, data: {
    resolvedBy?: string
  }) => {
    return request({
      url: `/alerts/${id}/resolve`,
      method: 'post',
      data
    })
  },

  /**
   * 获取告警统计信息
   */
  getAlertStats: () => {
    return request({
      url: '/alerts/stats',
      method: 'get'
    })
  },

  /**
   * 获取告警趋势数据
   */
  getAlertTrend: (params: {
    hours?: number
  }) => {
    return request({
      url: '/alerts/trend',
      method: 'get',
      params
    })
  }
}

/**
 * 告警规则 API
 */
export const alertRuleApi = {
  /**
   * 查询告警规则列表
   */
  getAlertRules: (params: {
    page?: number
    size?: number
  }) => {
    return request({
      url: '/alert-rules',
      method: 'get',
      params
    })
  },

  /**
   * 查询启用的告警规则
   */
  getEnabledAlertRules: () => {
    return request({
      url: '/alert-rules/enabled',
      method: 'get'
    })
  },

  /**
   * 查询告警规则详情
   */
  getAlertRule: (id: number) => {
    return request({
      url: `/alert-rules/${id}`,
      method: 'get'
    })
  },

  /**
   * 创建告警规则
   */
  createAlertRule: (data: {
    name: string
    displayName: string
    type: string
    metricName: string
    conditionOperator: string
    thresholdValue?: number
    severity: string
    enabled?: boolean
    coolDownMinutes?: number
    description?: string
  }) => {
    return request({
      url: '/alert-rules',
      method: 'post',
      data
    })
  },

  /**
   * 更新告警规则
   */
  updateAlertRule: (id: number, data: {
    name: string
    displayName: string
    type: string
    metricName: string
    conditionOperator: string
    thresholdValue?: number
    severity: string
    enabled?: boolean
    coolDownMinutes?: number
    description?: string
  }) => {
    return request({
      url: `/alert-rules/${id}`,
      method: 'put',
      data
    })
  },

  /**
   * 删除告警规则
   */
  deleteAlertRule: (id: number) => {
    return request({
      url: `/alert-rules/${id}`,
      method: 'delete'
    })
  },

  /**
   * 启用/禁用告警规则
   */
  toggleAlertRule: (id: number, enabled: boolean) => {
    return request({
      url: `/alert-rules/${id}/toggle`,
      method: 'put',
      data: { enabled }
    })
  }
}

/**
 * 通知配置 API
 */
export const notificationConfigApi = {
  /**
   * 获取通知配置
   */
  getConfig: () => {
    return request({
      url: '/notification/config',
      method: 'get'
    })
  },

  /**
   * 更新通知配置
   */
  updateConfig: (data: {
    channels: Array<{
      channel: string
      enabled: boolean
      config: string
      severityLevels?: string
    }>
  }) => {
    return request({
      url: '/notification/config',
      method: 'post',
      data
    })
  },

  /**
   * 发送测试通知
   */
  sendTestNotification: (data: {
    channel: string
  }) => {
    return request({
      url: '/notification/test',
      method: 'post',
      data
    })
  },

  /**
   * 删除通知配置
   */
  deleteConfig: (channel: string) => {
    return request({
      url: `/notification/config/${channel}`,
      method: 'delete'
    })
  }
}

export default {
  systemHealthApi,
  alertApi,
  alertRuleApi,
  notificationConfigApi
}
