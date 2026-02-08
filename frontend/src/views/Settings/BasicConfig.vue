<template>
  <div class="basic-config">
    <!-- 数据库配置卡片 -->
    <section class="config-card">
      <div class="card-header">
        <div class="header-left">
          <div class="header-icon icon-database">
            <el-icon :size="20"><Coin /></el-icon>
          </div>
          <div class="header-text">
            <h2 class="card-title">数据库配置</h2>
            <p class="card-desc">选择已配置的数据库实例作为分析目标</p>
          </div>
        </div>
        <div class="header-right">
          <span class="status-badge" :class="dbStatus.connected ? 'status-success' : 'status-error'">
            {{ dbStatus.connected ? 'Connected' : '未连接' }}
          </span>
        </div>
      </div>

      <div class="card-body">
        <div class="form-grid">
          <div class="form-group">
            <label class="form-label">数据库实例</label>
            <el-select
              v-model="selectedDatabaseId"
              placeholder="选择数据库实例"
              filterable
              @change="handleDatabaseChange"
              class="form-select"
            >
              <el-option
                v-for="instance in databaseInstances.filter(i => i.id)"
                :key="instance.id"
                :label="getInstanceLabel(instance)"
                :value="instance.id"
              >
                <span class="instance-option-inline">
                  {{ instance.instanceName }}
                  <el-tag v-if="instance.environment" size="small" :type="getEnvironmentTagType(instance.environment)" effect="light" class="compact-tag-inline">
                    {{ getEnvironmentLabelShort(instance.environment) }}
                  </el-tag>
                  <el-tag size="small" type="success" effect="light" class="compact-tag-inline">
                    {{ instance.instanceType?.toUpperCase() || 'MYSQL' }}
                  </el-tag>
                </span>
              </el-option>
            </el-select>
            <p class="form-hint">
              <el-icon><InfoFilled /></el-icon>
              从已配置的数据库实例中选择，如需新增请前往"实例管理"
            </p>
          </div>

          <div class="form-group">
            <label class="form-label">监听数据库 (Database)</label>
            <el-select
              v-model="selectedDatabases"
              multiple
              filterable
              collapse-tags
              collapse-tags-tooltip
              placeholder="请先选择数据库实例"
              :disabled="availableDatabases.length === 0"
              class="form-select"
            >
              <template #header>
                <div class="select-header">
                  <el-checkbox
                    v-model="checkAll"
                    :indeterminate="isIndeterminate"
                    @change="handleCheckAll"
                  >
                    全选所有数据库 ({{ availableDatabases.length }} 个)
                  </el-checkbox>
                </div>
              </template>
              <el-option
                v-for="db in availableDatabases"
                :key="db"
                :label="db"
                :value="db"
              />
            </el-select>
            <p class="form-hint">
              <el-icon><InfoFilled /></el-icon>
              DB-Doctor 将仅分析选中数据库内的查询
            </p>
          </div>
        </div>

        <!-- 测试结果提示 -->
        <div v-if="databaseTestStatus" class="test-result" :class="'test-result-' + databaseTestStatus">
          <el-icon v-if="databaseTestStatus === 'success'"><CircleCheck /></el-icon>
          <el-icon v-else-if="databaseTestStatus === 'error'"><CircleClose /></el-icon>
          <span v-if="databaseTestStatus === 'success'">
            连接成功！已加载 {{ availableDatabases.length }} 个数据库
          </span>
          <span v-else-if="databaseTestStatus === 'error'">
            {{ databaseTestError }}
          </span>
        </div>

        <div class="form-actions">
          <el-button @click="testDatabaseConnection" :loading="testing" class="btn-secondary">
            <el-icon style="margin-right: 6px"><Connection /></el-icon>
            测试连接
          </el-button>
          <el-button type="primary" @click="saveDatabaseConfig" :loading="saving" :disabled="!canSaveDatabase" class="btn-primary">
            保存配置
          </el-button>
        </div>
      </div>
    </section>

    <!-- AI 服务配置卡片 -->
    <section class="config-card">
      <div class="card-header">
        <div class="header-left">
          <div class="header-icon icon-ai">
            <el-icon :size="20"><MagicStick /></el-icon>
          </div>
          <div class="header-text">
            <h2 class="card-title">AI 服务配置</h2>
            <p class="card-desc">为 3 个 AI Agent 配置不同的推理服务实例</p>
          </div>
        </div>
        <div class="header-right">
          <div class="switch-container">
            <span class="switch-label">已启用服务</span>
            <el-switch
              v-model="aiEnabled"
              size="large"
            />
          </div>
        </div>
      </div>

      <div v-if="aiEnabled" class="card-body">
        <!-- Agent 1: 主治医生 -->
        <div class="agent-card">
          <div class="agent-icon-wrapper">
            <div class="agent-icon-circle">
              <el-icon :size="24"><User /></el-icon>
            </div>
          </div>
          <div class="agent-info">
            <div class="agent-header">
              <h3 class="agent-name">主治医生</h3>
              <span class="agent-tag tag-diagnosis">Diagnosis</span>
            </div>
            <p class="agent-desc">负责慢查询根因诊断与性能分析</p>
          </div>
          <div class="agent-select">
            <el-select
              v-model="aiConfig.diagnosis.instanceId"
              placeholder="选择AI服务实例"
              filterable
              @change="handleAiInstanceChange('diagnosis', $event)"
              class="form-select"
            >
              <el-option
                v-for="instance in aiServiceInstances.filter(i => i.id)"
                :key="instance.id"
                :label="instance.instanceName"
                :value="instance.id"
              >
                <div class="instance-option">
                  <span class="instance-name">{{ instance.instanceName }}</span>
                  <el-tag size="small" :type="getDeploymentTypeTagType(instance.deploymentType)" effect="light" class="compact-tag">
                    {{ getDeploymentTypeLabelShort(instance.deploymentType) }}
                  </el-tag>
                  <el-tag size="small" :type="getProviderTagType(instance.provider)" effect="light" class="compact-tag">
                    {{ getProviderLabelShort(instance.provider) }}
                  </el-tag>
                </div>
              </el-option>
            </el-select>
          </div>
        </div>

        <!-- Agent 2: 推理专家 -->
        <div class="agent-card">
          <div class="agent-icon-wrapper">
            <div class="agent-icon-circle">
              <el-icon :size="24"><ChatDotRound /></el-icon>
            </div>
          </div>
          <div class="agent-info">
            <div class="agent-header">
              <h3 class="agent-name">推理专家</h3>
              <span class="agent-tag tag-reasoning">Reasoning</span>
            </div>
            <p class="agent-desc">提供深度逻辑推理与复杂业务关联分析</p>
          </div>
          <div class="agent-select">
            <el-select
              v-model="aiConfig.reasoning.instanceId"
              placeholder="选择AI服务实例"
              filterable
              @change="handleAiInstanceChange('reasoning', $event)"
              class="form-select"
            >
              <el-option
                v-for="instance in aiServiceInstances.filter(i => i.id)"
                :key="instance.id"
                :label="instance.instanceName"
                :value="instance.id"
              >
                <div class="instance-option">
                  <span class="instance-name">{{ instance.instanceName }}</span>
                  <el-tag size="small" :type="getDeploymentTypeTagType(instance.deploymentType)" effect="light" class="compact-tag">
                    {{ getDeploymentTypeLabelShort(instance.deploymentType) }}
                  </el-tag>
                  <el-tag size="small" :type="getProviderTagType(instance.provider)" effect="light" class="compact-tag">
                    {{ getProviderLabelShort(instance.provider) }}
                  </el-tag>
                </div>
              </el-option>
            </el-select>
          </div>
        </div>

        <!-- Agent 3: 编码专家 -->
        <div class="agent-card">
          <div class="agent-icon-wrapper">
            <div class="agent-icon-circle">
              <el-icon :size="24"><EditPen /></el-icon>
            </div>
          </div>
          <div class="agent-info">
            <div class="agent-header">
              <h3 class="agent-name">编码专家</h3>
              <span class="agent-tag tag-coding">Coding</span>
            </div>
            <p class="agent-desc">负责 SQL 优化建议与索引重构建议</p>
          </div>
          <div class="agent-select">
            <el-select
              v-model="aiConfig.coding.instanceId"
              placeholder="选择AI服务实例"
              filterable
              @change="handleAiInstanceChange('coding', $event)"
              class="form-select"
            >
              <el-option
                v-for="instance in aiServiceInstances.filter(i => i.id)"
                :key="instance.id"
                :label="instance.instanceName"
                :value="instance.id"
              >
                <div class="instance-option">
                  <span class="instance-name">{{ instance.instanceName }}</span>
                  <el-tag size="small" :type="getDeploymentTypeTagType(instance.deploymentType)" effect="light" class="compact-tag">
                    {{ getDeploymentTypeLabelShort(instance.deploymentType) }}
                  </el-tag>
                  <el-tag size="small" :type="getProviderTagType(instance.provider)" effect="light" class="compact-tag">
                    {{ getProviderLabelShort(instance.provider) }}
                  </el-tag>
                </div>
              </el-option>
            </el-select>
          </div>
        </div>

        <div class="save-section">
          <p class="save-hint">所有 AI 推理将通过加密通道进行...</p>
          <el-button type="primary" size="large" @click="saveAiConfig" :loading="savingAi" class="btn-primary-large">
            <el-icon style="margin-right: 8px"><Check /></el-icon>
            应用并部署服务
          </el-button>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Coin,
  MagicStick,
  User,
  ChatDotRound,
  EditPen,
  Connection,
  Check,
  CircleCheck,
  CircleClose,
  InfoFilled
} from '@element-plus/icons-vue'
import {
  getAllDatabaseInstances,
  type DatabaseInstance
} from '@/api/database-instances'
import {
  getAllAiServiceInstances,
  type AiServiceInstance
} from '@/api/ai-service-instances'
import { batchUpdateConfigs, getAllConfigs, type ConfigData } from '@/api/config'

