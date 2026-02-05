<template>
  <div class="basic-config">
    <!-- 数据库配置 -->
    <div class="config-section modern-card">
      <div class="section-header">
        <div class="header-left">
          <div class="header-icon gradient-primary">
            <el-icon :size="24"><Coin /></el-icon>
          </div>
          <div class="header-text">
            <h2 class="section-title">数据库配置</h2>
            <p class="section-desc">选择已配置的数据库实例作为目标数据库</p>
          </div>
        </div>
        <div class="header-right">
          <el-tag :type="dbStatus.connected ? 'success' : 'danger'" effect="dark" size="large">
            <el-icon style="margin-right: 6px">
              <component :is="dbStatus.connected ? 'CircleCheck' : 'CircleClose'" />
            </el-icon>
            {{ dbStatus.connected ? '已连接' : '未连接' }}
          </el-tag>
        </div>
      </div>

      <el-divider />

      <!-- 实例选择 -->
      <div class="config-form">
        <el-form label-width="140px">
          <el-form-item label="数据库实例">
            <el-select
              v-model="selectedDatabaseId"
              placeholder="选择数据库实例"
              filterable
              style="width: 100%"
              @change="handleDatabaseChange"
            >
              <el-option
                v-for="instance in databaseInstances"
                :key="instance.id"
                :label="getInstanceLabel(instance)"
                :value="instance.id"
              >
                <div class="instance-option">
                  <span class="instance-name">{{ instance.instanceName }}</span>
                  <el-tag v-if="instance.environment" size="small" :type="getEnvironmentTagType(instance.environment)">
                    {{ getEnvironmentLabel(instance.environment) }}
                  </el-tag>
                </div>
              </el-option>
            </el-select>
            <div class="form-tip">
              <el-icon><InfoFilled /></el-icon>
              从已配置的数据库实例中选择，如需新增请前往"实例管理"
            </div>
          </el-form-item>

          <!-- 监听的数据库 -->
          <el-form-item label="监听的数据库">
            <el-select
              v-model="selectedDatabases"
              multiple
              filterable
              collapse-tags
              collapse-tags-tooltip
              placeholder="请先选择数据库实例"
              :disabled="availableDatabases.length === 0"
              style="width: 100%"
            >
              <template #header>
                <div style="padding: 8px; border-bottom: 1px solid var(--color-gray-200);">
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
            <div class="form-tip">
              <el-icon><InfoFilled /></el-icon>
              DB-Doctor 将仅分析选中的数据库
            </div>
          </el-form-item>

          <!-- 操作按钮 -->
          <el-form-item>
            <div class="action-buttons">
              <el-button type="primary" @click="testDatabaseConnection" :loading="testing" class="btn-gradient">
                <el-icon style="margin-right: 6px"><Connection /></el-icon>
                测试连接
              </el-button>
              <el-button type="success" @click="saveDatabaseConfig" :loading="saving" :disabled="!canSaveDatabase" class="btn-gradient">
                <el-icon style="margin-right: 6px"><Check /></el-icon>
                保存配置
              </el-button>
            </div>
          </el-form-item>

          <!-- 测试结果 -->
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
        </el-form>
      </div>
    </div>

    <!-- AI配置 -->
    <div class="config-section modern-card">
      <div class="section-header">
        <div class="header-left">
          <div class="header-icon gradient-success">
            <el-icon :size="24"><MagicStick /></el-icon>
          </div>
          <div class="header-text">
            <h2 class="section-title">AI服务配置</h2>
            <p class="section-desc">为3个AI Agent配置不同的服务实例</p>
          </div>
        </div>
        <div class="header-right">
          <el-switch
            v-model="aiEnabled"
            active-text="已启用"
            inactive-text="已禁用"
            size="large"
          />
        </div>
      </div>

      <el-divider />

      <!-- Agent配置 -->
      <div class="agent-configs" v-if="aiEnabled">
        <!-- 主治医生 -->
        <div class="agent-card modern-card">
          <div class="agent-header">
            <div class="agent-info">
              <el-icon class="agent-icon" color="#667eea"><User /></el-icon>
              <div>
                <h3 class="agent-name">主治医生</h3>
                <p class="agent-desc">慢查询诊断与分析</p>
              </div>
            </div>
            <el-tag type="primary" effect="dark">诊断</el-tag>
          </div>
          <el-form label-width="120px">
            <el-form-item label="服务实例">
              <el-select
                v-model="aiConfig.diagnosis.instanceId"
                placeholder="选择AI服务实例"
                filterable
                @change="handleAiInstanceChange('diagnosis', $event)"
                style="width: 100%"
              >
                <el-option
                  v-for="instance in aiServiceInstances"
                  :key="instance.id"
                  :label="instance.instanceName"
                  :value="instance.id"
                >
                  <div class="instance-option">
                    <span class="instance-name">{{ instance.instanceName }}</span>
                    <el-tag size="small" :type="getDeploymentTypeTagType(instance.deploymentType)">
                      {{ getDeploymentTypeLabel(instance.deploymentType) }}
                    </el-tag>
                    <el-tag size="small" :type="getProviderTagType(instance.provider)">
                      {{ getProviderLabel(instance.provider) }}
                    </el-tag>
                  </div>
                </el-option>
              </el-select>
            </el-form-item>
          </el-form>
        </div>

        <!-- 推理专家 -->
        <div class="agent-card modern-card">
          <div class="agent-header">
            <div class="agent-info">
              <el-icon class="agent-icon" color="#10b981"><ChatDotRound /></el-icon>
              <div>
                <h3 class="agent-name">推理专家</h3>
                <p class="agent-desc">深度推理与复杂分析</p>
              </div>
            </div>
            <el-tag type="success" effect="dark">推理</el-tag>
          </div>
          <el-form label-width="120px">
            <el-form-item label="服务实例">
              <el-select
                v-model="aiConfig.reasoning.instanceId"
                placeholder="选择AI服务实例"
                filterable
                @change="handleAiInstanceChange('reasoning', $event)"
                style="width: 100%"
              >
                <el-option
                  v-for="instance in aiServiceInstances"
                  :key="instance.id"
                  :label="instance.instanceName"
                  :value="instance.id"
                >
                  <div class="instance-option">
                    <span class="instance-name">{{ instance.instanceName }}</span>
                    <el-tag size="small" :type="getDeploymentTypeTagType(instance.deploymentType)">
                      {{ getDeploymentTypeLabel(instance.deploymentType) }}
                    </el-tag>
                    <el-tag size="small" :type="getProviderTagType(instance.provider)">
                      {{ getProviderLabel(instance.provider) }}
                    </el-tag>
                  </div>
                </el-option>
              </el-select>
            </el-form-item>
          </el-form>
        </div>

        <!-- 编码专家 -->
        <div class="agent-card modern-card">
          <div class="agent-header">
            <div class="agent-info">
              <el-icon class="agent-icon" color="#f59e0b"><EditPen /></el-icon>
              <div>
                <h3 class="agent-name">编码专家</h3>
                <p class="agent-desc">SQL优化与索引建议</p>
              </div>
            </div>
            <el-tag type="warning" effect="dark">编码</el-tag>
          </div>
          <el-form label-width="120px">
            <el-form-item label="服务实例">
              <el-select
                v-model="aiConfig.coding.instanceId"
                placeholder="选择AI服务实例"
                filterable
                @change="handleAiInstanceChange('coding', $event)"
                style="width: 100%"
              >
                <el-option
                  v-for="instance in aiServiceInstances"
                  :key="instance.id"
                  :label="instance.instanceName"
                  :value="instance.id"
                >
                  <div class="instance-option">
                    <span class="instance-name">{{ instance.instanceName }}</span>
                    <el-tag size="small" :type="getDeploymentTypeTagType(instance.deploymentType)">
                      {{ getDeploymentTypeLabel(instance.deploymentType) }}
                    </el-tag>
                    <el-tag size="small" :type="getProviderTagType(instance.provider)">
                      {{ getProviderLabel(instance.provider) }}
                    </el-tag>
                  </div>
                </el-option>
              </el-select>
            </el-form-item>
          </el-form>
        </div>

        <!-- 保存按钮 -->
        <div class="save-section">
          <el-button type="primary" size="large" @click="saveAiConfig" :loading="savingAi" class="btn-gradient">
            <el-icon style="margin-right: 8px"><Check /></el-icon>
            保存AI配置
          </el-button>
        </div>
      </div>
    </div>
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
.basic-config {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xl);
}

