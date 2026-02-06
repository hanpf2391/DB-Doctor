<template>
  <div class="sql-highlight-wrapper" :class="{ 'is-dark': isDark }">
    <div class="sql-container">
      <!-- 顶部工具栏 -->
      <div class="sql-toolbar" v-if="showHeader">
        <div class="toolbar-left">
          <div class="language-badge">
            <svg class="icon-sql" viewBox="0 0 24 24" fill="none">
              <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2z" stroke="currentColor" stroke-width="2"/>
              <path d="M8 12h8M12 8v8" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
            </svg>
            <span class="language-name">SQL</span>
          </div>
          <span class="sql-title">{{ title }}</span>
        </div>
        <div class="toolbar-right">
          <span class="line-count">{{ totalLines }} lines</span>
          <el-button
            class="copy-btn"
            :class="{ 'is-copied': isCopied }"
            size="small"
            @click="handleCopy"
          >
            <el-icon class="copy-icon">
              <Check v-if="isCopied" />
              <DocumentCopy v-else />
            </el-icon>
            <span class="copy-text">{{ isCopied ? '已复制' : '复制' }}</span>
          </el-button>
        </div>
      </div>

      <!-- 代码区域 -->
      <div class="sql-code-container">
        <div class="sql-gutter">
          <span
            v-for="line in totalLines"
            :key="line"
            class="line-number"
            :class="{ 'is-active': false }"
          >
            {{ line }}
          </span>
        </div>
        <pre class="sql-code" ref="codeRef"><code :class="`language-sql`" v-html="highlightedCode"></code></pre>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import hljs from 'highlight.js'
import { DocumentCopy, Check } from '@element-plus/icons-vue'

const props = withDefaults(defineProps<{
  code: string
  title?: string
  showHeader?: boolean
}>(), {
  title: 'SQL Query',
  showHeader: true
})

const isDark = ref(false)
const isCopied = ref(false)
const codeRef = ref<HTMLElement>()

// 检测当前是否为暗色主题
function checkTheme() {
  isDark.value = document.documentElement.getAttribute('data-theme') === 'dark'
}

// 计算总行数
const totalLines = computed(() => {
  return props.code.split('\n').length
})

const highlightedCode = computed(() => {
  return hljs.highlight(props.code, { language: 'sql' }).value
})

let copyTimer: ReturnType<typeof setTimeout> | null = null

async function handleCopy() {
  try {
    await navigator.clipboard.writeText(props.code)

    isCopied.value = true
    ElMessage.success({
      message: '代码已复制到剪贴板',
      duration: 2000,
      showClose: false
    })

    if (copyTimer) clearTimeout(copyTimer)
    copyTimer = setTimeout(() => {
      isCopied.value = false
    }, 2000)
  } catch (error) {
    ElMessage.error('复制失败')
  }
}

let themeObserver: MutationObserver | null = null

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
  if (copyTimer) {
    clearTimeout(copyTimer)
  }
})
</script>

<style scoped>
/* ============================================
   专业化 SQL 代码显示组件
   设计灵感：GitHub, VS Code, Vercel
============================================ */

.sql-highlight-wrapper {
  margin: 20px 0;
  border-radius: 12px;
  overflow: hidden;
  box-shadow:
    0 1px 3px rgba(0, 0, 0, 0.08),
    0 4px 12px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
}

.sql-highlight-wrapper:hover {
  box-shadow:
    0 2px 6px rgba(0, 0, 0, 0.12),
    0 8px 24px rgba(0, 0, 0, 0.08);
}

.sql-container {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  overflow: hidden;
}

/* 工具栏 - macOS 窗口风格 */
.sql-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: linear-gradient(180deg, #f9fafb 0%, #f3f4f6 100%);
  border-bottom: 1px solid #e5e7eb;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

/* 语言标识徽章 */
.language-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  background: rgba(59, 130, 246, 0.1);
  border: 1px solid rgba(59, 130, 246, 0.2);
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  color: #3b82f6;
}

.icon-sql {
  width: 14px;
  height: 14px;
  opacity: 0.8;
}

