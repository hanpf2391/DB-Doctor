<template>
  <div class="ai-config">
    <el-alert
      title="AI 配置说明"
      type="info"
      :closable="false"
      style="margin-bottom: 20px"
    >
      <p>DB-Doctor 使用 3 个 AI Agent 协作：主治医生、推理专家、编码专家。支持 OpenAI 和 Ollama 两种模式。</p>
      <p><b>💡 新功能：</b>可以从预配置的 AI 服务实例中选择，或手动输入配置信息。</p>
      <p>配置保存后立即生效（热加载），无需重启服务。</p>
    </el-alert>

    <el-form label-width="140px">
      <!-- AI 全局开关 -->
      <el-form-item label="启用 AI 分析">
        <el-switch
          v-model="form.enabled"
          active-text="启用"
          inactive-text="禁用"
        />
        <span class="form-tip">关闭后，系统将不再进行 AI 分析，只记录慢查询</span>
      </el-form-item>

      <template v-if="form.enabled">
        <!-- 实例选择提示 -->
        <el-alert
          title="快速配置"
          type="success"
          :closable="false"
          style="margin-bottom: 20px"
        >
          <p>您可以为每个 Agent 从预配置的 AI 服务实例中选择，或手动输入配置。</p>
          <el-link type="primary" @click="goToInstanceManagement">
            <el-icon><Plus /></el-icon>
            管理 AI 服务实例
          </el-link>
        </el-alert>

        <!-- 主治医生配置 -->
        <el-card shadow="never" style="margin-bottom: 15px;">
          <template #header>
            <div style="display: flex; align-items: center;">
              <el-icon style="margin-right: 8px;"><User /></el-icon>
              <span style="font-weight: bold;">主治医生</span>
              <el-tag size="small" type="primary" style="margin-left: 10px;">慢查询诊断</el-tag>
            </div>
          </template>

          <!-- 实例选择 -->
          <el-form-item label="从实例中选择">
            <el-select
              v-model="selectedInstances.diagnosis"
              placeholder="选择 AI 服务实例（可选）"
              filterable
              clearable
              @change="handleDiagnosisInstanceChange"
              style="width: 100%"
            >
              <el-option
                v-for="instance in availableInstances"
                :key="instance.id"
                :label="getInstanceLabel(instance)"
                :value="instance.id"
              >
                <div class="instance-option">
                  <span class="instance-name">{{ instance.instanceName }}</span>
                  <el-tag v-if="instance.isDefault" size="small" type="warning">默认</el-tag>
                  <el-tag size="small" :type="getProviderTagType(instance.provider)">
                    {{ getProviderLabel(instance.provider) }}
                  </el-tag>
                </div>
              </el-option>
            </el-select>
          </el-form-item>

          <el-divider content-position="left">或手动配置</el-divider>

          <el-form-item label="模型名称">
            <el-input
              v-model="form.diagnosis.modelName"
              :placeholder="form.provider === 'ollama' ? 'qwen2.5:7b' : 'gpt-4'"
              clearable
            />
            <span class="form-tip">负责慢查询的初步诊断和分析</span>
          </el-form-item>
          <el-form-item label="温度参数">
            <el-slider
              v-model="form.diagnosis.temperature"
              :min="0"
              :max="1"
              :step="0.1"
              show-stops
              style="width: 100%"
            />
            <span class="form-tip">推荐 0.1（低温度，输出更确定）</span>
          </el-form-item>
        </el-card>

        <!-- 推理专家配置 -->
        <el-card shadow="never" style="margin-bottom: 15px;">
          <template #header>
            <div style="display: flex; align-items: center;">
              <el-icon style="margin-right: 8px;"><ChatDotRound /></el-icon>
              <span style="font-weight: bold;">推理专家</span>
              <el-tag size="small" type="success" style="margin-left: 10px;">深度推理</el-tag>
            </div>
          </template>

          <!-- 实例选择 -->
          <el-form-item label="从实例中选择">
            <el-select
              v-model="selectedInstances.reasoning"
              placeholder="选择 AI 服务实例（可选）"
              filterable
              clearable
              @change="handleReasoningInstanceChange"
              style="width: 100%"
            >
              <el-option
                v-for="instance in availableInstances"
                :key="instance.id"
                :label="getInstanceLabel(instance)"
                :value="instance.id"
              >
                <div class="instance-option">
                  <span class="instance-name">{{ instance.instanceName }}</span>
                  <el-tag v-if="instance.isDefault" size="small" type="warning">默认</el-tag>
                  <el-tag size="small" :type="getProviderTagType(instance.provider)">
                    {{ getProviderLabel(instance.provider) }}
                  </el-tag>
                </div>
              </el-option>
            </el-select>
          </el-form-item>

          <el-divider content-position="left">或手动配置</el-divider>

          <el-form-item label="模型名称">
            <el-input
              v-model="form.reasoning.modelName"
              :placeholder="form.provider === 'ollama' ? 'deepseek-r1:7b' : 'gpt-4'"
              clearable
            />
            <span class="form-tip">负责复杂的逻辑推理和深度分析</span>
          </el-form-item>
          <el-form-item label="温度参数">
            <el-slider
              v-model="form.reasoning.temperature"
              :min="0"
              :max="1"
              :step="0.1"
              show-stops
              style="width: 100%"
            />
            <span class="form-tip">推荐 0.3（中等温度，平衡创造性和准确性）</span>
          </el-form-item>
        </el-card>

        <!-- 编码专家配置 -->
        <el-card shadow="never">
          <template #header>
            <div style="display: flex; align-items: center;">
              <el-icon style="margin-right: 8px;"><EditPen /></el-icon>
              <span style="font-weight: bold;">编码专家</span>
              <el-tag size="small" type="warning" style="margin-left: 10px;">SQL 优化</el-tag>
            </div>
          </template>

          <!-- 实例选择 -->
          <el-form-item label="从实例中选择">
            <el-select
              v-model="selectedInstances.coding"
              placeholder="选择 AI 服务实例（可选）"
              filterable
              clearable
              @change="handleCodingInstanceChange"
              style="width: 100%"
            >
              <el-option
                v-for="instance in availableInstances"
                :key="instance.id"
                :label="getInstanceLabel(instance)"
                :value="instance.id"
              >
                <div class="instance-option">
                  <span class="instance-name">{{ instance.instanceName }}</span>
                  <el-tag v-if="instance.isDefault" size="small" type="warning">默认</el-tag>
                  <el-tag size="small" :type="getProviderTagType(instance.provider)">
                    {{ getProviderLabel(instance.provider) }}
                  </el-tag>
                </div>
              </el-option>
            </el-select>
          </el-form-item>

          <el-divider content-position="left">或手动配置</el-divider>

          <el-form-item label="模型名称">
            <el-input
              v-model="form.coding.modelName"
              :placeholder="form.provider === 'ollama' ? 'deepseek-coder:6.7b' : 'gpt-4'"
              clearable
            />
            <span class="form-tip">负责生成优化的 SQL 语句和索引建议</span>
          </el-form-item>
          <el-form-item label="温度参数">
            <el-slider
              v-model="form.coding.temperature"
              :min="0"
              :max="1"
              :step="0.1"
              show-stops
              style="width: 100%"
            />
            <span class="form-tip">推荐 0.2（低温度，确保生成的 SQL 准确）</span>
          </el-form-item>
        </el-card>

        <!-- 超时时间 -->
        <el-form-item label="超时时间（秒）">
          <el-input-number
            v-model="form.timeoutSeconds"
            :min="10"
            :max="300"
          />
          <span class="form-tip">API 调用超时时间，默认 60 秒</span>
        </el-form-item>
      </template>

      <!-- 操作按钮 -->
      <el-form-item>
        <el-button type="primary" @click="saveConfig" :loading="saving">
          <el-icon><Check /></el-icon>
          保存配置（热加载）
        </el-button>
        <el-button @click="resetForm">重置</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Check, User, ChatDotRound, EditPen, Plus } from '@element-plus/icons-vue'
