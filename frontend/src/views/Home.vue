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
        <el-form-item label="数据库">
          <el-select v-model="filters.dbName" placeholder="全部" clearable>
            <el-option label="test" value="test" />
            <el-option label="production_db" value="production_db" />
          </el-select>
        </el-form-item>

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
            <el-tag :type="getSeverityTagType(scope.row.severityLevel)">
              {{ getSeverityText(scope.row.severityLevel) }}
            </el-tag>
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
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="scope">
            <el-button link type="primary" @click="viewReport(scope.row)">
              <el-icon><View /></el-icon>
              查看报告
            </el-button>
            <el-button
              link
              type="success"
              :loading="scope.row.reanalyzing"
              @click="handleReanalyze(scope.row)"
            >
              <el-icon><RefreshRight /></el-icon>
              重新诊断
            </el-button>
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
  dbName: '',
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
  filters.dbName = ''
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
 * 获取严重程度 Tag 类型
 */
function getSeverityTagType(severity: string): 'success' | 'warning' | 'danger' | 'info' {
  if (severity.includes('严重')) return 'danger'
  if (severity.includes('警告')) return 'warning'
  if (severity.includes('注意')) return 'info'
  return 'success'
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
  color: #606266;
  background-color: #f5f7fa;
  padding: 2px 6px;
  border-radius: 3px;
}
</style>
