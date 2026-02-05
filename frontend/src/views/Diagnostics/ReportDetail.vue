<template>
  <div class="report-detail-page">
    <!-- 返回按钮 + 标题 -->
    <div class="detail-header">
      <el-button link @click="handleBack" class="back-button">
        <el-icon><ArrowLeft /></el-icon>
        返回列表
      </el-button>
      <div class="header-info">
        <h1 class="report-title">慢查询诊断报告</h1>
        <el-tag :type="severityTagType" size="large">
          <SeverityBadge :severity="report.severity" />
        </el-tag>
      </div>
    </div>

    <!-- 主内容区：患者信息 + 诊断报告 -->
    <div class="content-wrapper">
      <!-- 左侧：患者信息卡片 -->
      <div class="patient-info-card">
        <div class="info-section">
          <div class="info-icon">
            <el-icon><Histogram /></el-icon>
          </div>
          <div class="info-content">
            <div class="info-label">数据库</div>
            <div class="info-value">{{ report.database }}</div>
          </div>
        </div>

        <div class="info-section">
          <div class="info-icon">
            <el-icon><Ticket /></el-icon>
          </div>
          <div class="info-content">
            <div class="info-label">指纹</div>
            <div class="info-value">{{ report.fingerprint }}</div>
          </div>
        </div>

        <div class="info-section">
          <div class="info-icon">
            <el-icon><Timer /></el-icon>
          </div>
          <div class="info-content">
            <div class="info-label">耗时</div>
            <div class="info-value highlight">{{ report.queryTime }}s</div>
          </div>
        </div>

        <div class="info-section">
          <div class="info-icon">
            <el-icon><Clock /></el-icon>
          </div>
          <div class="info-content">
            <div class="info-label">时间</div>
            <div class="info-value">{{ formatDateTime(report.timestamp) }}</div>
          </div>
        </div>

        <div class="info-section">
          <div class="info-icon">
            <el-icon><Warning /></el-icon>
          </div>
          <div class="info-content">
            <div class="info-label">等级</div>
            <div class="info-value">
              <SeverityBadge :severity="report.severity" />
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧：诊断报告内容 -->
      <div class="report-content-card">
        <!-- Tab 切换 -->
        <el-tabs v-model="activeTab" class="report-tabs">
          <el-tab-pane label="📋 诊断报告" name="report">
            <!-- Markdown 渲染的诊断报告 -->
            <div class="markdown-content">
              <h2 id="问题概述">问题概述</h2>
              <p>{{ report.diagnosis.summary }}</p>

              <h2 id="根本原因">根本原因分析</h2>
              <ul>
                <li v-for="(cause, index) in report.diagnosis.causes" :key="index">
                  {{ cause }}
                </li>
              </ul>

              <h2 id="优化建议">优化建议</h2>
              <div class="sql-suggestion">
                <div class="suggestion-header">
                  <el-icon><DocumentCopy /></el-icon>
                  <span>推荐的 SQL 语句</span>
                  <el-button
                    size="small"
                    text
                    @click="handleCopySql"
                  >
                    <el-icon><CopyDocument /></el-icon>
                    复制
                  </el-button>
                </div>
                <pre class="sql-code">{{ report.diagnosis.recommendation }}</pre>
              </div>

              <h2 id="预期效果">预期效果</h2>
              <div class="expected-result">
                <div class="result-item">
                  <span class="label">性能提升</span>
                  <span class="value success">约 {{ report.diagnosis.expectedImprovement }}%</span>
                </div>
                <div class="result-item">
                  <span class="label">预估耗时</span>
                  <span class="value">{{ report.diagnosis.estimatedTime }}s</span>
                </div>
              </div>
            </div>
          </el-tab-pane>

          <el-tab-pane label="🔍 执行计划" name="explain">
            <div class="explain-content">
              <el-alert
                type="info"
                :closable="false"
                show-icon
                style="margin-bottom: 16px"
              >
                以下是基于 EXPLAIN 分析的执行计划树状图
              </el-alert>

              <div class="explain-tree">
                <!-- 简化的执行计划展示 -->
                <div v-for="(item, index) in report.explainPlan" :key="index" class="explain-item">
                  <div class="explain-header">
                    <el-tag :type="item.type === 'ALL' ? 'danger' : 'success'" size="small">
                      {{ item.type }}
                    </el-tag>
                    <span class="table-name">{{ item.table }}</span>
                  </div>
                  <div class="explain-details">
                    <span>行数: {{ item.rows }}</span>
                    <span>成本: {{ item.cost }}</span>
                  </div>
                </div>
              </div>
            </div>
          </el-tab-pane>

          <el-tab-pane label="📄 原始样本" name="sample">
            <div class="sample-content">
              <div class="sql-preview-large">
                <div class="preview-header">
                  <span>完整的 SQL 语句</span>
                  <el-button
                    size="small"
                    text
                    @click="handleCopyFullSql"
                  >
                    <el-icon><CopyDocument /></el-icon>
                    复制
                  </el-button>
                </div>
                <pre class="sql-text">{{ report.sql }}</pre>
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import {
  ArrowLeft,
  Histogram,
  Ticket,
  Timer,
  Clock,
  Warning,
  DocumentCopy,
  CopyDocument
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import SeverityBadge from '@/components/diagnostics/SeverityBadge.vue'

const router = useRouter()
const activeTab = ref('report')

// 模拟诊断报告数据
const report = ref({
  id: '1',
  database: 'production_db',
  fingerprint: 'abc123',
  queryTime: 15.2,
  timestamp: new Date('2024-02-04T10:30:00'),
  severity: 'critical' as const,
  sql: `SELECT * FROM orders o
LEFT JOIN order_items oi ON o.id = oi.order_id
LEFT JOIN products p ON oi.product_id = p.id
WHERE o.created_at > "2024-01-01"
ORDER BY o.id DESC
LIMIT 1000`,
  diagnosis: {
    summary: '该查询存在严重的全表扫描问题，涉及 3 个表的 LEFT JOIN 操作，且缺乏适当的索引支持。在数据量增长的情况下，性能会急剧下降。',
    causes: [
      'orders 表的 created_at 字段缺少索引，导致范围扫描时进行全表扫描',
      'ORDER BY o.id DESC 无法利用索引，需要文件排序（filesort）',
      'LIMIT 1000 导致需要扫描大量数据后才返回结果',
      '多表 JOIN 顺序不合理，小表应该作为驱动表'
    ],
    recommendation: `-- 1. 添加索引
CREATE INDEX idx_orders_created_at_id ON orders(created_at, id);
CREATE INDEX idx_order_items_order_id ON order_items(order_id);

-- 2. 优化查询语句
SELECT o.*, oi.product_id, p.name
FROM orders o
INNER JOIN order_items oi ON o.id = oi.order_id
INNER JOIN products p ON oi.product_id = p.id
WHERE o.created_at > "2024-01-01"
ORDER BY o.id DESC
LIMIT 1000;`,
    expectedImprovement: 85,
    estimatedTime: 0.5
  },
  explainPlan: [
    { type: 'ALL', table: 'orders', rows: 150000, cost: 150.5 },
    { type: 'eq_ref', table: 'order_items', rows: 1, cost: 0.3 },
    { type: 'eq_ref', table: 'products', rows: 1, cost: 0.1 }
  ]
})

const severityTagType = computed(() => {
  const map = {
    critical: 'danger',
    warning: 'warning',
    optimized: 'success'
  }
  return map[report.value.severity]
})

function handleBack() {
  router.back()
}

function formatDateTime(date: Date): string {
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

function handleCopySql() {
  navigator.clipboard.writeText(report.value.diagnosis.recommendation)
  ElMessage.success('已复制到剪贴板')
}

function handleCopyFullSql() {
  navigator.clipboard.writeText(report.value.sql)
  ElMessage.success('已复制到剪贴板')
}
</script>

<style scoped>
.report-detail-page {
  max-width: 1600px;
  margin: 0 auto;
  padding: var(--spacing-xl);
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);
}

/* 返回按钮和标题 */
.detail-header {
  background: var(--color-bg-page);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--spacing-lg) var(--spacing-xl);
}

