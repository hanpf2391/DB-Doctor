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
            <el-option label="🔴 严重" value="critical" />
            <el-option label="🟠 警告" value="warning" />
            <el-option label="🟢 正常" value="normal" />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="applyFilters">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 报表列表 -->
      <el-table :data="reports" v-loading="loading" stripe>
        <el-table-column prop="fingerprint" label="指纹" width="200" show-overflow-tooltip />
        <el-table-column prop="dbName" label="数据库" width="120" />
        <el-table-column prop="tableName" label="表名" width="120" />
        <el-table-column prop="avgQueryTime" label="平均耗时(秒)" width="120">
          <template #default="scope">
            <el-tag v-if="scope.row.avgQueryTime > 5" type="danger">
              {{ scope.row.avgQueryTime }}
            </el-tag>
            <el-tag v-else-if="scope.row.avgQueryTime > 3" type="warning">
              {{ scope.row.avgQueryTime }}
            </el-tag>
            <el-tag v-else type="success">
              {{ scope.row.avgQueryTime }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="occurrenceCount" label="出现次数" width="100" />
        <el-table-column prop="severityLevel" label="严重程度" width="100" />
        <el-table-column prop="analysisStatus" label="分析状态" width="120">
          <template #default="scope">
            <el-tag v-if="scope.row.analysisStatus === 'COMPLETED'" type="success">
              已完成
            </el-tag>
            <el-tag v-else-if="scope.row.analysisStatus === 'PENDING'" type="warning">
              等待中
            </el-tag>
            <el-tag v-else type="info">
              {{ scope.row.analysisStatus }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastSeenTime" label="最后出现时间" width="180" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="scope">
            <el-button link type="primary" @click="viewReport(scope.row)">
              查看报告
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

    <!-- 报告详情对话框 -->
    <el-dialog
      v-model="showReportDetail"
      title="慢查询分析报告"
      width="80%"
      top="5vh"
    >
      <div v-html="renderedReport" class="report-content"></div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { marked } from 'marked'
import { getReports, getReportDetail } from '@/api/config'

const loading = ref(false)
const reports = ref([])
const showReportDetail = ref(false)
const renderedReport = ref('')

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
    reports.value = result.records || []
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
async function viewReport(row: any) {
  try {
    const result = await getReportDetail(row.id)
    const markdown = result.reportMarkdown || '# 暂无分析报告\n\n该慢查询还没有完成 AI 分析。'

    renderedReport.value = marked(markdown)
    showReportDetail.value = true
  } catch (error) {
    ElMessage.error('加载报告失败')
  }
}

onMounted(() => {
  loadReports()
})
</script>

<style scoped>
.reports-page {
  max-width: 1200px;
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

.report-content {
  max-height: 70vh;
  overflow-y: auto;
}

.report-content :deep(h1),
.report-content :deep(h2),
.report-content :deep(h3) {
  margin-top: 20px;
}

.report-content :deep(pre) {
  background-color: #f5f7fa;
  padding: 15px;
  border-radius: 4px;
}

.report-content :deep(code) {
  background-color: #f5f7fa;
  padding: 2px 6px;
  border-radius: 3px;
}
</style>
