<template>
  <el-drawer
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    size="80%"
    :destroy-on-close="true"
  >
    <template #header>
      <div class="drawer-header">
        <h3>🎯 慢查询诊断报告 #{{ reportId }}</h3>
      </div>
    </template>

    <div v-if="loading" class="loading-container">
      <el-icon class="is-loading" :size="40">
        <Loading />
      </el-icon>
      <p>加载中...</p>
    </div>

    <div v-else-if="reportData" class="report-content">
      <!-- 顶部核心指标卡片 -->
      <div class="vital-signs">
        <el-card
          v-for="sign in vitalSigns"
          :key="sign.key"
          :class="['vital-card', `vital-${sign.level}`]"
          shadow="hover"
        >
          <div class="vital-value">{{ sign.value }}</div>
          <div class="vital-label">{{ sign.label }}</div>
        </el-card>
      </div>

      <!-- 基本信息卡片 -->
      <el-card class="info-card" shadow="never">
        <template #header>
          <span class="card-title">基本信息</span>
        </template>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="数据库">
            {{ reportData.dbName || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="表名">
            {{ reportData.tableName || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="指纹">
            <el-input
              :model-value="reportData.fingerprint"
              readonly
              size="small"
              style="width: 200px"
            >
              <template #append>
                <el-button
                  :icon="DocumentCopy"
                  @click="copyFingerprint"
                />
              </template>
            </el-input>
          </el-descriptions-item>
          <el-descriptions-item label="严重程度">
            <el-tag :type="getSeverityType(reportData.severityLevel)">
              {{ reportData.severityLevel }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(reportData.analysisStatus)">
              {{ getStatusText(reportData.analysisStatus) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="最后出现">
            {{ reportData.lastSeenTime }}
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- Tab 组件：历史样本 + AI 调用链路 -->
      <el-tabs v-model="activeTab" class="detail-tabs">
        <!-- Tab 1: 历史样本列表 -->
        <el-tab-pane label="历史样本列表" name="samples">
          <template #header>
            <div class="tab-header">
              <span>历史执行样本（共 {{ samplesTotal }} 条）</span>
              <el-button
                size="small"
                @click="loadSamples"
                :loading="samplesLoading"
              >
                <el-icon><Refresh /></el-icon>
                刷新
              </el-button>
            </div>
          </template>

          <el-table :data="samples" v-loading="samplesLoading" stripe>
            <el-table-column prop="capturedAt" label="捕获时间" width="170" />
            <el-table-column prop="userHost" label="用户@主机" width="180" />
            <el-table-column prop="queryTime" label="耗时(秒)" width="100">
              <template #default="scope">
                {{ scope.row.queryTime.toFixed(3) }}
              </template>
            </el-table-column>
            <el-table-column prop="rowsExamined" label="扫描行数" width="100">
              <template #default="scope">
                {{ scope.row.rowsExamined.toLocaleString() }}
              </template>
            </el-table-column>
            <el-table-column prop="originalSql" label="SQL 语句" min-width="300">
              <template #default="scope">
                <SqlTooltip :sql="scope.row.originalSql" :max-length="100" />
              </template>
            </el-table-column>
          </el-table>

          <el-pagination
            v-model:current-page="samplesPagination.page"
            v-model:page-size="samplesPagination.size"
            :total="samplesPagination.total"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="loadSamples"
            @current-change="loadSamples"
            style="margin-top: 20px"
          />
        </el-tab-pane>

        <!-- Tab 2: AI 调用链路追踪 -->
        <el-tab-pane label="AI 调用链路" name="aiTrace">
          <template #header>
            <div class="tab-header">
              <span>AI 调用链路追踪</span>
              <el-button
                size="small"
                @click="loadAiTrace"
                :loading="aiTraceLoading"
              >
                <el-icon><Refresh /></el-icon>
                刷新
              </el-button>
            </div>
          </template>

          <div v-if="aiTraceData" class="ai-trace-content">
            <!-- 汇总信息卡片 -->
            <div class="ai-summary-header">
              <div class="summary-item">
                <div class="summary-label">总调用次数</div>
                <div class="summary-value">{{ aiTraceData.totalCalls }}</div>
              </div>
              <div class="summary-item">
                <div class="summary-label">总耗时</div>
                <div class="summary-value">{{ formatDuration(aiTraceData.totalDurationMs) }}</div>
              </div>
              <div class="summary-item">
                <div class="summary-label">总 Token</div>
                <div class="summary-value">{{ aiTraceData.totalTokens.toLocaleString() }}</div>
              </div>
              <div class="summary-item">
                <div class="summary-label">成功率</div>
                <div class="summary-value">{{ aiTraceData.successRate.toFixed(1) }}%</div>
              </div>
            </div>

            <!-- 时间范围信息 -->
            <el-alert
              v-if="aiTraceData.startTime && aiTraceData.endTime"
              type="info"
              :closable="false"
              style="margin-bottom: 20px"
            >
              <template #title>
                <span style="font-size: 13px; color: #606266">
                  <strong>分析时间：</strong>
                  {{ formatDateTime(aiTraceData.startTime) }} ~ {{ formatDateTime(aiTraceData.endTime) }}
                  （总耗时 {{ formatDuration(aiTraceData.totalDurationMs) }}）
                </span>
              </template>
            </el-alert>

            <!-- 调用链路列表 -->
            <div class="invocation-list">
              <div
                v-for="(invocation, index) in aiTraceData.invocations"
                :key="invocation.id"
                class="invocation-item"
              >
                <!-- 序号 + 时间 -->
                <div class="invocation-number">{{ index + 1 }}</div>

                <!-- 主要内容 -->
                <div class="invocation-content">
                  <!-- 标题行：Agent 角色 + 状态 -->
                  <div class="invocation-title">
                    <div class="title-left">
                      <el-tag
                        :type="getAgentTagType(invocation.agentCode)"
                        size="large"
                        effect="dark"
                      >
                        {{ invocation.agentDisplayName }}
                      </el-tag>
                      <span class="agent-code">{{ invocation.agentCode }}</span>
                    </div>
                    <div class="title-right">
                      <el-tag
                        :type="invocation.statusCode === 'SUCCESS' ? 'success' : 'danger'"
                        size="small"
                      >
                        {{ invocation.statusDisplayName }}
                      </el-tag>
                    </div>
                  </div>

                  <!-- 详细信息 -->
                  <div class="invocation-details">
                    <div class="detail-item">
                      <span class="detail-label">开始时间</span>
                      <span class="detail-value">{{ formatDateTime(invocation.startTime) }}</span>
                    </div>
                    <div class="detail-item">
                      <span class="detail-label">耗时</span>
                      <span class="detail-value time-cost">{{ formatDuration(invocation.durationMs) }}</span>
                    </div>
                    <div class="detail-item">
                      <span class="detail-label">模型</span>
                      <span class="detail-value">{{ invocation.modelName || '未知' }}</span>
                    </div>
                    <div class="detail-item">
                      <span class="detail-label">Token</span>
                      <span class="detail-value">
                        <span class="token-input">{{ invocation.inputTokens }}</span>
                        <span class="token-separator">→</span>
                        <span class="token-output">{{ invocation.outputTokens }}</span>
                        <span class="token-total">({{ invocation.totalTokens }})</span>
                      </span>
                    </div>
                  </div>

                  <!-- 错误信息 -->
                  <div v-if="invocation.errorMessage" class="error-box">
                    <el-icon class="error-icon"><WarningFilled /></el-icon>
                    <span class="error-text">{{ invocation.errorMessage }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <el-empty
            v-else-if="!aiTraceLoading"
            description="暂无调用链路数据"
            :image-size="100"
          />
        </el-tab-pane>

        <!-- 默认 Tab: AI 诊断报告 -->
        <el-tab-pane label="AI 诊断报告" name="report">
          <template #header>
            <span class="card-title">👨⚕️ AI 诊断报告</span>
          </template>

          <el-card class="sql-card" shadow="never">
            <template #header>
              <span class="card-title">样本 SQL（最慢）</span>
            </template>
            <SqlHighlight
              :code="reportData.sqlTemplate"
              title="SQL 语句"
            />
          </el-card>

          <el-card class="report-card" shadow="never">
            <MarkdownPreview
              v-if="reportData.aiAnalysisReport"
              :text="reportData.aiAnalysisReport"
            />
            <el-empty
              v-else
              description="暂无分析报告"
              :image-size="100"
            />
          </el-card>
        </el-tab-pane>
      </el-tabs>
    </div>

    <el-empty
      v-else
      description="报告不存在"
      :image-size="100"
    />
  </el-drawer>
</template>

<script setup lang="ts">
import { ref, computed, watch, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { DocumentCopy, Loading, Refresh, WarningFilled } from '@element-plus/icons-vue'
import { getReportDetail, getReportSamples, getAiAnalysisTrace } from '@/api/config'
import MarkdownPreview from '@/components/MarkdownPreview.vue'
import SqlHighlight from '@/components/SqlHighlight.vue'
import SqlTooltip from '@/components/SqlTooltip.vue'
import type { ReportDetailData, VitalSign } from './types'
import { formatSeconds, formatMilliseconds } from '@/utils/format'

const props = defineProps<{
  modelValue: boolean
  reportId: number
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
}>()

const loading = ref(false)
const reportData = ref<ReportDetailData | null>(null)

// 🆕 Tab 相关状态
const activeTab = ref('report') // 默认显示 AI 诊断报告
const samples = ref<any[]>([])
const samplesLoading = ref(false)
const samplesPagination = reactive({
  page: 1,
  size: 10,
  total: 0
})
const samplesTotal = computed(() => samplesPagination.total)

const aiTraceData = ref<any>(null)
const aiTraceLoading = ref(false)

// 指标卡片数据
const vitalSigns = computed<VitalSign[]>(() => {
  if (!reportData.value) return []

  const data = reportData.value

  return [
    {
      key: 'queryTime',
      label: '平均耗时',
      value: formatSeconds(data.avgQueryTime),
      level: getQueryTimeLevel(data.avgQueryTime)
    },
    {
      key: 'lockTime',
      label: '锁等待',
      value: formatMilliseconds(data.lockTime),
      level: getLockTimeLevel(data.lockTime)
    },
    {
      key: 'rowsExamined',
      label: '扫描行数',
      value: data.rowsExamined.toLocaleString(),
      level: getRowsExaminedLevel(data.rowsExamined)
    },
    {
      key: 'occurrenceCount',
      label: '累计出现',
      value: data.occurrenceCount,
      level: data.occurrenceCount > 5 ? 'warning' : 'info'
    }
  ]
})

// 耗时危险级别
function getQueryTimeLevel(time: number): VitalSign['level'] {
  if (time > 2.0) return 'danger'
  if (time > 1.0) return 'warning'
  if (time > 0.5) return 'info'
  return 'success'
}

// 锁等待危险级别
function getLockTimeLevel(time: number): VitalSign['level'] {
  if (time > 500) return 'danger'
  if (time > 100) return 'warning'
  return 'success'
}

// 扫描行数危险级别
function getRowsExaminedLevel(rows: number): VitalSign['level'] {
  if (rows > 50000) return 'danger'
  if (rows > 10000) return 'warning'
  return 'info'
}

// 获取严重程度 Tag 类型
function getSeverityType(severity: string): 'danger' | 'warning' | 'info' | 'success' {
  if (severity.includes('严重')) return 'danger'
  if (severity.includes('警告')) return 'warning'
  if (severity.includes('注意')) return 'info'
  return 'success'
}

// 获取状态 Tag 类型
function getStatusType(status: string): 'danger' | 'warning' | 'info' | 'success' {
  if (status === 'SUCCESS') return 'success'
  if (status === 'FAILED') return 'danger'
  if (status === 'DIAGNOSING') return 'warning'
  return 'info'
}

// 获取状态文本
function getStatusText(status: string): string {
  const statusMap: Record<string, string> = {
    'SUCCESS': '✅ 诊断完成',
    'FAILED': '❌ 诊断失败',
    'DIAGNOSING': '👨⚕️ 正在会诊...',
    'PENDING': '⏳ 排队中'
  }
  return statusMap[status] || status
}

// 复制指纹
async function copyFingerprint() {
  if (!reportData.value) return

  try {
    await navigator.clipboard.writeText(reportData.value.fingerprint)
    ElMessage.success('已复制到剪贴板')
  } catch (error) {
    ElMessage.error('复制失败')
  }
}

// 加载报告详情
async function loadReport() {
  if (!props.reportId) return

  loading.value = true
  try {
    reportData.value = await getReportDetail(props.reportId)
  } catch (error: any) {
    ElMessage.error(error.message || '加载报告失败')
    reportData.value = null
  } finally {
    loading.value = false
  }
}

// 加载历史样本列表
async function loadSamples() {
  if (!props.reportId) return

  samplesLoading.value = true
  try {
    const result = await getReportSamples(props.reportId, samplesPagination.page, samplesPagination.size)
    samples.value = result.records || []
    samplesPagination.total = result.total || 0
  } catch (error: any) {
    ElMessage.error(error.message || '加载样本失败')
  } finally {
    samplesLoading.value = false
  }
}

// 加载 AI 调用链路
async function loadAiTrace() {
  if (!reportData.value?.fingerprint) return

  aiTraceLoading.value = true
  try {
    aiTraceData.value = await getAiAnalysisTrace(reportData.value.fingerprint)
  } catch (error: any) {
    ElMessage.error(error.message || '加载调用链路失败')
  } finally {
    aiTraceLoading.value = false
  }
}

// 获取代理标签类型
function getAgentTagType(agentName: string): 'success' | 'info' | 'warning' | 'danger' {
  const typeMap: Record<string, string> = {
    'DIAGNOSIS': 'warning',
    'REASONING': 'info',
    'CODING': 'success'
  }
  return (typeMap[agentName] || 'info') as any
}

// 获取代理中文名称
function getAgentName(agentName: string): string {
  const nameMap: Record<string, string> = {
    'DIAGNOSIS': '主治医生',
    'REASONING': '推理专家',
    'CODING': '编码专家'
  }
  return nameMap[agentName] || agentName
}

// 格式化耗时
function formatDuration(ms: number): string {
  if (ms < 1000) return `${ms}ms`
  const seconds = (ms / 1000).toFixed(2)
  if (ms < 60000) return `${seconds}s`
  const minutes = Math.floor(ms / 60000)
  const remainingSeconds = ((ms % 60000) / 1000).toFixed(0)
  return `${minutes}m ${remainingSeconds}s`
}

// 格式化日期时间
function formatDateTime(isoString: string): string {
  if (!isoString) return '-'
  const date = new Date(isoString)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')

  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
}

// 监听抽屉打开状态，加载数据
watch(() => props.modelValue, (newVal) => {
  if (newVal) {
    loadReport()
    loadSamples()
  }
})

// 监听 Tab 切换，按需加载数据
watch(activeTab, (newTab) => {
  if (newTab === 'samples' && samples.value.length === 0) {
    loadSamples()
  }
  if (newTab === 'aiTrace' && !aiTraceData.value) {
    loadAiTrace()
  }
})
</script>

<style scoped>
.drawer-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: bold;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 0;
  color: #909399;
}

.loading-container p {
  margin-top: 16px;
  font-size: 14px;
}

.report-content {
  padding: 0 20px 20px 20px;
}

/* 指标卡片 */
.vital-signs {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.vital-card {
  text-align: center;
  padding: 16px;
  border-radius: 8px;
  transition: transform 0.2s, box-shadow 0.2s;
}

.vital-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.vital-value {
  font-size: 24px;
  font-weight: bold;
  margin-bottom: 8px;
}

.vital-label {
  font-size: 14px;
  color: #606266;
}

.vital-danger {
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid #ef4444;
}

.vital-warning {
  background: rgba(245, 158, 11, 0.1);
  border: 1px solid #f59e0b;
}

.vital-success {
  background: rgba(16, 185, 129, 0.1);
  border: 1px solid #10b981;
}

.vital-info {
  background: rgba(107, 114, 128, 0.1);
  border: 1px solid #6b7280;
}

[data-theme="dark"] .vital-danger {
  background: rgba(239, 68, 68, 0.2);
  border-color: #f87171;
}

[data-theme="dark"] .vital-warning {
  background: rgba(245, 158, 11, 0.2);
  border-color: #fbbf24;
}

[data-theme="dark"] .vital-success {
  background: rgba(16, 185, 129, 0.2);
  border-color: #34d399;
}

[data-theme="dark"] .vital-info {
  background: rgba(107, 114, 128, 0.2);
  border-color: #9ca3af;
}

/* 卡片样式 */
.info-card,
.sql-card,
.report-card {
  margin-bottom: 20px;
}

.card-title {
  font-weight: bold;
  font-size: 16px;
}

/* 响应式 */
@media (max-width: 768px) {
  .vital-signs {
    grid-template-columns: repeat(2, 1fr);
  }
}

/* Tab 样式 */
.detail-tabs {
  margin-top: 20px;
}

.tab-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

/* AI 调用链路样式 - Notion 风格 */
.ai-trace-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* 汇总信息 - 简约风格 */
.ai-summary-header {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1px;
  background: #e5e7eb;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  overflow: hidden;
}

.summary-item {
  background: white;
  padding: 20px 16px;
  text-align: center;
}

.summary-label {
  font-size: 12px;
  color: #6b7280;
  margin-bottom: 8px;
  font-weight: 500;
  letter-spacing: 0.5px;
  text-transform: uppercase;
}

.summary-value {
  font-size: 24px;
  font-weight: 600;
  color: #111827;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
}

/* 时间范围提示 */
.ai-trace-content .el-alert {
  border: none;
  background: #f9fafb;
  border-radius: 6px;
}

.ai-trace-content .el-alert__title {
  font-size: 13px;
  color: #6b7280;
  font-weight: 400;
}

/* 调用列表 */
.invocation-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.invocation-item {
  display: flex;
  gap: 12px;
  padding: 12px 16px;
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  transition: all 0.15s ease;
}

.invocation-item:hover {
  background: #f9fafb;
  border-color: #d1d5db;
}

/* 序号 - 简约设计 */
.invocation-number {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f3f4f6;
  color: #6b7280;
  border-radius: 4px;
  font-size: 13px;
  font-weight: 500;
}

/* 主要内容 */
.invocation-content {
  flex: 1;
  min-width: 0;
}

/* 标题行 */
.invocation-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.title-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.agent-code {
  font-size: 11px;
  color: #9ca3af;
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', 'Courier New', monospace;
  background: #f3f4f6;
  padding: 2px 6px;
  border-radius: 3px;
  font-weight: 500;
}

/* 详细信息 */
.invocation-details {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px 16px;
}

.detail-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
}

.detail-label {
  color: #9ca3af;
  font-size: 12px;
  min-width: 50px;
  font-weight: 400;
}

.detail-value {
  color: #374151;
  font-weight: 400;
  font-size: 13px;
}

.time-cost {
  color: #059669;
  font-weight: 500;
  font-family: 'SF Mono', 'Monaco', monospace;
}

/* Token 显示 - 更柔和 */
.token-input {
  color: #3b82f6;
  font-weight: 500;
  font-size: 12px;
}

.token-separator {
  color: #d1d5db;
  margin: 0 3px;
}

.token-output {
  color: #10b981;
  font-weight: 500;
  font-size: 12px;
}

.token-total {
  color: #9ca3af;
  font-size: 11px;
  margin-left: 3px;
  font-weight: 400;
}

/* 错误信息 */
.error-box {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-top: 10px;
  padding: 8px 12px;
  background: #fef2f2;
  border-radius: 4px;
  border: 1px solid #fecaca;
}

.error-icon {
  color: #dc2626;
  font-size: 14px;
  flex-shrink: 0;
  margin-top: 1px;
}

.error-text {
  flex: 1;
  color: #991b1b;
  font-size: 12px;
  line-height: 1.5;
  font-weight: 400;
}

/* 响应式 */
@media (max-width: 768px) {
  .ai-summary-header {
    grid-template-columns: repeat(2, 1fr);
  }

  .invocation-details {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
