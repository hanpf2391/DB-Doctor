/**
 * 报告详情页类型定义
 */

export interface ReportDetailProps {
  modelValue: boolean
  reportId: number
}

export interface ReportDetailData {
  id: number
  fingerprint: string
  dbName: string
  tableName: string
  sqlTemplate: string
  avgQueryTime: number
  maxQueryTime: number
  lockTime: number
  rowsExamined: number
  rowsSent: number
  occurrenceCount: number
  severityLevel: string
  analysisStatus: 'PENDING' | 'DIAGNOSING' | 'SUCCESS' | 'FAILED'
  lastSeenTime: string
  aiAnalysisReport: string
}

export interface VitalSign {
  key: string
  label: string
  value: string | number
  level: 'danger' | 'warning' | 'info' | 'success'
}

export interface TimelineEvent {
  timestamp: string
  agent: 'diagnosis' | 'reasoning' | 'coding'
  agentName: string
  action: string
  detail: string
  status: 'success' | 'running' | 'pending' | 'failed'
}
