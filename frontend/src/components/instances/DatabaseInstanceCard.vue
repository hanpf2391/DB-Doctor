<template>
  <div
    class="database-instance-card modern-card"
    :class="{ 'is-disabled': !instance.isEnabled }"
  >
    <!-- 卡片头部 - 扁平设计 -->
    <div class="card-header">
      <div class="header-top">
        <div class="instance-info">
          <div class="instance-icon">
            <el-icon :size="24">
              <Coin />
            </el-icon>
          </div>
          <div class="instance-text">
            <h3 class="instance-name">{{ instance.name }}</h3>
            <div class="instance-meta">
              <el-tag
                v-if="instance.isDefault"
                size="small"
                type="warning"
              >
                默认
              </el-tag>
              <el-tag
                :type="getEnvironmentTagType(instance.environment)"
                size="small"
              >
                {{ getEnvironmentLabel(instance.environment) }}
              </el-tag>
            </div>
          </div>
        </div>
        <div class="header-actions">
          <el-switch
            v-model="instance.isEnabled"
            :loading="instance._toggling"
            @change="handleToggle"
          />
        </div>
      </div>
    </div>

    <!-- 卡片内容 -->
    <div class="card-body">
      <div class="info-row">
        <el-icon class="row-icon"><Link /></el-icon>
        <span class="row-label">地址:</span>
        <span class="row-value">{{ formatUrl(instance) }}</span>
      </div>
      <div class="info-row">
        <el-icon class="row-icon"><User /></el-icon>
        <span class="row-label">用户:</span>
        <span class="row-value">{{ instance.username }}</span>
      </div>

      <!-- 验证状态 -->
      <div class="validation-status">
        <el-icon
          v-if="instance.isValid"
          :size="16"
          color="#10B981"
        >
          <CircleCheck />
        </el-icon>
        <el-icon
          v-else
          :size="16"
          color="#9CA3AF"
        >
          <Clock />
        </el-icon>
        <span class="validate-text">
          {{ validationText }}
        </span>
      </div>
    </div>

    <!-- 卡片底部 -->
    <div class="card-footer">
      <el-button
        link
        type="primary"
        @click="handleEdit"
        :disabled="instance._validating"
      >
        <el-icon><Edit /></el-icon>
        编辑
      </el-button>
      <el-button
        link
        type="danger"
        @click="handleDelete"
      >
        <el-icon><Delete /></el-icon>
        删除
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import {
  Coin,
  Link,
  User,
  CircleCheck,
  Clock,
  Edit,
  Delete
} from '@element-plus/icons-vue'
import type { DatabaseInstance, EnvironmentType } from '@/types/instances'
import { getEnvironmentLabel } from '@/types/instances'

interface Props {
  instance: DatabaseInstance
}

const props = defineProps<Props>()

const emit = defineEmits<{
  edit: [instance: DatabaseInstance]
  delete: [instance: DatabaseInstance]
  toggle: [instance: DatabaseInstance, value: boolean]
}>()

// 格式化 URL
function formatUrl(instance: DatabaseInstance): string {
  return `${instance.host}:${instance.port}`
}

// 环境标签类型
function getEnvironmentTagType(env: EnvironmentType) {
  const map = {
    production: 'danger',
    testing: 'warning',
    development: 'success'
  }
  return map[env] || 'info'
}

// 验证文本
const validationText = computed(() => {
  if (!props.instance.lastValidatedAt) {
    return '未验证'
  }
  const now = Date.now()
  const validated = new Date(props.instance.lastValidatedAt!).getTime()
  const diff = Math.floor((now - validated) / 1000 / 60) // 分钟

  if (diff < 1) {
    return '刚刚验证'
  } else if (diff < 60) {
    return `${diff}分钟前验证`
  } else if (diff < 1440) {
    return `${Math.floor(diff / 60)}小时前验证`
  } else {
    return `${Math.floor(diff / 1440)}天前验证`
  }
})

// 处理编辑
function handleEdit() {
  emit('edit', props.instance)
}

// 处理删除
function handleDelete() {
  emit('delete', props.instance)
}

// 处理启用/禁用
function handleToggle(value: boolean) {
  emit('toggle', props.instance, value)
}
</script>

<style scoped>
.database-instance-card {
  border: 1px solid var(--color-border);
  transition: all var(--transition-base);
}

.database-instance-card:hover {
  border-color: var(--color-primary);
}

.database-instance-card.is-disabled {
  opacity: 0.6;
}

.card-header {
  padding: var(--spacing-lg);
  background: var(--color-bg-sidebar);
  border-bottom: 1px solid var(--color-border);
}

.header-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: var(--spacing-md);
}

.instance-info {
  display: flex;
  gap: var(--spacing-sm);
  flex: 1;
  min-width: 0;
}

.instance-icon {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-md);
  background: var(--color-primary);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.instance-text {
  flex: 1;
  min-width: 0;
}

.instance-name {
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 4px 0;
  line-height: 1.2;
  word-break: break-word;
  color: var(--color-text-primary);
}

.instance-meta {
  display: flex;
  gap: var(--spacing-xs);
  flex-wrap: wrap;
}

.card-body {
  padding: var(--spacing-lg);
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
}

.info-row {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  font-size: 14px;
}

.row-icon {
  color: var(--color-text-muted);
  flex-shrink: 0;
}

.row-label {
  color: var(--color-text-secondary);
  flex-shrink: 0;
}

.row-value {
  color: var(--color-text-primary);
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
  min-width: 0;
}

.validation-status {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  margin-top: var(--spacing-sm);
  padding-top: var(--spacing-sm);
  border-top: 1px solid var(--color-border-light);
}

.validate-text {
  font-size: 12px;
  color: var(--color-text-secondary);
}

.card-footer {
  padding: var(--spacing-md) var(--spacing-lg);
  border-top: 1px solid var(--color-border-light);
  display: flex;
  justify-content: space-around;
}

.card-footer .el-button {
  font-weight: 500;
}
</style>
