/**
 * 定时批量通知配置 API
 *
 * @author DB-Doctor
 * @version 3.2.0
 */

import { request } from './index'

export const notificationScheduleApi = {
  /**
   * 查询配置
   */
  getConfig: () => {
    return request({
      url: '/notification-schedule/config',
      method: 'get'
    })
  },

  /**
   * 更新配置
   */
  updateConfig: (data: {
    batchCron: string
    enabledChannels: string[]
  }) => {
    return request({
      url: '/notification-schedule/config',
      method: 'post',
      data
    })
  },

  /**
   * 手动触发
   */
  triggerNow: (data?: {
    reason?: string
  }) => {
    return request({
      url: '/notification-schedule/trigger',
      method: 'post',
      data: data || {}
    })
  },

  /**
   * 查询执行日志
   */
  getLogs: (params: {
    page?: number
    size?: number
  }) => {
    return request({
      url: '/notification-schedule/logs',
      method: 'get',
      params
    })
  }
}

export default notificationScheduleApi
