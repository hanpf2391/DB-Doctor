<template>
  <div class="ai-monitor-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2>AI 监控中心</h2>
      <p>实时监控 AI 调用情况、Token 消耗和性能指标</p>
    </div>

    <!-- 顶部统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <el-statistic title="总调用次数" :value="stats.totalCalls">
            <template #suffix>
              <el-icon><DataAnalysis /></el-icon>
            </template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card primary" shadow="hover">
          <el-statistic title="总 Token 消耗" :value="formatTokens(stats.totalTokens)">
            <template #suffix>
              <span class="unit">Tokens</span>
            </template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card success" shadow="hover">
          <el-statistic title="平均耗时" :value="stats.avgDuration" :precision="0">
            <template #suffix>
              <span class="unit">ms</span>
            </template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" :class="getRateClass(stats.successRate)" shadow="hover">
          <el-statistic title="成功率" :value="stats.successRate" :precision="2">
            <template #suffix>
              <span class="unit">%</span>
            </template>
          </el-statistic>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="20" class="charts-row">
      <!-- Agent Token 分布（饼图） -->
      <el-col :span="12">
        <el-card class="chart-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span class="card-title">Agent Token 分布</span>
              <el-tag size="small" type="info">Token 消耗占比</el-tag>
            </div>
          </template>
          <v-chart
            :option="agentPieOption"
            style="height: 300px"
            autoresize
          />
        </el-card>
      </el-col>

      <!-- Agent 调用次数分布（饼图） -->
      <el-col :span="12">
        <el-card class="chart-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span class="card-title">Agent 调用次数分布</span>
              <el-tag size="small" type="info">工作量占比</el-tag>
            </div>
          </template>
          <v-chart
            :option="agentCallPieOption"
            style="height: 300px"
            autoresize
          />
        </el-card>
      </el-col>
    </el-row>

    <!-- 耗时趋势图（折线图） -->
    <el-row :gutter="20" class="charts-row">
      <el-col :span="24">
        <el-card class="chart-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span class="card-title">24 小时调用趋势</span>
              <div class="header-actions">
                <el-button size="small" @click="refreshData" :loading="loading">
                  <el-icon><Refresh /></el-icon>
                  刷新
                </el-button>
              </div>
            </div>
          </template>
          <v-chart
            :option="trendLineOption"
            style="height: 300px"
            autoresize
          />
        </el-card>
      </el-col>
    </el-row>

    <!-- 快捷操作 -->
    <el-card class="actions-card" shadow="never">
      <template #header>
        <span class="card-title">快捷操作</span>
      </template>
      <el-space :size="20">
        <el-button type="primary" @click="goToInvocationLog">
          <el-icon><View /></el-icon>
          查看调用流水
        </el-button>
        <el-button type="success" @click="refreshData">
          <el-icon><Refresh /></el-icon>
          刷新数据
        </el-button>
        <el-button type="info" @click="exportData">
          <el-icon><Download /></el-icon>
          导出数据
        </el-button>
      </el-space>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { DataAnalysis, View, Refresh, Download } from '@element-plus/icons-vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { PieChart, LineChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent
} from 'echarts/components'
import { getAiMonitorStats } from '@/api/ai-monitor'
import type { AiMonitorStats } from './types'
import { AGENT_NAME_MAP } from './types'

// 注册 ECharts 组件
use([
  CanvasRenderer,
  PieChart,
  LineChart,
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent
])

const router = useRouter()

// 数据
const loading = ref(false)
const stats = ref<AiMonitorStats>({
  totalCalls: 0,
  successCount: 0,
  successRate: 0,
  avgDuration: 0,
  maxDuration: 0,
  minDuration: 0,
  totalTokens: 0,
  inputTokens: 0,
  outputTokens: 0,
  agentTokenDistribution: {},
  agentCallDistribution: {},
  hourlyCallCount: {},
  timeRange: ''
})

