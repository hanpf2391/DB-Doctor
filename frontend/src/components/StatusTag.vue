<template>
  <div class="status-badge" :class="[`status-${status.toLowerCase()}`, { 'is-loading': status === 'DIAGNOSING', 'is-dark': isDark }]">
    <div class="status-indicator" v-if="status === 'DIAGNOSING'">
      <span class="dot"></span>
    </div>
    <svg v-else class="status-icon" viewBox="0 0 16 16" fill="currentColor">
      <path v-if="isSuccess" d="M8 0a8 8 0 1 1 0 16A8 8 0 0 1 8 0zm3.28 5.22a.75.75 0 0 0-1.06-1.06L6.75 7.81 5.78 6.78a.75.75 0 0 0-1.06 1.06l1.5 1.5a.75.75 0 0 0 1.06 0l4-4z"/>
      <path v-else-if="isFailed" d="M8 0a8 8 0 1 1 0 16A8 8 0 0 1 8 0zm2.97 5.47a.75.75 0 0 0-1.06-1.06L8 9.44 6.09 7.53a.75.75 0 0 0-1.06 1.06l2.25 2.25a.75.75 0 0 0 1.06 0l4-4z" transform="rotate(45 8 8)"/>
      <path v-else-if="isPending" d="M8 0a8 8 0 1 1 0 16A8 8 0 0 1 8 0zM4.5 7.5a.75.75 0 0 0 0 1.5h7a.75.75 0 0 0 0-1.5h-7z"/>
      <path v-else d="M8 0a8 8 0 1 1 0 16A8 8 0 0 1 8 0zM5.5 8a.75.75 0 0 1 .75-.75h3.5a.75.75 0 0 1 0 1.5h-3.5A.75.75 0 0 1 5.5 8zm0 2.5a.75.75 0 0 1 .75-.75h3.5a.75.75 0 0 1 0 1.5h-3.5a.75.75 0 0 1-.75-.75z"/>
    </svg>
    <span class="status-text">{{ statusText }}</span>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, onUnmounted } from 'vue'

const props = defineProps<{
  status: string
}>()

const isDark = ref(false)
let themeObserver: MutationObserver | null = null

function checkTheme() {
  // 支持多种暗色主题的实现方式
  const hasDataTheme = document.documentElement.getAttribute('data-theme') === 'dark'
  const hasClass = document.documentElement.classList.contains('dark')
  const hasBodyClass = document.body.classList.contains('dark')

  isDark.value = hasDataTheme || hasClass || hasBodyClass
}

onMounted(() => {
  // 立即检查一次主题
  checkTheme()

  // 监听主题变化 - 同时监听 data-theme 属性和 class 变化
  themeObserver = new MutationObserver(checkTheme)
  themeObserver.observe(document.documentElement, {
    attributes: true,
    attributeFilter: ['data-theme', 'class']
  })
  themeObserver.observe(document.body, {
    attributes: true,
    attributeFilter: ['class']
  })
})

onUnmounted(() => {
  if (themeObserver) {
    themeObserver.disconnect()
  }
})

const statusText = computed(() => {
  const statusMap: Record<string, string> = {
    'SUCCESS': '已完成',
    'COMPLETED': '已完成',
    'FAILED': '诊断失败',
    'DIAGNOSING': '分析中',
    'PENDING': '等待中',
    'ANALYZING': '分析中'
  }
  return statusMap[props.status] || props.status
})

const isSuccess = computed(() =>
  ['SUCCESS', 'COMPLETED'].includes(props.status)
)

const isFailed = computed(() =>
  props.status === 'FAILED'
)

const isPending = computed(() =>
  props.status === 'PENDING'
)
</script>

<style scoped>
/* ============================================
   Status Badge - 商业化设计
   设计灵感：Linear, Stripe, Notion
============================================ */

.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 0.2px;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid transparent;
}

.status-icon {
  width: 14px;
  height: 14px;
  flex-shrink: 0;
}

.status-text {
  white-space: nowrap;
}

/* 统一紫色主题 */
.status-success,
.status-completed,
.status-failed,
.status-diagnosing,
.status-analyzing,
.status-pending {
  background: rgba(139, 92, 246, 0.08);
  color: #7c3aed;
  border-color: rgba(139, 92, 246, 0.2);
}

.status-success .status-icon,
.status-completed .status-icon,
.status-failed .status-icon,
.status-diagnosing .status-icon,
.status-analyzing .status-icon,
.status-pending .status-icon {
  color: #8b5cf6;
}

/* 加载动画 */
.is-loading {
  position: relative;
}

.status-indicator {
  display: flex;
  align-items: center;
  gap: 4px;
}

.dot {
  width: 8px;
  height: 8px;
  background: currentColor;
  border-radius: 50%;
  animation: pulse 1.5s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.5;
    transform: scale(0.8);
  }
}

/* ============================================
   暗色主题 - 统一紫色
============================================ */

.is-dark.status-success,
.is-dark.status-completed,
.is-dark.status-failed,
.is-dark.status-diagnosing,
.is-dark.status-analyzing,
.is-dark.status-pending {
  background: rgba(139, 92, 246, 0.15) !important;
  color: #a78bfa !important;
  border-color: rgba(139, 92, 246, 0.3) !important;
}

.is-dark.status-success .status-icon,
.is-dark.status-completed .status-icon,
.is-dark.status-failed .status-icon,
.is-dark.status-diagnosing .status-icon,
.is-dark.status-analyzing .status-icon,
.is-dark.status-pending .status-icon {
  color: #a78bfa !important;
}
</style>