import { getConfigsByGroup, batchUpdateConfigs } from '@/api/config'
import { getAllAiServiceInstances, type AiServiceInstance } from '@/api/ai-service-instances'

const router = useRouter()
const saving = ref(false)

// 可用的 AI 服务实例列表
const availableInstances = ref<AiServiceInstance[]>([])
const selectedInstances = reactive({
  diagnosis: undefined as number | undefined,
  reasoning: undefined as number | undefined,
  coding: undefined as number | undefined
})

// 表单数据
const form = reactive({
  enabled: false,
  provider: 'ollama', // ollama 或 openai
  apiKey: '',
  baseUrl: 'http://localhost:11434',
  timeoutSeconds: 60,
  // 3 个 AI Agent 配置
  diagnosis: {
    modelName: 'qwen2.5:7b',
    temperature: 0.1
  },
  reasoning: {
    modelName: 'deepseek-r1:7b',
    temperature: 0.3
  },
  coding: {
    modelName: 'deepseek-coder:6.7b',
    temperature: 0.2
  }
})

/**
 * 加载 AI 服务实例列表
 */
async function loadAiServiceInstances() {
  try {
    availableInstances.value = await getAllAiServiceInstances()
  } catch (error: any) {
    console.error('加载 AI 服务实例列表失败:', error)
  }
}

/**
 * 获取实例标签文本
 */
function getInstanceLabel(instance: AiServiceInstance) {
  return `${instance.instanceName} (${instance.modelName})`
}

/**
 * 获取提供商标签类型
 */
function getProviderTagType(provider: string) {
  const map: Record<string, string> = {
    openai: 'success',
    ollama: 'primary',
    deepseek: 'warning',
    anthropic: '',
    azure: 'info'
  }
  return map[provider] || ''
}

/**
 * 获取提供商标签文本
 */