// 数据库状态
const dbStatus = ref({
  connected: false,
  lastError: null as string | null
})

const selectedDatabaseId = ref<number>()
const selectedDatabases = ref<string[]>([])
const availableDatabases = ref<string[]>([])
const testing = ref(false)
const saving = ref(false)
const databaseTestStatus = ref<'success' | 'error' | ''>('')
const databaseTestError = ref('')

const databaseInstances = ref<DatabaseInstance[]>([])
const aiServiceInstances = ref<AiServiceInstance[]>([])

// AI配置
const aiEnabled = ref(true)
const savingAi = ref(false)
const aiConfig = reactive({
  diagnosis: { instanceId: undefined as number | undefined },
  reasoning: { instanceId: undefined as number | undefined },
  coding: { instanceId: undefined as number | undefined }
})

// 全选逻辑
const isIndeterminate = computed(() => {
  return selectedDatabases.value.length > 0 && selectedDatabases.value.length < availableDatabases.value.length
})

const checkAll = computed({
  get: () => {
    return availableDatabases.value.length > 0 && selectedDatabases.value.length === availableDatabases.value.length
  },
  set: (val: boolean) => {
    selectedDatabases.value = val ? [...availableDatabases.value] : []
  }
})

function handleCheckAll(checked: boolean) {
  selectedDatabases.value = checked ? [...availableDatabases.value] : []
}

