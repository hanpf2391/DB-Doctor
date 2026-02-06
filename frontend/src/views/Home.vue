<template>
  <div class="reports-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <h3>慢查询报表</h3>
          <el-button type="primary" @click="loadReports">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </div>
      </template>

      <!-- 筛选器 -->
      <el-form :inline="true" class="filter-form">
        <el-form-item label="严重程度">
          <el-select v-model="filters.severity" placeholder="全部" clearable>
            <el-option label="严重" value="CRITICAL" />
            <el-option label="警告" value="WARNING" />
            <el-option label="注意" value="NOTE" />
            <el-option label="正常" value="NORMAL" />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="applyFilters">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 报表列表 -->
      <el-table :data="reports" v-loading="loading" stripe>
        <!-- 指纹列 -->
        <el-table-column label="指纹" width="180">
          <template #default="scope">
            <div class="fingerprint-cell">
              <code class="fingerprint-text">{{ scope.row.fingerprint?.substring(0, 8) }}...</code>
              <el-button
                link
                type="primary"
                :icon="DocumentCopy"
                @click="copyFingerprint(scope.row.fingerprint)"
              />
            </div>
          </template>
        </el-table-column>

        <!-- SQL 模板列 -->
        <el-table-column label="SQL 模板" min-width="300">
          <template #default="scope">
            <SqlTooltip :sql="scope.row.sqlTemplate" :max-length="80" />
          </template>
        </el-table-column>

        <!-- 数据库列 -->
        <el-table-column prop="dbName" label="数据库" width="120" />

        <!-- 表名列 -->
        <el-table-column prop="tableName" label="表名" width="120" />

        <!-- 平均耗时列 -->
        <el-table-column prop="avgQueryTime" label="平均耗时" width="120" sortable>
          <template #default="scope">
            <el-tag :type="getQueryTimeType(scope.row.avgQueryTime)">
              {{ formatSeconds(scope.row.avgQueryTime) }}
            </el-tag>
          </template>
        </el-table-column>

        <!-- 出现次数列 -->
        <el-table-column prop="occurrenceCount" label="出现次数" width="100" sortable />

        <!-- 严重程度列 -->
        <el-table-column prop="severityLevel" label="严重程度" width="100">
          <template #default="scope">
            <span class="severity-badge" :class="getSeverityClass(scope.row.severityLevel)">
              {{ getSeverityText(scope.row.severityLevel) }}
            </span>
          </template>
        </el-table-column>

        <!-- 状态列 -->
        <el-table-column prop="analysisStatus" label="状态" width="150">
          <template #default="scope">
            <StatusTag :status="scope.row.analysisStatus" />
          </template>
        </el-table-column>

        <!-- 最后出现时间列 -->
        <el-table-column prop="lastSeenTime" label="最后出现" width="170" />

        <!-- 操作列 -->
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="scope">
            <div class="action-buttons">
              <button
                class="action-btn btn-primary"
                @click="viewReport(scope.row)"
                title="查看报告详情"
              >
                <svg viewBox="0 0 16 16" fill="currentColor" class="btn-icon">
                  <path d="M8 0a8 8 0 1 1 0 16A8 8 0 0 1 8 0zm.5 11.5a.5.5 0 0 0 0 1h5a.5.5 0 0 0 0-1h-5zm0-3a.5.5 0 0 0 0 1h5a.5.5 0 0 0 0-1h-5zm0-3a.5.5 0 0 0 0 1h5a.5.5 0 0 0 0-1h-5zM3.5 3a.5.5 0 0 0-.5.5v9a.5.5 0 0 0 .5.5h2a.5.5 0 0 0 .5-.5v-9a.5.5 0 0 0-.5-.5h-2z"/>
                </svg>
                <span>查看报告</span>
              </button>
              <button
                class="action-btn btn-success"
                :class="{ 'is-loading': scope.row.reanalyzing }"
                :disabled="scope.row.reanalyzing"
                @click="handleReanalyze(scope.row)"
                title="重新分析"
              >
                <svg v-if="!scope.row.reanalyzing" viewBox="0 0 16 16" fill="currentColor" class="btn-icon">
                  <path d="M8 3a5 5 0 1 0 4.546 2.914.5.5 0 0 1 .908-.417A6 6 0 1 1 8 2v1z"/>
                  <path d="M8 4.466V.534a.25.25 0 0 1 .41-.192l2.36 1.966c.12.1.12.284 0 .384L8.41 4.658A.25.25 0 0 1 8 4.466z"/>
                </svg>
                <svg v-else viewBox="0 0 16 16" fill="currentColor" class="btn-icon spin">
                  <path d="M11.534 7h3.932a.25.25 0 0 1 .192.41l-1.966 2.36a.25.25 0 0 1-.384 0l-1.966-2.36a.25.25 0 0 1 .192-.41zm-11 2h3.932a.25.25 0 0 0 .192-.41L2.692 6.23a.25.25 0 0 0-.384 0L.342 8.59A.25.25 0 0 0 .534 9z"/>
                  <path fill-rule="evenodd" d="M8 3c-1.552 0-2.94.707-3.857 1.818a.5.5 0 1 1-.771-.636A6.002 6.002 0 0 1 13.917 7H12.9A5.002 5.002 0 0 0 8 3zM3.1 9a5.002 5.002 0 0 0 8.757 2.182.5.5 0 1 1 .771.636A6.002 6.002 0 0 1 2.083 9H3.1z"/>
                </svg>
                <span>{{ scope.row.reanalyzing ? '分析中...' : '重新分析' }}</span>
              </button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadReports"
        @current-change="loadReports"
        style="margin-top: 20px"
      />
    </el-card>

    <!-- 报告详情抽屉 -->
    <ReportDetail
      v-model="showReportDetail"
      :report-id="selectedReportId"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { DocumentCopy, View, RefreshRight, Refresh } from '@element-plus/icons-vue'
