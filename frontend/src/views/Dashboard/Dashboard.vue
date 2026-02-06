<template>
  <div class="dashboard-page">
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <el-statistic title="模板总数" :value="stats.templateTotal">
            <template #suffix>
              <el-icon><Collection /></el-icon>
            </template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card primary" shadow="hover">
          <el-statistic title="SQL 样本总数" :value="stats.sqlTotal">
            <template #suffix>
              <el-icon><Document /></el-icon>
            </template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card danger" shadow="hover">
          <el-statistic title="高危 SQL 数" :value="stats.highRiskCount">
            <template #suffix>
              <el-icon><Warning /></el-icon>
            </template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card info" shadow="hover">
          <el-statistic title="待分析任务" :value="stats.pendingTasks">
            <template #suffix>
              <el-icon><Clock /></el-icon>
            </template>
          </el-statistic>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="20" class="charts-row">
      <!-- 趋势图 -->
      <el-col :span="16">
        <el-card class="chart-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span class="card-title">慢查询趋势（24小时）</span>
              <div class="date-picker-wrapper">
                <el-button-group size="small">
                  <el-button
                    :type="isToday(trendDate) ? 'primary' : ''"
                    @click="selectDate('today')"
                  >
                    今日
                  </el-button>
                  <el-button
                    :type="isYesterday(trendDate) ? 'primary' : ''"
                    @click="selectDate('yesterday')"
                  >
                    昨日
                  </el-button>
                </el-button-group>
                <el-date-picker
                  v-model="trendDate"
                  type="date"
                  placeholder="选择日期"
                  format="YYYY-MM-DD"
                  value-format="YYYY-MM-DD"
                  @change="loadTrend"
                  size="small"
                  clearable
                  class="custom-date-picker"
                />
              </div>
            </div>
          </template>
          <v-chart
            v-if="trendOption"
            :option="trendOption"
            style="height: 300px"
            autoresize
          />
          <el-empty v-else description="暂无数据" :image-size="100" />
        </el-card>
      </el-col>

      <!-- Top 5 慢榜 -->
      <el-col :span="8">
        <el-card class="chart-card" shadow="never">
          <template #header>
            <span class="card-title">Top 5 慢查询</span>
          </template>
          <v-chart
            v-if="topOption"
            :option="topOption"
            style="height: 300px"
            autoresize
            @click="handleChartClick"
          />
          <el-empty v-else description="暂无数据" :image-size="100" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 快捷操作 -->
    <el-card class="actions-card" shadow="never">
      <template #header>
        <span class="card-title">快捷操作</span>
      </template>
      <el-space :size="20">
        <el-button type="primary" @click="goToReports">
          <el-icon><View /></el-icon>
          查看所有报告
        </el-button>
        <el-button type="success" @click="goToSettings">
          <el-icon><Setting /></el-icon>
          配置中心
        </el-button>
        <el-button @click="refreshAll">
          <el-icon><Refresh /></el-icon>
          刷新数据
        </el-button>
      </el-space>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Collection, Document, DataLine, Warning, Clock, View, Setting, Refresh } from '@element-plus/icons-vue'
import * as echarts from 'echarts/core'
import type { EChartsOption } from 'echarts'
import { getTodayOverview, getTrend, getTopSlow } from '@/api/dashboard'
import type { DashboardStats, TrendData, TopSlowData } from './types'
import { formatSeconds } from '@/utils/format'

const router = useRouter()

// 加载状态
const loading = ref(false)
const errorMessage = ref('')

// 数据
const stats = ref<DashboardStats>({
  templateTotal: 0,
  sqlTotal: 0,
  todayTotal: 0,
  highRiskCount: 0,
  avgQueryTime: 0,
  pendingTasks: 0,
  date: ''
})

const trendData = ref<TrendData | null>(null)
const topData = ref<TopSlowData | null>(null)
const trendDate = ref(new Date().toISOString().split('T')[0])

// 趋势图配置
const trendOption = computed<EChartsOption>(() => {
  if (!trendData.value) return null

  return {
    title: {
      text: `慢查询趋势（${trendData.value.date}）`,
      left: 'center',
      textStyle: { fontSize: 14 }
    },
    tooltip: {
      trigger: 'axis',
      formatter: '{b0}:00 - {c0} 次'
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '15%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: trendData.value.hours.map(h => `${h}:00`),
      boundaryGap: false
    },
    yAxis: {
      type: 'value',
      name: '慢查询数量'
    },
    series: [{
      name: '慢查询数量',
      type: 'line',
      data: trendData.value.counts,
      smooth: true,
      areaStyle: {
        color: {
          type: 'linear',
          x: 0,
          y: 0,
          x2: 0,
          y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(64, 158, 255, 0.5)' },
            { offset: 1, color: 'rgba(64, 158, 255, 0.1)' }
          ]
        }
      },
      itemStyle: {
        color: '#409eff'
      }
    }]
  }
})