// 是否可以保存数据库配置
const canSaveDatabase = computed(() => {
  return selectedDatabaseId.value && selectedDatabases.value.length > 0
})

// 加载实例列表
async function loadInstances() {
  try {
    databaseInstances.value = await getAllDatabaseInstances()
    aiServiceInstances.value = await getAllAiServiceInstances()
  } catch (error: any) {
    console.error('加载实例列表失败:', error)
  }
}

// 获取实例标签文本
function getInstanceLabel(instance: DatabaseInstance) {
  const parts = [instance.instanceName]
  if (instance.environment) {
    parts.push(`(${getEnvironmentLabel(instance.environment)})`)
  }
  return parts.join(' ')
}

// 获取环境标签类型
function getEnvironmentTagType(env: string) {
  const map: Record<string, string> = {
    production: 'danger',
    staging: 'warning',
    development: 'success',
    testing: 'info'
  }
  return map[env] || ''
}

// 获取环境标签文本
function getEnvironmentLabel(env: string) {
  const map: Record<string, string> = {
    production: '生产',
    staging: '预发布',
    development: '开发',
    testing: '测试'
  }
  return map[env] || env
}

// 获取环境标签短文本
function getEnvironmentLabelShort(env: string) {
  const map: Record<string, string> = {
    production: '产',
    staging: '预',
    development: '开',
    testing: '测'
  }
  return map[env] || env
}

