<template>
  <el-tooltip
    placement="top"
    :effect="tooltipEffect"
    :hide-after="0"
    :offset="8"
    popper-class="sql-tooltip-popper"
    :show-arrow="true"
    :arrow-offset="8"
  >
    <template #content>
      <div class="sql-tooltip-wrapper" :class="{ 'is-dark': isDark }">
        <div class="tooltip-header">
          <div class="tooltip-title">
            <svg class="icon-sql" viewBox="0 0 24 24" fill="none">
              <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2z" stroke="currentColor" stroke-width="2"/>
              <path d="M8 12h8M12 8v8" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
            </svg>
            <span>完整 SQL</span>
          </div>
          <el-button
            class="tooltip-copy-btn"
            size="small"
            text
            @click="handleCopy"
          >
            <el-icon><DocumentCopy /></el-icon>
          </el-button>
        </div>
        <pre class="tooltip-code">{{ fullSql }}</pre>
      </div>
    </template>
    <div class="sql-truncated" :class="{ 'is-dark': isDark }">
      <div class="sql-icon">
        <svg viewBox="0 0 24 24" fill="none">
          <path d="M4 4h16a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
          <path d="M2 10h20" stroke="currentColor" stroke-width="1.5"/>
          <path d="M8 14h.01M8 18h.01M12 14h.01M12 18h.01M16 14h.01M16 18h.01" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
        </svg>
      </div>
      <code class="sql-text" v-html="highlightedSql"></code>
      <span class="sql-ellipsis" v-if="isTruncated">...</span>
    </div>
  </el-tooltip>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { DocumentCopy } from '@element-plus/icons-vue'

const props = defineProps<{
  sql: string
  maxLength?: number
}>()

const isDark = ref(false)
let themeObserver: MutationObserver | null = null

// 检测当前是否为暗色主题
function checkTheme() {
  isDark.value = document.documentElement.getAttribute('data-theme') === 'dark'
}

onMounted(() => {
  checkTheme()
  // 监听主题变化
  themeObserver = new MutationObserver(checkTheme)
  themeObserver.observe(document.documentElement, {
    attributes: true,
    attributeFilter: ['data-theme']
  })
})

onUnmounted(() => {
  if (themeObserver) {
    themeObserver.disconnect()
  }
})

const tooltipEffect = computed(() => isDark.value ? 'dark' : 'light')

const fullSql = computed(() => props.sql.trim())

const isTruncated = computed(() => {
  const maxLen = props.maxLength || 80
  return fullSql.value.length > maxLen
})

const truncatedSql = computed(() => {
  if (!isTruncated.value) return fullSql.value
  const maxLen = props.maxLength || 80
  return fullSql.value.substring(0, maxLen)
})

const highlightedSql = computed(() => {
  // 增强的 SQL 关键字高亮
  let sql = truncatedSql.value
  const keywords = [
    'SELECT', 'FROM', 'WHERE', 'JOIN', 'LEFT JOIN', 'RIGHT JOIN', 'INNER JOIN',
    'OUTER JOIN', 'CROSS JOIN', 'FULL JOIN', 'ORDER BY', 'GROUP BY', 'HAVING',
    'LIMIT', 'OFFSET', 'AND', 'OR', 'NOT', 'XOR',
    'INSERT', 'UPDATE', 'DELETE', 'CREATE', 'DROP', 'ALTER', 'INDEX', 'TABLE',
    'AS', 'ON', 'IN', 'IS', 'NULL', 'LIKE', 'BETWEEN', 'UNION', 'DISTINCT', 'EXISTS',
    'COUNT', 'SUM', 'AVG', 'MAX', 'MIN', 'CASE', 'WHEN', 'THEN', 'ELSE', 'END',
    'DESC', 'ASC', 'BY', 'WITH', 'REPLACE', 'TRUNCATE', 'VIEW', 'PRIMARY', 'KEY'
  ]

  keywords.forEach(keyword => {
    const regex = new RegExp(`\\b${keyword}\\b`, 'gi')
    sql = sql.replace(regex, `<span class="sql-keyword">${keyword}</span>`)
  })

  return sql
})

async function handleCopy() {
  try {
    await navigator.clipboard.writeText(fullSql.value)
    ElMessage.success({
      message: 'SQL 已复制',
      duration: 1500,
      showClose: false
    })
  } catch (error) {
    ElMessage.error('复制失败')
  }
}
</script>

<style scoped>
/* ============================================
   SQL Tooltip 组件 - 商业化设计
   设计灵感：Linear, Vercel, Notion
============================================ */

