<template>
  <div class="system-health">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1>系统健康监控</h1>
      <el-button @click="refreshData" :loading="loading" link>
        <el-icon><Refresh /></el-icon>
      </el-button>
    </div>

    <!-- 网格布局 -->
    <div class="grid-container">
      <!-- 系统信息卡片 -->
      <div class="card card-blue">
        <div class="card-header">
          <div class="card-icon">
            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" fill="currentColor" viewBox="0 0 256 256"><path d="M208,32H48A16,16,0,0,0,32,48V208a16,16,0,0,0,16,16H208a16,16,0,0,0,16-16V48A16,16,0,0,0,208,32Zm0,176H48V48H208V208ZM184,88a12,12,0,1,1-12-12A12,12,0,0,1,184,88Z"></path></svg>
          </div>
          <h2 class="card-title">系统信息</h2>
        </div>
        <div class="card-content">
          <ul class="details-list">
            <li><span class="label">操作系统</span><span class="value">{{ systemInfo.osName }} {{ systemInfo.osArch }}</span></li>
            <li><span class="label">Java 版本</span><span class="value">{{ systemInfo.javaVersion }}</span></li>
            <li><span class="label">运行时长</span><span class="value">{{ systemInfo.uptime }}</span></li>
            <li><span class="label">启动于</span><span class="value">{{ formatTime(systemInfo.startTime) }}</span></li>
          </ul>
        </div>
      </div>

      <!-- CPU 使用率卡片 -->
      <div class="card card-purple">
        <div class="card-header">
          <div class="card-icon">
            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" fill="currentColor" viewBox="0 0 256 256"><path d="M160,40H96A16,16,0,0,0,80,56v8H48a8,8,0,0,0-8,8V96a8,8,0,0,0,8,8h32v48H48a8,8,0,0,0-8,8v24a8,8,0,0,0,8,8h32v8a16,16,0,0,0,16,16h64a16,16,0,0,0,16-16v-8h32a8,8,0,0,0,8-8V176a8,8,0,0,0-8-8H176V120h32a8,8,0,0,0,8-8V88a8,8,0,0,0-8-8H176V72h-8A16,16,0,0,0,160,56V40Zm0,16v8a8,8,0,0,0,8,8h8v16H160v48h16v16H160v16a8,8,0,0,0,8,8h8v8H96v-8a8,8,0,0,0-8-8H80V160h16V112H80V96h8a8,8,0,0,0,8-8V80h64v16H112v48h32v16h16V56Z"></path></svg>
          </div>
          <h2 class="card-title">CPU 使用率</h2>
        </div>
        <div class="card-content">
          <div class="metric-large">{{ ((metrics.cpuUsage || 0) * 100).toFixed(1) }}<span class="unit">%</span></div>
          <p class="metric-description">逻辑处理器: {{ systemInfo.processors || '-' }}</p>
        </div>
      </div>

      <!-- JVM 内存使用率卡片 -->
      <div class="card card-orange">
        <div class="card-header">
          <div class="card-icon">
            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" fill="currentColor" viewBox="0 0 256 256"><path d="M232,200a8,8,0,0,0-8,8v8a8,8,0,0,1-8,8H40a8,8,0,0,1-8-8v-8a8,8,0,0,0-16,0v8a24,24,0,0,0,24,24H208a24,24,0,0,0,24-24v-8A8,8,0,0,0,232,200ZM176,40a48,48,0,0,0-96,0,8,8,0,0,0,16,0,32,32,0,0,1,64,0,8,8,0,0,0,16,0Zm-48,56a8,8,0,0,0-8,8v56a8,8,0,0,0,16,0V104A8,8,0,0,0,128,96ZM88,96a8,8,0,0,0-8,8v56a8,8,0,0,0,16,0V104A8,8,0,0,0,88,96Zm80,0a8,8,0,0,0-8,8v56a8,8,0,0,0,16,0V104A8,8,0,0,0,168,96Z"></path></svg>
          </div>
          <h2 class="card-title">JVM 内存使用率</h2>
        </div>
        <div class="card-content">
          <div class="metric-large">{{ ((metrics.jvmMemoryUsage || 0) * 100).toFixed(1) }}<span class="unit">%</span></div>
          <p class="metric-description">{{ systemInfo.jvmMemory?.heapMemoryUsed || '-' }} / {{ systemInfo.jvmMemory?.heapMemoryMax || '-' }}</p>
        </div>
      </div>

      <!-- 业务指标卡片 -->
      <div class="card card-green">
        <div class="card-header">
          <div class="card-icon">
            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" fill="currentColor" viewBox="0 0 256 256"><path d="M224,80a8,8,0,0,0-8,8v56.42l-42.26-53.79a8,8,0,0,0-12.42.92l-38,50.64L89.83,92.54a8,8,0,0,0-13.66-6.42L32,154.26V88a8,8,0,0,0-16,0v88a8,8,0,0,0,8,8H216a8,8,0,0,0,8-8V88A8,8,0,0,0,224,80ZM44,176l38.17-57.25L118.66,176Zm164,0H138.54l30.08-40.1L208,179.58Z"></path></svg>
          </div>
          <h2 class="card-title">业务指标</h2>
        </div>
        <div class="card-content">
          <ul class="details-list">
            <li><span class="label">慢查询 QPS</span><span class="value">{{ metrics.slowQueryQps?.toFixed(2) || '0.00' }}</span></li>
            <li><span class="label">AI 分析耗时</span><span class="value">{{ metrics.aiAnalysisDuration?.toFixed(2) || '0.00' }}s</span></li>
            <li><span class="label">队列积压</span><span class="value">{{ metrics.queueBacklog || 0 }}</span></li>
          </ul>
        </div>
      </div>

      <!-- 健康检查卡片 -->
      <div class="card card-red">
        <div class="card-header">
          <div class="card-icon">
            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" fill="currentColor" viewBox="0 0 256 256"><path d="M136,80v40a8,8,0,0,1-16,0V80a8,8,0,0,1,16,0Zm-16,88a12,12,0,1,0-12-12A12,12,0,0,0,120,168Zm104-40A104,104,0,1,1,120,24,104.11,104.11,0,0,1,224,128Zm-16,0a88,88,0,1,0-88,88A88.1,88.1,0,0,0,208,128Z"></path></svg>
          </div>
          <h2 class="card-title">健康检查</h2>
        </div>
        <div class="card-content">
          <ul class="details-list">
            <li v-for="(indicator, key) in healthIndicators" :key="key">
              <span class="label">{{ getIndicatorDisplayName(key) }}</span>
              <span class="value" :class="indicator.healthy ? 'value-healthy' : 'value-warning'">
                {{ indicator.healthy ? '正常' : '异常' }}
              </span>
            </li>
          </ul>
        </div>
      </div>

      <!-- 线程池详情卡片 -->
      <div v-if="healthIndicators.threadPool?.details" class="card card-blue card-wide">
        <div class="card-header">
          <div class="card-icon">
            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" fill="currentColor" viewBox="0 0 256 256"><path d="M232,128A104,104,0,0,1,38.2,173.11l-14.4,28.8A8,8,0,0,0,32,216a8.14,8.14,0,0,0,3.1-.62l30-15A104,104,0,1,1,232,128Zm-16,0A88,88,0,1,0,60.19,183.1,8,8,0,0,1,56,184l-14.19,7.1,6.8-13.6a8,8,0,0,1,1.52-4.11A88,88,0,0,0,216,128Z"></path></svg>
          </div>
          <h2 class="card-title">线程池详情</h2>
        </div>
        <div class="card-content card-content-split">
          <div v-for="(poolData, poolName) in healthIndicators.threadPool.details" :key="poolName" class="pool-section">
            <h3 class="pool-title">{{ poolName }}</h3>
            <ul class="details-list">
              <li><span class="label">活跃/核心</span><span class="value">{{ poolData.activeCount || 0 }} / {{ poolData.corePoolSize || 0 }}</span></li>
              <li><span class="label">队列使用</span><span class="value">{{ poolData.queueSize || 0 }} / {{ poolData.queueCapacity || 0 }}</span></li>
              <li><span class="label">完成任务</span><span class="value">{{ poolData.completedTaskCount || 0 }}</span></li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { systemHealthApi } from '@/api/monitoring'

