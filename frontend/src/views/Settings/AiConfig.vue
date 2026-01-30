<template>
  <div class="ai-config">
    <!-- 全局开关 -->
    <el-form-item label="启用 AI 诊断">
      <el-switch
        v-model="aiEnabled"
        active-text="启用"
        inactive-text="禁用"
        @change="handleAiToggle"
      />
      <span class="form-tip">关闭后，系统将不再进行 AI 分析</span>
    </el-form-item>

    <!-- 主治医生配置 -->
    <AgentConfigCard
      title="主治医生 (Diagnosis Agent)"
      description="负责初步诊断慢查询问题，收集证据"
      v-model:provider="diagnosis.provider"
      v-model:baseUrl="diagnosis.baseUrl"
      v-model:modelName="diagnosis.modelName"
      v-model:temperature="diagnosis.temperature"
      v-model:apiKey="diagnosis.apiKey"
      @test="() => testAgent('diagnosis')"
    />

    <!-- 推理专家配置 -->
    <AgentConfigCard
      title="推理专家 (Reasoning Agent)"
      description="负责深度推理，找到根本原因"
      v-model:provider="reasoning.provider"
      v-model:baseUrl="reasoning.baseUrl"
      v-model:modelName="reasoning.modelName"
      v-model:temperature="reasoning.temperature"
      v-model:apiKey="reasoning.apiKey"
      @test="() => testAgent('reasoning')"
    />

    <!-- 编码专家配置 -->
    <AgentConfigCard
      title="编码专家 (Coding Agent)"
      description="负责生成优化 SQL 代码"
      v-model:provider="coding.provider"
      v-model:baseUrl="coding.baseUrl"
      v-model:modelName="coding.modelName"
      v-model:temperature="coding.temperature"
      v-model:apiKey="coding.apiKey"
      @test="() => testAgent('coding')"
    />

    <!-- 批量操作 -->
    <el-form-item>
      <el-button @click="testAll" :loading="testing">
        <el-icon><Connection /></el-icon>
        测试所有模型
      </el-button>
      <el-button type="primary" @click="saveConfig" :loading="saving">
        <el-icon><Check /></el-icon>
        保存并应用
      </el-button>
    </el-form-item>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useConfigStore } from '@/stores/config'
import { testAiConnection } from '@/api/config'
import AgentConfigCard from '@/components/AgentConfigCard.vue'

const configStore = useConfigStore()
const testing = ref(false)
const saving = ref(false)
const aiEnabled = ref(true)

// 3个 Agent 配置
const diagnosis = reactive({
  provider: 'ollama',
  baseUrl: 'http://localhost:11434',
  modelName: 'qwen2.5:7b',
  temperature: 0.0,
  apiKey: ''
})

const reasoning = reactive({
  provider: 'ollama',
  baseUrl: 'http://localhost:11434',
  modelName: 'qwen2.5:7b',
  temperature: 0.0,
  apiKey: ''
})

const coding = reactive({
  provider: 'ollama',
  baseUrl: 'http://localhost:11434',
  modelName: 'qwen2.5-coder:7b',
  temperature: 0.0,
  apiKey: ''
})

/**
 * 加载配置
 */
async function loadConfig() {
  try {
    await configStore.loadConfig()
    const aiConfig = configStore.config.ai

    if (aiConfig) {
      aiEnabled.value = aiConfig.enabled || false

      if (aiConfig.diagnosis) {
        Object.assign(diagnosis, aiConfig.diagnosis)
      }
      if (aiConfig.reasoning) {
        Object.assign(reasoning, aiConfig.reasoning)
      }
      if (aiConfig.coding) {
        Object.assign(coding, aiConfig.coding)
      }
    }
  } catch (error) {
    ElMessage.error('加载配置失败')
  }
}

/**
 * 测试单个 Agent
 */
async function testAgent(agentType: string) {
  const config = agentType === 'diagnosis' ? diagnosis :
                 agentType === 'reasoning' ? reasoning : coding

  try {
    const result = await testAiConnection({
      agentType,
      provider: config.provider,
      baseUrl: config.baseUrl,
      modelName: config.modelName,
      apiKey: config.apiKey
    } as any)

    if (result.success) {
      ElMessage.success(`${agentType} 连接成功 (${result.responseTime})`)
    } else {
      ElMessage.error(`${agentType} 连接失败`)
    }
  } catch (error: any) {
    ElMessage.error(error.message || '连接失败')
  }
}

/**
 * 测试所有模型
 */
async function testAll() {
  testing.value = true
  try {
    await Promise.all([
      testAgent('diagnosis'),
      testAgent('reasoning'),
      testAgent('coding')
    ])
    ElMessage.success('所有模型测试完成')
  } catch (error) {
    ElMessage.error('部分模型测试失败')
  } finally {
    testing.value = false
  }
}

/**
 * 保存配置
 */
async function saveConfig() {
  saving.value = true
  try {
    const configs: Record<string, string> = {
      'ai.enabled': aiEnabled.value.toString(),
      'ai.diagnosis.provider': diagnosis.provider,
      'ai.diagnosis.base-url': diagnosis.baseUrl,
      'ai.diagnosis.model-name': diagnosis.modelName,
      'ai.diagnosis.temperature': diagnosis.temperature.toString(),
      'ai.diagnosis.api-key': diagnosis.apiKey,
      'ai.reasoning.provider': reasoning.provider,
      'ai.reasoning.base-url': reasoning.baseUrl,
      'ai.reasoning.model-name': reasoning.modelName,
      'ai.reasoning.temperature': reasoning.temperature.toString(),
      'ai.reasoning.api-key': reasoning.apiKey,
      'ai.coding.provider': coding.provider,
      'ai.coding.base-url': coding.baseUrl,
      'ai.coding.model-name': coding.modelName,
      'ai.coding.temperature': coding.temperature.toString(),
      'ai.coding.api-key': coding.apiKey
    }

    const result = await configStore.saveConfigs('AI', configs)

    if (result.requiresRestart) {
      ElMessage.warning('配置已保存，但需要重启服务')
    } else {
      ElMessage.success('配置保存成功，AI 模型已重新加载')
    }
  } catch (error: any) {
    ElMessage.error(error.message || '保存失败')
  } finally {
    saving.value = false
  }
}

/**
 * AI 开关切换
 */
function handleAiToggle(value: boolean) {
  if (!value) {
    ElMessage.warning('已禁用 AI 诊断，系统将不再进行智能分析')
  }
}

onMounted(() => {
  loadConfig()
})
</script>

<style scoped>
.ai-config {
  max-width: 800px;
}

.form-tip {
  margin-left: 10px;
  font-size: 12px;
  color: #909399;
}
</style>
