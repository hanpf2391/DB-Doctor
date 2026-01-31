import request from '../index'
import type { AiMonitorStats, AiInvocationDetail, QueryParams } from '@/views/AiMonitor/types'

/**
 * AI 监控 API
 *
 * @author DB-Doctor
 * @version 2.3.0
 */

/**
 * 获取监控统计数据
 *
 * @param params 查询参数（可选）
 * @returns 统计数据
 */
export function getAiMonitorStats(params?: QueryParams): Promise<AiMonitorStats> {
  return request({
    url: '/api/ai-monitor/stats',
    method: 'get',
    params
  })
}

/**
 * 根据 SQL 指纹查询所有相关的 AI 调用
 *
 * @param traceId SQL 指纹
 * @returns 调用详情列表
 */
export function getAiInvocationByTrace(traceId: string): Promise<AiInvocationDetail[]> {
  return request({
    url: `/api/ai-monitor/by-trace/${traceId}`,
    method: 'get'
  })
}

/**
 * 分页查询调用日志
 *
 * @param params 查询参数
 * @returns 调用详情列表
 */
export function queryAiInvocations(params: QueryParams): Promise<AiInvocationDetail[]> {
  return request({
    url: '/api/ai-monitor/query',
    method: 'get',
    params
  })
}

/**
 * 获取错误分类统计
 *
 * @param params 查询参数（可选）
 * @returns 错误分类统计
 */
export function getAiErrorStats(params?: QueryParams): Promise<Record<string, number>> {
  return request({
    url: '/api/ai-monitor/error-stats',
    method: 'get',
    params
  })
}

/**
 * 获取默认时间范围
 *
 * @returns 时间范围
 */
export function getDefaultTimeRange(): Promise<{ startTime: string; endTime: string }> {
  return request({
    url: '/api/ai-monitor/default-time-range',
    method: 'get'
  })
}
