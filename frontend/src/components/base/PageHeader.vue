<template>
  <div class="page-header">
    <!-- 面包屑导航 -->
    <div v-if="breadcrumb && breadcrumb.length" class="breadcrumb">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item
          v-for="(item, index) in breadcrumb"
          :key="index"
          :to="index === breadcrumb.length - 1 ? undefined : item.path"
        >
          {{ item.title }}
        </el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <!-- 页面标题区域 -->
    <div class="header-content">
      <div class="header-left">
        <div v-if="icon" class="header-icon">
          <el-icon :size="32" :color="iconColor">
            <component :is="icon" />
          </el-icon>
        </div>
        <div class="header-text">
          <h1 class="page-title">{{ title }}</h1>
          <p v-if="subtitle" class="page-subtitle">{{ subtitle }}</p>
        </div>
      </div>

      <div v-if="$slots.extra" class="header-right">
        <slot name="extra"></slot>
      </div>
    </div>

    <!-- 额外内容区域 -->
    <div v-if="$slots.default" class="header-addons">
      <slot></slot>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { Component } from 'vue'

interface BreadcrumbItem {
  title: string
  path?: string
}

interface Props {
  title: string
  subtitle?: string
  icon?: Component | string
  iconColor?: string
  breadcrumb?: BreadcrumbItem[]
}

withDefaults(defineProps<Props>(), {
  iconColor: '#667eea'
})
</script>

<style scoped>
.page-header {
  margin-bottom: var(--spacing-lg);
}

/* 面包屑 */
.breadcrumb {
  margin-bottom: var(--spacing-md);
}

.breadcrumb :deep(.el-breadcrumb__item) {
  font-size: var(--font-size-sm);
}

.breadcrumb :deep(.el-breadcrumb__inner) {
  color: var(--color-gray-500);
  font-weight: 500;
}

.breadcrumb :deep(.el-breadcrumb__inner:hover) {
  color: var(--color-primary);
}

.breadcrumb :deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) {
  color: var(--color-gray-700);
  font-weight: 600;
}

/* 头部内容 */
.header-content {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: var(--spacing-lg);
}

.header-left {
  display: flex;
  align-items: flex-start;
  gap: var(--spacing-md);
  flex: 1;
  min-width: 0;
}

.header-icon {
  width: 56px;
  height: 56px;
  background: var(--gradient-primary);
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
  box-shadow: var(--shadow-primary);
}

.header-text {
  flex: 1;
  min-width: 0;
}

.page-title {
  font-size: var(--font-size-3xl);
  font-weight: 700;
  color: var(--color-gray-900);
  margin: 0 0 var(--spacing-xs) 0;
  line-height: 1.2;
  letter-spacing: -0.5px;
}

.page-subtitle {
  font-size: var(--font-size-base);
  color: var(--color-gray-500);
  margin: 0;
  line-height: 1.5;
}

.header-right {
  flex-shrink: 0;
}

/* 额外内容 */
.header-addons {
  margin-top: var(--spacing-lg);
}

/* 响应式 */
@media (max-width: 768px) {
  .header-content {
    flex-direction: column;
  }

  .header-right {
    width: 100%;
  }

  .page-title {
    font-size: var(--font-size-2xl);
  }
}
</style>
