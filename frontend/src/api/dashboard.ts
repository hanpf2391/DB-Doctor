import request from './index'
import type { DashboardStats, TrendData, TopSlowData } from '@/views/Dashboard/types'

/**
 * 获取今日概览统计
 */
export function getTodayOverview(): Promise<DashboardStats> {
  return request({
    url: '/system/overview',
    method: 'get'
  })
}

/**
 * 获取慢查询趋势数据
 */
export function getTrend(date: string): Promise<TrendData> {
  return request({
    url: '/reports/trend',
    method: 'get',
    params: { date }
  })
}

/**
 * 获取 Top N 慢查询
 */
export function getTopSlow(limit: number = 5): Promise<TopSlowData> {
  return request({
    url: '/reports/top',
    method: 'get',
    params: { limit }
  })
}

/**
 * 获取队列状态
 */
export function getQueueStatus() {
  return request({
    url: '/system/queue-status',
    method: 'get'
  })
}
