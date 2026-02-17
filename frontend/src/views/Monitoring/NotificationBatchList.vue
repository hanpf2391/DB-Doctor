<template>
  <div class="notification-batch-list">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1>通知历史</h1>
      <div class="header-actions">
        <el-button @click="showStatistics" :icon="DataAnalysis">统计信息</el-button>
        <el-button @click="refreshData" :loading="loading" :icon="Refresh">刷新</el-button>
      </div>
    </div>

    <!-- 统计信息卡片 -->
    <el-row v-if="statistics" :gutter="16" class="statistics-row">
      <el-col :span="6">
        <el-card shadow="hover">
          <el-statistic title="总批次数" :value="statistics.totalCount || 0" />
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <el-statistic title="成功" :value="statistics.successCount || 0">
            <template #suffix>
              <el-icon color="#67C23A"><SuccessFilled /></el-icon>
            </template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <el-statistic title="失败" :value="statistics.failedCount || 0">
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

    <!-- 批次卡片列表（三层折叠结构） -->
    <div v-loading="loading" class="batch-list">
      <el-empty v-if="batchList.length === 0" description="暂无通知记录" />

      <!-- 第一层：批次卡片 -->
      <div
        v-for="batch in batchList"
        :key="batch.batchId"
        class="batch-card"
      >
        <!-- 批次摘要（可点击展开） -->
        <div class="batch-header" @click="toggleBatchExpand(batch.batchId)">
          <div class="header-left">
            <el-icon class="expand-icon" :class="{ expanded: expandedBatches[batch.batchId] }">
              <ArrowRight />
            </el-icon>
            <div class="batch-info">
              <div class="batch-time">{{ formatBatchWindow(batch) }}</div>
              <div class="batch-meta">
                <el-tag size="small" type="info">{{ batch.totalCount }} 条慢查询</el-tag>
                <el-tag
                  size="small"
                  :type="batch.status === 'SENT' ? 'success' : 'danger'"
                >
                  {{ batch.status === 'SENT' ? '发送成功' : '发送失败' }}
                </el-tag>
                <span v-if="batch.criticalCount > 0" class="severity-tag critical">
                  🔴 严重: {{ batch.criticalCount }}
                </span>
                <span v-if="batch.warningCount > 0" class="severity-tag warning">
                  ⚠️ 警告: {{ batch.warningCount }}
                </span>
                <span v-if="batch.normalCount > 0" class="severity-tag normal">
                  ✅ 正常: {{ batch.normalCount }}
                </span>
              </div>
            </div>
          </div>
          <div class="header-right">
            <div class="performance-stats">
              <span>平均耗时: <strong>{{ batch.avgQueryTime?.toFixed(2) }}s</strong></span>
              <span>最慢: <strong>{{ batch.maxQueryTime?.toFixed(2) }}s</strong></span>
            </div>
            <div class="batch-time">{{ formatDateTime(batch.sentTime) }}</div>
          </div>
        </div>

        <!-- 第二层：慢查询列表（默认折叠） -->
        <el-collapse-transition>
          <div v-show="expandedBatches[batch.batchId]" class="batch-content">
            <div v-loading="detailLoading[batch.batchId]">
              <!-- 慢查询卡片 -->
              <div
                v-for="record in batchDetails[batch.batchId]"
                :key="record.id"
                class="query-card"
              >
                <!-- 慢查询摘要（可点击展开） -->
                <div class="query-header" @click="toggleQueryExpand(batch.batchId, record.id)">
                  <el-icon class="expand-icon" :class="{ expanded: expandedQueries[`${batch.batchId}-${record.id}`] }">
                    <ArrowRight />
                  </el-icon>
                  <div class="query-summary">
                    <div class="query-basic">
                      <el-tag
                        :type="getSeverityTagType(record.severity)"
                        size="small"
                      >
                        {{ getSeverityLabel(record.severity) }}
                      </el-tag>
                      <span class="db-name">{{ record.dbName }}</span>
                      <span v-if="record.tableName" class="table-name">.{{ record.tableName }}</span>
                    </div>
                    <div class="query-stats">
                      <span>执行 {{ record.occurrenceCount }} 次</span>
                      <span>均耗 {{ record.avgQueryTime?.toFixed(2) }}s</span>
                      <span v-if="record.maxQueryTime">最慢 {{ record.maxQueryTime?.toFixed(2) }}s</span>
                    </div>
                  </div>
                </div>

                <!-- 第三层：慢查询详情（默认折叠） -->
                <el-collapse-transition>
                  <div v-show="expandedQueries[`${batch.batchId}-${record.id}`]" class="query-content">
                    <!-- SQL 语句 -->
                    <div class="detail-section">
                      <div class="section-title">
                        <el-icon><Document /></el-icon>
                        <span>SQL 语句</span>
                      </div>
                      <div class="sql-code-block">
                        <pre>{{ record.sqlTemplate }}</pre>
                      </div>
                    </div>

                    <!-- 执行统计 -->
                    <div class="detail-section">
                      <div class="section-title">
                        <el-icon><DataAnalysis /></el-icon>
                        <span>执行统计</span>
                      </div>
                      <div class="stats-grid">
                        <div class="stat-item">
                          <span class="label">数据库名:</span>
                          <span class="value">{{ record.dbName }}</span>
                        </div>
                        <div class="stat-item">
                          <span class="label">表名:</span>
                          <span class="value">{{ record.tableName || '-' }}</span>
                        </div>
                        <div class="stat-item">
                          <span class="label">执行次数:</span>
                          <span class="value">{{ record.occurrenceCount }} 次</span>
                        </div>
                        <div class="stat-item">
                          <span class="label">平均耗时:</span>
                          <span class="value">{{ record.avgQueryTime?.toFixed(3) }} 秒</span>
                        </div>
                        <div class="stat-item">
                          <span class="label">最大耗时:</span>
                          <span class="value">{{ record.maxQueryTime?.toFixed(3) }} 秒</span>
                        </div>
                        <div class="stat-item">
                          <span class="label">分析时间:</span>
                          <span class="value">{{ formatDateTime(record.analyzedTime) }}</span>
                        </div>
                      </div>
                    </div>

                    <!-- AI 分析报告 -->
                    <div v-if="record.aiReport" class="detail-section">
                      <div class="section-title">
                        <el-icon><ChatDotRound /></el-icon>
                        <span>智能诊断报告</span>
                      </div>
                      <div class="ai-report-content" v-html="formatAiReport(record.aiReport)"></div>
                    </div>

                    <!-- 错误信息 -->
                    <div v-if="record.errorMessage" class="detail-section error-section">
                      <div class="section-title">
                        <el-icon><Warning /></el-icon>
                        <span>失败原因</span>
                      </div>
                      <el-alert type="error" :closable="false">
                        {{ record.errorMessage }}
                      </el-alert>
                    </div>
                  </div>
                </el-collapse-transition>
              </div>

              <el-empty
                v-if="!batchDetails[batch.batchId] || batchDetails[batch.batchId].length === 0"
                description="暂无详情"
                :image-size="80"
              />
            </div>
          </div>
        </el-collapse-transition>
      </div>
    </div>

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

    <!-- 统计信息对话框 -->
    <el-dialog v-model="statisticsVisible" title="统计信息" width="500px" class="no-border-dialog">
      <div v-if="statisticsData" class="statistics-dialog-content">
        <div class="stat-row">
          <span class="stat-label">总通知数</span>
          <span class="stat-value">{{ statisticsData.totalCount || 0 }}</span>
          <span class="stat-label">成功</span>
          <span class="stat-value success">{{ statisticsData.successCount || 0 }}</span>
        </div>
        <div class="stat-row">
          <span class="stat-label">失败</span>
          <span class="stat-value danger">{{ statisticsData.failedCount || 0 }}</span>
          <span class="stat-label">成功率</span>
          <span class="stat-value">{{ ((statisticsData.successCount || 0) / (statisticsData.totalCount || 1) * 100).toFixed(2) }}%</span>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  DataAnalysis,
  Refresh,
  Search,
  RefreshLeft,
  ArrowRight,
  Document,
  ChatDotRound,
  Warning
} from '@element-plus/icons-vue'
import axios from 'axios'