.sql-truncated {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  background: var(--color-bg-secondary, #f8fafc);
  border: 1px solid var(--color-border, #e2e8f0);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  max-width: 100%;
}

.sql-truncated:hover {
  background: var(--color-bg-hover, #eff6ff);
  border-color: var(--color-border-hover, #bfdbfe);
  transform: translateY(-1px);
  box-shadow:
    0 2px 8px rgba(59, 130, 246, 0.1),
    0 4px 16px rgba(0, 0, 0, 0.06);
}

.sql-truncated:active {
  transform: translateY(0);
}

/* SQL 图标 */
.sql-icon {
  flex-shrink: 0;
  width: 16px;
  height: 16px;
  color: var(--color-icon-secondary, #94a3b8);
  transition: color 0.2s ease;
}

.sql-truncated:hover .sql-icon {
  color: var(--color-icon-active, #3b82f6);
}

/* SQL 文本 */
.sql-text {
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', 'Courier New', monospace;
  font-size: 12px;
  line-height: 1.5;
  color: var(--color-text-primary, #334155);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
  min-width: 0;
}

.sql-ellipsis {
  flex-shrink: 0;
  color: var(--color-text-tertiary, #94a3b8);
  font-size: 12px;
  font-weight: 500;
}

/* 关键字高亮 */
:deep(.sql-keyword) {
  color: var(--el-color-primary, #3b82f6);
  font-weight: 600;
  letter-spacing: 0.3px;
}

/* ============================================
   暗色主题
============================================ */

.sql-truncated.is-dark {
  background: rgba(255, 255, 255, 0.06);
  border-color: rgba(255, 255, 255, 0.1);
}

.sql-truncated.is-dark:hover {
  background: rgba(59, 130, 246, 0.12);
  border-color: rgba(59, 130, 246, 0.3);
  box-shadow:
    0 2px 12px rgba(59, 130, 246, 0.15),
    0 4px 20px rgba(0, 0, 0, 0.3);
}

.sql-truncated.is-dark .sql-icon {
  color: #64748b;
}

.sql-truncated.is-dark:hover .sql-icon {
  color: #60a5fa;
}

.sql-truncated.is-dark .sql-text {
  color: #e2e8f0;
}

.sql-truncated.is-dark .sql-ellipsis {
  color: #64748b;
}

.sql-truncated.is-dark :deep(.sql-keyword) {
  color: #60a5fa;
}

/* ============================================
   Tooltip 内容样式（全局样式）
============================================ */
</style>

<style>
/* 全局样式 - Tooltip Popper */
.sql-tooltip-popper {
  max-width: 700px !important;
  padding: 0 !important;
}

.sql-tooltip-popper .el-tooltip__arrow {
  display: block !important;
}

.sql-tooltip-wrapper {
  border-radius: 12px;
  overflow: hidden;
  box-shadow:
    0 4px 20px rgba(0, 0, 0, 0.15),
    0 8px 40px rgba(0, 0, 0, 0.1);
  animation: tooltipFadeIn 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

@keyframes tooltipFadeIn {
  from {
    opacity: 0;
    transform: translateY(-8px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

/* Tooltip 头部 */
.tooltip-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 16px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
  background: rgba(0, 0, 0, 0.02);
}

.tooltip-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 600;
  color: #475569;
  letter-spacing: 0.3px;
}

.tooltip-title .icon-sql {
  width: 16px;
  height: 16px;
  color: #3b82f6;
  opacity: 0.8;
}

.tooltip-copy-btn {
  padding: 4px !important;
  color: #64748b;
  transition: all 0.2s ease;
}

.tooltip-copy-btn:hover {
  color: #3b82f6;
  background: rgba(59, 130, 246, 0.1) !important;
}

/* Tooltip 代码区域 */
.tooltip-code {
  margin: 0;
  padding: 16px 20px;
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', 'Courier New', monospace;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
  background: #ffffff;
  color: #334155;
  max-height: 400px;
  overflow-y: auto;
}

/* 暗色主题 Tooltip */
.sql-tooltip-wrapper.is-dark {
  box-shadow:
    0 4px 20px rgba(0, 0, 0, 0.4),
    0 8px 40px rgba(0, 0, 0, 0.3);
}

.sql-tooltip-wrapper.is-dark .tooltip-header {
  background: rgba(255, 255, 255, 0.04);
  border-bottom-color: rgba(255, 255, 255, 0.08);
}

.sql-tooltip-wrapper.is-dark .tooltip-title {
  color: #e2e8f0;
}

.sql-tooltip-wrapper.is-dark .tooltip-title .icon-sql {
  color: #60a5fa;
}

.sql-tooltip-wrapper.is-dark .tooltip-copy-btn {
  color: #94a3b8;
}

.sql-tooltip-wrapper.is-dark .tooltip-copy-btn:hover {
  color: #60a5fa;
  background: rgba(96, 165, 250, 0.15) !important;
}

.sql-tooltip-wrapper.is-dark .tooltip-code {
  background: #0f172a;
  color: #e2e8f0;
}

/* Tooltip 滚动条 */
.tooltip-code::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

.tooltip-code::-webkit-scrollbar-track {
  background: transparent;
}

.tooltip-code::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 4px;
  border: 2px solid transparent;
  background-clip: content-box;
}

.tooltip-code::-webkit-scrollbar-thumb:hover {
  background: #94a3b8;
  background-clip: content-box;
}

.sql-tooltip-wrapper.is-dark .tooltip-code::-webkit-scrollbar-thumb {
  background: #334155;
  background-clip: content-box;
}

.sql-tooltip-wrapper.is-dark .tooltip-code::-webkit-scrollbar-thumb:hover {
  background: #475569;
  background-clip: content-box;
}
</style>