// 获取部署类型标签类型
function getDeploymentTypeTagType(type?: string) {
  const map: Record<string, string> = {
    local: 'success',
    cloud: 'primary'
  }
  return map[type || 'cloud'] || ''
}

// 获取部署类型标签文本
function getDeploymentTypeLabel(type?: string) {
  const map: Record<string, string> = {
    local: '本地',
    cloud: '云端'
  }
  return map[type || 'cloud'] || '云端'
}

// 获取部署类型标签短文本
function getDeploymentTypeLabelShort(type?: string) {
  const map: Record<string, string> = {
    local: '本',
    cloud: '云'
  }
  return map[type || '云'] || '云'
}

// 获取提供商标签类型
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

// 获取提供商标签文本
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

// 获取提供商标签短文本
function getProviderLabelShort(provider: string) {
  const map: Record<string, string> = {
    openai: 'Open',
    ollama: 'Olla',
    deepseek: 'Deep',
    anthropic: 'Anth',
    azure: 'Azure'
  }
  return map[provider] || provider
}

// 数据库实例选择变化
async function handleDatabaseChange(instanceId: number) {
  const instance = databaseInstances.value.find(i => i.id === instanceId)
  if (instance) {
    ElMessage.info(`已选择实例 "${instance.instanceName}"，请点击"测试连接"获取数据库列表`)
  }
}

// AI实例选择变化
function handleAiInstanceChange(agent: 'diagnosis' | 'reasoning' | 'coding', instanceId: number) {
  const instance = aiServiceInstances.value.find(i => i.id === instanceId)
  if (instance) {
    ElMessage.info(`已为${getAgentName(agent)}选择实例 "${instance.instanceName}"`)
  }
}

function getAgentName(agent: 'diagnosis' | 'reasoning' | 'coding'): string {
  const map = {
    diagnosis: '主治医生',
    reasoning: '推理专家',
    coding: '编码专家'
  }
  return map[agent]
}

// 测试数据库连接
async function testDatabaseConnection() {
  if (!selectedDatabaseId.value) {
    ElMessage.warning('请先选择数据库实例')
    return
  }

  const instance = databaseInstances.value.find(i => i.id === selectedDatabaseId.value)
  if (!instance) return

  testing.value = true
  databaseTestStatus.value = ''
  databaseTestError.value = ''

  try {
    // 调用测试连接API
    const response = await fetch('/api/environment/test-connection', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        url: instance.url,
        username: instance.username,
        password: instance.password // 这里需要解密，但暂时简化
      })
    })

    const result = await response.json()

    if (result.code === 200 && result.data?.overallPassed) {
      databaseTestStatus.value = 'success'
      dbStatus.value.connected = true
      availableDatabases.value = result.data.availableDatabases || []
      selectedDatabases.value = []
      ElMessage.success('连接成功！已加载数据库列表')
    } else {
      databaseTestStatus.value = 'error'
      databaseTestError.value = result.data?.summary || '连接失败'
      dbStatus.value.connected = false
    }
  } catch (error: any) {
    databaseTestStatus.value = 'error'
    databaseTestError.value = error.message || '测试连接失败'
    dbStatus.value.connected = false
  } finally {
    testing.value = false
  }
}

// 保存数据库配置
async function saveDatabaseConfig() {
  if (!canSaveDatabase.value) {
    ElMessage.warning('请选择数据库实例并测试连接')
    return
  }

  saving.value = true
  try {
    const instance = databaseInstances.value.find(i => i.id === selectedDatabaseId.value)!
    const configs: Record<string, string> = {
      'database.instance_id': String(instance.id),
      'database.instance_name': instance.instanceName,
      'database.monitored_dbs': JSON.stringify(selectedDatabases.value)
    }

    await batchUpdateConfigs({ configs, updatedBy: 'admin' })
    ElMessage.success('数据库配置保存成功')
  } catch (error: any) {
    ElMessage.error(error.message || '保存失败')
  } finally {
    saving.value = false
  }
}