// Agent Token 分布饼图
const agentPieOption = computed(() => {
  const data = Object.entries(stats.value.agentTokenDistribution).map(([name, value]) => ({
    name: AGENT_NAME_MAP[name] || name,
    value
  }))

  return {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)'
    },
    legend: {
      orient: 'vertical',
      left: 'left',
      top: 'middle'
    },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      center: ['60%', '50%'],
      data,
      emphasis: {
        itemStyle: {
          shadowBlur: 10,
          shadowOffsetX: 0,
          shadowColor: 'rgba(0, 0, 0, 0.5)'
        }
      },
      label: {
        show: true,
        formatter: '{b}\n{d}%'
      }
    }]
  }
})

// Agent 调用次数饼图
const agentCallPieOption = computed(() => {
  const data = Object.entries(stats.value.agentCallDistribution).map(([name, value]) => ({
    name: AGENT_NAME_MAP[name] || name,
    value
  }))

  return {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} 次 ({d}%)'
    },
    legend: {
      orient: 'vertical',
      left: 'left',
      top: 'middle'
    },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      center: ['60%', '50%'],
      data,
      emphasis: {
        itemStyle: {
          shadowBlur: 10,
          shadowOffsetX: 0,
          shadowColor: 'rgba(0, 0, 0, 0.5)'
        }
      },
      label: {
        show: true,
        formatter: '{b}\n{d}%'
      }
    }]
  }
})

// 趋势折线图
const trendLineOption = computed(() => {
  const hours = Object.keys(stats.value.hourlyCallCount).map(h => `${h}:00`)
  const counts = Object.values(stats.value.hourlyCallCount)

  return {
    tooltip: {
      trigger: 'axis',
      formatter: '{b}<br/>{a}: {c} 次'
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: hours,
      boundaryGap: false,
      axisLabel: {
        interval: 2
      }
    },
    yAxis: {
      type: 'value',
      name: '调用次数'
    },
    series: [{
      name: '调用次数',
      type: 'line',
      data: counts,
      smooth: true,
      areaStyle: {
        color: {
          type: 'linear',
          x: 0,
          y: 0,
          x2: 0,
          y2: 1,
          colorStops: [{
            offset: 0, color: 'rgba(64, 158, 255, 0.3)'
          }, {
            offset: 1, color: 'rgba(64, 158, 255, 0.05)'
          }]
        }
      },
      lineStyle: {
        width: 2,
        color: '#409EFF'
      },
      itemStyle: {
        color: '#409EFF'
      }
    }]
  }
})

/**
 * 加载统计数据
 */
async function loadStats() {
  loading.value = true
  try {
    const result = await getAiMonitorStats({})
    stats.value = result
  } catch (error: any) {
    ElMessage.error(error.message || '加载统计数据失败')
  } finally {
    loading.value = false
  }
}

/**
 * 刷新数据
 */
function refreshData() {
  loadStats()
  ElMessage.success('数据已刷新')
}

/**
 * 跳转到调用流水页面
 */
function goToInvocationLog() {
  router.push('/ai-monitor/invocation-log')
}

/**
 * 导出数据
 */
function exportData() {
  ElMessage.info('导出功能开发中...')
}

/**
 * 格式化 Token 数
 */
function formatTokens(tokens: number): string {
  if (tokens >= 1000000) {
    return (tokens / 1000000).toFixed(1)
  }
  if (tokens >= 1000) {
    return (tokens / 1000).toFixed(1)
  }
  return tokens.toString()
}

/**
 * 根据成功率返回样式类
 */
function getRateClass(rate: number): string {
  if (rate >= 99) return 'success'
  if (rate >= 95) return 'warning'
  return 'danger'
}

onMounted(() => {
  loadStats()
})
</script>

<style scoped>
.ai-monitor-page {
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

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  text-align: center;
  transition: transform 0.2s;
}

.stat-card:hover {
  transform: translateY(-4px);
}

.stat-card.primary :deep(.el-statistic__content) {
  color: #409eff;
}

.stat-card.success :deep(.el-statistic__content) {
  color: #67c23a;
}

.stat-card.warning :deep(.el-statistic__content) {
  color: #e6a23c;
}

.stat-card.danger :deep(.el-statistic__content) {
  color: #f56c6c;
}

.charts-row {
  margin-bottom: 20px;
}

.chart-card {
  height: 420px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-weight: bold;
  font-size: 16px;
  color: #303133;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.actions-card {
  margin-bottom: 20px;
}

.unit {
  font-size: 14px;
  color: #909399;
  margin-left: 4px;
}
</style>
