/**
 * Dashboard 类型定义
 */

export interface DashboardStats {
  todayTotal: number
  highRiskCount: number
  avgQueryTime: number
  pendingTasks: number
  date: string
}

export interface TrendData {
  hours: number[]
  counts: number[]
  date: string
}

export interface TopSlowItem {
  id: number
  fingerprint: string
  dbName: string
  tableName: string
  sqlTemplate: string
  maxQueryTime: number
  avgQueryTime: number
  occurrenceCount: number
  severityLevel: string
}

export interface TopSlowData {
  records: TopSlowItem[]
  total: number
}
