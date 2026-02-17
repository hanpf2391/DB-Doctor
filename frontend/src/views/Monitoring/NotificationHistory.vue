<template>
  <div class="notification-history">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1>通知历史</h1>
      <div class="header-actions">
        <el-button @click="showStatistics = true" :icon="DataAnalysis">统计信息</el-button>
        <el-button @click="refreshData" :loading="loading" :icon="Refresh">刷新</el-button>
      </div>
    </div>

    <!-- 统计信息卡片 -->
    <el-row v-if="statistics" :gutter="16" class="statistics-row">
      <el-col :span="6">
        <el-card shadow="hover">
          <el-statistic title="总通知数" :value="statistics.totalCount" />
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <el-statistic title="成功" :value="statistics.successCount">
            <template #suffix>
              <el-icon color="#67C23A"><SuccessFilled /></el-icon>
            </template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <el-statistic title="失败" :value="statistics.failedCount">
            <template #suffix>
              <el-icon color="#F56C6C"><CircleCloseFilled /></el-icon>
            </template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <el-statistic title="成功率" :value="successRate" :precision="2" suffix="%" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 筛选器 -->
    <el-form :inline="true" class="filter-form">
      <el-form-item label="状态">
        <el-select v-model="filters.status" placeholder="全部" clearable style="width: 120px">
          <el-option label="成功" value="SENT" />
          <el-option label="失败" value="FAILED" />
        </el-select>
      </el-form-item>
      <el-form-item label="日期范围">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          style="width: 280px"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleQuery" :icon="Search">查询</el-button>
        <el-button @click="handleReset" :icon="RefreshLeft">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 数据表格 -->
    <el-table :data="tableData" v-loading="loading" stripe>
      <el-table-column prop="batchId" label="批次ID" width="200" show-overflow-tooltip />
      <el-table-column prop="sentTime" label="发送时间" width="180">
        <template #default="{ row }">
          {{ formatDateTime(row.sentTime) }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.notificationStatus === 'SENT' ? 'success' : 'danger'">
            {{ row.notificationStatus === 'SENT' ? '成功' : '失败' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="dbName" label="数据库" width="150" show-overflow-tooltip />
      <el-table-column prop="tableName" label="表名" width="150" show-overflow-tooltip />
      <el-table-column label="严重程度" width="120">
        <template #default="{ row }">
          <el-tag :type="getSeverityTagType(row.severity)">
            {{ getSeverityLabel(row.severity) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="avgQueryTime" label="平均耗时" width="120" align="right">
        <template #default="{ row }">
          {{ row.avgQueryTime?.toFixed(2) }}s
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right" align="center">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleViewTimeline(row)" :icon="Clock">时间线</el-button>
          <el-button link type="primary" @click="handleViewDetail(row)" :icon="View">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination-container">
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        @current-change="handlePageChange"
        layout="total, prev, pager, next, jumper"
      />
    </div>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailVisible" title="通知详情" width="800px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="批次ID" :span="2">{{ detailData.batchId }}</el-descriptions-item>
        <el-descriptions-item label="数据库">{{ detailData.dbName }}</el-descriptions-item>
        <el-descriptions-item label="表名">{{ detailData.tableName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="SQL指纹" :span="2">{{ detailData.sqlFingerprint }}</el-descriptions-item>
        <el-descriptions-item label="严重程度">
          <el-tag :type="getSeverityTagType(detailData.severity)">
            {{ getSeverityLabel(detailData.severity) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="分析时间">
          {{ formatDateTime(detailData.analyzedTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="发送时间">
          {{ formatDateTime(detailData.sentTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="detailData.notificationStatus === 'SENT' ? 'success' : 'danger'">
            {{ detailData.notificationStatus === 'SENT' ? '成功' : '失败' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="平均耗时">
          {{ detailData.avgQueryTime?.toFixed(2) }}s
        </el-descriptions-item>
        <el-descriptions-item label="执行次数">
          {{ detailData.occurrenceCount }}
        </el-descriptions-item>
        <el-descriptions-item label="失败原因" :span="2" v-if="detailData.errorMessage">
          <el-text type="danger">{{ detailData.errorMessage }}</el-text>
        </el-descriptions-item>
      </el-descriptions>

      <el-divider content-position="left">SQL 模板</el-divider>
      <el-input
        v-model="detailData.sqlTemplate"
        type="textarea"
        :rows="4"
        readonly
        style="font-family: monospace;"
      />

      <el-divider content-position="left">AI 分析报告</el-divider>
      <div class="ai-report" v-html="formatAiReport(detailData.aiReport)"></div>
    </el-dialog>

    <!-- 时间线对话框 -->
    <el-dialog v-model="timelineVisible" :title="`时间线 - ${timelineData.fingerprint}`" width="1000px">
      <el-timeline>
        <el-timeline-item
          v-for="item in timelineData.list"
          :key="item.id"
          :timestamp="formatDateTime(item.analyzedTime)"
          placement="top"
          :type="getSeverityTimelineType(item.severity)"
        >
          <el-card>
            <template #header>
              <div class="timeline-header">
                <el-tag :type="getSeverityTagType(item.severity)">
                  {{ getSeverityLabel(item.severity) }}
                </el-tag>
                <span class="query-time">平均耗时: {{ item.avgQueryTime?.toFixed(2) }}s</span>
              </div>
            </template>
            <div class="ai-report" v-html="formatAiReport(item.aiReport)"></div>
          </el-card>
        </el-timeline-item>
      </el-timeline>

      <div class="timeline-pagination">
        <el-pagination
          v-model:current-page="timelinePagination.page"
          v-model:page-size="timelinePagination.size"
          :total="timelinePagination.total"
          @current-change="loadTimeline"
          small
          layout="prev, pager, next"
        />
      </div>
    </el-dialog>

    <!-- 统计信息对话框 -->
    <el-dialog v-model="showStatistics" title="统计信息" width="600px">
      <div v-if="statistics">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="统计时间范围">
            {{ dateRange?.[0] }} 至 {{ dateRange?.[1] }}
          </el-descriptions-item>
          <el-descriptions-item label="总通知数">
            {{ statistics.totalCount }}
          </el-descriptions-item>
          <el-descriptions-item label="成功">
            {{ statistics.successCount }}
          </el-descriptions-item>
          <el-descriptions-item label="失败">
            {{ statistics.failedCount }}
          </el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">按状态统计</el-divider>
        <div v-if="statistics.statusStats">
          <p>成功: {{ statistics.statusStats.SENT || 0 }}</p>
          <p>失败: {{ statistics.statusStats.FAILED || 0 }}</p>
        </div>

        <el-divider content-position="left">按严重程度统计</el-divider>
        <div v-if="statistics.severityStats">
          <p>🔥 严重: {{ statistics.severityStats.CRITICAL || 0 }}</p>
          <p>⚠️  警告: {{ statistics.severityStats.WARNING || 0 }}</p>
          <p>✅ 正常: {{ statistics.severityStats.NORMAL || 0 }}</p>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Refresh, Search, RefreshLeft, View, Clock, DataAnalysis, SuccessFilled, CircleCloseFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { notificationHistoryApi } from '@/api/notification'

// 响应式数据
const loading = ref(false)
const tableData = ref<any[]>([])
const detailVisible = ref(false)
const detailData = ref<any>({})
const timelineVisible = ref(false)
const timelineData = ref<any>({
  fingerprint: '',
  list: []
})
const showStatistics = ref(false)
const statistics = ref<any>(null)

const filters = ref({
  status: ''
})

const dateRange = ref<[string, string] | null>(null)

const pagination = ref({
  page: 1,
  size: 20,
  total: 0
})

const timelinePagination = ref({
  page: 1,
  size: 10,
  total: 0
})

// 计算属性
const successRate = computed(() => {
  if (!statistics.value || !statistics.value.totalCount) return 0
  return (statistics.value.successCount / statistics.value.totalCount) * 100
})

// 格式化时间
const formatDateTime = (time: string) => {
  if (!time) return '-'
  return time.replace('T', ' ').substring(0, 19)
}

// 格式化 AI 报告（保留换行和格式）
const formatAiReport = (report: string) => {
  if (!report) return ''
  return report
    .replace(/\n/g, '<br>')
    .replace(/```([\s\S]*?)```/g, '<pre><code>$1</code></pre>')
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
}

// 获取严重程度标签类型
const getSeverityTagType = (severity: string) => {
  const typeMap: Record<string, any> = {
    'CRITICAL': 'danger',
    'WARNING': 'warning',
    'NORMAL': 'success'
  }
  return typeMap[severity] || 'info'
}

// 获取严重程度标签颜色（时间线用）
const getSeverityTimelineType = (severity: string) => {
  const typeMap: Record<string, any> = {
    'CRITICAL': 'danger',
    'WARNING': 'warning',
    'NORMAL': 'success'
  }
  return typeMap[severity] || 'primary'
}

// 获取严重程度标签文本
const getSeverityLabel = (severity: string) => {
  const labelMap: Record<string, string> = {
    'CRITICAL': '🔥 严重',
    'WARNING': '⚠️  警告',
    'NORMAL': '✅ 正常'
  }
  return labelMap[severity] || severity
}

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    const res = await notificationHistoryApi.getHistory({
      page: pagination.value.page - 1,
      size: pagination.value.size,
      startDate: dateRange.value?.[0],
      endDate: dateRange.value?.[1],
      status: filters.value.status as any
    })

    tableData.value = res.list
    pagination.value.total = res.total
  } catch (error: any) {
    console.error('加载通知历史失败:', error)
    ElMessage.error(error.message || '加载数据失败')
  } finally {
    loading.value = false
  }
}

// 加载统计信息
const loadStatistics = async () => {
  if (!dateRange.value) {
    ElMessage.warning('请先选择日期范围')
    return
  }

  try {
    const res = await notificationHistoryApi.getStatistics({
      startDate: dateRange.value[0],
      endDate: dateRange.value[1]
    })

    statistics.value = res
  } catch (error: any) {
    console.error('加载统计信息失败:', error)
    ElMessage.error(error.message || '加载统计信息失败')
  }
}

// 加载时间线
const loadTimeline = async (page: number = timelinePagination.value.page) => {
  try {
    const res = await notificationHistoryApi.getTimeline({
      fingerprint: timelineData.value.fingerprint,
      page: page - 1,
      size: timelinePagination.value.size
    })

    timelineData.value.list = res.timeline
    timelinePagination.value.total = res.total
    timelinePagination.value.page = page
  } catch (error: any) {
    console.error('加载时间线失败:', error)
    ElMessage.error(error.message || '加载时间线失败')
  }
}

// 刷新数据
const refreshData = () => {
  loadData()
  if (dateRange.value) {
    loadStatistics()
  }
}

// 查询
const handleQuery = () => {
  pagination.value.page = 1
  loadData()
  if (dateRange.value) {
    loadStatistics()
  }
}

// 重置
const handleReset = () => {
  filters.value.status = ''
  dateRange.value = null
  pagination.value.page = 1
  statistics.value = null
  loadData()
}

// 翻页
const handlePageChange = (page: number) => {
  pagination.value.page = page
  loadData()
}

// 查看详情
const handleViewDetail = (row: any) => {
  detailData.value = row
  detailVisible.value = true
}

// 查看时间线
const handleViewTimeline = (row: any) => {
  timelineData.value.fingerprint = row.sqlFingerprint
  timelinePagination.value.page = 1
  timelineVisible.value = true
  loadTimeline(1)
}

// 初始化
onMounted(() => {
  // 设置默认日期范围为最近7天
  const end = new Date()
  const start = new Date()
  start.setDate(start.getDate() - 7)
  dateRange.value = [
    start.toISOString().split('T')[0],
    end.toISOString().split('T')[0]
  ]

  loadData()
  loadStatistics()
})
</script>

<style scoped lang="scss">
.notification-history {
  padding: 20px;

  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;

    h1 {
      font-size: 24px;
      font-weight: 600;
      margin: 0;
    }

    .header-actions {
      display: flex;
      gap: 10px;
    }
  }

  .statistics-row {
    margin-bottom: 20px;
  }

  .filter-form {
    background: #fff;
    padding: 16px;
    border-radius: 4px;
    margin-bottom: 16px;
  }

  .pagination-container {
    display: flex;
    justify-content: center;
    margin-top: 16px;
  }

  .ai-report {
    max-height: 400px;
    overflow-y: auto;
    line-height: 1.6;
    font-size: 14px;

    :deep(pre) {
      background: #f5f7fa;
      padding: 12px;
      border-radius: 4px;
      overflow-x: auto;
    }

    :deep(code) {
      font-family: 'Courier New', monospace;
      background: #f5f7fa;
      padding: 2px 6px;
      border-radius: 3px;
    }
  }

  .timeline-header {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .query-time {
      font-weight: 600;
      color: #409EFF;
    }
  }

  .timeline-pagination {
    display: flex;
    justify-content: center;
    margin-top: 20px;
  }
}
</style>