// 响应式数据
const loading = ref(false)
const batchList = ref<any[]>([])
const expandedBatches = ref<Record<string, boolean>>({})
const expandedQueries = ref<Record<string, boolean>>({})
const batchDetails = ref<Record<string, any[]>>({})
const detailLoading = ref<Record<string, boolean>>({})
const statistics = ref<any>(null)
const statisticsData = ref<any>(null)
const statisticsVisible = ref(false)

const dateRange = ref<[string, string]>()
const filters = ref({
  status: ''
})

const pagination = ref({
  page: 1,
  size: 20,
  total: 0
})

// 计算属性
const successRate = computed(() => {
  if (!statistics.value || !statistics.value.totalCount) return 0
  return (
    (statistics.value.successCount / statistics.value.totalCount) *
    100
  ).toFixed(2)
})

// 加载批次列表
const loadBatchList = async () => {
  loading.value = true
  try {
    const params: any = {
      page: pagination.value.page - 1,
      size: pagination.value.size
    }

    if (filters.value.status) {
      params.status = filters.value.status
    }

    if (dateRange.value && dateRange.value.length === 2) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }

    const response = await axios.get('/api/notifications/batches', { params })

    if (response.data.code === 'SUCCESS') {
      batchList.value = response.data.data.list
      pagination.value.total = response.data.data.total
    } else {
      ElMessage.error(response.data.message || '查询失败')
    }
  } catch (error: any) {
    console.error('加载批次列表失败:', error)
    ElMessage.error(error.response?.data?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

// 加载批次详情
const loadBatchDetails = async (batchId: string) => {
  if (batchDetails.value[batchId]) {
    return // 已加载过
  }

  detailLoading.value[batchId] = true
  try {
    const response = await axios.get(`/api/notifications/history/batch/${batchId}`)

    if (response.data.code === 'SUCCESS') {
      batchDetails.value[batchId] = response.data.data.records
    } else {
      ElMessage.error(response.data.message || '加载详情失败')
    }
  } catch (error: any) {
    console.error('加载批次详情失败:', error)
    ElMessage.error(error.response?.data?.message || '加载详情失败')
  } finally {
    detailLoading.value[batchId] = false
  }
}

// 展开/收起批次
const toggleBatchExpand = async (batchId: string) => {
  expandedBatches.value[batchId] = !expandedBatches.value[batchId]

  if (expandedBatches.value[batchId]) {
    await loadBatchDetails(batchId)
  }
}

// 展开/收起慢查询
const toggleQueryExpand = (batchId: string, queryId: number) => {
  const key = `${batchId}-${queryId}`
  expandedQueries.value[key] = !expandedQueries.value[key]
}

// 格式化时间
const formatDateTime = (dateTime: string) => {
  if (!dateTime) return '-'
  return new Date(dateTime).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false
  })
}

