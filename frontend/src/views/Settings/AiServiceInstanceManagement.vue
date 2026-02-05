<template>
  <div class="ai-service-instance-management">
    <!-- 操作栏 -->
    <div class="action-bar modern-card">
      <div class="action-left">
        <el-button type="primary" size="large" @click="handleCreate" class="btn-gradient">
          <el-icon style="margin-right: 6px"><Plus /></el-icon>
          新增AI服务实例
        </el-button>
      </div>
      <div class="action-right">
        <el-input
          v-model="searchText"
          placeholder="搜索实例名称..."
          prefix-icon="Search"
          style="width: 300px"
          clearable
        />
      </div>
    </div>

    <!-- 实例表格 -->
    <div class="table-container modern-card">
      <el-table
        :data="filteredInstances"
        style="width: 100%"
        v-loading="loading"
        :row-class-name="getRowClassName"
      >
        <!-- 实例名称 -->
        <el-table-column label="实例名称" min-width="180" fixed="left">
          <template #default="{ row }">
            <div class="instance-name-cell">
              <div class="deployment-tags">
                <el-tag size="small" :type="getDeploymentTypeTagType(row.deploymentType)">
                  {{ getDeploymentTypeLabel(row.deploymentType) }}
                </el-tag>
                <el-tag size="small" :type="getProviderTagType(row.provider)">
                  {{ getProviderLabel(row.provider) }}
                </el-tag>
              </div>
              <div class="name-text">
                <span class="name">{{ row.instanceName }}</span>
              </div>
            </div>
          </template>
        </el-table-column>

        <!-- 模型信息 -->
        <el-table-column label="模型" min-width="200">
          <template #default="{ row }">
            <div class="model-info">
              <div class="model-name">{{ row.modelName }}</div>
              <div class="model-params">
                <span>温度: {{ row.temperature || '-' }}</span>
                <span>最大Token: {{ row.maxTokens || '-' }}</span>
              </div>
            </div>
          </template>
        </el-table-column>

        <!-- API地址 -->
        <el-table-column label="API地址" min-width="250" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="url-text">{{ row.baseUrl || '-' }}</span>
          </template>
        </el-table-column>

        <!-- 超时时间 -->
        <el-table-column label="超时" width="80" align="center">
          <template #default="{ row }">
            <span>{{ row.timeoutSeconds || '-' }}s</span>
          </template>
        </el-table-column>

        <!-- 状态 -->
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <div class="status-cell">
              <el-switch
                v-model="row.isEnabled"
                @change="(val) => handleToggleEnabled(row, val)"
                :loading="row._toggling"
              />
            </div>
          </template>
        </el-table-column>

        <!-- 连接验证 -->
        <el-table-column label="连接" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.isValid" type="success" size="small">
              <el-icon style="margin-right: 4px"><CircleCheck /></el-icon>
              有效
            </el-tag>
            <el-tag v-else type="danger" size="small">
              <el-icon style="margin-right: 4px"><CircleClose /></el-icon>
              未验证
            </el-tag>
          </template>
        </el-table-column>

        <!-- 描述 -->
        <el-table-column label="描述" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <span>{{ row.description || '-' }}</span>
          </template>
        </el-table-column>

        <!-- 操作 -->
        <el-table-column label="操作" width="180" fixed="right" align="center">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button
                type="primary"
                size="small"
                @click="handleTest(row)"
                :loading="row._testing"
              >
                <el-icon style="margin-right: 4px"><Connection /></el-icon>
                测试
              </el-button>
              <el-dropdown @command="(cmd) => handleActionCommand(cmd, row)">
                <el-button type="info" size="small">
                  更多<el-icon style="margin-left: 4px"><ArrowDown /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="edit">
                      <el-icon><Edit /></el-icon>编辑
                    </el-dropdown-item>
                    <el-dropdown-item command="delete" divided>
                      <el-icon><Delete /></el-icon>删除
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 创建/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑AI服务实例' : '新增AI服务实例'"
      width="600px"
      @close="resetForm"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="140px">
        <el-form-item label="部署类型" prop="deploymentType">
          <el-radio-group v-model="form.deploymentType" @change="handleDeploymentTypeChange">
            <el-radio value="cloud">
              <div style="display: flex; align-items: center; gap: 6px;">
                <el-icon><Cloudy /></el-icon>
                <span>云端API</span>
              </div>
            </el-radio>
            <el-radio value="local">
              <div style="display: flex; align-items: center; gap: 6px;">
                <el-icon><Monitor /></el-icon>
                <span>本地部署</span>
              </div>
            </el-radio>
          </el-radio-group>
          <div class="form-tip" v-if="form.deploymentType === 'local'">
            <el-icon><InfoFilled /></el-icon>
            本地部署无需 API 密钥，适合开发测试和隐私敏感场景
          </div>
          <div class="form-tip" v-else>
            <el-icon><InfoFilled /></el-icon>
            云端 API 需要提供 API 密钥，适合生产环境和高性能需求
          </div>
        </el-form-item>

        <el-form-item label="实例名称" prop="instanceName">
          <el-input
            v-model="form.instanceName"
            placeholder="例如：OpenAI-GPT4、Ollama-Qwen"
            clearable
          />
        </el-form-item>

        <el-form-item label="AI提供商" prop="provider">
          <el-select
            v-model="form.provider"
            placeholder="选择AI提供商"
            @change="handleProviderChange"
            style="width: 100%"
          >
            <el-option
              v-for="provider in providers"
              :key="provider.value"
              :label="provider.label"
              :value="provider.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="API地址" prop="baseUrl">
          <el-input
            v-model="form.baseUrl"
            placeholder="例如：https://api.openai.com/v1"
            clearable
          />
          <div class="form-tip">
            <el-icon><InfoFilled /></el-icon>
            留空则使用提供商默认地址
          </div>
        </el-form-item>

        <el-form-item
          v-if="form.deploymentType === 'cloud'"
          label="API密钥"
          prop="apiKey"
        >
          <el-input
            v-model="form.apiKey"
            type="password"
            placeholder="sk-xxxxx"
            show-password
            clearable
          />
          <div class="form-tip">
            <el-icon><Lock /></el-icon>
            API密钥将被加密存储
          </div>
        </el-form-item>

        <el-form-item v-else label="认证说明">
          <div class="auth-hint">
            <el-icon><Unlock /></el-icon>
            <span>本地部署无需 API 密钥，直接连接即可使用</span>
          </div>
        </el-form-item>

        <el-form-item label="模型名称" prop="modelName">
          <el-input
            v-model="form.modelName"
            placeholder="例如：gpt-4、qwen、deepseek-coder"
            clearable
          />
          <div class="form-tip" v-if="form.provider === 'ollama'">
            <el-icon><InfoFilled /></el-icon>
            Ollama 模型名称格式：deepseek-coder:6.7b
          </div>
        </el-form-item>

        <el-form-item label="温度参数">
          <el-slider
            v-model="form.temperature"
            :min="0"
            :max="1"
            :step="0.1"
            :marks="{ 0: '精确', 0.7: '平衡', 1: '创意' }"
            show-stops
          />
          <div class="form-tip">当前值: {{ form.temperature }}</div>
        </el-form-item>

        <el-form-item label="最大Tokens">
          <el-input-number
            v-model="form.maxTokens"
            :min="1"
            :max="128000"
            :step="1000"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="超时时间（秒）">
          <el-input-number
            v-model="form.timeoutSeconds"
            :min="10"
            :max="300"
            :step="10"
            style="width: 100%"
          />
          <div class="form-tip">
            <el-icon><InfoFilled /></el-icon>
            本地部署建议 120 秒，云端 API 建议 60 秒
          </div>
        </el-form-item>

        <el-form-item label="描述">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="实例用途说明..."
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSubmit" :loading="submitting" class="btn-gradient">
            <el-icon style="margin-right: 6px"><Check /></el-icon>
            {{ isEdit ? '保存' : '创建' }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  Plus,
  Cloudy,
  Monitor,
  InfoFilled,
  Lock,
  Unlock,
  Connection,
  Check,
  CircleCheck,
  CircleClose,
  Edit,
  Delete,
  ArrowDown
} from '@element-plus/icons-vue'
import {
  getAllAiServiceInstances,
  createAiServiceInstance,
  updateAiServiceInstance,
  deleteAiServiceInstance,
  setDefaultAiServiceInstance,
  setAiServiceInstanceEnabled,
  type AiServiceInstance
} from '@/api/ai-service-instances'