function getProviderLabel(provider: string) {
  const map: Record<string, string> = {
    openai: 'OpenAI',
    ollama: 'Ollama',
    deepseek: 'DeepSeek',
    anthropic: 'Anthropic',
    azure: 'Azure'
  }
  return map[provider] || provider
}

/**
 * 跳转到实例管理页面
 */
function goToInstanceManagement() {
  router.push('/settings/ai-service-instances')
}

/**
 * 主治医生实例选择变化
 */
function handleDiagnosisInstanceChange(instanceId: number | undefined) {
  if (!instanceId) return

  const instance = availableInstances.value.find(i => i.id === instanceId)
  if (instance) {
    form.diagnosis.modelName = instance.modelName
    form.diagnosis.temperature = instance.temperature || 0.1
    ElMessage.info(`已加载实例 "${instance.instanceName}" 配置到主治医生`)
  }
}

/**
 * 推理专家实例选择变化
 */
function handleReasoningInstanceChange(instanceId: number | undefined) {
  if (!instanceId) return

  const instance = availableInstances.value.find(i => i.id === instanceId)
  if (instance) {
    form.reasoning.modelName = instance.modelName
    form.reasoning.temperature = instance.temperature || 0.3
    ElMessage.info(`已加载实例 "${instance.instanceName}" 配置到推理专家`)
  }
}

/**
 * 编码专家实例选择变化
 */
function handleCodingInstanceChange(instanceId: number | undefined) {
  if (!instanceId) return

  const instance = availableInstances.value.find(i => i.id === instanceId)
  if (instance) {
    form.coding.modelName = instance.modelName
    form.coding.temperature = instance.temperature || 0.2
    ElMessage.info(`已加载实例 "${instance.instanceName}" 配置到编码专家`)
  }
}

/**
 * 加载配置
 */
async function loadConfig() {
  try {
    const configs = await getConfigsByGroup('ai')

    if (configs) {
      Object.assign(form, {
        enabled: configs['ai.enabled'] === 'true',
        provider: configs['ai.provider'] || 'ollama',
        apiKey: configs['ai.api_key'] || '',
        baseUrl: configs['ai.base_url'] || 'http://localhost:11434',
        timeoutSeconds: parseInt(configs['ai.timeout_seconds'] || '60')
      })

      // 加载 3 个 Agent 的配置
      if (configs['ai.diagnosis.model_name']) {
        form.diagnosis.modelName = configs['ai.diagnosis.model_name']
        form.diagnosis.temperature = parseFloat(configs['ai.diagnosis.temperature'] || '0.1')
      }

      if (configs['ai.reasoning.model_name']) {
        form.reasoning.modelName = configs['ai.reasoning.model_name']
        form.reasoning.temperature = parseFloat(configs['ai.reasoning.temperature'] || '0.3')
      }

      if (configs['ai.coding.model_name']) {
        form.coding.modelName = configs['ai.coding.model_name']
        form.coding.temperature = parseFloat(configs['ai.coding.temperature'] || '0.2')
      }
    }
  } catch (error) {
    console.error('加载 AI 配置失败:', error)
  }
}

/**
 * 保存配置
 */
async function saveConfig() {
  saving.value = true
  try {
    const configs: Record<string, string> = {
      'ai.enabled': form.enabled.toString(),
      'ai.provider': form.provider,
      'ai.api_key': form.apiKey,
      'ai.base_url': form.baseUrl,

      // 保存 3 个 AI Agent 配置
      // 主治医生
      'ai.diagnosis.model_name': form.diagnosis.modelName,
      'ai.diagnosis.temperature': form.diagnosis.temperature.toString(),

      // 推理专家
      'ai.reasoning.model_name': form.reasoning.modelName,
      'ai.reasoning.temperature': form.reasoning.temperature.toString(),

      // 编码专家
      'ai.coding.model_name': form.coding.modelName,
      'ai.coding.temperature': form.coding.temperature.toString(),

      // 超时时间
      'ai.timeout_seconds': form.timeoutSeconds.toString()
    }

    await batchUpdateConfigs({
      configs,
      updatedBy: 'admin'
    })

    ElMessage.success({
      message: '✅ AI 配置保存成功，已自动热加载到系统！',
      duration: 3000
    })
  } catch (error: any) {
    ElMessage.error(error.message || '保存配置失败')
  } finally {
    saving.value = false
  }
}

/**
 * 重置表单
 */
function resetForm() {
  selectedInstances.diagnosis = undefined
  selectedInstances.reasoning = undefined
  selectedInstances.coding = undefined
  loadConfig()
}

onMounted(() => {
  loadConfig()
  loadAiServiceInstances()
})
</script>

<style scoped>
.ai-config {
  max-width: 800px;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
  display: block;
  line-height: 1.5;
}

.instance-option {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.instance-name {
  flex: 1;
}

:deep(.el-alert p) {
  margin: 5px 0;
}

:deep(.el-divider) {
  margin: 20px 0 15px;
}

:deep(.el-card__header) {
  padding: 12px 16px;
}
</style>