// Top 5 图表配置
const topOption = computed<EChartsOption>(() => {
  if (!topData.value || !topData.value.records.length) return null

  const records = topData.value.records

  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      },
      formatter: (params: any) => {
        const data = records[params[0].dataIndex]
        return `
          <div style="padding: 8px;">
            <div style="margin-bottom: 4px; font-weight: bold;">${data.sqlTemplate?.substring(0, 50)}...</div>
            <div>数据库: ${data.dbName}</div>
            <div>表名: ${data.tableName}</div>
            <div>最大耗时: ${formatSeconds(data.maxQueryTime)}</div>
          </div>
        `
      }
    },
    grid: {
      left: '20%',
      right: '5%',
      top: '5%',
      bottom: '5%'
    },
    xAxis: {
      type: 'value',
      name: '耗时(秒)'
    },
    yAxis: {
      type: 'category',
      data: records.map(r => ({
        value: r.fingerprint?.substring(0, 12) + '...',
        textStyle: { fontSize: 11 }
      }))
    },
    series: [{
      type: 'bar',
      data: records.map(r => ({
        value: r.maxQueryTime,
        itemStyle: {
          color: getSeverityColor(r.severityLevel)
        }
      })),
      label: {
        show: true,
        position: 'right',
        formatter: '{c}s'
      }
    }]
  }
})

/**
 * 获取严重程度颜色
 */
function getSeverityColor(severity: string): string {
  if (severity.includes('严重')) return '#f56c6c'
  if (severity.includes('警告')) return '#e6a23c'
  if (severity.includes('注意')) return '#909399'
  return '#67c23a'
}

/**
 * 加载今日概览
 */
async function loadStats() {
  try {
    const result = await getTodayOverview()
    stats.value = result
  } catch (error: any) {
    ElMessage.error(error.message || '加载统计数据失败')
  }
}

/**
 * 加载趋势图
 */
async function loadTrend() {
  try {
    const result = await getTrend(trendDate.value)
    trendData.value = result
  } catch (error: any) {
    ElMessage.error(error.message || '加载趋势图失败')
  }
}

/**
 * 加载 Top 5
 */
async function loadTop() {
  try {
    const result = await getTopSlow(5)
    topData.value = result
  } catch (error: any) {
    ElMessage.error(error.message || '加载 Top 榜失败')
  }
}

/**
 * 刷新所有数据
 */
async function refreshAll() {
  try {
    await Promise.all([
      loadStats(),
      loadTrend(),
      loadTop()
    ])
    // 刷新成功不显示提示
  } catch (error) {
    // 刷新失败已经在各自函数中处理了错误提示
  }
}

/**
 * 跳转到报告列表
 */
function goToReports() {
  router.push('/reports')
}

/**
 * 跳转到设置中心
 */
function goToSettings() {
  router.push('/settings')
}

/**
 * 处理图表点击
 */
function handleChartClick(params: any) {
  if (topData.value && topData.value.records[params.dataIndex]) {
    const record = topData.value.records[params.dataIndex]
    router.push({ path: '/', query: { id: record.id.toString() } })
  }
}

/**
 * 快捷选择日期
 */
function selectDate(type: 'today' | 'yesterday') {
  const today = new Date()
  const date = new Date()

  if (type === 'yesterday') {
    date.setDate(date.getDate() - 1)
  }

  trendDate.value = date.toISOString().split('T')[0]
  loadTrend()
}

/**
 * 判断是否是今天
 */
function isToday(dateStr: string): boolean {
  const today = new Date().toISOString().split('T')[0]
  return dateStr === today
}

/**
 * 判断是否是昨天
 */
function isYesterday(dateStr: string): boolean {
  const yesterday = new Date()
  yesterday.setDate(yesterday.getDate() - 1)
  const yesterdayStr = yesterday.toISOString().split('T')[0]
  return dateStr === yesterdayStr
}

onMounted(() => {
  console.log('仪表盘已加载')
  refreshAll()
})
</script>

<style scoped>
.dashboard-page {
  max-width: 1400px;
  margin: 0 auto;
}

.stats-row {
  margin-top: 24px;
  margin-bottom: 20px;
}

.stat-card {
  text-align: center;
  transition: transform 0.2s;
}

.stat-card:hover {
  transform: translateY(-4px);
}

.stat-card :deep(.el-statistic__head) {
  font-size: 14px;
  color: #909399;
  margin-bottom: 8px;
}

.stat-card :deep(.el-statistic__content) {
  font-size: 32px;
  font-weight: bold;
}

.stat-card.danger :deep(.el-statistic__content) {
  color: #f56c6c;
}

.stat-card.warning :deep(.el-statistic__content) {
  color: #e6a23c;
}

.stat-card.info :deep(.el-statistic__content) {
  color: #409eff;
}

.stat-card.primary :deep(.el-statistic__content) {
  color: #67c23a;
}

.charts-row {
  margin-bottom: 20px;
}

.chart-card {
  height: 400px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-weight: bold;
  font-size: 16px;
}

.date-picker-wrapper {
  display: flex;
  gap: 10px;
  align-items: center;
}

.custom-date-picker {
  width: 140px;
}

.actions-card {
  margin-bottom: 20px;
}
</style>
