<template>
  <div class="instance-management">
    <!-- 实例类型切换 -->
    <div class="type-selector modern-card">
      <div class="type-header">
        <h2 class="section-title">
          <el-icon><Coin /></el-icon>
          实例管理
        </h2>
        <p class="section-desc">统一管理数据库和AI服务实例，测试成功后即可新增</p>
      </div>
      <div class="type-tabs">
        <div
          :class="['type-tab', { active: activeType === 'database' }]"
          @click="activeType = 'database'"
        >
          <el-icon :size="20"><Database /></el-icon>
          <span>数据库实例</span>
        </div>
        <div
          :class="['type-tab', { active: activeType === 'ai' }]"
          @click="activeType = 'ai'"
        >
          <el-icon :size="20"><MagicStick /></el-icon>
          <span>AI服务实例</span>
        </div>
      </div>
    </div>

    <!-- 数据库实例管理 -->
    <transition name="fade" mode="out-in">
      <div v-if="activeType === 'database'" key="database" class="database-section">
        <DatabaseInstanceManagement />
      </div>
    </transition>

    <!-- AI服务实例管理 -->
    <transition name="fade" mode="out-in">
      <div v-if="activeType === 'ai'" key="ai" class="ai-section">
        <AiServiceInstanceManagement />
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Coin, MagicStick } from '@element-plus/icons-vue'
import DatabaseInstanceManagement from './DatabaseInstanceManagement.vue'
import AiServiceInstanceManagement from './AiServiceInstanceManagement.vue'

const activeType = ref('database')
</script>

<style scoped>
.instance-management {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);
}

/* 类型选择器 */
.type-selector {
  padding: var(--spacing-xl);
}

.type-header {
  margin-bottom: var(--spacing-lg);
}

.section-title {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  font-size: var(--font-size-2xl);
  font-weight: 700;
  color: var(--color-text-primary);
  margin: 0 0 var(--spacing-xs) 0;
}

.section-desc {
  font-size: var(--font-size-base);
  color: var(--color-text-secondary);
  margin: 0;
}

.type-tabs {
  display: flex;
  gap: var(--spacing-md);
  background: var(--color-bg-hover);
  padding: var(--spacing-xs);
  border-radius: var(--radius-lg);
}

.type-tab {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-md);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-base);
  color: var(--color-text-secondary);
  font-weight: 500;
}

.type-tab:hover {
  background: var(--color-bg-page);
  color: var(--color-primary);
}

.type-tab.active {
  background: var(--color-bg-page);
  color: var(--color-primary);
  font-weight: 600;
}

[data-theme="dark"] .type-tabs {
  background: #1a1a1a;
}

[data-theme="dark"] .type-tab {
  color: #a3a3a3;
}

[data-theme="dark"] .type-tab:hover {
  background: #2a2a2a;
  color: #818cf8;
}

[data-theme="dark"] .type-tab.active {
  background: #2a2a2a;
  color: #818cf8;
}

/* 过渡动画 */
.fade-enter-active, .fade-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.fade-enter-from {
  opacity: 0;
  transform: translateX(20px);
}

.fade-leave-to {
  opacity: 0;
  transform: translateX(-20px);
}
</style>
