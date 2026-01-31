<template>
  <div class="system-monitor">
    <el-badge :value="pendingTasks" :hidden="pendingTasks === 0" type="danger">
      <el-button circle @click="showDetail = true" :title="'系统状态'">
        <el-icon><Monitor /></el-icon>
      </el-button>
    </el-badge>

    <el-drawer v-model="showDetail" title="系统监控" size="400px" direction="rtl">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="待分析任务">
          <el-tag :type="pendingTasks > 10 ? 'danger' : 'info'">
            {{ pendingTasks }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="正在分析">
          <el-tag type="warning">{{ processingTasks }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="AI 服务状态">
          <el-tag :type="aiOnline ? 'success' : 'danger'">
            {{ aiOnline ? '🟢 在线' : '🔴 离线' }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>

      <el-divider />

      <h4>队列实时监控</h4>
      <div class="queue-chart">
        <el-progress
          type="circle"
          :percentage="queuePercentage"
          :status="queueStatus"
        >
          <template #default="{ percentage }">
            <span class="percentage-value">{{ percentage }}%</span>
            <span class="percentage-label">队列负载</span>
          </template>
        </el-progress>
      </div>

      <el-divider />

      <h4>系统信息</h4>
      <el-descriptions :column="1" size="small" border>
        <el-descriptions-item label="版本">
          {{ systemInfo.version || 'v2.2.0' }}
        </el-descriptions-item>
        <el-descriptions-item label="构建时间">
          {{ systemInfo.buildTime || '未知' }}
        </el-descriptions-item>
        <el-descriptions-item label="Git 提交">
          {{ systemInfo.gitCommit || 'unknown' }}
        </el-descriptions-item>
      </el-descriptions>

      <div class="actions">
        <el-button type="primary" @click="refresh" :loading="loading">
          <el-icon><Refresh /></el-icon>
          刷新状态
        </el-button>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Monitor, Refresh } from '@element-plus/icons-vue'
import { getQueueStatus } from '@/api/dashboard'
import request from '@/api/index'

const showDetail = ref(false)
const loading = ref(false)
const pendingTasks = ref(0)
const processingTasks = ref(0)
const aiOnline = ref(true)
const systemInfo = ref<any>({})

let refreshTimer: any = null

// 队列负载百分比
const queuePercentage = computed(() => {
  const total = pendingTasks.value + processingTasks.value
  const maxQueue = 50 // 假设队列容量为 50
  return Math.min(Math.round((total / maxQueue) * 100), 100)
})

// 队列状态
const queueStatus = computed(() => {
  if (queuePercentage.value >= 90) return 'exception'
  if (queuePercentage.value >= 70) return 'warning'
  return undefined
})

/**
 * 加载队列状态
 */
async function loadQueueStatus() {
  try {
    const status = await getQueueStatus()
    pendingTasks.value = status.pendingTasks
    processingTasks.value = status.processingTasks
    aiOnline.value = status.aiServiceStatus === 'online'
  } catch (error) {
    console.error('加载队列状态失败:', error)
  }
}

/**
 * 加载系统信息
 */
async function loadSystemInfo() {
  try {
    const result = await request({
      url: '/system/info',
      method: 'get'
    })
    systemInfo.value = result
  } catch (error) {
    console.error('加载系统信息失败:', error)
  }
}

/**
 * 刷新状态
 */
async function refresh() {
  loading.value = true
  try {
    await loadQueueStatus()
    await loadSystemInfo()
    ElMessage.success('状态已刷新')
  } catch (error) {
    ElMessage.error('刷新失败')
  } finally {
    loading.value = false
  }
}

/**
 * 启动定时刷新
 */
function startAutoRefresh() {
  // 每 30 秒刷新一次
  refreshTimer = setInterval(() => {
    loadQueueStatus()
  }, 30000)
}

/**
 * 停止定时刷新
 */
function stopAutoRefresh() {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
}

onMounted(() => {
  loadQueueStatus()
  loadSystemInfo()
  startAutoRefresh()
})

onUnmounted(() => {
  stopAutoRefresh()
})
</script>

<style scoped>
.system-monitor {
  position: fixed;
  top: 20px;
  right: 20px;
  z-index: 1000;
}

.queue-chart {
  display: flex;
  justify-content: center;
  padding: 20px 0;
}

.percentage-value {
  display: block;
  font-size: 28px;
  font-weight: bold;
}

.percentage-label {
  display: block;
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.actions {
  margin-top: 20px;
  text-align: center;
}

h4 {
  margin: 20px 0 10px;
  font-size: 16px;
  font-weight: bold;
}
</style>
