<template>
  <el-drawer
    v-model="drawerVisible"
    :title="drawerTitle"
    direction="rtl"
    size="500px"
    @close="handleClose"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-position="top"
    >
      <el-form-item label="AI 提供商" prop="provider">
        <el-select
          v-model="form.provider"
          placeholder="选择提供商"
          class="w-full"
          @change="handleProviderChange"
        >
          <el-option
            v-for="(config, key) in PROVIDER_CONFIG"
            :key="key"
            :label="config.name"
            :value="key"
          >
            <div class="provider-option">
              <span>{{ config.name }}</span>
              <el-tag :type="config.color" size="small">
                {{ key }}
              </el-tag>
            </div>
          </el-option>
        </el-select>
      </el-form-item>

      <el-form-item label="Base URL" prop="baseUrl">
        <el-input
          v-model="form.baseUrl"
          placeholder="API Base URL"
          clearable
        >
          <template #prefix>
            <el-icon><Link /></el-icon>
          </template>
        </el-input>
      </el-form-item>

      <el-form-item label="API Key" prop="apiKey">
        <el-input
          v-model="form.apiKey"
          type="password"
          show-password
          placeholder="API 密钥"
          clearable
        >
          <template #prefix>
            <el-icon><Key /></el-icon>
          </template>
        </el-input>
      </el-form-item>

      <el-form-item label="模型名称" prop="model">
        <el-input
          v-model="form.model"
          placeholder="例如: gpt-4-turbo"
          clearable
        >
          <template #prefix>
            <el-icon><Document /></el-icon>
          </template>
        </el-input>
      </el-form-item>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="Temperature">
            <el-input-number
              v-model="form.temperature"
              :min="0"
              :max="2"
              :step="0.1"
              :precision="1"
              class="w-full"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="Max Tokens">
            <el-input-number
              v-model="form.maxTokens"
              :min="100"
              :max="100000"
              :step="100"
              class="w-full"
            />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <template #footer>
      <div class="drawer-footer">
        <!-- 测试结果面板 -->
        <div class="test-result-panel">
          <!-- 测试成功 -->
          <transition name="fade">
            <div
              v-if="testState === 'success'"
              class="result-item result-success"
            >
              <el-icon :size="24"><CircleCheckFilled /></el-icon>
              <div class="result-text">
                <div class="result-title">连接成功</div>
                <div class="result-detail">延迟 {{ testLatency }}ms</div>
                <div v-if="testResponse" class="result-response">
                  "{{ testResponse }}"
                </div>
              </div>
            </div>
          </transition>

          <!-- 测试失败 -->
          <transition name="fade">
            <div
              v-if="testState === 'fail'"
              class="result-item result-fail"
            >
              <el-icon :size="24"><CircleCloseFilled /></el-icon>
              <div class="result-text">
                <div class="result-title">连接失败</div>
                <div class="result-detail">{{ testError }}</div>
              </div>
            </div>
          </transition>

          <!-- 测试中 -->
          <transition name="fade">
            <div
              v-if="testState === 'testing'"
              class="result-item result-testing"
            >
              <el-icon :size="24" class="rotating"><Loading /></el-icon>
              <div class="result-text">
                <div class="result-title">正在测试连接...</div>
              </div>
            </div>
          </transition>
        </div>

        <!-- 按钮组 -->
        <div class="drawer-actions">
          <el-button @click="handleClose">取消</el-button>
          <el-button
            type="info"
            plain
            :loading="testState === 'testing'"
            :disabled="testState === 'testing'"
            @click="handleTest"
          >
            <el-icon><Connection /></el-icon>
            测试连接
          </el-button>
          <el-button
            type="primary"
            :disabled="!canSave"
            :loading="saving"
            @click="handleSave"
          >
            <el-icon><Check /></el-icon>
            保存配置
          </el-button>
        </div>
      </div>
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Link,
  Key,
  Document,
  CircleCheckFilled,
  CircleCloseFilled,
  Loading,
  Check,
  Connection
} from '@element-plus/icons-vue'
import type { AiServiceInstance, AiServiceForm, AgentType, ProviderType } from '@/types/instances'
import { AGENT_CONFIG, PROVIDER_CONFIG, getProviderDefaults } from '@/types/instances'

interface Props {
  visible: boolean
  agent: AiServiceInstance | null
}

const props = defineProps<Props>()
const emit = defineEmits<{
  'update:visible': [value: boolean]
  saved: []
}>()

const formRef = ref()
const saving = ref(false)

// 测试状态
type TestState = 'none' | 'testing' | 'success' | 'fail'
const testState = ref<TestState>('none')
const testLatency = ref(0)
const testError = ref('')
const testResponse = ref('')

