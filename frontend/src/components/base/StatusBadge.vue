<template>
  <div :class="['status-badge', `status-${type}`, { 'dot-only': dotOnly }]">
    <span v-if="!dotOnly" class="badge-dot"></span>
    <slot>{{ text }}</slot>
  </div>
</template>

<script setup lang="ts">
interface Props {
  type?: 'success' | 'warning' | 'danger' | 'info' | 'neutral'
  text?: string
  dotOnly?: boolean
}

withDefaults(defineProps<Props>(), {
  type: 'neutral',
  dotOnly: false
})
</script>

<style scoped>
.status-badge {
  display: inline-flex;
  align-items: center;
  gap: var(--spacing-xs);
  padding: var(--spacing-xs) var(--spacing-sm);
  border-radius: var(--radius-full);
  font-size: var(--font-size-xs);
  font-weight: 600;
  line-height: 1;
  white-space: nowrap;
}

.badge-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}

/* 成功状态 */
.status-success {
  background: rgba(16, 185, 129, 0.1);
  color: var(--color-success);
}

.status-success .badge-dot {
  background: var(--color-success);
  box-shadow: 0 0 0 2px rgba(16, 185, 129, 0.2);
}

/* 警告状态 */
.status-warning {
  background: rgba(245, 158, 11, 0.1);
  color: var(--color-warning);
}

.status-warning .badge-dot {
  background: var(--color-warning);
  box-shadow: 0 0 0 2px rgba(245, 158, 11, 0.2);
}

/* 危险状态 */
.status-danger {
  background: rgba(239, 68, 68, 0.1);
  color: var(--color-danger);
}

.status-danger .badge-dot {
  background: var(--color-danger);
  box-shadow: 0 0 0 2px rgba(239, 68, 68, 0.2);
}

/* 信息状态 */
.status-info {
  background: rgba(59, 130, 246, 0.1);
  color: var(--color-info);
}

.status-info .badge-dot {
  background: var(--color-info);
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.2);
}

/* 中性状态 */
.status-neutral {
  background: rgba(107, 114, 128, 0.1);
  color: var(--color-gray-600);
}

.status-neutral .badge-dot {
  background: var(--color-gray-400);
  box-shadow: 0 0 0 2px rgba(107, 114, 128, 0.2);
}

/* 仅圆点模式 */
.dot-only {
  padding: 0;
}

.dot-only .badge-dot {
  width: 8px;
  height: 8px;
}

/* 动画 */
.status-badge {
  transition: all var(--transition-base);
}

.status-badge:hover {
  transform: scale(1.05);
}
</style>