// 加载已保存的配置
async function loadSavedConfigs() {
  try {
    const configData = await getAllConfigs()

    if (!configData.configs) {
      return
    }

    const configs = configData.configs

    // 加载数据库配置
    if (configs['database.instance_id']) {
      selectedDatabaseId.value = Number(configs['database.instance_id'])
    }

    if (configs['database.monitored_dbs']) {
      try {
        selectedDatabases.value = JSON.parse(configs['database.monitored_dbs'])
        dbStatus.value.connected = true
      } catch (e) {
        console.warn('解析监听数据库列表失败:', e)
      }
    }

    // 加载AI配置
    if (configs['ai.diagnosis.instance_id']) {
      aiConfig.diagnosis.instanceId = Number(configs['ai.diagnosis.instance_id'])
    }
    if (configs['ai.reasoning.instance_id']) {
      aiConfig.reasoning.instanceId = Number(configs['ai.reasoning.instance_id'])
    }
    if (configs['ai.coding.instance_id']) {
      aiConfig.coding.instanceId = Number(configs['ai.coding.instance_id'])
    }
  } catch (error: any) {
    console.warn('加载已保存配置失败:', error)
    // 如果是首次使用，没有配置是正常的
  }
}

// 保存AI配置
async function saveAiConfig() {
  savingAi.value = true
  try {
    // 收集3个AI角色的配置
    const configs: Record<string, string> = {}

    // 主治医生
    if (aiConfig.diagnosis.instanceId) {
      const instance = aiServiceInstances.value.find(i => i.id === aiConfig.diagnosis.instanceId)
      if (instance) {
        configs['ai.diagnosis.instance_id'] = String(instance.id)
        configs['ai.diagnosis.instance_name'] = instance.instanceName
      }
    }

    // 推理专家
    if (aiConfig.reasoning.instanceId) {
      const instance = aiServiceInstances.value.find(i => i.id === aiConfig.reasoning.instanceId)
      if (instance) {
        configs['ai.reasoning.instance_id'] = String(instance.id)
        configs['ai.reasoning.instance_name'] = instance.instanceName
      }
    }

    // 编码专家
    if (aiConfig.coding.instanceId) {
      const instance = aiServiceInstances.value.find(i => i.id === aiConfig.coding.instanceId)
      if (instance) {
        configs['ai.coding.instance_id'] = String(instance.id)
        configs['ai.coding.instance_name'] = instance.instanceName
      }
    }

    if (Object.keys(configs).length === 0) {
      ElMessage.warning('请至少为一个AI角色选择实例')
      return
    }

    await batchUpdateConfigs({ configs, updatedBy: 'admin' })
    ElMessage.success('AI配置保存成功')
  } catch (error: any) {
    ElMessage.error(error.message || '保存失败')
  } finally {
    savingAi.value = false
  }
}

onMounted(async () => {
  await loadInstances()
  await loadSavedConfigs()
})
</script>

<style scoped>
/* ============================================
   基础配置 - 参考设计风格
============================================ */

.basic-config {
  display: flex;
  flex-direction: column;
  gap: 32px;
}

/* === 配置卡片 === */
.config-card {
  background: var(--color-bg-page);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-2xl);
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  transition: all var(--transition-base);
}

.config-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

/* === 卡片头部 === */
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid var(--color-border-light);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-icon {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: white;
}

