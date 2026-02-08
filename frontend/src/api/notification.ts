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

export default {
  notificationScheduleLogApi
}
