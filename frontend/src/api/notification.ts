// 通知管理 API
import { request } from './index'

/**
 * 通知调度日志 API
 */
export const notificationScheduleLogApi = {
  /**
   * 查询通知调度日志
   */
  getLogs: (params: {
    page?: number
    size?: number
    status?: string
    startDate?: string
    endDate?: string
  }) => {
    return request({
      url: '/notification-schedule/logs',
      method: 'get',
      params
    })
  },

  /**
   * 查询日志详情
   */
  getLogDetail: (id: number) => {
    return request({
      url: `/notification-schedule/logs/${id}`,
      method: 'get'
    })
  }
}

/**
 * 通知历史 API（真正的邮件通知历史）
 */
export const notificationHistoryApi = {
  /**
   * 查询通知历史列表
   */
  getHistory: (params: {
    page?: number
    size?: number
    startDate?: string
    endDate?: string
    status?: 'SENT' | 'FAILED'
  }) => {
    return request({
      url: '/notifications/history',
      method: 'get',
      params
    })
  },

  /**
   * 查询批次详情
   */
  getBatchDetail: (batchId: string) => {
    return request({
      url: `/notifications/history/batch/${batchId}`,
      method: 'get'
    })
  },

  /**
   * 查询时间线（按SQL指纹）
   */
  getTimeline: (params: {
    fingerprint: string
    page?: number
    size?: number
  }) => {
    return request({
      url: '/notifications/timeline',
      method: 'get',
      params
    })
  },

  /**
   * 统计信息
   */
  getStatistics: (params: {
    startDate: string
    endDate: string
  }) => {
    return request({
      url: '/notifications/statistics',
      method: 'get',
      params
    })
  },

  /**
   * 查询批次中的所有指纹
   */
  getBatchFingerprints: (batchId: string) => {
    return request({
      url: `/notifications/history/batch/${batchId}/fingerprints`,
      method: 'get'
    })
  }
}

export default {
  notificationScheduleLogApi,
  notificationHistoryApi
}