import { getReports, reanalyzeReport } from '@/api/config'
import ReportDetail from './ReportDetail/ReportDetail.vue'
import StatusTag from '@/components/StatusTag.vue'
import SqlTooltip from '@/components/SqlTooltip.vue'
import { formatSeconds } from '@/utils/format'

const loading = ref(false)
const reports = ref<any[]>([])
const showReportDetail = ref(false)
const selectedReportId = ref(0)

const filters = reactive({
  severity: ''
})

const pagination = reactive({
  page: 1,
  size: 20,
  total: 0
})

/**
 * 加载慢查询报表列表
 */
async function loadReports() {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      size: pagination.size,
      ...filters
    }

    const result = await getReports(params)
    reports.value = (result.records || []).map((r: any) => ({ ...r, reanalyzing: false }))
    pagination.total = result.total || 0
  } catch (error) {
    ElMessage.error('加载报表失败')
  } finally {
    loading.value = false
  }
}

/**
 * 应用筛选条件
 */
function applyFilters() {
  pagination.page = 1
  loadReports()
}

/**
 * 重置筛选条件
 */
function resetFilters() {
  filters.severity = ''
  applyFilters()
}

/**
 * 查看报告详情
 */
function viewReport(row: any) {
  selectedReportId.value = row.id
  showReportDetail.value = true
}

/**
 * 复制指纹
 */
async function copyFingerprint(fingerprint: string) {
  try {
    await navigator.clipboard.writeText(fingerprint)
    ElMessage.success('已复制到剪贴板')
  } catch (error) {
    ElMessage.error('复制失败')
  }
}

/**
 * 重新诊断
 */
async function handleReanalyze(row: any) {
  row.reanalyzing = true
  try {
    await reanalyzeReport(row.id)
    ElMessage.success('已提交重新分析')
    // 延迟刷新，让用户看到成功提示
    setTimeout(() => {
      loadReports()
    }, 1000)
  } catch (error: any) {
    ElMessage.error(error.message || '重新分析失败')
  } finally {
    row.reanalyzing = false
  }
}

/**
 * 获取耗时 Tag 类型
 */
function getQueryTimeType(time: number): 'success' | 'warning' | 'danger' {
  if (time > 2.0) return 'danger'
  if (time > 1.0) return 'warning'
  return 'success'
}

/**
 * 获取严重程度样式类
 */
function getSeverityClass(severity: string): string {
  if (severity.includes('严重')) return 'severity-critical'
  if (severity.includes('警告')) return 'severity-warning'
  if (severity.includes('注意')) return 'severity-note'
  return 'severity-normal'
}

