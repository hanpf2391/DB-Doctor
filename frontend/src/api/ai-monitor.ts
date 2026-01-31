import request from './index'
import type { AiMonitorStats, AiInvocationDetail, QueryParams, AnalysisTraceSummary, AnalysisTraceDetail, CostStats } from '@/views/AiMonitor/types'

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
    url: '/ai-monitor/stats',
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
    url: `/ai-monitor/by-trace/${traceId}`,
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
    url: '/ai-monitor/query',
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
    url: '/ai-monitor/error-stats',
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
    url: '/ai-monitor/default-time-range',
    method: 'get'
  })
}

// ===== 🆕 单次分析详情相关 API（v2.3.1） =====

/**
 * 获取分析记录列表（分页）- 🆕
 *
 * @param params 查询参数
 * @returns 分页结果
 */
export function getAnalysisTraces(params: {
  startTime?: string
  endTime?: string
  page?: number
  size?: number
}): Promise<{
  content: AnalysisTraceSummary[]
  totalElements: number
  totalPages: number
}> {
  return request({
    url: '/ai-monitor/analysis-traces',
    method: 'get',
    params
  })
}

/**
 * 获取单次分析详情 - 🆕
 *
 * @param traceId SQL 指纹
 * @returns 分析详情
 */
export function getAnalysisTraceDetail(traceId: string): Promise<AnalysisTraceDetail> {
  return request({
    url: `/ai-monitor/analysis-trace/${traceId}`,
    method: 'get'
  })
}

// ===== 🆕 成本分析相关 API（v2.3.2） =====

/**
 * 获取成本统计 - 🆕
 *
 * @param params 查询参数
 * @returns 成本统计
 */
export function getCostStats(params?: {
  startTime?: string
  endTime?: string
}): Promise<CostStats> {
  return request({
    url: '/ai-monitor/cost-stats',
    method: 'get',
    params
  })
}