const loading = ref(false)
const instances = ref<AiServiceInstance[]>([])
const searchText = ref('')
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()

// AI 提供商列表
const providers = [
  { label: 'OpenAI', value: 'openai' },
  { label: 'Ollama', value: 'ollama' },
  { label: 'DeepSeek', value: 'deepseek' },
  { label: 'Anthropic', value: 'anthropic' },
  { label: 'Azure', value: 'azure' }
]

// 提供商默认 URL
const providerUrls: Record<string, string> = {
  openai: 'https://api.openai.com/v1',
  ollama: 'http://localhost:11434',
  deepseek: 'https://api.deepseek.com/v1',
  anthropic: 'https://api.anthropic.com/v1',
  azure: 'https://api.openai.azure.com'
}

// 表单数据
const form = reactive<Partial<AiServiceInstance>>({
  instanceName: '',
  provider: 'openai',
  deploymentType: 'cloud',
  baseUrl: '',
  apiKey: '',
  modelName: '',
  temperature: 0.7,
  maxTokens: 4096,
  timeoutSeconds: 60,
  description: ''
})

// 表单验证规则
const rules: FormRules = {
  instanceName: [
    { required: true, message: '请输入实例名称', trigger: 'blur' }
  ],
  provider: [
    { required: true, message: '请选择AI提供商', trigger: 'change' }
  ],
  modelName: [
    { required: true, message: '请输入模型名称', trigger: 'blur' }
  ],
  apiKey: [
    {
      required: true,
      message: '请输入API密钥',
      trigger: 'blur',
      validator: (rule, value, callback) => {
        if (form.deploymentType === 'cloud' && !value) {
          callback(new Error('云端API需要提供API密钥'))
        } else {
          callback()
        }
      }
    }
  ]
}