/**
 * 获取严重程度文本
 */
function getSeverityText(severity: string): string {
  if (severity.includes('严重')) return '严重'
  if (severity.includes('警告')) return '警告'
  if (severity.includes('注意')) return '注意'
  if (severity.includes('正常')) return '正常'
  return severity
}

onMounted(() => {
  loadReports()
})
</script>

<style scoped>
.reports-page {
  max-width: 1400px;
  margin: 0 auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h3 {
  margin: 0;
}

.filter-form {
  margin-bottom: 20px;
}

.fingerprint-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.fingerprint-text {
  font-family: 'Courier New', monospace;
  font-size: 13px;
  color: var(--color-text-primary, #606266);
  background-color: var(--color-bg-secondary, #f5f7fa);
  padding: 2px 6px;
  border-radius: 3px;
}

/* 暗色主题下的指纹文本 */
[data-theme="dark"] .fingerprint-text {
  color: #f3f4f6;
  background-color: #374151;
}

/* 严重程度标签 - 优化后的样式 */
.severity-badge {
  display: inline-block;
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
  letter-spacing: 0.3px;
}

/* 浅色主题 */
.severity-critical {
  background-color: #fef2f2;
  color: #dc2626;
  border: 1px solid #fecaca;
}

.severity-warning {
  background-color: #fffbeb;
  color: #d97706;
  border: 1px solid #fde68a;
}

.severity-note {
  background-color: #eff6ff;
  color: #2563eb;
  border: 1px solid #bfdbfe;
}

.severity-normal {
  background-color: #f0fdf4;
  color: #16a34a;
  border: 1px solid #bbf7d0;
}

/* 暗色主题下的严重程度标签 - 柔和的颜色 */
[data-theme="dark"] .severity-critical {
  background-color: rgba(248, 113, 113, 0.08);
  color: #fca5a5;
  border: 1px solid rgba(248, 113, 113, 0.2);
}

[data-theme="dark"] .severity-warning {
  background-color: rgba(251, 191, 36, 0.08);
  color: #fcd34d;
  border: 1px solid rgba(251, 191, 36, 0.2);
}

[data-theme="dark"] .severity-note {
  background-color: rgba(96, 165, 250, 0.08);
  color: #93c5fd;
  border: 1px solid rgba(96, 165, 250, 0.2);
}

[data-theme="dark"] .severity-normal {
  background-color: rgba(74, 222, 128, 0.08);
  color: #86efac;
  border: 1px solid rgba(74, 222, 128, 0.2);
}

/* 表格行 hover 效果 */
.reports-page :deep(.el-table__body tr:hover > td) {
  background-color: var(--color-bg-hover, #f5f7fa) !important;
}

[data-theme="dark"] .reports-page :deep(.el-table__body tr:hover > td) {
  background-color: #374151 !important;
}

/* 筛选表单标签样式优化 */
.filter-form :deep(.el-form-item__label) {
  font-size: 13px;
  color: var(--color-text-secondary, #6b7280);
  font-weight: 500;
}

[data-theme="dark"] .filter-form :deep(.el-form-item__label) {
  color: #d1d5db;
}

/* ============================================
   下拉选择 - Notion 风格
============================================ */

.filter-form :deep(.el-select) {
  width: 160px;
}

/* Select 输入框 */
.filter-form :deep(.el-select__wrapper) {
  border-radius: 6px;
  border: 1px solid #e5e7eb;
  box-shadow: none;
  transition: all 0.2s ease;
}

.filter-form :deep(.el-select__wrapper:hover) {
  border-color: #d1d5db;
}

.filter-form :deep(.el-select__wrapper.is-focused) {
  border-color: #7c3aed;
  box-shadow: 0 0 0 2px rgba(139, 92, 246, 0.1);
}

.filter-form :deep(.el-select__placeholder) {
  color: #9ca3af;
  font-size: 13px;
}

.filter-form :deep(.el-select__selected-item) {
  color: #374151;
  font-size: 13px;
}

.filter-form :deep(.el-select__input) {
  font-size: 13px;
}

/* 下拉箭头 */
.filter-form :deep(.el-select__caret) {
  color: #9ca3af;
}

/* 下拉面板 */
.filter-form :deep(.el-select-dropdown) {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  padding: 4px;
}

.filter-form :deep(.el-select-dropdown__item) {
  border-radius: 4px;
  padding: 8px 12px;
  font-size: 13px;
  color: #374151;
  transition: all 0.15s ease;
}

.filter-form :deep(.el-select-dropdown__item:hover) {
  background: #f3f4f6;
}

.filter-form :deep(.el-select-dropdown__item.is-selected) {
  background: rgba(139, 92, 246, 0.08);
  color: #7c3aed;
  font-weight: 500;
}

.filter-form :deep(.el-select-dropdown__item.is-selected:hover) {
  background: rgba(139, 92, 246, 0.12);
}

/* 暗色主题 */
[data-theme="dark"] .filter-form :deep(.el-select__wrapper) {
  border-color: rgba(255, 255, 255, 0.15);
}

[data-theme="dark"] .filter-form :deep(.el-select__wrapper:hover) {
  border-color: rgba(255, 255, 255, 0.25);
}

[data-theme="dark"] .filter-form :deep(.el-select__wrapper.is-focused) {
  border-color: #8b5cf6;
  box-shadow: 0 0 0 2px rgba(139, 92, 246, 0.2);
}

[data-theme="dark"] .filter-form :deep(.el-select__placeholder) {
  color: #6b7280;
}

[data-theme="dark"] .filter-form :deep(.el-select__selected-item) {
  color: #e5e7eb;
}

[data-theme="dark"] .filter-form :deep(.el-select__caret) {
  color: #6b7280;
}

[data-theme="dark"] .filter-form :deep(.el-select-dropdown) {
  background: #000000;
  border-color: rgba(255, 255, 255, 0.15);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.4);
}

[data-theme="dark"] .filter-form :deep(.el-select-dropdown__item) {
  color: #e5e7eb;
}

[data-theme="dark"] .filter-form :deep(.el-select-dropdown__item:hover) {
  background: rgba(255, 255, 255, 0.05);
}

[data-theme="dark"] .filter-form :deep(.el-select-dropdown__item.is-selected) {
  background: rgba(139, 92, 246, 0.15);
  color: #a78bfa;
}

[data-theme="dark"] .filter-form :deep(.el-select-dropdown__item.is-selected:hover) {
  background: rgba(139, 92, 246, 0.2);
}

/* ============================================
   操作按钮 - 商业化设计
   设计灵感：Linear, Vercel, Stripe
============================================ */

.action-buttons {
  display: flex;
  align-items: center;
  gap: 8px;
}

.action-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border: none;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  white-space: nowrap;
  background: transparent;
}

.action-btn .btn-icon {
  width: 14px;
  height: 14px;
  flex-shrink: 0;
}

.action-btn span {
  font-size: 13px;
}

/* 主要按钮 - 蓝色 */
.btn-primary {
  color: #2563eb;
  background: rgba(37, 99, 235, 0.08);
}

.btn-primary:hover {
  background: rgba(37, 99, 235, 0.15);
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.15);
}

.btn-primary:active {
  transform: translateY(0);
}

/* 成功按钮 - 绿色 */
.btn-success {
  color: #059669;
  background: rgba(5, 150, 105, 0.08);
}

.btn-success:hover {
  background: rgba(5, 150, 105, 0.15);
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(5, 150, 105, 0.15);
}

.btn-success:active {
  transform: translateY(0);
}

/* 禁用状态 */
.action-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none !important;
}

/* 加载状态 */
.action-btn.is-loading {
  cursor: wait;
}

.action-btn.is-loading .btn-icon.spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

/* ============================================
   暗色主题 - 操作按钮
============================================ */

[data-theme="dark"] .btn-primary {
  color: #60a5fa;
  background: rgba(59, 130, 246, 0.12);
}

[data-theme="dark"] .btn-primary:hover {
  background: rgba(59, 130, 246, 0.2);
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.25);
}

[data-theme="dark"] .btn-success {
  color: #34d399;
  background: rgba(16, 185, 129, 0.12);
}

[data-theme="dark"] .btn-success:hover {
  background: rgba(16, 185, 129, 0.2);
  box-shadow: 0 2px 8px rgba(16, 185, 129, 0.25);
}
</style>
