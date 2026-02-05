<template>
  <div
    class="ai-service-card"
    :class="[
      { 'is-disabled': !agent.isEnabled },
      `card-${agent.agentType}`
    ]"
  >
    <!-- 卡片头部 - 扁平设计 -->
    <div class="card-header" :class="`header-${agent.agentType}`">
      <div class="header-top">
        <div class="agent-info">
          <div class="agent-icon">{{ agentConfig.icon }}</div>
          <div class="agent-text">
            <h3 class="agent-name">{{ agentConfig.name }}</h3>
            <div class="agent-meta">
              <el-tag
                v-if="agent.provider"
                size="small"
                type="warning"
              >
                {{ providerName }}
              </el-tag>
              <el-tag
                v-if="agent.isEnabled"
                size="small"
                type="success"
              >
                <el-icon><CircleCheck /></el-icon>
                已配置
              </el-tag>
              <el-tag
                v-else
                size="small"
                type="info"
              >
                未配置
              </el-tag>
            </div>
          </div>
        </div>
        <div class="header-actions">
          <el-switch
            v-if="agent.provider"
            v-model="agent.isEnabled"
            :loading="agent._toggling"
            @change="handleToggle"
          />
        </div>
      </div>
    </div>

    <!-- 卡片内容 -->
    <div class="card-body">
      <p class="agent-description">{{ agentConfig.description }}</p>

      <template v-if="agent.provider">
        <div class="info-row">
          <el-icon class="row-icon"><Link /></el-icon>
          <span class="row-label">提供商:</span>
          <span class="row-value">{{ providerName }}</span>
        </div>
        <div class="info-row">
          <el-icon class="row-icon"><Document /></el-icon>
          <span class="row-label">模型:</span>
          <span class="row-value">{{ agent.model }}</span>
        </div>
        <div class="info-row">
          <el-icon class="row-icon"><Location /></el-icon>
          <span class="row-label">Base URL:</span>
          <span class="row-value url-text">{{ formatUrl(agent.baseUrl) }}</span>
        </div>

        <!-- 验证状态 -->
        <div class="validation-status">
          <el-icon
            v-if="agent.isValid && agent.lastValidatedAt"
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
      </template>

      <div v-else class="empty-state">
        <el-icon :size="32" color="#9CA3AF"><Box /></el-icon>
        <p>尚未配置此 Agent</p>
      </div>
    </div>

    <!-- 卡片底部 -->
    <div class="card-footer">
      <el-button
        v-if="agent.provider"
        link
        type="primary"
        @click="handleTest"
        :disabled="agent._validating || !agent.isEnabled"
      >
        <el-icon><Connection /></el-icon>
        测试
      </el-button>
      <el-button
        link
        type="primary"
        @click="handleConfigure"
      >
        <el-icon><Setting /></el-icon>
        配置
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import {
  Link,
  Document,
  Location,
  CircleCheck,
  Clock,
  Connection,
  Setting,
  Box
} from '@element-plus/icons-vue'
import type { AiServiceInstance, AgentType, ProviderType } from '@/types/instances'
import { AGENT_CONFIG, getProviderName } from '@/types/instances'

interface Props {
  agent: AiServiceInstance
}

const props = defineProps<Props>()

const emit = defineEmits<{
  configure: [agent: AiServiceInstance]
  test: [agent: AiServiceInstance]
  toggle: [agent: AiServiceInstance, value: boolean]
}>()

// Agent 配置
const agentConfig = computed(() => {
  return AGENT_CONFIG[props.agent.agentType]
})

// 提供商名称
const providerName = computed(() => {
  return props.agent.provider ? getProviderName(props.agent.provider) : ''
})

// 格式化 URL
function formatUrl(url: string): string {
  if (url.length > 40) {
    return url.substring(0, 40) + '...'
  }
  return url
}

// 验证文本
const validationText = computed(() => {
  if (!props.agent.lastValidatedAt) {
    return '未验证'
  }
  const now = Date.now()
  const validated = new Date(props.agent.lastValidatedAt).getTime()
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

// 配置
function handleConfigure() {
  emit('configure', props.agent)
}

// 测试
function handleTest() {
  emit('test', props.agent)
}

// 切换启用状态
function handleToggle(value: boolean) {
  emit('toggle', props.agent, value)
}
</script>

<style scoped>
.ai-service-card {
  border: 1px solid var(--color-border);
  transition: all var(--transition-base);
}

.ai-service-card:hover {
  border-color: var(--color-primary);
}

.ai-service-card.is-disabled {
  opacity: 0.6;
}

.card-header {
  padding: var(--spacing-lg);
  border-bottom: 1px solid var(--color-border);
}

.header-diagnosis {
  background: #f3e8ff; /* 浅紫色 */
}

.header-reasoning {
  background: #fce7f3; /* 浅粉色 */
}

.header-coding {
  background: #e0f2fe; /* 浅蓝色 */
}

.header-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: var(--spacing-md);
}

.agent-info {
  display: flex;
  gap: var(--spacing-sm);
  flex: 1;
  min-width: 0;
}

.agent-icon {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  flex-shrink: 0;
  background: var(--color-bg-page);
}

.card-diagnosis .agent-icon {
  color: #8b5cf6; /* violet */
}

.card-reasoning .agent-icon {
  color: #ec4899; /* pink */
}

.card-coding .agent-icon {
  color: #3b82f6; /* blue */
}

.agent-text {
  flex: 1;
  min-width: 0;
}

.agent-name {
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 4px 0;
  line-height: 1.2;
  word-break: break-word;
  color: var(--color-text-primary);
}

.agent-meta {
  display: flex;
  gap: var(--spacing-xs);
  flex-wrap: wrap;
}

.card-body {
  padding: var(--spacing-lg);
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.agent-description {
  font-size: 14px;
  color: var(--color-text-secondary);
  margin: 0;
  line-height: 1.5;
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

.url-text {
  font-family: monospace;
  font-size: 12px;
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

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-xl) 0;
  color: var(--color-text-muted);
}

.empty-state p {
  margin: 0;
  font-size: 14px;
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
