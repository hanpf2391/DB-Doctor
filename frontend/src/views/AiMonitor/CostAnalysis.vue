<template>
  <div class="cost-analysis-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2>成本分析</h2>
      <p>分析 AI 调用的成本消耗（基于真实 Token 统计）</p>
    </div>

    <!-- 查询条件 -->
    <el-card class="filter-card" shadow="never">
      <el-form :inline="true" :model="queryForm">
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="dateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery" :loading="loading">
            <el-icon><Search /></el-icon>
            查询
          </el-button>
          <el-button @click="handleReset">
            <el-icon><RefreshLeft /></el-icon>
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 成本统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon total-cost">
              <el-icon><Wallet /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-label">总成本</div>
              <div class="stat-value">${{ formatCost(stats.totalCost) }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon tokens">
              <el-icon><Coin /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-label">总 Token 消耗</div>
              <div class="stat-value">{{ formatTokens(stats.totalTokens) }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon calls">
              <el-icon><DataAnalysis /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-label">总调用次数</div>
              <div class="stat-value">{{ stats.totalCalls?.toLocaleString() || 0 }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon avg-cost">
              <el-icon><TrendCharts /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-label">平均每次成本</div>
              <div class="stat-value">${{ formatCost(stats.avgCostPerCall) }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="20" class="charts-row">
      <!-- 各模型成本分布 -->
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>
            <h3>各模型成本分布</h3>
          </template>
          <div ref="modelCostChartRef" style="height: 300px"></div>
        </el-card>
      </el-col>

      <!-- 各 Agent 成本分布 -->
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>
            <h3>各 Agent 成本分布</h3>
          </template>
          <div ref="agentCostChartRef" style="height: 300px"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Token 组成分析 -->
    <el-row :gutter="20" class="charts-row">
      <el-col :span="24">
        <el-card class="chart-card">
          <template #header>
            <h3>Token 组成分析</h3>
          </template>
          <div ref="tokenCompositionChartRef" style="height: 250px"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, RefreshLeft, Wallet, Coin, DataAnalysis, TrendCharts } from '@element-plus/icons-vue'
import { getCostStats } from '@/api/ai-monitor'
import type { CostStats } from './types'
import * as echarts from 'echarts'

// 查询表单
const queryForm = reactive({})
const dateRange = ref<string[]>([])
const loading = ref(false)

// 成本统计
const stats = ref<CostStats>({
  totalCost: 0,
  costByModel: {},
  costByAgent: {},
  totalInputTokens: 0,
  totalOutputTokens: 0,
  totalTokens: 0,
  totalCalls: 0,
  avgCostPerCall: 0,
  timeRange: ''
})

// 图表引用
const modelCostChartRef = ref<HTMLDivElement>()
const agentCostChartRef = ref<HTMLDivElement>()
const tokenCompositionChartRef = ref<HTMLDivElement>()

// 图表实例
let modelCostChart: echarts.ECharts | null = null
let agentCostChart: echarts.ECharts | null = null
let tokenCompositionChart: echarts.ECharts | null = null

/**
 * 查询成本统计
 */
async function handleQuery() {
  loading.value = true
  try {
    const params = {
      startTime: dateRange.value?.[0],
      endTime: dateRange.value?.[1]
    }

    const result = await getCostStats(params)
    stats.value = result

    // 等待 DOM 更新后渲染图表
    await nextTick()
    renderCharts()

    ElMessage.success('查询成功')
  } catch (error: any) {
    ElMessage.error(error.message || '查询失败')
  } finally {
    loading.value = false
  }
}

/**
 * 重置查询
 */
function handleReset() {
  dateRange.value = []
  handleQuery()
}

/**
 * 渲染所有图表
 */
function renderCharts() {
  renderModelCostChart()
  renderAgentCostChart()
  renderTokenCompositionChart()
}

/**
 * 渲染各模型成本分布饼图
 */
function renderModelCostChart() {
  if (!modelCostChartRef.value) return

  if (!modelCostChart) {
    modelCostChart = echarts.init(modelCostChartRef.value)
  }

  const data = Object.entries(stats.value.costByModel || {}).map(([name, value]) => ({
    name,
    value: value as number
  }))

  const option: echarts.EChartsOption = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: ${c} ({d}%)'
    },
    legend: {
      orient: 'vertical',
      right: 10,
      top: 'center'
    },
    series: [
      {
        name: '模型成本',
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: false,
          position: 'center'
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 20,
            fontWeight: 'bold'
          }
        },
        labelLine: {
          show: false
        },
        data
      }
    ]
  }

  modelCostChart.setOption(option)
}