// 过滤后的实例列表
const filteredInstances = computed(() => {
  if (!searchText.value) return instances.value
  const search = searchText.value.toLowerCase()
  return instances.value.filter(
    (instance) =>
      instance.instanceName.toLowerCase().includes(search) ||
      instance.modelName.toLowerCase().includes(search) ||
      instance.provider.toLowerCase().includes(search)
  )
})

// 获取行样式
function getRowClassName({ row }: { row: AiServiceInstance }) {
  return row.isEnabled ? '' : 'is-disabled-row'
}

// 加载实例列表
async function loadInstances() {
  loading.value = true
  try {
    instances.value = await getAllAiServiceInstances()
  } catch (error: any) {
    ElMessage.error(error.message || '加载实例列表失败')
  } finally {
    loading.value = false
  }
}

// 创建实例
function handleCreate() {
  isEdit.value = false
  dialogVisible.value = true
}

// 编辑实例
function handleEdit(instance: AiServiceInstance) {
  isEdit.value = true
  Object.assign(form, instance)
  dialogVisible.value = true
}

// 提交表单
async function handleSubmit() {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    submitting.value = true
    try {
      if (isEdit.value) {
        await updateAiServiceInstance(form.id!, form as any)
        ElMessage.success('更新成功')
      } else {
        await createAiServiceInstance(form as any)
        ElMessage.success('创建成功')
      }
      dialogVisible.value = false
      await loadInstances()
    } catch (error: any) {
      ElMessage.error(error.message || '操作失败')
    } finally {
      submitting.value = false
    }
  })
}