.icon-database {
  background: linear-gradient(135deg, #6366f1 0%, #4f46e5 100%);
}

.icon-ai {
  background: linear-gradient(135deg, #8b5cf6 0%, #7c3aed 100%);
}

.header-text {
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0;
}

.card-desc {
  font-size: 11px;
  color: var(--color-text-muted);
  margin: 0;
}

/* === 状态徽章 === */
.status-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.status-badge::before {
  content: '';
  width: 6px;
  height: 6px;
  border-radius: 50%;
}

.status-success {
  background: rgba(16, 185, 129, 0.08);
  color: #059669;
  border: 1px solid rgba(16, 185, 129, 0.15);
}

.status-success::before {
  background: #10b981;
}

.status-error {
  background: rgba(239, 68, 68, 0.08);
  color: #dc2626;
  border: 1px solid rgba(239, 68, 68, 0.15);
}

.status-error::before {
  background: #ef4444;
}

/* === 开关容器 === */
.switch-container {
  display: flex;
  align-items: center;
  gap: 12px;
}

.switch-label {
  font-size: 12px;
  color: var(--color-text-muted);
}

/* === 卡片内容 === */
.card-body {
  padding: 18px 20px;
}

/* === 表单网格 === */
.form-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px 24px;
  margin-bottom: 16px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-label {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-primary);
}

.form-select {
  width: 100%;
}

/* 下拉框包装器 */
:deep(.form-select .el-select__wrapper) {
  background: var(--color-bg-sidebar);
  border-color: var(--color-border);
  border-radius: var(--radius-md);
  box-shadow: none;
  padding: 1px 6px;
  min-height: 30px;
}

:deep(.form-select .el-select__wrapper:hover) {
  border-color: var(--color-primary);
}

:deep(.form-select .el-select__wrapper.is-focus) {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 2px rgba(99, 102, 241, 0.1);
}

/* 输入框wrapper */
:deep(.form-select .el-select__input-wrapper) {
  height: 26px;
  line-height: 26px;
}

/* 输入框 */
:deep(.form-select .el-select__input) {
  font-size: 13px;
  height: 26px;
  line-height: 26px;
  min-width: 80px !important;
  padding: 0;
}

:deep(.form-select .el-select__input-calculator) {
  min-width: 80px !important;
}

/* 占位符 */
:deep(.form-select .el-select__placeholder) {
  font-size: 13px;
  color: var(--color-text-placeholder);
  line-height: 26px;
  height: 26px;
}

/* 选中项容器 */
:deep(.form-select .el-select__selection) {
  display: flex;
  align-items: center;
  gap: 3px;
  min-height: 26px;
  line-height: 26px;
}

/* 多选标签样式优化 */
:deep(.form-select .el-tag) {
  height: 20px;
  line-height: 18px;
  padding: 0 6px;
  font-size: 12px;
  border-radius: 4px;
  margin: 0;
}

:deep(.form-select .el-select__tags-text) {
  font-size: 12px;
  line-height: 18px;
}

/* 下拉图标 */
:deep(.form-select .el-select__caret) {
  font-size: 14px;
}

/* 禁用状态 */
:deep(.form-select .el-select__wrapper.is-disabled) {
  background: var(--color-bg-page);
  opacity: 0.6;
}

:deep(.form-select .el-select__wrapper.is-disabled:hover) {
  border-color: var(--color-border);
}

/* 多选标签容器 */
:deep(.form-select .el-select__selection.is-near) {
  gap: 4px;
}

.form-hint {
  font-size: 10px;
  color: var(--color-text-muted);
  display: flex;
  align-items: center;
  gap: 3px;
  margin: 0;
  margin-top: 3px;
  line-height: 1.4;
}

:deep(.form-hint .el-icon) {
  font-size: 11px;
  flex-shrink: 0;
}

.select-header {
  padding: 8px;
  border-bottom: 1px solid var(--color-border);
}

.instance-option {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

/* 紧凑版实例选项样式 */
.instance-option-compact {
  display: flex;
  align-items: center;
  gap: 6px;
  width: 100%;
}

.instance-option-compact .instance-name {
  flex: 0 1 auto;
  margin-right: 4px;
}

.instance-name {
  flex: 1;
  font-size: 14px;
}

/* 紧凑标签样式 */
.compact-tag {
  height: 18px;
  line-height: 16px;
  padding: 0 4px;
  font-size: 11px;
}

:deep(.compact-tag .el-tag__content) {
  font-size: 11px;
  line-height: 16px;
}

/* 内联紧凑选项样式 - 用于下拉框 */
.instance-option-inline {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  white-space: nowrap;
  font-size: 13px;
}

.compact-tag-inline {
  display: inline-flex;
  height: 14px;
  line-height: 12px;
  padding: 0 2px;
  font-size: 9px;
  margin-left: 1px;
  vertical-align: middle;
  transform: scale(0.85);
  transform-origin: center;
}

:deep(.compact-tag-inline .el-tag__content) {
  font-size: 9px;
  line-height: 12px;
}

/* 下拉选项样式优化 */
:deep(.el-select-dropdown__item) {
  height: auto;
  min-height: 28px;
  padding: 4px 8px;
  font-size: 13px;
  line-height: 20px;
}

:deep(.el-select-dropdown__item:hover) {
  background-color: var(--color-bg-hover, #f5f5f5);
}

:deep(.el-select-dropdown__item.is-selected) {
  color: var(--color-primary);
  font-weight: 500;
}

/* === 测试结果 === */
.test-result {
  padding: 10px 14px;
  border-radius: var(--radius-md);
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 20px;
}

.test-result :deep(.el-icon) {
  font-size: 16px;
}

.test-result-success {
  background: var(--color-success-bg);
  color: var(--color-success);
  border: 1px solid var(--color-success-border);
}

.test-result-error {
  background: #fee2e2;
  color: #991b1b;
  border: 1px solid rgba(239, 68, 68, 0.3);
}

/* === 表单操作按钮 === */
.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding-top: 14px;
  border-top: 1px solid var(--color-border-light);
}

.btn-secondary {
  background: var(--color-bg-page);
  border: 1px solid var(--color-border);
  color: var(--color-text-primary);
  padding: 6px 14px;
  border-radius: var(--radius-md);
  font-weight: 500;
  font-size: 13px;
  transition: all var(--transition-base);
  height: 32px;
}

.btn-secondary:hover {
  background: var(--color-bg-hover);
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.btn-primary {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: #ffffff;
  padding: 6px 18px;
  border-radius: var(--radius-md);
  font-weight: 500;
  font-size: 13px;
  transition: all var(--transition-base);
  height: 32px;
}

.btn-primary:hover {
  background: var(--color-primary-dark);
  border-color: var(--color-primary-dark);
  transform: translateY(-1px);
  box-shadow: 0 4px 6px rgba(99, 102, 241, 0.2);
}

:deep(.btn-secondary .el-icon),
:deep(.btn-primary .el-icon) {
  font-size: 14px;
}

/* === Agent 卡片 === */
.agent-card {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 20px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
  background: var(--color-bg-sidebar);
  transition: all var(--transition-base);
  margin-bottom: 16px;
}

.agent-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1);
}

.agent-icon-wrapper {
  flex-shrink: 0;
}

.agent-icon-circle {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: var(--color-bg-page);
  border: 1px solid var(--color-border);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-primary);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.agent-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.agent-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.agent-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0;
}