// 格式化批次时间窗口
const formatBatchWindow = (batch: any) => {
  const startDate = new Date(batch.windowStart)
  const endDate = new Date(batch.windowEnd)

  // 判断是否为同一天
  const isSameDay = startDate.toDateString() === endDate.toDateString()

  // 格式化开始时间
  const startTime = startDate.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    hour12: false
  })

  // 格式化结束时间
  const endTime = endDate.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    hour12: false
  })

  if (isSameDay) {
    // 同一天：只显示一次日期 "02/17 23:39 - 23:40"
    const dateStr = startDate.toLocaleDateString('zh-CN', {
      month: '2-digit',
      day: '2-digit'
    })
    return `${dateStr} ${startTime} - ${endTime}`
  } else {
    // 不同天：显示完整日期时间 "02/17 23:39 - 02/18 00:00"
    const startStr = startDate.toLocaleString('zh-CN', {
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    })
    const endStr = endDate.toLocaleString('zh-CN', {
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    })
    return `${startStr} - ${endStr}`
  }
}

// 获取严重程度标签类型
const getSeverityTagType = (severity: string) => {
  const typeMap: Record<string, any> = {
    CRITICAL: 'danger',
    WARNING: 'warning',
    NORMAL: 'success'
  }
  return typeMap[severity] || 'info'
}

// 获取严重程度标签文本
const getSeverityLabel = (severity: string) => {
  const labelMap: Record<string, string> = {
    CRITICAL: '严重',
    WARNING: '警告',
    NORMAL: '正常'
  }
  return labelMap[severity] || severity
}

// 格式化 AI 报告
const formatAiReport = (report: string) => {
  if (!report) return ''

  // Markdown 转 HTML
  return report
    // 代码块
    .replace(/```sql\n([\s\S]*?)```/g, '<pre class="sql-code"><code>$1</code></pre>')
    .replace(/```([\s\S]*?)```/g, '<pre class="code-block"><code>$1</code></pre>')
    // 粗体
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    // 列表
    .replace(/^\- (.*?)(<br>|$)/gm, '<li>$1</li>')
    .replace(/(<li>.*<\/li>)/g, '<ul>$1</ul>')
    // 换行
    .replace(/\n/g, '<br>')
}

