<template>
  <div class="invocation-log-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2>AI 调用流水</h2>
      <p>查看所有 AI 调用的详细日志记录</p>
    </div>

    <!-- 查询条件 -->
    <el-card class="filter-card" shadow="never">
      <el-form :inline="true" :model="queryForm">
        <el-form-item label="Agent">
          <el-select v-model="queryForm.agentName" placeholder="全部" clearable style="width: 150px">
            <el-option label="主治医生" value="DIAGNOSIS" />
            <el-option label="推理专家" value="REASONING" />
            <el-option label="编码专家" value="CODING" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="成功" value="SUCCESS" />
            <el-option label="失败" value="FAILED" />
            <el-option label="超时" value="TIMEOUT" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="dateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 360px"
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
      <template #header>
        <div class="table-header">
          <span>调用记录列表</span>
          <el-tag size="small">共 {{ tableData.length }} 条</el-tag>
        </div>
      </template>
      <el-table
        :data="tableData"
        v-loading="loading"
        stripe
        highlight-current-row
        style="width: 100%"
      >
        <el-table-column prop="startTime" label="调用时间" width="180" fixed />
        <el-table-column prop="agentDisplayName" label="Agent" width="100" />
        <el-table-column prop="modelName" label="模型" width="150" show-overflow-tooltip />
        <el-table-column prop="provider" label="供应商" width="100" />
        <el-table-column prop="durationMs" label="耗时" width="100">
          <template #default="{ row }">
            {{ row.durationDescription }}
          </template>
        </el-table-column>
        <el-table-column prop="totalTokens" label="Token" width="120">
          <template #default="{ row }">
            {{ row.inputTokens }} / {{ row.outputTokens }}
          </template>
        </el-table-column>
        <el-table-column prop="statusDisplayName" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.statusCode)" size="small">
              {{ row.statusDisplayName }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="traceId" label="SQL 指纹" min-width="200" show-overflow-tooltip />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleViewDetail(row)">
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 详情弹窗 -->
    <el-dialog
      v-model="detailVisible"
      title="AI 调用详情"
      width="800px"
      destroy-on-close
    >
      <el-descriptions :column="2" border v-if="currentDetail.id">
        <el-descriptions-item label="调用时间">
          {{ currentDetail.startTime }}
        </el-descriptions-item>
        <el-descriptions-item label="Agent">
          {{ currentDetail.agentDisplayName }}
        </el-descriptions-item>
        <el-descriptions-item label="模型">
          {{ currentDetail.modelName }}
        </el-descriptions-item>
        <el-descriptions-item label="供应商">
          {{ currentDetail.provider }}
        </el-descriptions-item>
        <el-descriptions-item label="耗时">
          {{ currentDetail.durationDescription }}
        </el-descriptions-item>
        <el-descriptions-item label="Token">
          {{ currentDetail.inputTokens }} / {{ currentDetail.outputTokens }} / {{ currentDetail.totalTokens }}
        </el-descriptions-item>
        <el-descriptions-item label="状态" :span="2">
          <el-tag :type="getStatusType(currentDetail.statusCode)">
            {{ currentDetail.statusDisplayName }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="SQL 指纹" :span="2">
          <el-text class="fingerprint-text">{{ currentDetail.traceId }}</el-text>
        </el-descriptions-item>
        <el-descriptions-item v-if="currentDetail.errorMessage" label="错误信息" :span="2">
          <el-text type="danger">{{ currentDetail.errorMessage }}</el-text>
        </el-descriptions-item>
        <el-descriptions-item v-if="currentDetail.errorCategory" label="错误分类">
          {{ currentDetail.errorCategoryDisplayName }}
        </el-descriptions-item>
      </el-descriptions>

      <!-- 提示词和响应（如果有） -->
      <el-tabs v-if="currentDetail.promptText || currentDetail.responseText" class="detail-tabs">
        <el-tab-pane label="提示词" name="prompt">
          <el-input
            type="textarea"
            :model-value="currentDetail.promptText"
            :rows="10"
            readonly
          />
        </el-tab-pane>
        <el-tab-pane label="响应" name="response">
          <el-input
            type="textarea"
            :model-value="currentDetail.responseText"
            :rows="10"
            readonly
          />
        </el-tab-pane>
      </el-tabs>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, RefreshLeft } from '@element-plus/icons-vue'
import { queryAiInvocations } from '@/api/ai-monitor'
import type { AiInvocationDetail, QueryParams } from './types'
import { AGENT_NAME_MAP, STATUS_NAME_MAP, ERROR_CATEGORY_NAME_MAP } from './types'

// 查询表单
const queryForm = ref<QueryParams>({})
const dateRange = ref<string[]>([])
const loading = ref(false)
const tableData = ref<AiInvocationDetail[]>([])

// 详情弹窗
const detailVisible = ref(false)
const currentDetail = ref<AiInvocationDetail>({} as any)

/**
 * 查询数据
 */
async function handleQuery() {
  loading.value = true
  try {
    const params: QueryParams = {
      agentName: queryForm.value.agentName,
      status: queryForm.value.status
    }

    if (dateRange.value && dateRange.value.length === 2) {
      params.startTime = dateRange.value[0]
      params.endTime = dateRange.value[1]
    }

    tableData.value = await queryAiInvocations(params)
    ElMessage.success(`查询成功，共 ${tableData.value.length} 条记录`)
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
  queryForm.value = {}
  dateRange.value = []
  handleQuery()
}

/**
 * 查看详情
 */
function handleViewDetail(row: AiInvocationDetail) {
  currentDetail.value = row
  detailVisible.value = true
}

/**
 * 获取状态标签类型
 */
function getStatusType(status: string): string {
  if (status === 'SUCCESS') return 'success'
  if (status === 'TIMEOUT') return 'warning'
  if (status === 'FAILED') return 'danger'
  return 'info'
}

onMounted(() => {
  handleQuery()
})
</script>

<style scoped>
.invocation-log-page {
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

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.fingerprint-text {
  font-family: monospace;
  font-size: 12px;
  word-break: break-all;
}

.detail-tabs {
  margin-top: 20px;
}
</style>