.agent-tag {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 10px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.tag-diagnosis {
  background: #dbeafe;
  color: #1e40af;
}

.tag-reasoning {
  background: #f3e8ff;
  color: #6b21a8;
}

.tag-coding {
  background: #fef3c7;
  color: #92400e;
}

.agent-desc {
  font-size: 12px;
  color: var(--color-text-muted);
  margin: 0;
}

.agent-select {
  width: 288px;
  flex-shrink: 0;
}

/* Agent下拉框单独优化 */
:deep(.agent-select .el-select__wrapper) {
  min-height: 34px;
  padding: 2px 8px;
}

:deep(.agent-select .el-select__input-wrapper) {
  height: 30px;
  line-height: 30px;
}

:deep(.agent-select .el-select__input) {
  height: 30px;
  line-height: 30px;
  min-width: 100px !important;
}

:deep(.agent-select .el-select__placeholder) {
  height: 30px;
  line-height: 30px;
}

/* === 保存区域 === */
.save-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 16px;
  border-top: 1px solid var(--color-border-light);
}

.save-hint {
  font-size: 12px;
  color: var(--color-text-muted);
  font-style: italic;
  margin: 0;
}

.btn-primary-large {
  background: linear-gradient(135deg, #6366f1 0%, #4f46e5 100%);
  border: none;
  color: #ffffff;
  padding: 12px 32px;
  border-radius: var(--radius-xl);
  font-size: 14px;
  font-weight: 600;
  box-shadow: 0 10px 15px -3px rgba(99, 102, 241, 0.3);
  transition: all var(--transition-base);
}

.btn-primary-large:hover {
  transform: translateY(-1px);
  box-shadow: 0 20px 25px -5px rgba(99, 102, 241, 0.4);
}

.btn-primary-large:active {
  transform: translateY(0) scale(0.95);
}

/* === 暗色主题适配 === */
[data-theme="dark"] .config-card {
  background: #0a0a0a;
  border-color: #262626;
}

[data-theme="dark"] .config-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.4);
}

[data-theme="dark"] .card-header {
  border-bottom-color: #262626;
}