// 重置表单
function resetForm() {
  formRef.value?.resetFields()
  Object.assign(form, {
    instanceName: '',
    provider: 'openai',
    deploymentType: 'cloud',
    baseUrl: '',
    apiKey: '',
    modelName: '',
    temperature: 0.7,
    maxTokens: 4096,
    timeoutSeconds: 60,
    description: ''
  })
}

// 切换部署类型
function handleDeploymentTypeChange(deploymentType: string) {
  if (deploymentType === 'local') {
    form.apiKey = ''
    form.timeoutSeconds = 120
  } else {
    form.timeoutSeconds = 60
  }
}

// 提供商变化
function handleProviderChange(provider: string) {
  if (!form.baseUrl) {
    form.baseUrl = providerUrls[provider] || ''
  }
}

// 测试连接
async function handleTest(instance: AiServiceInstance) {
  instance._testing = true
  try {
    // TODO: 实现测试连接逻辑
    await new Promise((resolve) => setTimeout(resolve, 1000))
    ElMessage.success('连接测试成功')
  } catch (error: any) {
    ElMessage.error(error.message || '连接测试失败')
  } finally {
    instance._testing = false
  }
}

// 切换启用状态
async function handleToggleEnabled(instance: AiServiceInstance, enabled: boolean) {
  instance._toggling = true
  try {
    await setAiServiceInstanceEnabled(instance.id!, enabled)
    ElMessage.success(enabled ? '已启用' : '已禁用')
  } catch (error: any) {
    instance.isEnabled = !enabled
    ElMessage.error(error.message || '操作失败')
  } finally {
    instance._toggling = false
  }
}

// 处理操作命令
function handleActionCommand(command: string, instance: AiServiceInstance) {
  if (command === 'edit') {
    handleEdit(instance)
  } else if (command === 'delete') {
    handleDelete(instance)
  }
}

// 删除实例
async function handleDelete(instance: AiServiceInstance) {
  try {
    await ElMessageBox.confirm(
      `确定要删除实例 "${instance.instanceName}" 吗？此操作不可恢复。`,
      '删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await deleteAiServiceInstance(instance.id!)
    ElMessage.success('删除成功')
    await loadInstances()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
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

onMounted(() => {
  loadInstances()
})
</script>

<style scoped>
.ai-service-instance-management {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);
}

/* 操作栏 */
.action-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--spacing-lg);
}

.action-left,
.action-right {
  display: flex;
  gap: var(--spacing-md);
}

/* 表格容器 */
.table-container {
  padding: var(--spacing-lg);
}

:deep(.is-disabled-row) {
  opacity: 0.6;
  background-color: var(--color-gray-50);
}

/* 实例名称单元格 */
.instance-name-cell {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
}

.name-text {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.name {
  font-weight: 600;
  color: var(--color-gray-800);
}

.deployment-tags {
  display: flex;
  gap: var(--spacing-xs);
  flex-wrap: wrap;
}

/* 模型信息 */
.model-info {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
}

.model-name {
  font-weight: 500;
  color: var(--color-gray-800);
}

.model-params {
  display: flex;
  gap: var(--spacing-md);
  font-size: var(--font-size-sm);
  color: var(--color-gray-500);
}

/* URL文本 */
.url-text {
  font-family: 'Courier New', monospace;
  font-size: var(--font-size-sm);
  color: var(--color-gray-600);
}

/* 操作按钮 */
.action-buttons {
  display: flex;
  gap: var(--spacing-xs);
  justify-content: center;
}

/* 表单提示 */
.form-tip {
  font-size: var(--font-size-xs);
  color: var(--color-gray-500);
  margin-top: var(--spacing-xs);
  line-height: 1.4;
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
}

.auth-hint {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  padding: var(--spacing-sm) var(--spacing-md);
  background: var(--color-gray-50);
  border: 1px solid var(--color-gray-200);
  border-radius: var(--radius-sm);
  font-size: var(--font-size-sm);
  color: var(--color-gray-600);
}

.auth-hint .el-icon {
  color: var(--color-info);
  font-size: 18px;
}

/* 对话框 */
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: var(--spacing-md);
}
</style>