// 表单数据
const form = ref<AiServiceForm>({
  agentType: 'diagnosis',
  provider: 'openai',
  baseUrl: '',
  apiKey: '',
  model: '',
  temperature: 0.7,
  maxTokens: 2000
})

// 是否可以保存
const canSave = computed(() => testState.value === 'success')

// 抽屉标题
const drawerTitle = computed(() => {
  if (props.agent) {
    return `配置 ${AGENT_CONFIG[props.agent.agentType].name}`
  }
  return '配置 AI 服务'
})

// 对话框显示状态
const drawerVisible = ref(props.visible)

// 监听 props.visible 变化
watch(() => props.visible, (val) => {
  drawerVisible.value = val
  if (val) {
    // 打开时初始化表单
    initForm()
  }
})

// 监听 drawerVisible 变化
watch(drawerVisible, (val) => {
  emit('update:visible', val)
})

// 初始化表单
function initForm() {
  if (props.agent && props.agent.provider) {
    // 编辑模式
    form.value = {
      id: props.agent.id,
      agentType: props.agent.agentType,
      provider: props.agent.provider,
      baseUrl: props.agent.baseUrl,
      apiKey: '******', // 不显示真实密钥
      model: props.agent.model,
      temperature: props.agent.temperature || 0.7,
      maxTokens: props.agent.maxTokens || 2000
    }
  } else {
    // 新增模式
    form.value.agentType = props.agent?.agentType || 'diagnosis'
  }
}

// 提供商变化
function handleProviderChange(provider: ProviderType) {
  const defaults = getProviderDefaults(provider)
  form.value.baseUrl = defaults.baseUrl
  form.value.model = defaults.model
  // 重置测试状态
  testState.value = 'none'
}

// 表单验证规则
const rules = {
  provider: [
    { required: true, message: '请选择提供商', trigger: 'change' }
  ],
  baseUrl: [
    { required: true, message: '请输入 Base URL', trigger: 'blur' }
  ],
  apiKey: [
    { required: true, message: '请输入 API Key', trigger: 'blur' }
  ],
  model: [
    { required: true, message: '请输入模型名称', trigger: 'blur' }
  ]
}

// 测试连接
async function handleTest() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  testState.value = 'testing'
  const startTime = Date.now()

  try {
    // TODO: 调用测试 API
    await new Promise(resolve => setTimeout(resolve, 1500)) // 模拟 API 调用

    testLatency.value = Date.now() - startTime
    testResponse.value = 'Hello! I am ready to assist you.'
    testState.value = 'success'

    ElMessage.success(`连接成功！延迟 ${testLatency.value}ms`)
  } catch (error: any) {
    testState.value = 'fail'
    testError.value = error.response?.data?.error || error.message || '连接失败'
    ElMessage.error(testError.value)
  }
}

// 保存配置
async function handleSave() {
  if (!canSave.value) {
    ElMessage.warning('请先测试连接通过后再保存')
    return
  }

  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    // TODO: 调用保存 API
    await new Promise(resolve => setTimeout(resolve, 1000)) // 模拟 API 调用

    ElMessage.success('保存成功')
    drawerVisible.value = false
    emit('saved')
  } catch (error: any) {
    ElMessage.error('保存失败：' + error.message)
  } finally {
    saving.value = false
  }
}

// 关闭抽屉
function handleClose() {
  drawerVisible.value = false
}
</script>

<style scoped>
.drawer-footer {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);
  padding: var(--spacing-lg);
  background: var(--color-gray-50);
  border-top: 1px solid var(--color-gray-100);
}

.test-result-panel {
  width: 100%;
}

.result-item {
  display: flex;
  align-items: flex-start;
  gap: var(--spacing-sm);
  padding: var(--spacing-md);
  border-radius: var(--radius-sm);
  animation: slideIn 0.3s ease;
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.result-success {
  background: #D1FAE5;
  color: #065F46;
}

.result-fail {
  background: #FEE2E2;
  color: #991B1B;
}

.result-testing {
  background: #DBEAFE;
  color: #1E40AF;
}

.result-text {
  flex: 1;
}

.result-title {
  font-weight: 600;
  font-size: var(--font-size-sm);
  margin-bottom: var(--spacing-xs);
}

.result-detail {
  font-size: var(--font-size-xs);
  opacity: 0.8;
}

.result-response {
  margin-top: var(--spacing-sm);
  padding: var(--spacing-sm);
  background: rgba(255, 255, 255, 0.5);
  border-radius: var(--radius-sm);
  font-size: var(--font-size-xs);
  font-style: italic;
}

.drawer-actions {
  display: flex;
  gap: var(--spacing-sm);
}

.provider-option {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  width: 100%;
}

.rotating {
  animation: rotate 1s linear infinite;
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

/* 过渡动画 */
.fade-enter-active,
.fade-leave-active {
  transition: all 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}
</style>