.back-button {
  font-size: 14px;
  margin-bottom: var(--spacing-md);
}

.header-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.report-title {
  font-size: 24px;
  font-weight: 600;
  margin: 0;
  color: var(--color-text-primary);
}

/* 主内容区布局 */
.content-wrapper {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: var(--spacing-lg);
  align-items: start;
}

/* 患者信息卡片 */
.patient-info-card {
  background: var(--color-bg-page);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--spacing-lg);
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.info-section {
  display: flex;
  gap: var(--spacing-sm);
  padding-bottom: var(--spacing-sm);
  border-bottom: 1px solid var(--color-border-light);
}

.info-section:last-child {
  border-bottom: none;
}

.info-icon {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-md);
  background: var(--color-bg-hover);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-primary);
  flex-shrink: 0;
}

.info-content {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.info-label {
  font-size: 12px;
  color: var(--color-text-secondary);
}

.info-value {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-primary);
}

.info-value.highlight {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-primary);
}

/* 诊断报告内容卡片 */
.report-content-card {
  background: var(--color-bg-page);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--spacing-xl);
  min-height: 600px;
}

.markdown-content h2 {
  font-size: 20px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 24px 0 12px 0;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--color-border-light);
}

.markdown-content h2:first-child {
  margin-top: 0;
}