/**
 * 渲染各 Agent 成本分布饼图
 */
function renderAgentCostChart() {
  if (!agentCostChartRef.value) return

  if (!agentCostChart) {
    agentCostChart = echarts.init(agentCostChartRef.value)
  }

  const data = Object.entries(stats.value.costByAgent || {}).map(([name, value]) => ({
    name: AGENT_NAME_MAP[name] || name,
    value: value as number
  }))

  const option: echarts.EChartsOption = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: ${c} ({d}%)'
    },
    legend: {
      orient: 'vertical',
      right: 10,
      top: 'center'
    },
    series: [
      {
        name: 'Agent 成本',
        type: 'pie',
        radius: '60%',
        data,
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)'
          }
        }
      }
    ]
  }

  agentCostChart.setOption(option)
}

/**
 * 渲染 Token 组成分析条形图
 */
function renderTokenCompositionChart() {
  if (!tokenCompositionChartRef.value) return

  if (!tokenCompositionChart) {
    tokenCompositionChart = echarts.init(tokenCompositionChartRef.value)
  }

  const option: echarts.EChartsOption = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'value',
      axisLabel: {
        formatter: (value: number) => formatTokens(value)
      }
    },
    yAxis: {
      type: 'category',
      data: ['输入 Token', '输出 Token']
    },
    series: [
      {
        name: 'Token 数',
        type: 'bar',
        data: [
          {
            value: stats.value.totalInputTokens || 0,
            itemStyle: { color: '#409EFF' }
          },
          {
            value: stats.value.totalOutputTokens || 0,
            itemStyle: { color: '#67C23A' }
          }
        ],
        label: {
          show: true,
          position: 'right',
          formatter: (params: any) => formatTokens(params.value)
        }
      }
    ]
  }

  tokenCompositionChart.setOption(option)
}

/**
 * 格式化成本（美元）
 */
function formatCost(cost: number | undefined): string {
  if (cost === undefined || cost === null) return '0.00'
  return cost.toFixed(4)
}

/**
 * 格式化 Token 数
 */
function formatTokens(tokens: number | undefined): string {
  if (tokens === undefined || tokens === null) return '0 Tokens'
  if (tokens >= 1000000) return (tokens / 1000000).toFixed(1) + 'M Tokens'
  if (tokens >= 1000) return (tokens / 1000).toFixed(1) + 'K Tokens'
  return tokens + ' Tokens'
}

/**
 * Agent 名称映射
 */
const AGENT_NAME_MAP: Record<string, string> = {
  DIAGNOSIS: '主治医生',
  REASONING: '推理专家',
  CODING: '编码专家'
}

/**
 * 窗口大小变化时重新渲染图表
 */
function handleResize() {
  modelCostChart?.resize()
  agentCostChart?.resize()
  tokenCompositionChart?.resize()
}

onMounted(() => {
  handleQuery()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  modelCostChart?.dispose()
  agentCostChart?.dispose()
  tokenCompositionChart?.dispose()
})
</script>

<style scoped>
.cost-analysis-page {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0 0 10px 0;
  font-size: 24px;
  color: #303133;
}

.page-header p {
  margin: 0;
  font-size: 14px;
  color: #909399;
}

.filter-card {
  margin-bottom: 20px;
}

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  height: 120px;
}

.stat-content {
  display: flex;
  align-items: center;
  height: 100%;
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  color: #fff;
  margin-right: 15px;
}

.stat-icon.total-cost {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.stat-icon.tokens {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.stat-icon.calls {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.stat-icon.avg-cost {
  background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
}

.stat-info {
  flex: 1;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 5px;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
}

.charts-row {
  margin-bottom: 20px;
}

.chart-card {
  height: 400px;
}

.chart-card h3 {
  margin: 0;
  font-size: 16px;
  font-weight: bold;
  color: #303133;
}
</style>