// 响应式数据
const loading = ref(false)
const systemInfo = ref<any>({})
const healthIndicators = ref<Record<string, any>>({})
const metrics = ref<any>({})

let refreshTimer: number | null = null

// 格式化时间
const formatTime = (time: string) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false
  }).replace(/\//g, '-')
}

// 获取指标显示名称（英文转中文）
const getIndicatorDisplayName = (key: string): string => {
  const nameMap: Record<string, string> = {
    'datasource': '数据源',
    'threadPool': '线程池',
    'diskSpace': '磁盘空间',
    'aiService': 'AI 服务'
  }
  return nameMap[key] || key
}

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    const [healthData, metricsData] = await Promise.all([
      systemHealthApi.getHealthStatus(),
      systemHealthApi.getMetrics()
    ])

    // axios 响应拦截器已经解包了 data，所以直接使用
    if (healthData) {
      systemInfo.value = healthData.systemInfo || {}
      healthIndicators.value = healthData.indicators || {}
    }

    if (metricsData) {
      metrics.value = {
        slowQueryQps: metricsData.slowQueryMetrics?.qps || 0,
        aiAnalysisDuration: metricsData.aiAnalysisMetrics?.durationAvg || 0,
        cpuUsage: metricsData.systemResourceMetrics?.cpuUsage || 0,
        jvmMemoryUsage: metricsData.systemResourceMetrics?.memoryUsage || 0,
        queueBacklog: metricsData.queueBacklog || 0
      }
    }
  } catch (error: any) {
    console.error('加载健康监控数据失败:', error)
    ElMessage.error(error.message || '加载数据失败')
  } finally {
    loading.value = false
  }
}

