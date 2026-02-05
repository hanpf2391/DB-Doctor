<template>
  <div class="workbench-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <div class="header-left">
          <div class="header-icon">
            <el-icon :size="28">
              <Briefcase />
            </el-icon>
          </div>
          <div class="header-text">
            <h1 class="page-title">慢查询诊疗工作台</h1>
            <p class="page-subtitle">实时监控和分析慢查询，AI 智能诊断根因并提供优化建议</p>
          </div>
        </div>
        <div class="header-right">
          <el-button type="primary" @click="handleRefresh">
            <el-icon><Refresh /></el-icon>
            刷新数据
          </el-button>
        </div>
      </div>
    </div>

    <!-- 筛选器 - 扁平化卡片 -->
    <div class="filter-card">
      <div class="filter-header">
        <el-icon><Filter /></el-icon>
        <span>筛选条件</span>
      </div>
      <div class="filter-content">
        <el-select v-model="filters.severity" placeholder="严重等级" clearable>
          <el-option label="全部" value="" />
          <el-option label="严重" value="critical" />
          <el-option label="警告" value="warning" />
          <el-option label="已优化" value="optimized" />
        </el-select>
        <el-select v-model="filters.timeRange" placeholder="时间范围">
          <el-option label="最近1小时" value="1h" />
          <el-option label="最近24小时" value="24h" />
          <el-option label="最近7天" value="7d" />
          <el-option label="最近30天" value="30d" />
        </el-select>
        <el-input
          v-model="filters.search"
          placeholder="搜索 SQL 语句、数据库名..."
          prefix-icon="Search"
          clearable
          class="search-input"
        />
      </div>
    </div>

    <!-- 慢查询列表 -->
    <div v-if="loading" class="loading-container">
      <el-skeleton :rows="3" animated />
    </div>

    <div v-else-if="filteredQueries.length === 0" class="empty-container">
      <el-empty description="暂无慢查询数据" />
    </div>

    <div v-else class="query-list">
      <div
        v-for="query in filteredQueries"
        :key="query.id"
        class="query-card"
        @click="handleViewDetail(query)"
      >
        <!-- 卡片头部：严重等级 + 时间 -->
        <div class="card-header" :class="`severity-${query.severity}`">
          <SeverityBadge :severity="query.severity" />
          <span class="query-time">{{ formatTime(query.timestamp) }}</span>
        </div>

        <!-- SQL 片段 -->
        <div class="sql-preview">
          <pre class="sql-text">{{ truncateSql(query.sql) }}</pre>
        </div>

        <!-- 元信息 -->
        <div class="query-meta">
          <div class="meta-item">
            <el-icon><Timer /></el-icon>
            <span>耗时 {{ query.queryTime }}s</span>
          </div>
          <div class="meta-item">
            <el-icon><Coin /></el-icon>
            <span>{{ query.database }}</span>
          </div>
          <div class="meta-item">
            <el-icon><Ticket /></el-icon>
            <span>指纹: {{ query.fingerprint }}</span>
          </div>
        </div>

        <!-- 查看详情按钮 -->
        <div class="card-footer">
          <el-button type="primary" link>
            查看详情
            <el-icon class="el-icon--right"><ArrowRight /></el-icon>
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import {
  Briefcase,
  Refresh,
  Filter,
  Timer,
  Coin,
  Connection,
  ArrowRight,
  Ticket
} from '@element-plus/icons-vue'
import SeverityBadge from '@/components/diagnostics/SeverityBadge.vue'

const router = useRouter()

const loading = ref(false)
const filters = reactive({
  severity: '',
  timeRange: '24h',
  search: ''
})

// 慢查询数据（从后端API获取）
const queries = ref<any[]>([])

// 筛选后的查询列表
const filteredQueries = computed(() => {
  let result = queries.value

  // 按严重等级筛选
  if (filters.severity) {
    result = result.filter(q => q.severity === filters.severity)
  }

  // 按搜索关键词筛选
  if (filters.search) {
    const search = filters.search.toLowerCase()
    result = result.filter(q =>
      q.sql.toLowerCase().includes(search) ||
      q.database.toLowerCase().includes(search) ||
      q.fingerprint.toLowerCase().includes(search)
    )
  }

  return result
})

// 截断 SQL
function truncateSql(sql: string): string {
  if (sql.length > 120) {
    return sql.substring(0, 120) + '...'
  }
  return sql
}

// 格式化时间
function formatTime(date: Date): string {
  const now = new Date()
  const diff = Math.floor((now.getTime() - date.getTime()) / 1000 / 60) // 分钟

  if (diff < 1) return '刚刚'
  if (diff < 60) return `${diff}分钟前`
  if (diff < 1440) return `${Math.floor(diff / 60)}小时前`
  return `${Math.floor(diff / 1440)}天前`
}

// 刷新数据
function handleRefresh() {
  loading.value = true
  setTimeout(() => {
    loading.value = false
  }, 1000)
}

// 查看详情
function handleViewDetail(query: any) {
  router.push({
    name: 'DiagnosticsReports',
    params: { id: query.id }
  })
}
</script>

<style scoped>
.workbench-page {
  max-width: 1400px;
  margin: 0 auto;
  padding: var(--spacing-xl);
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);
}

/* 页面头部 */
.page-header {
  background: var(--color-bg-page);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--spacing-xl);
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
}

.header-icon {
  width: 56px;
  height: 56px;
  background: var(--color-primary);
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  margin: 0 0 4px 0;
  color: var(--color-text-primary);
}

.page-subtitle {
  font-size: 14px;
  color: var(--color-text-secondary);
  margin: 0;
}

/* 筛选卡片 */
.filter-card {
  background: var(--color-bg-page);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--spacing-lg);
}

.filter-header {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  font-weight: 600;
  font-size: 16px;
  color: var(--color-text-primary);
  margin-bottom: var(--spacing-md);
}

.filter-content {
  display: flex;
  gap: var(--spacing-md);
  flex-wrap: wrap;
}

.filter-content .el-select {
  width: 180px;
}

.search-input {
  flex: 1;
  min-width: 300px;
}

/* 慢查询列表 */
.query-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.query-card {
  background: var(--color-bg-page);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--spacing-lg);
  cursor: pointer;
  transition: all var(--transition-base);
}

.query-card:hover {
  border-color: var(--color-primary);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-md);
  padding-bottom: var(--spacing-sm);
  border-bottom: 1px solid var(--color-border-light);
}

.query-time {
  font-size: 13px;
  color: var(--color-text-secondary);
}

.sql-preview {
  margin-bottom: var(--spacing-md);
}

.sql-text {
  font-family: 'SF Mono', Monaco, 'Cascadia Code', 'Roboto Mono', Consolas, monospace;
  font-size: 13px;
  color: var(--color-text-primary);
  background: var(--color-bg-sidebar);
  padding: var(--spacing-md);
  border-radius: var(--radius-md);
  overflow-x: auto;
  white-space: pre-wrap;
  word-break: break-all;
}

.query-meta {
  display: flex;
  gap: var(--spacing-lg);
  margin-bottom: var(--spacing-md);
  flex-wrap: wrap;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--color-text-secondary);
}

.meta-item .el-icon {
  font-size: 16px;
}

.card-footer {
  display: flex;
  justify-content: flex-end;
  padding-top: var(--spacing-sm);
  border-top: 1px solid var(--color-border-light);
}

.loading-container,
.empty-container {
  padding: var(--spacing-2xl);
  text-align: center;
}
</style>