[data-theme="dark"] .icon-database {
  background: linear-gradient(135deg, #818cf8 0%, #6366f1 100%);
}

[data-theme="dark"] .icon-ai {
  background: linear-gradient(135deg, #a78bfa 0%, #8b5cf6 100%);
}

[data-theme="dark"] .status-success {
  background: rgba(16, 185, 129, 0.12);
  color: #34d399;
  border-color: rgba(16, 185, 129, 0.2);
}

[data-theme="dark"] .status-success::before {
  background: #34d399;
}

[data-theme="dark"] .status-error {
  background: rgba(248, 113, 113, 0.12);
  color: #f87171;
  border-color: rgba(248, 113, 113, 0.2);
}

[data-theme="dark"] .status-error::before {
  background: #f87171;
}

[data-theme="dark"] :deep(.form-select .el-select__wrapper) {
  background: #0a0a0a;
  border-color: #262626;
}

[data-theme="dark"] :deep(.form-select .el-select__wrapper:hover) {
  border-color: #818cf8;
}

[data-theme="dark"] :deep(.form-select .el-select__wrapper.is-focus) {
  border-color: #818cf8;
  box-shadow: 0 0 0 2px rgba(129, 140, 248, 0.2);
}

[data-theme="dark"] :deep(.form-select .el-select__input) {
  color: #ffffff;
}

[data-theme="dark"] :deep(.form-select .el-select__placeholder) {
  color: #737373;
}

/* Agent下拉框暗色主题 */
[data-theme="dark"] :deep(.agent-select .el-select__wrapper) {
  background: #0a0a0a;
  border-color: #262626;
}

[data-theme="dark"] :deep(.agent-select .el-select__wrapper:hover) {
  border-color: #818cf8;
}

[data-theme="dark"] :deep(.agent-select .el-select__wrapper.is-focus) {
  border-color: #818cf8;
  box-shadow: 0 0 0 2px rgba(129, 140, 248, 0.2);
}

[data-theme="dark"] :deep(.agent-select .el-select__input) {
  color: #ffffff;
}

[data-theme="dark"] :deep(.agent-select .el-select__placeholder) {
  color: #737373;
}

[data-theme="dark"] .agent-card {
  background: #0a0a0a;
  border-color: #262626;
}

[data-theme="dark"] .agent-icon-circle {
  background: #1a1a1a;
  border-color: #262626;
  color: #818cf8;
}

[data-theme="dark"] .tag-diagnosis {
  background: rgba(59, 130, 246, 0.2);
  color: #60a5fa;
  border: 1px solid rgba(59, 130, 246, 0.3);
}

[data-theme="dark"] .tag-reasoning {
  background: rgba(139, 92, 246, 0.2);
  color: #a78bfa;
  border: 1px solid rgba(139, 92, 246, 0.3);
}

[data-theme="dark"] .tag-coding {
  background: rgba(251, 191, 36, 0.2);
  color: #fbbf24;
  border: 1px solid rgba(251, 191, 36, 0.3);
}

[data-theme="dark"] .btn-secondary {
  background: #1a1a1a;
  border-color: #262626;
  color: #d4d4d4;
}

[data-theme="dark"] .btn-secondary:hover {
  background: #2a2a2a;
  border-color: #818cf8;
  color: #818cf8;
}

[data-theme="dark"] .btn-primary {
  background: #818cf8;
  border-color: #818cf8;
  color: #000000;
}

[data-theme="dark"] .btn-primary:hover {
  background: #a5b4fc;
  border-color: #a5b4fc;
}

[data-theme="dark"] .save-section {
  border-top-color: #262626;
}

[data-theme="dark"] .btn-primary-large {
  background: linear-gradient(135deg, #818cf8 0%, #6366f1 100%);
}

[data-theme="dark"] .test-result-error {
  background: rgba(248, 113, 113, 0.15);
  color: #f87171;
  border-color: rgba(248, 113, 113, 0.3);
}

/* 暗色主题下的下拉框标签 */
[data-theme="dark"] :deep(.form-select .el-tag) {
  background: #1a1a1a;
  border-color: #262626;
}

[data-theme="dark"] :deep(.form-select .el-tag--info) {
  background: rgba(129, 140, 248, 0.15);
  border-color: rgba(129, 140, 248, 0.3);
  color: #818cf8;
}
</style>