// 刷新数据
const refreshData = () => {
  loadData()
}

// 页面加载（移除自动刷新）
onMounted(() => {
  loadData()
})

onUnmounted(() => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
  }
})
</script>

<style scoped lang="scss">
.system-health {
  padding: 24px 32px;
  background-color: #f7f8fa;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;

  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 32px;

    h1 {
      font-size: 28px;
      font-weight: 700;
      margin: 0;
      color: #1f2937;
    }
  }

  .grid-container {
    display: grid;
    grid-template-columns: repeat(12, 1fr);
    gap: 24px;
  }

  .card {
    background: #ffffff;
    border-radius: 12px;
    padding: 24px;
    border: 1px solid #e5e7eb;
    transition: transform 0.2s ease-in-out, box-shadow 0.2s ease-in-out;
    display: flex;
    flex-direction: column;
    grid-column: span 12;

    &:hover {
      transform: translateY(-4px);
      box-shadow: 0 10px 15px -3px rgba(99, 102, 241, 0.1), 0 4px 6px -4px rgba(99, 102, 241, 0.1);
    }

    @media (min-width: 768px) {
      &:not(.card-wide) {
        grid-column: span 6;
      }
    }

    @media (min-width: 1024px) {
      &:not(.card-wide) {
        grid-column: span 4;
      }

      &.card-wide {
        grid-column: span 12;
      }
    }
  }

  .card-header {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 20px;
  }

  .card-icon {
    width: 40px;
    height: 40px;
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #fff;
  }

  .card-blue .card-icon {
    background: linear-gradient(135deg, #60a5fa, #3b82f6);
  }

  .card-purple .card-icon {
    background: linear-gradient(135deg, #a78bfa, #7c3aed);
  }

  .card-orange .card-icon {
    background: linear-gradient(135deg, #fbbf24, #f97316);
  }

  .card-green .card-icon {
    background: linear-gradient(135deg, #4ade80, #16a34a);
  }

  .card-red .card-icon {
    background: linear-gradient(135deg, #f87171, #ef4444);
  }

  .card-title {
    font-size: 16px;
    font-weight: 600;
    color: #1f2937;
    margin: 0;
  }

  .card-content {
    flex-grow: 1;
  }

  .details-list {
    list-style: none;
    padding: 0;
    margin: 0;
    display: flex;
    flex-direction: column;
    gap: 12px;

    li {
      display: flex;
      justify-content: space-between;
      align-items: center;
      font-size: 14px;
    }

    .label {
      color: #6b7280;
      display: flex;
      align-items: center;
      gap: 6px;
    }

    .value {
      font-weight: 600;
      background-color: #f7f8fa;
      padding: 2px 8px;
      border-radius: 6px;
      font-family: 'SF Mono', 'Fira Code', 'Fira Mono', 'Roboto Mono', monospace;
      font-size: 13px;
    }

    .value-healthy {
      color: #10b981;
      background-color: #ecfdf5;
    }

    .value-warning {
      color: #f97316;
      background-color: #fffbeb;
    }
  }

  .metric-large {
    font-size: 32px;
    font-weight: 700;
    color: #1f2937;
    display: flex;
    align-items: baseline;

    .unit {
      font-size: 18px;
      font-weight: 500;
      color: #6b7280;
      margin-left: 6px;
    }
  }

  .metric-description {
    font-size: 14px;
    color: #6b7280;
    margin-top: 8px;
    margin-bottom: 0;
  }

  .card-content-split {
    display: flex;
    gap: 24px;

    @media (max-width: 768px) {
      flex-direction: column;
    }
  }

  .pool-section {
    flex: 1;

    &:not(:first-child) {
      border-left: 1px solid #e5e7eb;
      padding-left: 24px;

      @media (max-width: 768px) {
        border-left: none;
        border-top: 1px solid #e5e7eb;
        padding-left: 0;
        padding-top: 24px;
      }
    }
  }

  .pool-title {
    font-size: 14px;
    font-weight: 600;
    margin: 0 0 12px 0;
    color: #1f2937;
    text-transform: capitalize;
  }
}
</style>