// 查询
const handleQuery = () => {
  pagination.value.page = 1
  expandedBatches.value = {}
  expandedQueries.value = {}
  batchDetails.value = {}
  loadBatchList()
}

// 重置
const handleReset = () => {
  filters.value.status = ''
  dateRange.value = undefined
  handleQuery()
}

// 刷新
const refreshData = () => {
  loadBatchList()
}

// 分页改变
const handlePageChange = () => {
  expandedBatches.value = {}
  expandedQueries.value = {}
  batchDetails.value = {}
  loadBatchList()
}

// 显示统计信息
const showStatistics = async () => {
  try {
    const params: any = {}
    if (dateRange.value && dateRange.value.length === 2) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    } else {
      // 默认最近7天
      const end = new Date()
      const start = new Date()
      start.setDate(start.getDate() - 7)
      params.startDate = start.toISOString().split('T')[0]
      params.endDate = end.toISOString().split('T')[0]
    }

    const response = await axios.get('/api/notifications/statistics', { params })

    if (response.data.code === 'SUCCESS') {
      statisticsData.value = response.data.data
      statisticsVisible.value = true
    } else {
      ElMessage.error(response.data.message || '查询统计失败')
    }
  } catch (error: any) {
    console.error('加载统计信息失败:', error)
    ElMessage.error('加载统计信息失败')
  }
}

// 初始化
onMounted(() => {
  loadBatchList()
})
</script>