.language-name {
  font-family: 'SF Mono', 'Monaco', 'Courier New', monospace;
  letter-spacing: 0.5px;
}

.sql-title {
  font-size: 14px;
  font-weight: 600;
  color: #374151;
  letter-spacing: 0.2px;
}

.line-count {
  font-size: 12px;
  color: #9ca3af;
  font-family: 'SF Mono', 'Monaco', monospace;
}

/* 复制按钮 - 动画效果 */
.copy-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  background: white;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  color: #4b5563;
  font-size: 13px;
  font-weight: 500;
  transition: all 0.2s ease;
}

.copy-btn:hover {
  background: #f9fafb;
  border-color: #9ca3af;
  transform: translateY(-1px);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08);
}

.copy-btn:active {
  transform: translateY(0);
}

.copy-btn.is-copied {
  background: #ecfdf5;
  border-color: #10b981;
  color: #059669;
}

.copy-icon {
  font-size: 14px;
  transition: transform 0.2s ease;
}

.copy-btn.is-copied .copy-icon {
  animation: checkmark 0.4s ease;
}

@keyframes checkmark {
  0% {
    transform: scale(0);
    opacity: 0;
  }
  50% {
    transform: scale(1.2);
  }
  100% {
    transform: scale(1);
    opacity: 1;
  }
}

.copy-text {
  font-size: 13px;
}

/* 代码区域 - GitHub 风格 */
.sql-code-container {
  display: flex;
  background: #ffffff;
  overflow-x: auto;
}

/* 行号 - VS Code 风格 */
.sql-gutter {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  padding: 16px 12px;
  padding-right: 16px;
  background: #f9fafb;
  border-right: 1px solid #e5e7eb;
  user-select: none;
}

.line-number {
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.6;
  color: #9ca3af;
  min-width: 24px;
  text-align: right;
  padding-right: 8px;
  margin-bottom: 0;
}

.line-number::before {
  content: attr(data-line);
}

/* 代码内容 */
.sql-code {
  flex: 1;
  margin: 0;
  padding: 16px 20px;
  background: transparent;
  overflow-x: auto;
}

.sql-code code {
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.6;
  color: #24292f;
  background: transparent !important;
}

/* GitHub 浅色主题语法高亮 */
.sql-code :deep(.hljs) {
  background: transparent !important;
}

.sql-code :deep(.hljs-keyword) {
  color: #cf222e !important; /* 红色 - SELECT, FROM 等 */
  font-weight: 600;
}

.sql-code :deep(.hljs-string) {
  color: #0a3069 !important; /* 深蓝 - 字符串 */
}

.sql-code :deep(.hljs-number) {
  color: #0550ae !important; /* 蓝色 - 数字 */
}

.sql-code :deep(.hljs-comment) {
  color: #6e7781 !important; /* 灰色 - 注释 */
  font-style: italic;
}

.sql-code :deep(.hljs-function) {
  color: #8250df !important; /* 紫色 - 函数 */
}

.sql-code :deep(.hljs-operator) {
  color: #cf222e !important; /* 红色 - 运算符 */
}

.sql-code :deep(.hljs-built_in) {
  color: #0550ae !important; /* 蓝色 - 内置函数 */
}

.sql-code :deep(.hljs-literal) {
  color: #cf222e !important; /* 红色 - TRUE, FALSE, NULL */
}

/* ============================================
   暗色主题 - GitHub Dark / VS Code Dark
============================================ */

.sql-highlight-wrapper.is-dark .sql-container {
  background: #0d1117;
  border-color: #30363d;
}

