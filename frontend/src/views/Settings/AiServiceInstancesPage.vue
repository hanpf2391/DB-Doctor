<template>
  <div class="ai-service-instances-page">
    <PageHeader
      title="AI 服务实例管理"
      subtitle="为三个 AI Agent 配置算力服务，支持 OpenAI、Ollama、DeepSeek 等"
      :icon="MagicStick"
      :breadcrumb="[
        { title: '设置中心' },
        { title: 'AI 服务实例' }
      ]"
    >
      <template #extra>
        <el-button type="success" @click="loadInstances" :loading="loading">
          <el-icon><Refresh /></el-icon>
          刷新状态
        </el-button>
      </template>
    </PageHeader>

    <!-- 说明卡片 -->
    <PageCard class="info-card">
      <el-alert
        type="info"
        :closable="false"
        show-icon
      >
        <template #title>
          <strong>AI Agent 配置说明</strong>
        </template>
        <p>
          DB-Doctor 使用三个专业的 AI Agent 来完成不同的任务。每个 Agent 可以配置不同的 AI 服务提供商。
          请确保至少配置一个 Agent 后才能使用慢查询诊疗功能。
        </p>
      </el-alert>
    </PageCard>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-container">
      <el-skeleton :rows="3" animated />
    </div>

    <!-- Agent 卡片网格 -->
    <el-row v-else :gutter="24" class="agents-grid">
      <el-col
        v-for="agent in agents"
        :key="agent.agentType"
        :xs="24"
        :sm="24"
        :md="24"
        :lg="8"
      >
        <AiServiceCard
          :agent="agent"
          @configure="handleConfigure"
          @test="handleTest"
          @toggle="handleToggle"
        />
      </el-col>
    </el-row>

    <!-- 配置抽屉 -->
    <AiServiceDrawer
      v-model:visible="drawerVisible"
      :agent="configuringAgent"
      @saved="handleSaved"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, MagicStick } from '@element-plus/icons-vue'
import PageHeader from '@/components/base/PageHeader.vue'
import PageCard from '@/components/base/PageCard.vue'
import AiServiceCard from '@/components/instances/AiServiceCard.vue'
import AiServiceDrawer from '@/components/instances/AiServiceDrawer.vue'
import type { AiServiceInstance, AgentType } from '@/types/instances'

// 状态
const loading = ref(false)
const agents = ref<AiServiceInstance[]>([])
const drawerVisible = ref(false)
const configuringAgent = ref<AiServiceInstance | null>(null)

// 初始化三个 Agent
function initAgents() {
  const agentTypes: AgentType[] = ['diagnosis', 'reasoning', 'coding']

  agents.value = agentTypes.map(type => ({
    id: `ai-${type}`,
    agentType: type,
    provider: null as any,
    baseUrl: '',
    apiKey: '',
    model: '',
    temperature: 0.7,
    maxTokens: 2000,
    isEnabled: false,
    isValid: false,
    createdAt: new Date(),
    updatedAt: new Date()
  }))
}

// 配置 Agent
function handleConfigure(agent: AiServiceInstance) {
  configuringAgent.value = agent
  drawerVisible.value = true
}

// 测试 Agent
async function handleTest(agent: AiServiceInstance) {
  if (!agent.provider) {
    ElMessage.warning('请先配置此 Agent')
    return
  }

  agent._validating = true

  try {
    // TODO: 调用测试 API
    await new Promise(resolve => setTimeout(resolve, 1500))

    agent.isValid = true
    agent.lastValidatedAt = new Date()
    ElMessage.success(`${AGENT_CONFIG[agent.agentType].name} 测试成功`)
  } catch (error: any) {
    agent.isValid = false
    ElMessage.error('测试失败：' + error.message)
  } finally {
    agent._validating = false
  }
}

// 切换启用状态
async function handleToggle(agent: AiServiceInstance, value: boolean) {
  try {
    agent._toggling = true

    // TODO: 调用切换 API
    await new Promise(resolve => setTimeout(resolve, 500))

    agent.isEnabled = value
    ElMessage.success(value ? '已启用' : '已禁用')
  } catch (error: any) {
    ElMessage.error('操作失败：' + error.message)
    agent.isEnabled = !value
  } finally {
    agent._toggling = false
  }
}

// 保存成功回调
function handleSaved() {
  // 重新加载数据
  loadInstances()
}

// 加载实例列表
async function loadInstances() {
  loading.value = true
  try {
    // TODO: 调用 API
    await new Promise(resolve => setTimeout(resolve, 1000))

    // 模拟数据（部分已配置）
    agents.value[0] = {
      ...agents.value[0],
      provider: 'openai',
      baseUrl: 'https://api.openai.com/v1',
      apiKey: 'sk-******',
      model: 'gpt-4-turbo',
      temperature: 0.7,
      maxTokens: 2000,
      isEnabled: true,
      isValid: true,
      lastValidatedAt: new Date(Date.now() - 1000 * 60 * 5) // 5分钟前
    }

    agents.value[1] = {
      ...agents.value[1],
      provider: 'deepseek',
      baseUrl: 'https://api.deepseek.com',
      apiKey: 'sk-******',
      model: 'deepseek-chat',
      temperature: 0.7,
      maxTokens: 2000,
      isEnabled: true,
      isValid: true,
      lastValidatedAt: new Date(Date.now() - 1000 * 60 * 60) // 1小时前
    }

    // coding agent 未配置
  } catch (error: any) {
    ElMessage.error('加载失败：' + error.message)
  } finally {
    loading.value = false
  }
}

// 组件挂载
onMounted(() => {
  initAgents()
  loadInstances()
})
</script>

<style scoped>
.ai-service-instances-page {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);
}

.info-card {
  margin-bottom: var(--spacing-md);
}

.info-card :deep(.el-alert__content) {
  line-height: 1.6;
}

.agents-grid {
  margin-top: var(--spacing-md);
}

.loading-container {
  padding: var(--spacing-2xl);
  text-align: center;
}
</style>
