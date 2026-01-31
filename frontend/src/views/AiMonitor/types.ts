/**
 * AI 监控前端类型定义
 *
 * @author DB-Doctor
 * @version 2.3.0
 */

/**
 * AI 监控统计数据
 */
export interface AiMonitorStats {
  /** 总调用次数 */
  totalCalls: number
  /** 成功调用次数 */
  successCount: number
  /** 成功率（百分比，0-100） */
  successRate: number
  /** 平均耗时（毫秒） */
  avgDuration: number
  /** 最大耗时（毫秒） */
  maxDuration: number
  /** 最小耗时（毫秒） */
  minDuration: number
  /** 总 Token 消耗 */
  totalTokens: number
  /** 输入 Token 总数 */
  inputTokens: number
  /** 输出 Token 总数 */
  outputTokens: number
  /** 各 Agent 的 Token 分布 */
  agentTokenDistribution: Record<string, number>
  /** 各 Agent 的调用次数分布 */
  agentCallDistribution: Record<string, number>
  /** 按小时的调用次数（用于趋势图） */
  hourlyCallCount: Record<number, number>
  /** 统计时间范围 */
  timeRange: string
}

/**
 * AI 调用详情
 */
export interface AiInvocationDetail {
  /** 主键 ID */
  id: number
  /** SQL 指纹 */
  traceId: string
  /** Agent 角色名称（中文） */
  agentDisplayName: string
  /** Agent 角色代码 */
  agentCode: string
  /** 模型名称 */
  modelName: string
  /** 供应商 */
  provider: string
  /** 输入 Token 数 */
  inputTokens: number
  /** 输出 Token 数 */
  outputTokens: number
  /** 总 Token 数 */
  totalTokens: number
  /** 耗时（毫秒） */
  durationMs: number
  /** 耗时描述（人类可读） */
  durationDescription: string
  /** 开始时间 */
  startTime: string
  /** 结束时间 */
  endTime: string
  /** 状态代码（SUCCESS/FAILED/TIMEOUT） */
  statusCode: string
  /** 状态显示名称（中文） */
  statusDisplayName: string
  /** 错误分类 */
  errorCategory: string
  /** 错误分类显示名称（中文） */
  errorCategoryDisplayName: string
  /** 错误信息 */
  errorMessage?: string
  /** 提示词（可选） */
  promptText?: string
  /** 响应内容（可选） */
  responseText?: string
  /** 创建时间 */
  createdTime: string
}

/**
 * 查询参数
 */
export interface QueryParams {
  /** 开始时间 */
  startTime?: string
  /** 结束时间 */
  endTime?: string
  /** Agent 名称 */
  agentName?: string
  /** 状态 */
  status?: string
}

/**
 * Agent 显示名称映射
 */
export const AGENT_NAME_MAP: Record<string, string> = {
  DIAGNOSIS: '主治医生',
  REASONING: '推理专家',
  CODING: '编码专家'
}

/**
 * 状态显示名称映射
 */
export const STATUS_NAME_MAP: Record<string, string> = {
  SUCCESS: '成功',
  FAILED: '失败',
  TIMEOUT: '超时'
}

/**
 * 错误分类显示名称映射
 */
export const ERROR_CATEGORY_NAME_MAP: Record<string, string> = {
  TIMEOUT: '超时',
  API_ERROR: 'API错误',
  RATE_LIMIT: '速率限制',
  NETWORK_ERROR: '网络错误',
  CONFIG_ERROR: '配置错误',
  AUTH_ERROR: '认证错误',
  CONTENT_FILTER: '内容过滤',
  TOKEN_LIMIT: '令牌超限',
  UNKNOWN: '未知错误'
}

// ===== 🆕 单次分析详情相关类型（v2.3.1） =====

/**
 * 单次分析详情
 */
export interface AnalysisTraceDetail {
  /** SQL 指纹 */
  traceId: string
  /** SQL 示例（用于展示） */
  sampleSql?: string
  /** 数据库名称 */
  dbName?: string
  /** 分析开始时间 */
  startTime: string
  /** 分析结束时间 */
  endTime: string
  /** 总耗时（毫秒） */
  totalDurationMs: number
  /** 总 Token 消耗 */
  totalTokens: number
  /** 总调用次数 */
  totalCalls: number
  /** 成功率（0-100） */
  successRate: number
  /** AI 调用详情列表（按时间顺序） */
  invocations: AiInvocationDetail[]
  /** 状态：SUCCESS（全部成功）/ PARTIAL_FAILURE（部分失败）/ FAILED（全部失败） */
  status: 'SUCCESS' | 'PARTIAL_FAILURE' | 'FAILED'
}

/**
 * 单次分析摘要
 */
export interface AnalysisTraceSummary {
  /** SQL 指纹 */
  traceId: string
  /** 分析开始时间 */
  startTime: string
  /** 总调用次数 */
  totalCalls: number
  /** 总耗时（毫秒） */
  totalDurationMs: number
  /** 总 Token 消耗 */
  totalTokens: number
  /** 状态 */
  status: 'SUCCESS' | 'PARTIAL_FAILURE' | 'FAILED'
}

// ===== 🆕 成本分析相关类型（v2.3.2） =====

/**
 * 成本统计数据
 */
export interface CostStats {
  /** 总成本（美元） */
  totalCost: number
  /** 各模型成本分布 */
  costByModel: Record<string, number>
  /** 各 Agent 成本分布 */
  costByAgent: Record<string, number>
  /** 输入 Token 总数 */
  totalInputTokens: number
  /** 输出 Token 总数 */
  totalOutputTokens: number
  /** 总 Token 数 */
  totalTokens: number
  /** 调用总次数 */
  totalCalls: number
  /** 平均每次调用成本 */
  avgCostPerCall: number
  /** 统计时间范围 */
  timeRange: string
}