.sql-highlight-wrapper.is-dark .sql-toolbar {
  background: linear-gradient(180deg, #161b22 0%, #0d1117 100%);
  border-bottom-color: #30363d;
}

.sql-highlight-wrapper.is-dark .language-badge {
  background: rgba(56, 139, 253, 0.15);
  border-color: rgba(56, 139, 253, 0.3);
  color: #58a6ff;
}

.sql-highlight-wrapper.is-dark .sql-title {
  color: #e6edf3;
}

.sql-highlight-wrapper.is-dark .line-count {
  color: #6e7681;
}

.sql-highlight-wrapper.is-dark .copy-btn {
  background: #21262d;
  border-color: #30363d;
  color: #c9d1d9;
}

.sql-highlight-wrapper.is-dark .copy-btn:hover {
  background: #30363d;
  border-color: #6e7681;
}

.sql-highlight-wrapper.is-dark .copy-btn.is-copied {
  background: rgba(46, 160, 67, 0.15);
  border-color: #3fb950;
  color: #3fb950;
}

.sql-highlight-wrapper.is-dark .sql-code-container {
  background: #0d1117;
}

.sql-highlight-wrapper.is-dark .sql-gutter {
  background: #0d1117;
  border-right-color: #21262d;
}

.sql-highlight-wrapper.is-dark .line-number {
  color: #6e7681;
}

.sql-highlight-wrapper.is-dark .sql-code code {
  color: #e6edf3;
}

/* GitHub Dark 暗色主题语法高亮 */
.sql-highlight-wrapper.is-dark .sql-code :deep(.hljs) {
  background: transparent !important;
}

.sql-highlight-wrapper.is-dark .sql-code :deep(.hljs-keyword) {
  color: #ff7b72 !important; /* 红色 - 关键字 */
  font-weight: 600;
}

.sql-highlight-wrapper.is-dark .sql-code :deep(.hljs-string) {
  color: #a5d6ff !important; /* 浅蓝 - 字符串 */
}

.sql-highlight-wrapper.is-dark .sql-code :deep(.hljs-number) {
  color: #79c0ff !important; /* 蓝色 - 数字 */
}

.sql-highlight-wrapper.is-dark .sql-code :deep(.hljs-comment) {
  color: #8b949e !important; /* 灰色 - 注释 */
  font-style: italic;
}

.sql-highlight-wrapper.is-dark .sql-code :deep(.hljs-function) {
  color: #d2a8ff !important; /* 紫色 - 函数 */
}

.sql-highlight-wrapper.is-dark .sql-code :deep(.hljs-operator) {
  color: #ff7b72 !important; /* 红色 - 运算符 */
}

.sql-highlight-wrapper.is-dark .sql-code :deep(.hljs-built_in) {
  color: #79c0ff !important; /* 蓝色 - 内置函数 */
}

.sql-highlight-wrapper.is-dark .sql-code :deep(.hljs-literal) {
  color: #ff7b72 !important; /* 红色 - 字面量 */
}

/* 滚动条样式 */
.sql-code-container::-webkit-scrollbar,
.sql-code::-webkit-scrollbar {
  height: 10px;
  width: 10px;
}

.sql-code-container::-webkit-scrollbar-track,
.sql-code::-webkit-scrollbar-track {
  background: transparent;
}

.sql-code-container::-webkit-scrollbar-thumb,
.sql-code::-webkit-scrollbar-thumb {
  background: #d1d5db;
  border-radius: 5px;
  border: 2px solid transparent;
  background-clip: content-box;
}

.sql-code-container::-webkit-scrollbar-thumb:hover,
.sql-code::-webkit-scrollbar-thumb:hover {
  background: #9ca3af;
  background-clip: content-box;
}

.sql-highlight-wrapper.is-dark .sql-code-container::-webkit-scrollbar-thumb,
.sql-highlight-wrapper.is-dark .sql-code::-webkit-scrollbar-thumb {
  background: #30363d;
  background-clip: content-box;
}

.sql-highlight-wrapper.is-dark .sql-code-container::-webkit-scrollbar-thumb:hover,
.sql-highlight-wrapper.is-dark .sql-code::-webkit-scrollbar-thumb:hover {
  background: #484f58;
  background-clip: content-box;
}

/* 响应式 */
@media (max-width: 768px) {
  .sql-toolbar {
    flex-direction: column;
    gap: 10px;
    align-items: flex-start;
  }

  .toolbar-right {
    width: 100%;
    justify-content: space-between;
  }
}
</style>