.markdown-content p {
  font-size: 15px;
  line-height: 1.6;
  color: var(--color-text-primary);
  margin-bottom: 16px;
}

.markdown-content ul {
  margin: 0 0 16px 0;
  padding-left: 20px;
}

.markdown-content li {
  font-size: 15px;
  line-height: 1.6;
  color: var(--color-text-primary);
  margin-bottom: 8px;
}

/* SQL 建议代码块 */
.sql-suggestion {
  background: var(--color-bg-sidebar);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  overflow: hidden;
  margin: 16px 0;
}

.suggestion-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: var(--color-bg-page);
  border-bottom: 1px solid var(--color-border);
  font-weight: 600;
  font-size: 14px;
}

.sql-code {
  font-family: 'SF Mono', Monaco, 'Cascadia Code', 'Roboto Mono', Consolas, monospace;
  font-size: 13px;
  line-height: 1.5;
  color: var(--color-text-primary);
  padding: 16px;
  margin: 0;
  overflow-x: auto;
}

/* 预期效果 */
.expected-result {
  display: flex;
  gap: var(--spacing-lg);
  padding: var(--spacing-md);
  background: var(--color-bg-sidebar);
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
}

.result-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.result-item .label {
  font-size: 13px;
  color: var(--color-text-secondary);
}

.result-item .value {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.result-item .value.success {
  color: #10B981;
}

/* 执行计划 */
.explain-tree {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
}

.explain-item {
  padding: var(--spacing-md);
  background: var(--color-bg-sidebar);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}

.explain-header {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  margin-bottom: 8px;
}

.table-name {
  font-weight: 600;
  font-size: 14px;
  color: var(--color-text-primary);
}

.explain-details {
  display: flex;
  gap: var(--spacing-md);
  font-size: 13px;
  color: var(--color-text-secondary);
}

/* 原始 SQL */
.sql-preview-large {
  background: var(--color-bg-sidebar);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  overflow: hidden;
}

.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: var(--color-bg-page);
  border-bottom: 1px solid var(--color-border);
  font-weight: 600;
  font-size: 14px;
}

.sql-text {
  font-family: 'SF Mono', Monaco, 'Cascadia Code', 'Roboto Mono', Consolas, monospace;
  font-size: 13px;
  line-height: 1.5;
  color: var(--color-text-primary);
  padding: 16px;
  margin: 0;
  overflow-x: auto;
}
</style>
