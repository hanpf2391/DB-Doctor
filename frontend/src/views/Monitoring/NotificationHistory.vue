<template>
  <div class="notification-history">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1>通知历史</h1>
      <el-button @click="refreshData" :loading="loading">
        <el-icon><Refresh /></el-icon> 刷新
      </el-button>
    </div>

    <!-- 筛选器 -->
    <el-form :inline="true" class="filter-form">
      <el-form-item label="状态">
        <el-select v-model="filters.status" placeholder="全部" clearable style="width: 120px">
          <el-option label="成功" value="SUCCESS" />
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
        <el-button type="primary" @click="handleQuery">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 数据表格 -->
    <el-table :data="tableData" v-loading="loading" stripe>
      <el-table-column prop="triggerTime" label="触发时间" width="180" />
      <el-table-column label="时间窗口" width="200">
        <template #default="{ row }">
          {{ formatTimeRange(row.windowStart, row.windowEnd) }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'SUCCESS' ? 'success' : 'danger'">
            {{ row.status === 'SUCCESS' ? '成功' : '失败' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="waitingCount" label="等待数量" width="100" align="center" />
      <el-table-column prop="sentCount" label="发送数量" width="100" align="center" />
      <el-table-column label="耗时" width="120" align="right">
        <template #default="{ row }">
          {{ (row.durationMs / 1000).toFixed(2) }}s
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100" fixed="right" align="center">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleViewDetail(row)">
            详情
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
        @current-change="handlePageChange"
        layout="total, prev, pager, next, jumper"
      />
    </div>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailVisible" title="通知详情" width="600px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="执行ID">{{ detailData.executionId }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="detailData.status === 'SUCCESS' ? 'success' : 'danger'">
            {{ detailData.status === 'SUCCESS' ? '成功' : '失败' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="触发时间">{{ formatDateTime(detailData.triggerTime) }}</el-descriptions-item>
        <el-descriptions-item label="耗时">{{ (detailData.durationMs / 1000).toFixed(2) }}s</el-descriptions-item>
        <el-descriptions-item label="时间窗口" :span="2">
          {{ formatTimeRange(detailData.windowStart, detailData.windowEnd) }}
        </el-descriptions-item>
        <el-descriptions-item label="等待数量">{{ detailData.waitingCount }}</el-descriptions-item>
        <el-descriptions-item label="发送数量">{{ detailData.sentCount }}</el-descriptions-item>
        <el-descriptions-item v-if="detailData.failedChannels" label="失败渠道" :span="2">
          <el-tag type="danger">{{ detailData.failedChannels }}</el-tag>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { notificationScheduleLogApi } from '@/api/notification'

// 响应式数据
const loading = ref(false)
const tableData = ref<any[]>([])
const detailVisible = ref(false)
const detailData = ref<any>({})

const filters = ref({
  status: ''
})

const dateRange = ref<[string, string] | null>(null)

const pagination = ref({
  page: 1,
  size: 20,
  total: 0
})

// 格式化时间
const formatDateTime = (time: string) => {
  if (!time) return '-'
  return time.replace('T', ' ').substring(0, 19)
}

// 格式化时间范围
const formatTimeRange = (start: string, end: string) => {
  if (!start || !end) return '-'
  const startDate = formatDateTime(start).substring(11, 16)
  const endDate = formatDateTime(end).substring(11, 16)
  return `${startDate} - ${endDate}`
}

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    const res = await notificationScheduleLogApi.getLogs({
      page: pagination.value.page - 1,
      size: pagination.value.size,
      status: filters.value.status || undefined,
      startDate: dateRange.value?.[0],
      endDate: dateRange.value?.[1]
    })

    if (res.code === 'SUCCESS') {
      tableData.value = res.data.list
      pagination.value.total = res.data.total
    } else {
      ElMessage.error(res.message || '加载数据失败')
    }
  } catch (error: any) {
    console.error('加载通知历史失败:', error)
    ElMessage.error(error.message || '加载数据失败')
  } finally {
    loading.value = false
  }
}

// 刷新数据
const refreshData = () => {
  loadData()
}

// 查询
const handleQuery = () => {
  pagination.value.page = 1
  loadData()
}

// 重置
const handleReset = () => {
  filters.value.status = ''
  dateRange.value = null
  pagination.value.page = 1
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

// 初始化
onMounted(() => {
  loadData()
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
}
</style>
