<template>
  <div class="analysis-trace-list-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2>分析详情</h2>
      <p>查看每个 SQL 的完整分析过程（调用链追踪）</p>
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

    <!-- 数据表格 -->
    <el-card class="table-card" shadow="never">
      <el-table
        :data="tableData"
        v-loading="loading"
        stripe
        highlight-current-row
      >
        <el-table-column prop="startTime" label="分析时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.startTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="traceId" label="SQL 指纹" width="200" show-overflow-tooltip />
        <el-table-column prop="totalCalls" label="调用次数" width="100" align="center">
          <template #default="{ row }">
            <el-tag>{{ row.totalCalls }} 次</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="totalDurationMs" label="总耗时" width="120" align="center">
          <template #default="{ row }">
            {{ formatDuration(row.totalDurationMs) }}
          </template>
        </el-table-column>
        <el-table-column prop="totalTokens" label="Token 消耗" width="120" align="center">
          <template #default="{ row }">
            {{ formatTokens(row.totalTokens) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleViewDetail(row)">
              <el-icon><View /></el-icon>
              查看详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- 详情弹窗 -->
    <el-dialog
      v-model="detailVisible"
      :title="`分析详情 - ${currentDetail.traceId}`"
      width="900px"
      top="5vh"
    >
      <div v-if="currentDetail.traceId">
        <!-- 基本信息卡片 -->
        <el-descriptions :column="3" border class="detail-header">
          <el-descriptions-item label="SQL 指纹" :span="3">
            {{ currentDetail.traceId }}
          </el-descriptions-item>
          <el-descriptions-item label="分析时间">
            {{ formatTime(currentDetail.startTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="调用次数">
            <el-tag>{{ currentDetail.totalCalls }} 次</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(currentDetail.status)">
              {{ getStatusText(currentDetail.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="总耗时">
            {{ formatDuration(currentDetail.totalDurationMs) }}
          </el-descriptions-item>
          <el-descriptions-item label="Token 消耗">
            {{ formatTokens(currentDetail.totalTokens) }}
          </el-descriptions-item>
          <el-descriptions-item label="成功率">
            {{ currentDetail.successRate?.toFixed(2) }}%
          </el-descriptions-item>
        </el-descriptions>

        <!-- 调用链追踪 -->
        <div class="trace-section">
          <h3>🔗 调用链追踪</h3>
          <el-timeline>
            <el-timeline-item
              v-for="(invocation, index) in currentDetail.invocations"
              :key="invocation.id"
              :timestamp="formatTime(invocation.startTime)"
              placement="top"
            >
              <el-card>
                <template #header>
                  <div class="invocation-header">
                    <span class="agent-name">
                      <el-tag :type="getAgentTagType(invocation.agentCode)">
                        {{ invocation.agentDisplayName }}
                      </el-tag>
                    </span>
                    <span class="invocation-info">
                      耗时: <strong>{{ invocation.durationMs }}ms</strong> |
                      Token: <strong>{{ invocation.inputTokens }} / {{ invocation.outputTokens }}</strong> |
                      状态: <el-tag :type="getStatusType(invocation.statusCode)" size="small">
                        {{ invocation.statusDisplayName }}
                      </el-tag>
                    </span>
                  </div>
                </template>

                <!-- 详细信息 -->
                <el-descriptions :column="2" size="small" border class="invocation-details">
                  <el-descriptions-item label="模型">{{ invocation.modelName }}</el-descriptions-item>
                  <el-descriptions-item label="供应商">{{ invocation.provider }}</el-descriptions-item>
                  <el-descriptions-item label="输入 Tokens">
                    {{ invocation.inputTokens }}
                  </el-descriptions-item>
                  <el-descriptions-item label="输出 Tokens">
                    {{ invocation.outputTokens }}
                  </el-descriptions-item>
                </el-descriptions>

                <!-- 错误信息（如果有） -->
                <el-alert
                  v-if="invocation.errorMessage"
                  :title="invocation.errorCategoryDisplayName || '错误'"
                  type="error"
                  :description="invocation.errorMessage"
                  :closable="false"
                  show-icon
                  style="margin-top: 10px;"
                />
              </el-card>
            </el-timeline-item>
          </el-timeline>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, RefreshLeft, View } from '@element-plus/icons-vue'
import { getAnalysisTraces, getAnalysisTraceDetail } from '@/api/ai-monitor'
import type { AnalysisTraceSummary, AnalysisTraceDetail } from './types'

// 查询表单
const queryForm = reactive({})
const dateRange = ref<string[]>([])
const loading = ref(false)
const tableData = ref<AnalysisTraceSummary[]>([])

// 分页
const pagination = reactive({
  page: 0,
  size: 20,
  total: 0
})

// 详情弹窗
const detailVisible = ref(false)
const currentDetail = ref<AnalysisTraceDetail>({} as any)

/**
 * 查询数据
 */
async function handleQuery() {
  loading.value = true
  try {
    const params = {
      startTime: dateRange.value?.[0],
      endTime: dateRange.value?.[1],
      page: pagination.page,
      size: pagination.size
    }

    const result = await getAnalysisTraces(params)
    tableData.value = result.content
    pagination.total = result.totalElements
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
  pagination.page = 0
  handleQuery()
}

/**
 * 查看详情
 */
async function handleViewDetail(row: AnalysisTraceSummary) {
  try {
    const detail = await getAnalysisTraceDetail(row.traceId)
    currentDetail.value = detail
    detailVisible.value = true
  } catch (error: any) {
    ElMessage.error(error.message || '查询详情失败')
  }
}

/**
 * 分页大小变化
 */
function handleSizeChange(size: number) {
  pagination.size = size
  pagination.page = 0
  handleQuery()
}

/**
 * 页码变化
 */
function handlePageChange(page: number) {
  pagination.page = page
  handleQuery()
}

/**
 * 格式化时间
 */
function formatTime(timeStr: string): string {
  if (!timeStr) return '-'
  return timeStr.replace('T', ' ').substring(0, 19)
}

/**
 * 格式化耗时
 */
function formatDuration(ms: number): string {
  if (ms < 1000) return ms + 'ms'
  if (ms < 60000) return (ms / 1000).toFixed(1) + 's'
  const minutes = Math.floor(ms / 60000)
  const seconds = ((ms % 60000) / 1000).toFixed(0)
  return `${minutes}m${seconds}s`
}

/**
 * 格式化 Token 数
 */
function formatTokens(tokens: number): string {
  if (tokens >= 1000000) return (tokens / 1000000).toFixed(1) + 'M'
  if (tokens >= 1000) return (tokens / 1000).toFixed(1) + 'K'
  return tokens.toString()
}

/**
 * 获取状态标签类型
 */
function getStatusType(status: string): string {
  const map: Record<string, string> = {
    'SUCCESS': 'success',
    'FAILED': 'danger',
    'PARTIAL_FAILURE': 'warning'
  }
  return map[status] || 'info'
}

/**
 * 获取状态文本
 */
function getStatusText(status: string): string {
  const map: Record<string, string> = {
    'SUCCESS': '成功',
    'FAILED': '失败',
    'PARTIAL_FAILURE': '部分失败'
  }
  return map[status] || status
}

/**
 * 获取 Agent 标签类型
 */
function getAgentTagType(agentCode: string): string {
  const map: Record<string, string> = {
    'DIAGNOSIS': 'primary',
    'REASONING': 'success',
    'CODING': 'warning'
  }
  return map[agentCode] || 'info'
}

onMounted(() => {
  handleQuery()
})
</script>

<style scoped>
.analysis-trace-list-page {
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

.table-card {
  margin-bottom: 20px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

.detail-header {
  margin-bottom: 20px;
}

.trace-section {
  margin-top: 20px;
}

.trace-section h3 {
  margin: 0 0 15px 0;
  font-size: 16px;
  font-weight: bold;
  color: #303133;
}

.invocation-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.agent-name {
  font-weight: bold;
}

.invocation-info {
  font-size: 14px;
  color: #606266;
}

.invocation-info strong {
  color: #303133;
}

.invocation-details {
  margin-top: 10px;
}
</style>