<style scoped lang="scss">
.notification-batch-list {
  padding: 20px;

  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;

    h1 {
      margin: 0;
      font-size: 24px;
      font-weight: 600;
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
    margin-bottom: 20px;
  }

  .batch-list {
    min-height: 400px;

    .batch-card {
      margin-bottom: 16px;
      border: 1px solid var(--el-border-color-lighter);
      border-radius: 8px;
      background: var(--el-bg-color);
      overflow: hidden;

      // 第一层：批次头部
      .batch-header {
        padding: 20px;
        cursor: pointer;
        display: flex;
        justify-content: space-between;
        align-items: center;
        background: var(--el-fill-color-light);
        transition: background 0.2s;

        &:hover {
          background: var(--el-fill-color);
        }

        .header-left {
          display: flex;
          align-items: center;
          gap: 12px;

          .expand-icon {
            font-size: 16px;
            transition: transform 0.2s;

            &.expanded {
              transform: rotate(90deg);
            }
          }

          .batch-info {
            .batch-time {
              font-size: 16px;
              font-weight: 600;
              color: var(--el-text-color-primary);
              margin-bottom: 8px;
            }

            .batch-meta {
              display: flex;
              gap: 8px;
              align-items: center;

              .severity-tag {
                font-size: 12px;
                padding: 4px 10px;
                border-radius: 12px;
                background: var(--el-fill-color);
                color: var(--el-text-color-regular);
              }
            }
          }
        }

        .header-right {
          text-align: right;

          .performance-stats {
            display: flex;
            gap: 20px;
            margin-bottom: 8px;
            font-size: 13px;
            color: var(--el-text-color-secondary);

            strong {
              color: var(--el-text-color-primary);
            }
          }

          .batch-time {
            font-size: 12px;
            color: var(--el-text-color-placeholder);
          }
        }
      }

      // 第二层：批次内容（慢查询列表）
      .batch-content {
        border-top: 1px solid var(--el-border-color-lighter);

        .query-card {
          border-bottom: 1px solid var(--el-border-color-lighter);
          background: var(--el-bg-color);

          &:last-child {
            border-bottom: none;
          }

          // 第二层：慢查询头部
          .query-header {
            padding: 16px 20px;
            cursor: pointer;
            display: flex;
            align-items: center;
            gap: 12px;
            background: var(--el-bg-color);
            transition: background 0.2s;

            &:hover {
              background: var(--el-fill-color-light);
            }

            .expand-icon {
              font-size: 14px;
              transition: transform 0.2s;

              &.expanded {
                transform: rotate(90deg);
              }
            }

            .query-summary {
              flex: 1;
              display: flex;
              justify-content: space-between;
              align-items: center;

              .query-basic {
                display: flex;
                align-items: center;
                gap: 8px;

                .db-name {
                  font-weight: 600;
                  color: var(--el-text-color-regular);
                }

                .table-name {
                  color: var(--el-text-color-secondary);
                }
              }

              .query-stats {
                display: flex;
                gap: 16px;
                font-size: 12px;
                color: var(--el-text-color-secondary);
              }
            }
          }

          // 第三层：慢查询内容
          .query-content {
            padding: 0 20px 20px;
            background: var(--el-fill-color-extra-light);

            .detail-section {
              margin-bottom: 16px;

              &:last-child {
                margin-bottom: 0;
              }

              .section-title {
                display: flex;
                align-items: center;
                gap: 8px;
                margin-bottom: 12px;
                font-size: 14px;
                font-weight: 600;
                color: var(--el-text-color-regular);

                .el-icon {
                  font-size: 16px;
                }
              }

              &.error-section {
                .section-title {
                  color: var(--el-color-danger);
                }
              }

              // SQL 代码块
              .sql-code-block {
                pre {
                  background: var(--el-bg-color-page);
                  color: var(--el-text-color-primary);
                  padding: 16px;
                  border-radius: 6px;
                  font-family: 'SF Mono', 'Menlo', 'Monaco', 'Courier New', monospace;
                  font-size: 13px;
                  line-height: 1.6;
                  overflow-x: auto;
                  margin: 0;
                }
              }

              // 统计网格
              .stats-grid {
                display: grid;
                grid-template-columns: repeat(3, 1fr);
                gap: 12px;
                padding: 16px;
                background: var(--el-bg-color);
                border-radius: 6px;
                border: 1px solid var(--el-border-color-lighter);

                .stat-item {
                  display: flex;
                  justify-content: space-between;
                  font-size: 13px;

                  .label {
                    color: var(--el-text-color-secondary);
                  }

                  .value {
                    font-weight: 600;
                    color: var(--el-text-color-regular);
                  }
                }
              }

              // AI 报告
              .ai-report-content {
                :deep(.sql-code),
                :deep(.code-block) {
                  background: var(--el-bg-color-page);
                  color: var(--el-text-color-primary);
                  padding: 12px;
                  border-radius: 6px;
                  border: 1px solid var(--el-border-color-light);
                  font-family: 'SF Mono', 'Menlo', 'Monaco', 'Courier New', monospace;
                  font-size: 13px;
                  line-height: 1.6;
                  white-space: pre-wrap;
                  word-break: break-all;
                  margin: 8px 0;
                }

                :deep(ul) {
                  margin: 8px 0;
                  padding-left: 20px;
                }

                :deep(li) {
                  line-height: 1.8;
                  margin-bottom: 4px;
                }

                :deep(strong) {
                  color: #2d3748;
                  font-weight: 600;
                }
              }
            }
          }
        }
      }
    }
  }

  .pagination-container {
    display: flex;
    justify-content: center;
    margin-top: 20px;
  }

  // 统计对话框样式 - 移除边框和阴影
  :deep(.no-border-dialog.el-dialog) {
    border: none !important;
    box-shadow: none !important;
  }

  :deep(.no-border-dialog .el-dialog__header) {
    border-bottom: none !important;
  }

  :deep(.no-border-dialog .el-dialog__body) {
    background-color: var(--el-bg-color);
    border-radius: 8px;
  }

  .statistics-dialog-content {
    padding: 8px 0;
  }

  .stat-row {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 16px;
    padding: 12px 0;
    border-bottom: 1px solid var(--el-border-color-lighter);

    &:last-child {
      border-bottom: none;
    }
  }

  .stat-label {
    font-size: 14px;
    color: var(--el-text-color-regular);
    text-align: center;
  }

  .stat-value {
    font-size: 16px;
    font-weight: 600;
    color: var(--el-text-color-primary);
    text-align: center;

    &.success {
      color: var(--el-color-success);
    }

    &.danger {
      color: var(--el-color-danger);
    }
  }
}
</style>

<style lang="scss">
// 全局样式：彻底移除 Element Plus Dialog 的边框和阴影

// 1. 移除 dialog 本身的边框和阴影
.el-dialog.no-border-dialog {
  border: none !important;
  box-shadow: none !important;
  background: var(--el-bg-color) !important;
  --el-dialog-box-shadow: none !important;
}

// 2. 移除头部下边框
.el-dialog.no-border-dialog .el-dialog__header {
  border-bottom: none !important;
  padding-bottom: 0 !important;
}

// 3. 移除 body 的边框
.el-dialog.no-border-dialog .el-dialog__body {
  background-color: var(--el-bg-color) !important;
  border: none !important;
  padding: 20px !important;
}

// 4. 移除 overlay 层的样式
.el-overlay-dialog .no-border-dialog {
  box-shadow: none !important;
  border: none !important;
}
</style>
