/**
 * 告警设置 API
 *
 * @author DB-Doctor
 * @version 3.2.0
 */

import { request } from './index'

export const alertSettingsApi = {
  /**
   * 查询告警设置
   */
  getSettings: () => {
    return request({
      url: '/alert-settings',
      method: 'get'
    })
  },

  /**
   * 更新告警设置
   */
  updateSettings: (data: {
    severityThreshold: number
    coolDownHours: number
    degradationMultiplier: number
  }) => {
    return request({
      url: '/alert-settings',
      method: 'post',
      data
    })
  },

  /**
   * 重置为默认值
   */
  resetSettings: () => {
    return request({
      url: '/alert-settings/reset',
      method: 'post'
    })
  }
}

export default alertSettingsApi