/* 配置区域 */
.config-section {
  padding: var(--spacing-xl);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-lg);
}

.header-left {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
}

.header-icon {
  width: 56px;
  height: 56px;
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.header-text {
  flex: 1;
}

.section-title {
  font-size: var(--font-size-xl);
  font-weight: 700;
  color: var(--color-gray-800);
  margin: 0 0 var(--spacing-xs) 0;
}

.section-desc {
  font-size: var(--font-size-sm);
  color: var(--color-gray-500);
  margin: 0;
}

.header-right {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
}

/* 配置表单 */
.config-form {
  padding: var(--spacing-lg);
  background: var(--color-gray-50);
  border-radius: var(--radius-md);
}

.form-tip {
  font-size: var(--font-size-sm);
  color: var(--color-gray-500);
  margin-top: var(--spacing-xs);
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
}

.instance-option {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  width: 100%;
}

.instance-name {
  flex: 1;
}

.action-buttons {
  display: flex;
  gap: var(--spacing-md);
}

/* 测试结果 */
.test-result {
  margin-top: var(--spacing-md);
  padding: var(--spacing-md) var(--spacing-lg);
  border-radius: var(--radius-md);
  font-size: var(--font-size-sm);
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.test-result-success {
  background: rgba(16, 185, 129, 0.1);
  color: var(--color-success);
  border: 1px solid rgba(16, 185, 129, 0.2);
}

.test-result-error {
  background: rgba(239, 68, 68, 0.1);
  color: var(--color-danger);
  border: 1px solid rgba(239, 68, 68, 0.2);
}

/* Agent配置 */
.agent-configs {
  padding: var(--spacing-lg);
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);
}

.agent-card {
  padding: var(--spacing-lg);
}

.agent-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-md);
}

.agent-info {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
}

.agent-icon {
  font-size: 32px;
}

.agent-name {
  font-size: var(--font-size-lg);
  font-weight: 600;
  color: var(--color-gray-800);
  margin: 0 0 var(--spacing-xs) 0;
}

.agent-desc {
  font-size: var(--font-size-sm);
  color: var(--color-gray-500);
  margin: 0;
}

.save-section {
  display: flex;
  justify-content: center;
  padding: var(--spacing-xl);
}
</style>
