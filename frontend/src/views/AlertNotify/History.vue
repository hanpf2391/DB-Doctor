<template>
  <div class="alert-history">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1>告警历史</h1>
      <el-button @click="refreshData" :loading="loading">
        <el-icon><Refresh /></el-icon> 刷新
      </el-button>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-cards">
      <el-card class="stat-card" shadow="hover">
        <div class="stat-value">{{ stats.total || 0 }}</div>
        <div class="stat-label">总告警数</div>
      </el-card>
      <el-card class="stat-card critical" shadow="hover">
        <div class="stat-value">{{ stats.severity_CRITICAL || 0 }}</div>
        <div class="stat-label">严重告警</div>
      </el-card>
      <el-card class="stat-card warning" shadow="hover">
        <div class="stat-value">{{ stats.severity_WARNING || 0 }}</div>
        <div class="stat-label">警告告警</div>
      </el-card>
      <el-card class="stat-card success" shadow="hover">
        <div class="stat-value">{{ stats.RESOLVED || 0 }}</div>
        <div class="stat-label">已解决</div>
      </el-card>
    </div>

    <!-- 搜索筛选 -->
    <el-card class="filter-card" shadow="never">
      <el-form :model="filters" inline>
        <el-form-item label="严重程度">
          <el-select v-model="filters.severity" placeholder="全部" clearable>
            <el-option label="严重" value="CRITICAL" />
            <el-option label="警告" value="WARNING" />
            <el-option label="信息" value="INFO" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.status" placeholder="全部" clearable>
            <el-option label="触发中" value="FIRING" />
            <el-option label="已解决" value="RESOLVED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadAlerts">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 告警列表 -->
    <el-card class="table-card" shadow="never">
      <el-table
        :data="alerts"
        v-loading="loading"
        stripe
        @row-click="showAlertDetail"
      >
        <el-table-column prop="ruleName" label="告警规则" width="200" />
        <el-table-column prop="severity" label="严重程度" width="120">
          <template #default="{ row }">
            <el-tag :type="getSeverityTagType(row.severity)">
              {{ row.severity }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="message" label="告警内容" show-overflow-tooltip />
        <el-table-column prop="triggeredAt" label="触发时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.triggeredAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'FIRING'"
              type="success"
              size="small"
              @click.stop="resolveAlert(row)"
            >
              标记解决
            </el-button>
            <el-button
              type="primary"
              size="small"
              @click.stop="showAlertDetail(row)"
            >
              查看详情
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
        @size-change="loadAlerts"
        @current-change="loadAlerts"
        style="margin-top: 20px; justify-content: center"
      />
    </el-card>

    <!-- 告警详情弹窗 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="告警详情"
      width="600px"
    >
      <div v-if="selectedAlert">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="告警规则">
            {{ selectedAlert.ruleName }}
          </el-descriptions-item>
          <el-descriptions-item label="严重程度">
            <el-tag :type="getSeverityTagType(selectedAlert.severity)">
              {{ selectedAlert.severity }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusTagType(selectedAlert.status)">
              {{ getStatusText(selectedAlert.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="触发时间">
            {{ formatTime(selectedAlert.triggeredAt) }}
          </el-descriptions-item>
          <el-descriptions-item label="告警内容">
            <pre style="white-space: pre-wrap;">{{ selectedAlert.message }}</pre>
          </el-descriptions-item>
          <el-descriptions-item label="指标数据" v-if="selectedAlert.metrics">
            <pre>{{ formatJson(selectedAlert.metrics) }}</pre>
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { alertApi } from '@/api/monitoring'

// 响应式数据
const loading = ref(false)
const alerts = ref<any[]>([])
const stats = ref<any>({})
const filters = ref({
  severity: '',
  status: ''
})
const pagination = ref({
  page: 1,
  size: 20,
  total: 0
})
const detailDialogVisible = ref(false)
const selectedAlert = ref<any>(null)

// 格式化时间
const formatTime = (time: string) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

// 格式化 JSON
const formatJson = (jsonStr: string) => {
  try {
    return JSON.stringify(JSON.parse(jsonStr), null, 2)
  } catch {
    return jsonStr
  }
}

// 获取严重程度标签类型
const getSeverityTagType = (severity: string) => {
  const map: Record<string, any> = {
    'CRITICAL': 'danger',
    'WARNING': 'warning',
    'INFO': 'info'
  }
  return map[severity] || ''
}

// 获取状态标签类型
const getStatusTagType = (status: string) => {
  return status === 'FIRING' ? 'danger' : 'success'
}

// 获取状态文本
const getStatusText = (status: string) => {
  return status === 'FIRING' ? '触发中' : '已解决'
}

// 加载告警列表
const loadAlerts = async () => {
  loading.value = true
  try {
    const res = await alertApi.getAlerts({
      page: pagination.value.page - 1,
      size: pagination.value.size,
      ...filters.value
    })

    if (res.code === 'SUCCESS') {
      alerts.value = res.data.list || []
      pagination.value.total = res.data.total || 0
    }
  } catch (error: any) {
    console.error('加载告警列表失败:', error)
    ElMessage.error(error.message || '加载失败')
  } finally {
    loading.value = false
  }
}

// 加载统计数据
const loadStats = async () => {
  try {
    const res = await alertApi.getAlertStats()
    if (res.code === 'SUCCESS') {
      stats.value = res.data || {}
    }
  } catch (error: any) {
    console.error('加载统计数据失败:', error)
  }
}

// 解决告警
const resolveAlert = async (alert: any) => {
  try {
    await ElMessageBox.confirm(
      `确认标记告警 "${alert.ruleName}" 为已解决？`,
      '确认操作',
      {
        type: 'warning'
      }
    )

    const res = await alertApi.resolveAlert(alert.id, {
      resolvedBy: 'admin'
    })

    if (res.code === 'SUCCESS') {
      ElMessage.success('已标记为解决')
      loadAlerts()
      loadStats()
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('操作失败:', error)
      ElMessage.error(error.message || '操作失败')
    }
  }
}

// 显示告警详情
const showAlertDetail = (alert: any) => {
  selectedAlert.value = alert
  detailDialogVisible.value = true
}

// 刷新数据
const refreshData = () => {
  loadAlerts()
  loadStats()
}

// 重置筛选
const resetFilters = () => {
  filters.value = {
    severity: '',
    status: ''
  }
  pagination.value.page = 1
  loadAlerts()
}

onMounted(() => {
  loadAlerts()
  loadStats()
})
</script>

<style scoped lang="scss">
.alert-history {
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
  }

  .stats-cards {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
    gap: 20px;
    margin-bottom: 20px;
  }

  .stat-card {
    text-align: center;

    .stat-value {
      font-size: 32px;
      font-weight: bold;
      color: var(--el-color-primary);
    }

    &.critical .stat-value {
      color: #F56C6C;
    }

    &.warning .stat-value {
      color: #E6A23C;
    }

    &.success .stat-value {
      color: #67C23A;
    }

    .stat-label {
      margin-top: 10px;
      color: var(--el-text-color-secondary);
    }
  }

  .filter-card {
    margin-bottom: 20px;
  }

  .table-card {
    pre {
      background: #f5f7fa;
      padding: 10px;
      border-radius: 4px;
      font-size: 12px;
    }
  }
}
</style>
