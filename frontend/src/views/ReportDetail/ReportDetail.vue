<template>
  <el-drawer
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    size="70%"
    :destroy-on-close="true"
  >
    <template #header>
      <div class="drawer-header">
        <h3>慢查询诊断报告 #{{ reportId }}</h3>
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

      <!-- SQL 样本 -->
      <el-card class="sql-card" shadow="never">
        <template #header>
          <span class="card-title">样本 SQL（最慢）</span>
        </template>
        <SqlHighlight
          :code="reportData.sqlTemplate"
          title="SQL 语句"
        />
      </el-card>

      <!-- AI 诊断报告 -->
      <el-card class="report-card" shadow="never">
        <template #header>
          <span class="card-title">👨⚕️ AI 诊断报告</span>
        </template>
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
    </div>

    <el-empty
      v-else
      description="报告不存在"
      :image-size="100"
    />
  </el-drawer>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { DocumentCopy, Loading } from '@element-plus/icons-vue'
import { getReportDetail } from '@/api/config'
import MarkdownPreview from '@/components/MarkdownPreview.vue'
import SqlHighlight from '@/components/SqlHighlight.vue'
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

// 监听抽屉打开状态，加载数据
watch(() => props.modelValue, (newVal) => {
  if (newVal) {
    loadReport()
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
  background: linear-gradient(135deg, #fee 0%, #fdd 100%);
  border: 1px solid #f56c6c;
}

.vital-warning {
  background: linear-gradient(135deg, #fef9e7 0%, #fdf5e6 100%);
  border: 1px solid #e6a23c;
}

.vital-success {
  background: linear-gradient(135deg, #f0f9ff 0%, #e6f7ff 100%);
  border: 1px solid #67c23a;
}

.vital-info {
  background: linear-gradient(135deg, #f4f4f5 0%, #e9e9eb 100%);
  border: 1px solid #909399;
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
</style>
