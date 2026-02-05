<template>
  <div :class="['page-card', { 'hover-lift': hoverLift, 'no-padding': noPadding }]">
    <!-- 卡片头部 -->
    <div v-if="$slots.header || title" class="card-header">
      <slot name="header">
        <div class="header-content">
          <div class="header-left">
            <div v-if="icon" class="header-icon">
              <el-icon :size="20" :color="iconColor">
                <component :is="icon" />
              </el-icon>
            </div>
            <h3 class="card-title">{{ title }}</h3>
          </div>
          <div v-if="$slots.extra" class="header-right">
            <slot name="extra"></slot>
          </div>
        </div>
      </slot>
    </div>

    <!-- 卡片内容 -->
    <div class="card-body" :class="bodyClass">
      <slot></slot>
    </div>

    <!-- 卡片底部 -->
    <div v-if="$slots.footer" class="card-footer">
      <slot name="footer"></slot>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { Component } from 'vue'

interface Props {
  title?: string
  icon?: Component | string
  iconColor?: string
  hoverLift?: boolean
  noPadding?: boolean
  bodyClass?: string
}

withDefaults(defineProps<Props>(), {
  iconColor: '#667eea',
  hoverLift: true,
  noPadding: false
})
</script>

<style scoped>
.page-card {
  background: var(--color-bg-page);
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border);
  box-shadow: none;
  transition: all var(--transition-slow);
  overflow: hidden;
}

.page-card.hover-lift {
  cursor: pointer;
}

.page-card.hover-lift:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-lg);
  border-color: var(--color-primary-light);
}

/* 卡片头部 */
.card-header {
  padding: var(--spacing-lg);
  border-bottom: 1px solid var(--color-border-light);
  background: var(--color-bg-page);
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--spacing-md);
}

.header-left {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  flex: 1;
  min-width: 0;
}

.header-icon {
  flex-shrink: 0;
}

.card-title {
  font-size: var(--font-size-lg);
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0;
}

.header-right {
  flex-shrink: 0;
}

/* 卡片内容 */
.card-body {
  padding: var(--spacing-lg);
}

.page-card.no-padding .card-body {
  padding: 0;
}

/* 卡片底部 */
.card-footer {
  padding: var(--spacing-md) var(--spacing-lg);
  border-top: 1px solid var(--color-border-light);
  background: var(--color-bg-sidebar);
}

/* 响应式 */
@media (max-width: 768px) {
  .card-header,
  .card-body,
  .card-footer {
    padding: var(--spacing-md);
  }
}
</style>
